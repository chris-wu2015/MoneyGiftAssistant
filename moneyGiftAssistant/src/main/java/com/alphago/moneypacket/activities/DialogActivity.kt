package com.alphago.moneypacket.activities

import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.Settings
import android.support.v7.app.AppCompatActivity
import android.view.Window
import com.alphago.extensions.dialog
import com.alphago.moneypacket.R

/**
 * @author Chris
 * @Desc
 * @Date 2017/2/16 016
 */
class DialogActivity : AppCompatActivity() {
    val dialog = lazy {
        dialog {
            title(R.string.app_name)
            message(R.string.service_not_running)
            positiveButton(R.string.go_right_now) {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                dismiss()
            }
            negativeButton(R.string.already_know) { dismiss() }
            neutralButton(R.string.dont_show_again) {
                PreferenceManager.getDefaultSharedPreferences(ctx)
                        .edit()
                        .putBoolean(getString(R.string.dont_show_again), false)
                        .apply()
                dismiss()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        dialog.value?.show()
                ?.setOnDismissListener {
                    finish()
                }
    }

    override fun onStop() {
        super.onStop()
        if (dialog.isInitialized()) {
            dialog.value?.dismiss()
        }
    }
}