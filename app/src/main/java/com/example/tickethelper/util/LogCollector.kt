package com.example.tickethelper.util

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

object LogCollector {
    // 日志缓存（最多保存100条）
    private val _logList = MutableStateFlow<MutableList<LogItem>>(mutableListOf())
    val logList: StateFlow<List<LogItem>> = _logList.asStateFlow()

    // 日志格式化器
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    // 目标日志TAG（仅收集F1_TicketRefresh）
    private const val TARGET_TAG = "F1_TicketRefresh"

    /**
     * 收集日志（需在打印日志时调用）
     */
    fun collectLog(level: String, tag: String, message: String) {
        if (tag != TARGET_TAG) return // 仅收集目标TAG的日志

        val logItem = LogItem(
            time = dateFormat.format(Date()),
            level = level,
            tag = tag,
            message = message
        )

        // 线程安全更新日志列表
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
     * 清空日志
     */
    fun clearLogs() {
        _logList.value = mutableListOf()
    }

    /**
     * 日志数据类
     */
    data class LogItem(
        val time: String, // 时间戳 HH:mm:ss.SSS
        val level: String, // 日志级别 D/I/W/E/V
        val tag: String, // 日志TAG
        val message: String // 日志内容
    )
}

// 扩展函数：快速打印并收集日志
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