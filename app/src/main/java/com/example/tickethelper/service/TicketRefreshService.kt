package com.example.tickethelper.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.example.tickethelper.MainActivity
import com.example.tickethelper.R
import com.example.tickethelper.data.TicketTarget
import com.example.tickethelper.data.TicketTargetDataStore
import com.example.tickethelper.util.NotificationHelper
import com.example.tickethelper.util.TicketService
import com.example.tickethelper.util.TargetTicketStatus
import com.example.tickethelper.util.TicketSessionDetail
import com.example.tickethelper.util.logD
import com.example.tickethelper.util.logE
import com.example.tickethelper.util.logI
import com.example.tickethelper.util.logV
import com.example.tickethelper.util.logW
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TicketRefreshService : LifecycleService() {
    // 固定常量
    private val NOTIFICATION_ID = 1002
    private val SERVICE_CHANNEL_ID = "ticket_service_channel"
    private val SERVICE_CHANNEL_NAME = "余票监控服务"
    private val TAG = "F1_TicketRefresh"

    // 依赖对象（懒加载）
    private val ticketService by lazy { TicketService.create() }
    private val ticketTargetDataStore by lazy { TicketTargetDataStore.getInstance(applicationContext) }

    // 运行状态&缓存
    private var isRunning = false
    private var lastStatusMap = emptyMap<String, TargetTicketStatus>()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (!isRunning) {
            isRunning = true
            // 适配Android 14+的前台服务启动
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Android 14+：指定前台服务类型
                startForeground(
                    NOTIFICATION_ID,
                    createNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC // 匹配Manifest中的type
                )
            } else {
                // 低版本：直接启动
                startForeground(NOTIFICATION_ID, createNotification())
            }
            startRefreshLoop()
        }

        return START_STICKY
    }

    /**
     * 后台刷新循环（核心逻辑）
     */
    private fun startRefreshLoop() {
        lifecycleScope.launch(Dispatchers.IO) {
            //Log.d(TAG, "后台刷新循环已启动 | 刷新间隔：10秒") // 启动日志
            logD(TAG, "后台刷新循环已启动")
            while (isRunning) {
                // 读取自动刷新配置，仅在开启时执行
                val config = ticketTargetDataStore.getAutoRefreshConfig.first()
                val randomOffset = (Math.random() * 3000).toLong() // 0-3秒随机偏移
                if (config.enabled) {
                    val targets = ticketTargetDataStore.getTicketTargets.first()
                    logD(TAG, "开始执行余票查询 | 监控目标数：${targets.size}") // 查询开始
                    refreshTicketStatus(targets)
                    logD(TAG, "余票查询执行完成 | 等待10~13秒后下次查询") // 查询完成
                } else {
                    logD(TAG, "自动刷新未启用 | 跳过本次查询")
                }

                // 间隔10~13秒刷新
                delay(10000 + randomOffset)
                logV(TAG, "间隔已结束 | 准备下一次查询")
            }
        }
    }

    /**
     * 刷新余票状态并检测变化
     */
    private suspend fun refreshTicketStatus(targets: List<TicketTarget>) {
        if (targets.isEmpty()) {
            logW(TAG, "刷新跳过 | 无监控目标")
            return
        }

        val newStatusMap = mutableMapOf<String, TargetTicketStatus>()

        targets.forEach { target ->
            try {
                // 调用接口获取状态
                val response = ticketService.getTicketStatus(target.targetId)
                logI(TAG, "查询结果 | 目标ID：${target.id} | 接口状态码：${response.statusCode}")

                if (response.statusCode == 200) {
                    val sessions = response.data.sessionVOs

                    // 显式构建 TicketSessionDetail 列表
                    val sessionDetails: List<TicketSessionDetail> = sessions.mapIndexed { index, session ->
                        // 票种映射
                        val sessionType = if (sessions.size == 1) {
                            "三日票"
                        } else {
                            val sessionTypeMapping = mapOf(0 to "三日票", 1 to "周六单日票", 2 to "周日单日票", 3 to "周五单日票")
                            sessionTypeMapping.getOrElse(index) { "未知票种${index+1}" }
                        }

                        // TicketSessionDetail
                        TicketSessionDetail(
                            sessionId = session.bizShowSessionId,
                            sessionType = sessionType,
                            sessionStatus = session.sessionStatus
                        )
                    }

                    // 整体状态判断
                    val overallStatus = if (sessionDetails.any { it.sessionStatus == "ON_SALE" }) {
                        "ON_SALE"
                    } else {
                        "LACK_OF_TICKET"
                    }

                    // 显式构建 TargetTicketStatus
                    newStatusMap[target.id] = TargetTicketStatus(
                        overallStatus = overallStatus,
                        sessionDetails = sessionDetails
                    )
                } else {
                    // 接口返回非200
                    newStatusMap[target.id] = TargetTicketStatus(
                        overallStatus = "ERROR",
                        sessionDetails = emptyList()
                    )
                }
            } catch (e: Exception) {
                // 异常捕获
                e.printStackTrace()
                logE(TAG, "查询异常 | 目标ID：${target.id} | 错误信息：${e.message}")
                newStatusMap[target.id] = TargetTicketStatus(
                    overallStatus = "ERROR",
                    sessionDetails = emptyList()
                )
            }
        }

        lastStatusMap = newStatusMap.toMap()
        updateNotification(targets.size)
        logD(TAG, "刷新完成 | 本次更新${newStatusMap.size}个目标状态") // 整体完成

        // 检测状态变化并发送通知
        newStatusMap.forEach { (targetId, newStatus) ->
            val oldStatus = lastStatusMap[targetId]
            val target = targets.find { it.id == targetId }

            // 仅当「无票→有票」时发送通知
            if (target != null &&
                //oldStatus?.overallStatus == "LACK_OF_TICKET" &&
                newStatus.overallStatus == "ON_SALE") {
                withContext(Dispatchers.Main) {
                    NotificationHelper.showTicketAvailableNotification(
                        context = applicationContext,
                        targetName = target.category ?: "F1门票"
                    )
                }
            }
        }

        // 更新缓存&前台通知
        lastStatusMap = newStatusMap.toMap()
        updateNotification(targets.size)
    }

    /**
     * 创建前台服务通知（初始）
     */
    private fun createNotification(): Notification {
        // 点击通知返回主界面
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntentFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, pendingIntentFlag)

        // 显式构建 Notification（解决泛型推断问题）
        return NotificationCompat.Builder(this, SERVICE_CHANNEL_ID)
            .setContentTitle("F1余票监控中")
            .setContentText("正在监控0个目标")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // 替换为你的图标
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true) // 前台服务通知不可手动关闭
            .build()
    }

    /**
     * 更新前台通知内容（刷新目标数量）
     */
    private fun updateNotification(targetCount: Int) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntentFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, pendingIntentFlag)

        val updatedNotification = NotificationCompat.Builder(this, SERVICE_CHANNEL_ID)
            .setContentTitle("F1余票监控中")
            .setContentText("正在监控${targetCount}个目标")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, updatedNotification)
    }

    /**
     * 创建通知渠道（Android O+ 必需）
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                SERVICE_CHANNEL_ID,
                SERVICE_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "余票监控服务"
                setSound(null, null) // 关闭服务通知声音
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        isRunning = false // 停止循环
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }
}