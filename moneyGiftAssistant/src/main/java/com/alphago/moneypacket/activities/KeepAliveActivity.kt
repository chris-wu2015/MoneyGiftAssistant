package com.alphago.moneypacket.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Gravity
import android.widget.FrameLayout
import com.alphago.moneypacket.utils.ScreenManager
import org.jetbrains.anko.newTask

/**
 * @author Chris
 * @Desc
 * @Date 2017/2/23 023
 */
class KeepAliveActivity : AppCompatActivity() {
    companion object {
        fun open(context: Context) {
            val intent = Intent(context, KeepAliveActivity::class.java)
            if (context !is Activity) {
                intent.newTask()
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        println("KeepAliveActivity.onCreate")
        window.run {
            setGravity(Gravity.START or Gravity.TOP)
            val attrs = attributes
            attrs.run {
                width = 1
                height = 1
                x = 0
                y = 0
            }
            attributes = attrs
        }
    }

    override fun onResume() {
        super.onResume()
        ScreenManager.setActivity(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        println("KeepAliveActivity.onDestroy")
    }
}