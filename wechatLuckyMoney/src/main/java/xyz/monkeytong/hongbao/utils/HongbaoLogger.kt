package xyz.monkeytong.hongbao.utils

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Created by Zhongyi on 1/22/16.
 */
class HongbaoLogger(private val context: Context) {
    private var database: SQLiteDatabase? = null

    init {
        this.initSchemaAndDatabase()
    }

    private fun initSchemaAndDatabase() {
        this.database = context.openOrCreateDatabase("WeChatLuckyMoney", Context.MODE_PRIVATE, null)

        this.database!!.beginTransaction()
        this.database!!.execSQL(createDatabaseSQL)
        this.database!!.endTransaction()
    }

    fun writeHongbaoLog(sender: String, content: String, amount: String) {

    }

    fun getAllLogs() {

    }

    companion object {

        private val DATABASE_VERSION = 1
        private val DATABASE_NAME = "WeChatLuckyMoney.db"
        private val createDatabaseSQL = "CREATE TABLE IF NOT EXISTS HongbaoLog (id INTEGER PRIMARY KEY AUTOINCREMENT, sender TEXT, content TEXT, time TEXT, amount TEXT);"
    }
}
