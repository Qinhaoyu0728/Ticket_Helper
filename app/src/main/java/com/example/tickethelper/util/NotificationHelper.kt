package com.example.tickethelper.util

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.tickethelper.MyApplication
import com.example.tickethelper.R

object NotificationHelper {
    private const val NOTIFICATION_ID = 1001

    fun showTicketAvailableNotification(context: Context, targetName: String) {
        val notificationManager = context.getSystemService(
            NotificationManager::class.java
        ) as NotificationManager

        // 构建通知内容（支持加粗样式）
        val contentText = "你关注的 <b>$targetName</b> 有票！快去抢！"

        val builder = NotificationCompat.Builder(context, MyApplication.TICKET_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("抢票通知")
            .setContentText("你关注的$targetName 有票！快去抢！")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(android.text.Html.fromHtml(contentText, android.text.Html.FROM_HTML_MODE_COMPACT))
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        // 显示通知
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.build()
        } else {
            builder.build().apply {
                flags = Notification.FLAG_AUTO_CANCEL
            }
        }

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}