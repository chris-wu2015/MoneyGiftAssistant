package com.alphago.apps

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        val fab = findViewById(R.id.fab) as FloatingActionButton
        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action") { v ->
                        openAccessibilitySetting(v.context)
                    }.show()
        }
    }

    private fun openAccessibilitySetting(context: Context) {
        context.startActivity(
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    override fun onResume() {
        super.onResume()
        checkAccessibilityEnable()
    }

    private fun checkAccessibilityEnable() {
        try {
            val enable = Settings.Secure.getInt(applicationContext.contentResolver,
                    Settings.Secure.ACCESSIBILITY_ENABLED)
            if (enable != 1) {
                openAccessibilitySetting(this)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId


        if (id == R.id.action_settings) {
            return true
        }

        return super.onOptionsItemSelected(item)
    }
}
