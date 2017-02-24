package com.alphago.moneypacket.services

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Rect
import android.preference.PreferenceManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import com.alphago.moneypacket.R
import com.alphago.moneypacket.utils.Signature
import com.alphago.moneypacket.utils.WeChatHongBaoSignature

/**
 * @author Chris
 * @Desc
 * @Date 2017/2/13 013
 */
open class WeChatProcessor(context: Context,
                           override val BETTER_LUCK: String = context.getString(R.string.wechat_better_luck),
                           override val DETAILS: String = context.getString(R.string.wechat_details),
                           override val EXPIRES: String = context.getString(R.string.wechat_expires),
                           override val CHATTING_UI: String = context.getString(R.string.wechat_chatting_ui),
                           override val DETAIL_UI: String = context.getString(R.string.wechat_detail_ui),
                           override val GENERAL_UI: String = context.getString(R.string.wechat_general_ui),
                           override val RECEIVER_UI: String = context.getString(R.string.wechat_receiver_ui),
                           override val NOTIFICATION_TIP: String = context.getString(R.string.wechat_notification),
                           override val VIEW_OTHERS: String = context.getString(R.string.wechat_view_other),
                           override val VIEW_SELF: String = context.getString(R.string.wechat_view_self),
                           override val signature: Signature = WeChatHongBaoSignature()) :
        HongBaoProcessor(context.packageManager,
                PreferenceManager.getDefaultSharedPreferences(context)) {
    protected var mReceiveNode: AccessibilityNodeInfo? = null
    protected var mUnpackNode: AccessibilityNodeInfo? = null
    protected var mLuckyMoneyPicked: Boolean = false
    protected var mLuckyMoneyReceived: Boolean = false
    protected var mUnpackCount = 0
    protected val bounds = Rect()

    override fun watchChat(service: AccessibilityService, rootNodeInfo: AccessibilityNodeInfo?,
                           event: AccessibilityEvent) {
        if (rootNodeInfo == null) return
        mReceiveNode = null
        mUnpackNode = null

        checkNodeInfo(service, rootNodeInfo, event.eventType)

        val receiveNode = mReceiveNode
        val delay = sharedPreference.getInt(IHongBaoProcessor.OPEN_DELAY, 0) * 1000L
        processReceivedNode(receiveNode, delay, service)
    }

    override fun lookingForMoneyPacket(nodeInfo: AccessibilityNodeInfo, vararg texts: String?):
            List<AccessibilityNodeInfo> {
        val lastNode = getTheLastNode(nodeInfo, *texts)
        return if (lastNode != null) arrayListOf(lastNode) else arrayListOf()
    }

    fun getTheLastNode(nodeInfo: AccessibilityNodeInfo, vararg texts: String?):
            AccessibilityNodeInfo? {
        var bottom = 0
        var lastNode: AccessibilityNodeInfo? = null
        var tempNode: AccessibilityNodeInfo?
        var nodes: List<AccessibilityNodeInfo?>?

        for (text in texts) {
            if (text == null) continue

            nodes = nodeInfo.findAccessibilityNodeInfosByText(text)

            if (nodes != null && !nodes.isEmpty()) {
                tempNode = nodes[nodes.size - 1]
                if (tempNode == null) return null
                if ((tempNode.text?.contains(text) ?: false).not()) continue
                bounds.setEmpty()
                tempNode.getBoundsInScreen(bounds)
                if (bounds.bottom > bottom) {
                    lastNode = tempNode
                    signature.others = text == VIEW_OTHERS
                    return lastNode
                }
                bottom = bounds.bottom
            }
        }
        return lastNode
    }

    fun checkNodeInfo(service: AccessibilityService, rootNodeInfo: AccessibilityNodeInfo, type: Int) {
        if (signature.commentString != null) {
            sendComment(rootNodeInfo)
            signature.commentString = null
        }

        val nodeList = if (sharedPreference.getBoolean(IHongBaoProcessor.WATCH_SELF, false)) {
            lookingForMoneyPacket(rootNodeInfo, VIEW_OTHERS, VIEW_SELF)
        } else {
            lookingForMoneyPacket(rootNodeInfo, VIEW_OTHERS)
        }
        val node1 = if (nodeList.isNotEmpty()) nodeList[0] else null
        if (node1 != null && (currentActivityName.contains(CHATTING_UI) ||
                currentActivityName.contains(GENERAL_UI))) {
            val excludeWords = sharedPreference.getString(IHongBaoProcessor.WATCH_EXCLUDE_WORDS, "")
            if (signature.generateSignature(node1, excludeWords)) {
                mLuckyMoneyReceived = true
                mReceiveNode = node1
                mLuckyMoneyPicked = false
            }
            return
        }
        if (mLuckyMoneyReceived) {
            val node2 = findOpenButton(rootNodeInfo)
            if (judgeOpenButton(node2)) {
                mUnpackNode = node2
                mUnpackCount += 1
                return
            }
        }

        if (mMutex && type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
                hasOneOfThoseNodes(rootNodeInfo, BETTER_LUCK, DETAILS, EXPIRES) &&
                (currentActivityName.contains(DETAIL_UI) ||
                        currentActivityName.contains(RECEIVER_UI))) {
            mMutex = false
            mLuckyMoneyPicked = false
            mUnpackCount = 0
            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
            signature.commentString = generateCommentString()
        }
    }

    override fun processReceivedNode(node: AccessibilityNodeInfo?, delay: Long,
                                     service: AccessibilityService?) {
        if (mLuckyMoneyReceived && mLuckyMoneyPicked.not() && node != null) {
            mMutex = true
            println("打开红包:${node.parent.getChild(0).text}")
            node.parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)

        }

        if (mUnpackCount >= 1 && mUnpackNode != null) {
            handler.postDelayed({
                try {
                    openPacket(mUnpackNode)
                    mLuckyMoneyReceived = false
                    mLuckyMoneyPicked = true
                } catch (e: Exception) {
                    e.printStackTrace()
                    mMutex = false
                    mLuckyMoneyPicked = false
                    mLuckyMoneyReceived = false
                    mLuckyMoneyPicked = true
                    mUnpackCount = 0
                }
            }, delay)
        }
    }

    override fun findOpenButton(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.childCount == 0) {
            if (Button::class.java.name == node.className) {
                return node
            } else {
                return null
            }
        }

        var button: AccessibilityNodeInfo? = null
        for (i in 0..node.childCount - 1) {
            button = findOpenButton(node.getChild(i))
            if (button != null) return button
        }
        return button
    }
}