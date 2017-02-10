package xyz.monkeytong.hongbao.services

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import xyz.monkeytong.hongbao.R

/**
 * @author Chris
 * @Desc
 * @Date 2017/2/10 010
 */
interface HongBaoProcessor {
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

open class WechatHongBaoProcessor(context: Context,
                                  override val BETTER_LUCK: String = context.getString(
                                          R.string.better_luck),
                                  override val DETAILS: String = context.getString(R.string.detail),
                                  override val EXPIRES: String = context.getString(R.string.expires),
                                  override val LUCK_MONEY_CHATTING_ACTIVITY: String
                                  = "LuckyMoneyReceiveUI",
                                  override val LUCK_MONEY_DETAIL_ACTIVITY: String = "LuckyMoneyDetailUI",
                                  override val LUCK_MONEY_GENERAL_ACTIVITY: String = "LauncherUI",
                                  override val LUCK_MONEY_RECEIVE_ACTIVITY: String = "ChattingUI",
                                  override val NOTIFICATION_TIP: String = context.getString(
                                          R.string.wechat_notification_tip),
                                  override val VIEW_OTHERS: String = "领取红包",
                                  override val VIEW_SELF: String = "查看红包") : HongBaoProcessor {
    override fun updateCurrentActivityName(packageManager: PackageManager,
                                           event: AccessibilityEvent): String? {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return null
        }
        try {
            val componentName = ComponentName(
                    event.packageName.toString(),
                    event.className.toString())
            packageManager.getActivityInfo(componentName, 0)
            return componentName.flattenToShortString()
        } catch (e: PackageManager.NameNotFoundException) {
            return LUCK_MONEY_GENERAL_ACTIVITY
        }
    }

    override fun getTheLastNode(nodeInfo: AccessibilityNodeInfo, vararg texts: String): AccessibilityNodeInfo? {
        var bottom = 0
        var lastNode: AccessibilityNodeInfo? = null
        var tempNode: AccessibilityNodeInfo? = null
        var nodes: MutableList<AccessibilityNodeInfo?>?
        val bounds = Rect()
        for (text in texts) {
            nodes = nodeInfo.findAccessibilityNodeInfosByText(text)
            nodes?.run {
                if (isNotEmpty())
                    tempNode = this[this.size - 1]
            }
            bounds.setEmpty()
            tempNode?.getBoundsInScreen(bounds)
            if (bounds.bottom > bottom) {
                bottom = bounds.bottom
                lastNode = tempNode
            }
        }
        return lastNode
    }

    override fun findOpenButton(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        return node?.run {
            if (childCount == 0) {
                return if (Button::class.java.name == className) this else null
            }
            var button: AccessibilityNodeInfo? = null
            (0..childCount - 1).any {
                button = findOpenButton(getChild(it))
                button != null
            }
            return button
        }
    }
}