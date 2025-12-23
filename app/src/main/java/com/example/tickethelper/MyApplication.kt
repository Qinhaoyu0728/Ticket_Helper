package com.example.tickethelper

import android.app.Application
import com.example.tickethelper.data.FeatureOrderDataStore

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

// 全局应用类，DataStore和通知
class MyApplication : Application() {

    companion object {
        const val TICKET_CHANNEL_ID = "ticket_notification_channel"
        const val TICKET_CHANNEL_NAME = "抢票通知"
    }
    lateinit var featureOrderDataStore: FeatureOrderDataStore

    override fun onCreate() {
        super.onCreate()
        featureOrderDataStore = FeatureOrderDataStore(this)
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                TICKET_CHANNEL_ID,
                TICKET_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "抢票助手有票通知"
                enableVibration(true)
            }

            // 服务通知渠道
            val serviceChannel = NotificationChannel(
                "ticket_service_channel",
                "余票监控服务",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "后台监控余票状态的服务"
                setSound(null, null) // 服务通知不发声
                enableVibration(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            notificationManager.createNotificationChannel(serviceChannel)
        }
    }
}