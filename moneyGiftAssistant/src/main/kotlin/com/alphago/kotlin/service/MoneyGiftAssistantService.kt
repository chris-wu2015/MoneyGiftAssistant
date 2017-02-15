package com.alphago.kotlin.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Notification
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.RelativeLayout
import com.alphago.kotlin.extensions.log
import com.alphago.kotlin.extensions.support

/**
 * @author Chris
 * @Desc
 * @Date 2017/2/6 006
 */
class MoneyGiftAssistantService : AccessibilityService() {
    companion object {
        val LAUNCHER_UI = "com.tencent.mm.ui.LauncherUI"//微信聊天界面
        val RECEIVE_UI = "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI"//抢红包界面
        val DETAIL_UI = "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI"//红包详情(即抢过后的界面)
        val MONEY_GIFT_NOTIFICATION = "[微信红包]"
        val MONEY_GIFT_OPEN = "领取红包"
        val packages = arrayOf("com.tencent.mobileqq", "com.tencent.mm")
        val regex = Regex(".*?@([\\w]{8});")
    }

    private val parents: MutableList<AccessibilityNodeInfo> = mutableListOf()
//    private var openPacketHash: HashSet<String> = hashSetOf()
    /**服务启动回调*/
    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo()
        info.run {
            eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK
            packageNames = packages
            support(18, {
                flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            })
            notificationTimeout = 100
        }
        serviceInfo = info
    }

    /**窗口变化回调*/
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> {
                val text = event.text
                text.forEach {
                    if (it.contains(MONEY_GIFT_NOTIFICATION)) {
                        //模拟打开通知栏
                        val parcelableData = event.parcelableData
                        (parcelableData as? Notification)?.run {
                            try {
                                this.contentIntent.send()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                when (event.className) {
                    LAUNCHER_UI -> {
                        getLastPacket(event)
                    }
                    RECEIVE_UI -> {
                        openPacket(event)
                    }
                    DETAIL_UI -> {
                        performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
                    }
                }
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
//                getLastPacket(event)
            }
        }
    }

    private fun getLastPacket(event: AccessibilityEvent) {
        val rootNode = rootInActiveWindow ?: event.source
        getMoneyGiftPacket(rootNode)
        for (k in parents) {
            performOpenPacket(k)
        }
        parents.clear()

    }

    private fun performOpenPacket(k: AccessibilityNodeInfo) {
        regex.find(k.toString())?.groups?.get(1)?.value?.run {
            "检测红包是否打开:hash=$this,text=${k.support(18, {
                findAccessibilityNodeInfosByViewId("com.tencent.mm:id/a5t")
                        .map {
                            it.text
                        }
            })}".log()
//            if (openPacketHash.contains(this).not()) {
            "未领取红包：$this".log()
            if (performClick(k)) {
//                    openPacketHash.add(this)
                "打开红包：$this".log()
            } else "红包:$this--${k.support(18, { viewIdResourceName })}领取失败".log()

//            }
//            "红包已领取：${k.support(18, {
//                findAccessibilityNodeInfosByViewId("com.tencent.mm:id/a5t")
//                        .map {
//                            it.text
//                        }
//            })},hash=$this,hashs=$openPacketHash".log()
            k.recycle()
        }
    }

    private fun openPacket(event: AccessibilityEvent) {
        val rootNode = rootInActiveWindow ?: event.source
        getOpenButton(rootNode)
    }

    private fun getOpenButton(info: AccessibilityNodeInfo) {
        when (info.childCount) {
            0 -> {
                if (Button::class.java.name == info.className && info.isClickable &&
                        info.support(18, { viewIdResourceName.isNullOrEmpty().not() })) {
                    performClick(info)
                } else {
                    val parent = info.parent
                    if (parent != null) {
                        if (RelativeLayout::class.java.simpleName == parent.className &&
                                parent.childCount > 3) {
                            val node = parent.getChild(2)
                            if (node.isClickable) {
                                performClick(node)
                            }
                        }
                    }
                }
            }
            else -> {
                for (i in 0..info.childCount - 1) {
                    info.getChild(i)?.run {
                        getOpenButton(this)
                    }
                }
            }
        }
        info.recycle()
    }

    private fun getMoneyGiftPacket(info: AccessibilityNodeInfo): Boolean {
        when (info.childCount) {
            0 -> {
                val text = info.text?.toString()
                if (MONEY_GIFT_OPEN == text) {
                    "接收到红包".log()
                    if (info.isClickable) {
                        return performClick(info)
                    }
                    var parent = info.parent
                    while (parent != null) {
                        if (parent.isClickable && parent.support(18, {
                            viewIdResourceName.isNullOrEmpty().not()
                        })) {
                            parents.add(parent)
//                            performOpenPacket(parent)
                            return true
                        }
                        parent = parent.parent
                    }
                }
                return false
            }
            else -> {
                return (info.childCount - 1 downTo 0)
                        .map { info.getChild(it) }
                        .filter { it != null }
                        .firstOrNull {
                            getMoneyGiftPacket(it)
                        } != null
            }
        }
    }

    private fun performClick(nodeInfo: AccessibilityNodeInfo): Boolean {
        if (nodeInfo.isClickable) {
            return nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } else return false
    }

    /**服务中断回调*/
    override fun onInterrupt() {
        parents.clear()
//        openPacketHash.clear()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        onInterrupt()
        return super.onUnbind(intent)
    }
}