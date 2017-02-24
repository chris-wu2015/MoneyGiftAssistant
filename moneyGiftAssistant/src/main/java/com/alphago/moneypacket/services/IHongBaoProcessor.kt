package com.alphago.moneypacket.services

import android.accessibilityservice.AccessibilityService
import android.app.Notification
import android.content.ComponentName
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Handler
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.EditText
import com.alphago.moneypacket.utils.Signature
import org.jetbrains.anko.bundleOf

/**
 * @author Chris
 * @Desc
 * @Date 2017/2/10 010
 */
interface IHongBaoProcessor {
    companion object {
        val WATCH_NOTIFICATION = "pref_watch_notification"
        val WATCH_LIST = "pref_watch_list"
        val WATCH_CHAT = "pref_watch_chat"
        val OPEN_DELAY = "pref_open_delay"
        val WATCH_SELF = "pref_watch_self"
        val WATCH_EXCLUDE_WORDS = "pref_watch_exclude_words"
        val WATCH_ON_LOCK = "pref_watch_on_lock"
        val COMMENT_SWITCH = "pref_comment_switch"
        val COMMENT_WORDS = "pref_comment_words"
        val COMMENT_AT = "pref_comment_at"
    }

    val DETAILS: String
    val BETTER_LUCK: String
    val EXPIRES: String
    val VIEW_SELF: String
    val VIEW_OTHERS: String
    val NOTIFICATION_TIP: String
    val RECEIVER_UI: String
    val DETAIL_UI: String
    val GENERAL_UI: String
    val CHATTING_UI: String

    fun updateCurrentActivityName(packageManager: PackageManager, event: AccessibilityEvent):
            String?

    fun lookingForMoneyPacket(nodeInfo: AccessibilityNodeInfo, vararg texts: String?):
            List<AccessibilityNodeInfo>

    fun findOpenButton(node: AccessibilityNodeInfo?): AccessibilityNodeInfo?

    fun watchChat(service: AccessibilityService, rootNodeInfo: AccessibilityNodeInfo?,
                  event: AccessibilityEvent)
}

abstract class HongBaoProcessor(val packageManager: PackageManager,
                                val sharedPreference: SharedPreferences) : IHongBaoProcessor {
    protected var currentActivityName: String = ""
    protected var mMutex = false
    protected var mListMutex = false
    protected var mChatMutex = false
    protected val splitRegex by lazyOf(Regex(" +"))
    protected val handler by lazyOf(Handler())
    protected abstract val signature: Signature

    fun processAccessibilityEvent(service: AccessibilityService,
                                  rootNodeInfo: AccessibilityNodeInfo?, event: AccessibilityEvent) {
        updateCurrentActivityName(packageManager, event)?.run {
            currentActivityName = this
        }
        if (mMutex.not()) {
            if (sharedPreference.getBoolean(IHongBaoProcessor.WATCH_NOTIFICATION, false) &&
                    watchNotification(event)) return
            if (sharedPreference.getBoolean(IHongBaoProcessor.WATCH_LIST, false) && watchList(event))
                return
            mListMutex = false
        }

        if (mChatMutex.not()) {
            mChatMutex = true
            if (sharedPreference.getBoolean(IHongBaoProcessor.WATCH_CHAT, false))
                watchChat(service, rootNodeInfo, event)
            mChatMutex = false
        }
    }

    override fun updateCurrentActivityName(packageManager: PackageManager,
                                           event: AccessibilityEvent): String? {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return null
        try {
            val componentName = ComponentName(event.packageName.toString(), event.className.toString())
            packageManager.getActivityInfo(componentName, 0)
            return componentName.flattenToShortString()
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun watchNotification(event: AccessibilityEvent): Boolean {
        if (event.eventType != AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            return false
        } else {
            val tip = event.text.toString()
            if (tip.contains(NOTIFICATION_TIP).not()) return true
            (event.parcelableData as? Notification)?.run {
                try {
                    signature.cleanSignature()
                    contentIntent.send()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return true
        }
    }

    fun watchList(event: AccessibilityEvent): Boolean {
        if (mListMutex) return false
        mListMutex = true
        val eventSource = event.source
        println("text=${eventSource.text},class=${eventSource.className}")
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED || eventSource == null)
            return false
        val nodes = eventSource.findAccessibilityNodeInfosByText(NOTIFICATION_TIP)
        if (nodes?.isNotEmpty() ?: false && currentActivityName.contains(GENERAL_UI)) {
            val nodeToClick = nodes[0] ?: return false
            val contentDescription = nodeToClick.contentDescription
            if (signature.contentDescription != contentDescription) {
                signature.contentDescription = contentDescription?.toString() ?: ""
                return true
            }
        }
        return true
    }

    abstract fun processReceivedNode(node: AccessibilityNodeInfo?, delay: Long,
                                     service: AccessibilityService? = null)

    open fun openPacket(node: AccessibilityNodeInfo?): Boolean {
        return node?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: false
    }

    fun hasOneOfThoseNodes(rootNodeInfo: AccessibilityNodeInfo?, vararg texts: String?): Boolean {
        if (rootNodeInfo == null) return false
        var nodes: List<AccessibilityNodeInfo>?
        for (text in texts) {
            if (text == null) continue
            nodes = rootNodeInfo.findAccessibilityNodeInfosByText(text)
            if (nodes?.isNotEmpty() ?: false) return true
        }
        return false
    }

    open fun judgeOpenButton(node: AccessibilityNodeInfo?): Boolean {
        return node != null && Button::class.java.name == node.className
    }


    open fun sendComment(rootNodeInfo: AccessibilityNodeInfo?) {
        val inputNode = getInputNode(rootNodeInfo)
        if (EditText::class.java.name == inputNode?.className) {
            try {
                val arguments = bundleOf(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE to
                        (signature.commentString ?: ""))
                inputNode?.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    open fun getInputNode(rootNodeInfo: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        val outNode = rootNodeInfo?.getChild(0)?.getChild(0)
        return outNode?.getChild(outNode.childCount - 1)?.getChild(0)?.getChild(1)
    }

    open fun generateCommentString(): String? {
        if (!signature.others) return null

        val needComment = sharedPreference.getBoolean(IHongBaoProcessor.COMMENT_SWITCH, false)
        if (!needComment) return null

        val wordsArray = sharedPreference.getString(IHongBaoProcessor.COMMENT_WORDS, "")
                .split(splitRegex).dropLastWhile(String::isEmpty).toTypedArray()
        if (wordsArray.isEmpty()) return null

        val atSender = sharedPreference.getBoolean(IHongBaoProcessor.COMMENT_AT, false)
        if (atSender) {
            return "@${signature.sender} ${wordsArray[(Math.random() * wordsArray.size).toInt()]}"
        } else {
            return wordsArray[(Math.random() * wordsArray.size).toInt()]
        }
    }
}