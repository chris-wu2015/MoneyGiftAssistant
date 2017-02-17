package com.alphago.moneypacket.utils

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.LinearLayout
import android.widget.RelativeLayout

/**
 * Created by Zhongyi on 1/21/16.
 */
interface Signature {
    var sender: String
    var content: String
    var time: String
    var contentDescription: String
    var commentString: String?
    var others: Boolean

    fun generateSignature(node: AccessibilityNodeInfo, excludeWords: String): Boolean
    fun cleanSignature()
}

abstract class HongBaoSignature : Signature {
    override var sender: String = ""
    override var content: String = ""
    override var time: String = ""
    override var contentDescription = ""
    override var commentString: String? = null
    override var others: Boolean = false
    protected val bounds = Rect()

    override fun generateSignature(node: AccessibilityNodeInfo, excludeWords: String): Boolean {
        try {
            /* The hongbao container node. It should be a LinearLayout. By specifying that, we can avoid text messages. */
            val hongbaoNode = node.parent
//            if ("android.widget.LinearLayout" != hongbaoNode.className) return false
            if (judgeHongBaoContainer(hongbaoNode.className)) return false
            /* The text in the hongbao. Should mean something. */
            val hongbaoContent = hongbaoNode.getChild(0).text?.toString()
            if (hongbaoContent == null || "查看红包" == hongbaoContent) return false

            /* Check the user's exclude words list. */
            val excludeWordsArray = excludeWords.split(" +".toRegex()).dropLastWhile(String::isEmpty)
            if (excludeWordsArray
                    .any { it.isNotEmpty() && hongbaoContent.contains(it) }) {
                return false
            }

            /* The container node for a piece of message. It should be inside the screen.
                Or sometimes it will get opened twice while scrolling. */
            val messageNode = hongbaoNode.parent

            bounds.setEmpty()
            messageNode.getBoundsInScreen(bounds)
            if (bounds.top < 0) return false

            /* The sender and possible timestamp. Should mean something too. */
            val hongbaoInfo = arrayOf("unknownSender", "unknownTime")
            getSenderContentDescriptionFromNode(messageNode, hongbaoInfo)
            if (this.getSignature(hongbaoInfo[0], hongbaoContent, hongbaoInfo[1]) == this.toString()) return false

            /* So far we make sure it's a valid new coming hongbao. */
            this.sender = hongbaoInfo[0]
            this.time = hongbaoInfo[1]
            this.content = hongbaoContent
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }

    }

    abstract fun judgeHongBaoContainer(className: CharSequence): Boolean

    override fun toString(): String {
        return this.getSignature(this.sender, this.content, this.time)
    }

    private fun getSignature(vararg strings: String?): String {
        val builder = StringBuilder("")
        for (str in strings) {
            if (str == null) return builder.toString()
            builder.append(str).append("|")
        }
        if (builder.isNotEmpty()) {
            builder.deleteCharAt(builder.length - 1)
        }
        return builder.toString()
    }

    private fun getSenderContentDescriptionFromNode(node: AccessibilityNodeInfo,
                                                    result: Array<String>) {
        val count = node.childCount
        (0..count - 1)
                .map { node.getChild(it) }
                .takeWhile { "unknownSender" == result[0] || "unknownTime" == result[1] }
                .forEach {
                    if ("android.widget.ImageView" == it.className && "unknownSender" == result[0]) {
                        val contentDescription = it.contentDescription
                        if (contentDescription != null) result[0] =
                                contentDescription.toString().replace("头像$".toRegex(), "")
                    } else if ("android.widget.TextView" == it.className && "unknownTime" == result[1]) {
                        val thisNodeText = it.text
                        if (thisNodeText != null) result[1] = thisNodeText.toString()
                    }
                }
    }

    override fun cleanSignature() {
        this.content = ""
        this.time = ""
        this.sender = ""
    }
}

class WeChatHongBaoSignature : HongBaoSignature() {
    override fun judgeHongBaoContainer(className: CharSequence): Boolean {
        return LinearLayout::class.java.name != className
    }
}

class QQHongBaoSignature : HongBaoSignature() {
    override fun judgeHongBaoContainer(className: CharSequence): Boolean {
        return RelativeLayout::class.java.name != className
    }
}
