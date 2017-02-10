package xyz.monkeytong.hongbao.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.app.Notification
import android.app.PendingIntent
import android.content.ComponentName
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import xyz.monkeytong.hongbao.utils.HongbaoSignature
import xyz.monkeytong.hongbao.utils.PowerUtil

class HongbaoService : AccessibilityService(), SharedPreferences.OnSharedPreferenceChangeListener {
    companion object {
        private val WECHAT_DETAILS_EN = "Details"
        private val WECHAT_DETAILS_CH = "红包详情"
        private val WECHAT_BETTER_LUCK_EN = "Better luck next time!"
        private val WECHAT_BETTER_LUCK_CH = "手慢了"
        private val WECHAT_EXPIRES_CH = "已超过24小时"
        private val WECHAT_VIEW_SELF_CH = "查看红包"
        private val WECHAT_VIEW_OTHERS_CH = "领取红包"
        private val WECHAT_NOTIFICATION_TIP = "[微信红包]"
        private val WECHAT_LUCKMONEY_RECEIVE_ACTIVITY = "LuckyMoneyReceiveUI"
        private val WECHAT_LUCKMONEY_DETAIL_ACTIVITY = "LuckyMoneyDetailUI"
        private val WECHAT_LUCKMONEY_GENERAL_ACTIVITY = "LauncherUI"
        private val WECHAT_LUCKMONEY_CHATTING_ACTIVITY = "ChattingUI"
    }

    private var currentActivityName = WECHAT_LUCKMONEY_GENERAL_ACTIVITY

    private var rootNodeInfo: AccessibilityNodeInfo? = null
    private var mReceiveNode: AccessibilityNodeInfo? = null
    private var mUnpackNode: AccessibilityNodeInfo? = null
    private var mLuckyMoneyPicked: Boolean = false
    private var mLuckyMoneyReceived: Boolean = false
    private var mUnpackCount = 0
    private var mMutex = false
    private var mListMutex = false
    private var mChatMutex = false
    private val signature = HongbaoSignature()

    private var powerUtil: PowerUtil? = null
    private var sharedPreferences: SharedPreferences? = null

    public override fun onServiceConnected() {
        super.onServiceConnected()
        this.watchFlagsFromPreference()
    }

    /**
     * AccessibilityEvent

     * @param event 事件
     */
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (sharedPreferences == null) return

        setCurrentActivityName(event)
        /* 检测通知消息 */
        if (!mMutex) {
            if (sharedPreferences!!.getBoolean("pref_watch_notification", false) && watchNotifications(event))
                return
            if (sharedPreferences!!.getBoolean("pref_watch_list", false) && watchList(event)) return
            mListMutex = false
        }

        if (!mChatMutex) {
            mChatMutex = true
            if (sharedPreferences!!.getBoolean("pref_watch_chat", false)) watchChat(event)
            mChatMutex = false
        }
    }

    private fun watchChat(event: AccessibilityEvent) {
        this.rootNodeInfo = rootInActiveWindow

        if (rootNodeInfo == null) return

        mReceiveNode = null
        mUnpackNode = null

        checkNodeInfo(event.eventType)

        /* 如果已经接收到红包并且还没有戳开 */
        if (mLuckyMoneyReceived && !mLuckyMoneyPicked && mReceiveNode != null) {
            mMutex = true

            mReceiveNode!!.parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            mLuckyMoneyReceived = false
            mLuckyMoneyPicked = true
        }
        /* 如果戳开但还未领取 */
        if (mUnpackCount == 1 && mUnpackNode != null) {
            val delayFlag = sharedPreferences!!.getInt("pref_open_delay", 0) * 1000
            android.os.Handler().postDelayed(
                    {
                        try {
                            openPacket()
                        } catch (e: Exception) {
                            mMutex = false
                            mLuckyMoneyPicked = false
                            mUnpackCount = 0
                        }
                    },
                    delayFlag.toLong())
        }
    }

    private fun openPacket() {
        val metrics = resources.displayMetrics
        val dpi = metrics.density
        if (android.os.Build.VERSION.SDK_INT <= 23) {
            mUnpackNode!!.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } else {
            if (android.os.Build.VERSION.SDK_INT > 23) {

                val path = Path()
                if (640f == dpi) {
                    path.moveTo(720f, 1575f)
                } else {
                    path.moveTo(540f, 1060f)
                }
                val builder = GestureDescription.Builder()
                val gestureDescription = builder.addStroke(GestureDescription.StrokeDescription(path, 450, 50)).build()
                dispatchGesture(gestureDescription, object : AccessibilityService.GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription) {
                        Log.d("test", "onCompleted")
                        mMutex = false
                        super.onCompleted(gestureDescription)
                    }

                    override fun onCancelled(gestureDescription: GestureDescription) {
                        Log.d("test", "onCancelled")
                        mMutex = false
                        super.onCancelled(gestureDescription)
                    }
                }, null)

            }
        }
    }

    private fun setCurrentActivityName(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }

        try {
            val componentName = ComponentName(
                    event.packageName.toString(),
                    event.className.toString()
            )

            packageManager.getActivityInfo(componentName, 0)
            currentActivityName = componentName.flattenToShortString()
        } catch (e: PackageManager.NameNotFoundException) {
            currentActivityName = WECHAT_LUCKMONEY_GENERAL_ACTIVITY
        }

    }

    private fun watchList(event: AccessibilityEvent): Boolean {
        if (mListMutex) return false
        mListMutex = true
        val eventSource = event.source
        // Not a message
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED || eventSource == null)
            return false

        val nodes = eventSource.findAccessibilityNodeInfosByText(WECHAT_NOTIFICATION_TIP)
        //增加条件判断currentActivityName.contains(WECHAT_LUCKMONEY_GENERAL_ACTIVITY)
        //避免当订阅号中出现标题为“[微信红包]拜年红包”（其实并非红包）的信息时误判
        if (!nodes.isEmpty() && currentActivityName.contains(WECHAT_LUCKMONEY_GENERAL_ACTIVITY)) {
            val nodeToClick = nodes[0] ?: return false
            val contentDescription = nodeToClick.contentDescription
            if (contentDescription != null && signature.contentDescription != contentDescription) {
                nodeToClick.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                signature.contentDescription = contentDescription.toString()
                return true
            }
        }
        return false
    }

    private fun watchNotifications(event: AccessibilityEvent): Boolean {
        // Not a notification
        if (event.eventType != AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED)
            return false

        // Not a hongbao
        val tip = event.text.toString()
        if (!tip.contains(WECHAT_NOTIFICATION_TIP)) return true

        val parcelable = event.parcelableData
        if (parcelable is Notification) {
            try {
                /* 清除signature,避免进入会话后误判 */
                signature.cleanSignature()

                parcelable.contentIntent.send()
            } catch (e: PendingIntent.CanceledException) {
                e.printStackTrace()
            }

        }
        return true
    }

    override fun onInterrupt() {

    }

    private fun findOpenButton(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null)
            return null

        //非layout元素
        if (node.childCount == 0) {
            if ("android.widget.Button" == node.className)
                return node
            else
                return null
        }

        //layout元素，遍历找button
        var button: AccessibilityNodeInfo?
        for (i in 0..node.childCount - 1) {
            button = findOpenButton(node.getChild(i))
            if (button != null)
                return button
        }
        return null
    }

    private fun checkNodeInfo(eventType: Int) {
        println("checkNodeInfo")
        if (this.rootNodeInfo == null) return

        if (signature.commentString != null) {
            sendComment()
            signature.commentString = null
        }

        /* 聊天会话窗口，遍历节点匹配“领取红包”和"查看红包" */
        val node1 = if (sharedPreferences!!.getBoolean("pref_watch_self", false))
            this.getTheLastNode(WECHAT_VIEW_OTHERS_CH, WECHAT_VIEW_SELF_CH)
        else
            this.getTheLastNode(WECHAT_VIEW_OTHERS_CH)
        if (node1 != null && (currentActivityName.contains(WECHAT_LUCKMONEY_CHATTING_ACTIVITY) || currentActivityName.contains(WECHAT_LUCKMONEY_GENERAL_ACTIVITY))) {
            val excludeWords = sharedPreferences!!.getString("pref_watch_exclude_words", "")
            if (this.signature.generateSignature(node1, excludeWords)) {
                mLuckyMoneyReceived = true
                mReceiveNode = node1
                Log.d("sig", this.signature.toString())
            }
            return
        }

        /* 戳开红包，红包还没抢完，遍历节点匹配“拆红包” */
        val node2 = findOpenButton(this.rootNodeInfo)
        if (node2 != null && "android.widget.Button" == node2.className && currentActivityName.contains(WECHAT_LUCKMONEY_RECEIVE_ACTIVITY)) {
            mUnpackNode = node2
            mUnpackCount += 1
            return
        }

        /* 戳开红包，红包已被抢完，遍历节点匹配“红包详情”和“手慢了” */
        val hasNodes = this.hasOneOfThoseNodes(
                WECHAT_BETTER_LUCK_CH, WECHAT_DETAILS_CH,
                WECHAT_BETTER_LUCK_EN, WECHAT_DETAILS_EN, WECHAT_EXPIRES_CH)
        if (mMutex && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && hasNodes
                && (currentActivityName.contains(WECHAT_LUCKMONEY_DETAIL_ACTIVITY) || currentActivityName.contains(WECHAT_LUCKMONEY_RECEIVE_ACTIVITY))) {
            mMutex = false
            mLuckyMoneyPicked = false
            mUnpackCount = 0
            performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
            signature.commentString = generateCommentString()
        }
    }

    private fun sendComment() {
        try {
            val outNode = rootInActiveWindow.getChild(0).getChild(0)
            val nodeToInput = outNode.getChild(outNode.childCount - 1).getChild(0).getChild(1)

            if ("android.widget.EditText" == nodeToInput.className) {
                val arguments = Bundle()
                arguments.putCharSequence(AccessibilityNodeInfo
                        .ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, signature.commentString)
                nodeToInput.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            }
        } catch (e: Exception) {
            // Not supported
        }

    }


    private fun hasOneOfThoseNodes(vararg texts: String): Boolean {
        var nodes: List<AccessibilityNodeInfo>?
        for (text in texts) {
            if (text == null) continue

            nodes = this.rootNodeInfo!!.findAccessibilityNodeInfosByText(text)

            if (nodes != null && !nodes.isEmpty()) return true
        }
        return false
    }

    private fun getTheLastNode(vararg texts: String): AccessibilityNodeInfo? {
        var bottom = 0
        var lastNode: AccessibilityNodeInfo? = null
        var tempNode: AccessibilityNodeInfo?
        var nodes: List<AccessibilityNodeInfo>?

        for (text in texts) {
            if (text == null) continue

            nodes = this.rootNodeInfo!!.findAccessibilityNodeInfosByText(text)

            if (nodes != null && !nodes.isEmpty()) {
                tempNode = nodes[nodes.size - 1]
                if (tempNode == null) return null
                val bounds = Rect()
                tempNode.getBoundsInScreen(bounds)
                if (bounds.bottom > bottom) {
                    bottom = bounds.bottom
                    lastNode = tempNode
                    signature.others = text == WECHAT_VIEW_OTHERS_CH
                }
            }
        }
        return lastNode
    }

    private fun watchFlagsFromPreference() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        sharedPreferences!!.registerOnSharedPreferenceChangeListener(this)

        this.powerUtil = PowerUtil(this)
        val watchOnLockFlag = sharedPreferences!!.getBoolean("pref_watch_on_lock", false)
        this.powerUtil!!.handleWakeLock(watchOnLockFlag)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (key == "pref_watch_on_lock") {
            val changedValue = sharedPreferences.getBoolean(key, false)
            this.powerUtil!!.handleWakeLock(changedValue)
        }
    }

    override fun onDestroy() {
        this.powerUtil!!.handleWakeLock(false)
        super.onDestroy()
    }

    private fun generateCommentString(): String? {
        if (!signature.others) return null

        val needComment = sharedPreferences!!.getBoolean("pref_comment_switch", false)
        if (!needComment) return null

        val wordsArray = sharedPreferences!!.getString("pref_comment_words", "")!!.split(" +".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (wordsArray.size == 0) return null

        val atSender = sharedPreferences!!.getBoolean("pref_comment_at", false)
        if (atSender) {
            return "@" + signature.sender + " " + wordsArray[(Math.random() * wordsArray.size).toInt()]
        } else {
            return wordsArray[(Math.random() * wordsArray.size).toInt()]
        }
    }
}
