package com.example.tickethelper.util

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

object LogCollector {
    // 日志缓存（max 100条）
    private val _logList = MutableStateFlow<MutableList<LogItem>>(mutableListOf())
    val logList: StateFlow<List<LogItem>> = _logList.asStateFlow()

    // 日志格式化器
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    // TAG（仅收集F1_TicketRefresh）
    private const val TARGET_TAG = "F1_TicketRefresh"

    /**
     * 收集
     */
    fun collectLog(level: String, tag: String, message: String) {
        if (tag != TARGET_TAG) return

        val logItem = LogItem(
            time = dateFormat.format(Date()),
            level = level,
            tag = tag,
            message = message
        )

        synchronized(_logList) {
            val newList = _logList.value.toMutableList()
            newList.add(logItem)
            // 超过100条时移除最旧的
            if (newList.size > 100) {
                newList.removeAt(0)
            }
            _logList.value = newList
        }
    }

    /**
     * 清空
     */
    fun clearLogs() {
        _logList.value = mutableListOf()
    }

    /**
     * 日志数据
     */
    data class LogItem(
        val time: String, // 时间戳 HH:mm:ss.SSS
        val level: String, // 级别 D/I/W/E/V
        val tag: String, // TAG
        val message: String // 内容
    )
}

// 定义log
fun logD(tag: String, msg: String) {
    Log.d(tag, msg)
    LogCollector.collectLog("D", tag, msg)
}

fun logI(tag: String, msg: String) {
    Log.i(tag, msg)
    LogCollector.collectLog("I", tag, msg)
}

fun logW(tag: String, msg: String) {
    Log.w(tag, msg)
    LogCollector.collectLog("W", tag, msg)
}

fun logE(tag: String, msg: String) {
    Log.e(tag, msg)
    LogCollector.collectLog("E", tag, msg)
}

fun logV(tag: String, msg: String) {
    Log.v(tag, msg)
    LogCollector.collectLog("V", tag, msg)
}