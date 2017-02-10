package xyz.monkeytong.hongbao.services

import android.app.Notification
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import xyz.monkeytong.hongbao.utils.HongbaoSignature

/**
 * @author Chris
 * @Desc
 * @Date 2017/2/10 010
 */
interface HongBaoProcessor {
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
    val LUCK_MONEY_RECEIVE_ACTIVITY: String
    val LUCK_MONEY_DETAIL_ACTIVITY: String
    val LUCK_MONEY_GENERAL_ACTIVITY: String
    val LUCK_MONEY_CHATTING_ACTIVITY: String

    fun updateCurrentActivityName(packageManager: PackageManager, event: AccessibilityEvent): String?

    fun getTheLastNode(nodeInfo: AccessibilityNodeInfo, vararg texts: String): AccessibilityNodeInfo?

    fun findOpenButton(node: AccessibilityNodeInfo?): AccessibilityNodeInfo?
}

abstract class HongBaoProcessorImpl(val packageManager: PackageManager,
                                    val sharedPreference: SharedPreferences) : HongBaoProcessor {
    protected abstract var currentActivityName: String
    protected var rootNodeInfo: AccessibilityNodeInfo? = null
    protected var mReceiveNode: AccessibilityNodeInfo? = null
    protected var mUnpackNode: AccessibilityNodeInfo? = null
    protected var mLuckyMoneyPicked: Boolean = false
    protected var mLuckyMoneyReceived: Boolean = false
    protected var mUnpackCount = 0
    protected var mMutex = false
    protected var mListMutex = false
    protected var mChatMutex = false
    protected val signature = HongbaoSignature()

    fun processAccessibilityEvent(event: AccessibilityEvent) {

        updateCurrentActivityName(event)?.run {
            currentActivityName = this
        }
        if (mMutex.not()) {
            if (sharedPreference.getBoolean(HongBaoProcessor.WATCH_NOTIFICATION, false) &&
                    watchNotification(event)) return
            if (sharedPreference.getBoolean(HongBaoProcessor.WATCH_LIST, false) && watchList(event))
                return
            mListMutex = true
        }

        if (mChatMutex.not()) {
            mChatMutex = true
            if (sharedPreference.getBoolean(HongBaoProcessor.WATCH_CHAT, false))
                watchChat(event)
            mChatMutex = false
        }
    }

    abstract fun updateCurrentActivityName(event: AccessibilityEvent): String?

    fun watchNotification(event: AccessibilityEvent): Boolean {
        if (event.eventType != AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            return false
        } else {
            val tip = event.text
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
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED || eventSource == null)
            return false
        val nodes = eventSource.findAccessibilityNodeInfosByText(NOTIFICATION_TIP)
        if (nodes?.isNotEmpty() ?: false && currentActivityName.contains(LUCK_MONEY_GENERAL_ACTIVITY)) {
            val nodeToClick = nodes[0] ?: return false
            val contentDescription = nodeToClick.contentDescription
            if (signature.contentDescription != contentDescription) {
                signature.contentDescription = contentDescription.toString()
                return true
            }
        }
        return true
    }

    fun watchChat(rootNodeInfo: AccessibilityNodeInfo?, event: AccessibilityEvent) {
        if (rootNodeInfo == null) return
        mReceiveNode = null
        mUnpackNode = null

    }

    fun checkNodeInfo(rootNodeInfo: AccessibilityNodeInfo, type: Int) {
        if (signature.commentString != null) {
            sendComment()
            signature.commentString = null
        }

        if (sharedPreference.getBoolean(HongBaoProcessor.WATCH_SELF, false)) {

        }
    }

    fun getTheLastNode() {

    }
    fun sendComment() {

    }
}