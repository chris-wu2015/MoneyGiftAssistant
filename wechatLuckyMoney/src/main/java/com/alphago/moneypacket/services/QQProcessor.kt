package com.alphago.moneypacket.services

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.preference.PreferenceManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import com.alphago.moneypacket.R
import com.alphago.moneypacket.utils.QQHongBaoSignature
import com.alphago.moneypacket.utils.Signature
import java.util.*

/**
 * @author Chris
 * @Desc
 * @Date 2017/2/14 014
 */
class QQProcessor(context: Context,
                  override val BETTER_LUCK: String = context.getString(R.string.qq_better_luck),
                  override val DETAILS: String = context.getString(R.string.wechat_details),
                  override val EXPIRES: String = context.getString(R.string.qq_expires),
                  override val CHATTING_UI: String = context.getString(R.string.qq_chatting_ui),
                  override val DETAIL_UI: String = context.getString(R.string.qq_detail_ui),
                  override val GENERAL_UI: String = context.getString(R.string.qq_general_ui),
                  override val RECEIVER_UI: String = context.getString(R.string.qq_receiver_ui),
                  override val NOTIFICATION_TIP: String = context.getString(R.string.qq_notification),
                  override val VIEW_OTHERS: String = context.getString(R.string.qq_view_other),
                  override val VIEW_SELF: String = context.getString(R.string.qq_view_self),
                  val VIEW_PASSWORD: String = context.getString(R.string.qq_password_packet),
                  val VIEW_INPUT_PASSWORD: String = context.getString(R.string.qq_input_password),
                  override val signature: Signature = QQHongBaoSignature()) :
        HongBaoProcessor(context.packageManager,
                PreferenceManager.getDefaultSharedPreferences(context)) {
    val packetRegex = Regex(".+?红包$")
    override fun watchChat(service: AccessibilityService, rootNodeInfo: AccessibilityNodeInfo?,
                           event: AccessibilityEvent) {
        if (rootNodeInfo == null) return
        if (signature.commentString != null) {
            sendComment(rootNodeInfo)
            signature.commentString = null
        }
        val watchSelf = sharedPreference.getBoolean(IHongBaoProcessor.WATCH_SELF, false)
        val nodes = if (watchSelf) {
            lookingForMoneyPacket(rootNodeInfo, VIEW_OTHERS, VIEW_PASSWORD, VIEW_SELF)
        } else {
            lookingForMoneyPacket(rootNodeInfo, VIEW_OTHERS, VIEW_PASSWORD)
        }
        openPackets(nodes, service)//点击（打开）红包
        closeReceiveUI(event, service)//关闭红包结果页面
        openPasswordPacket(event)//点击输入口令
    }

    private fun openPasswordPacket(event: AccessibilityEvent) {
        if (AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED == event.eventType) {
            val eventSource = event.source
            val findAccessibilityNodeInfosByText = eventSource?.findAccessibilityNodeInfosByText(VIEW_INPUT_PASSWORD)
            if (findAccessibilityNodeInfosByText?.isNotEmpty()
                    ?: false) {
                openPacket(eventSource.parent)
            }
            if (Button::class.java.name == eventSource.className) {
                openPacket(eventSource)
            }
        }
    }

    private fun closeReceiveUI(event: AccessibilityEvent, service: AccessibilityService) {
        if (AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED == event.eventType &&
                currentActivityName.contains(RECEIVER_UI)) {//关闭红包详情页和抢红包结果窗口
            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
        }
    }

    fun openPackets(nodes: List<AccessibilityNodeInfo>, service: AccessibilityService) {
        val delay = sharedPreference.getInt(IHongBaoProcessor.OPEN_DELAY, 0) * 1000L
        nodes.forEach {
            processReceivedNode(it, delay, service)
        }
    }

    override fun lookingForMoneyPacket(nodeInfo: AccessibilityNodeInfo, vararg texts: String?):
            ArrayList<AccessibilityNodeInfo> {
        val list = arrayListOf<AccessibilityNodeInfo>()
        var nodes: List<AccessibilityNodeInfo?>?
        for (text in texts) {
            if (text == null) continue
            nodes = nodeInfo.findAccessibilityNodeInfosByText(text)
            if (nodes?.isNotEmpty() ?: false) {
                list.addAll(nodes.filter { it != null && packetRegex.matches(it.text) })
            }
        }
        return list
    }

    @Synchronized
    override fun processReceivedNode(node: AccessibilityNodeInfo?, delay: Long,
                                     service: AccessibilityService?) {
        mMutex = true
        handler.postDelayed({
            try {
                openPacket(node?.parent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            mMutex = false
        }, delay)
    }

    override fun findOpenButton(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        return node
    }
}