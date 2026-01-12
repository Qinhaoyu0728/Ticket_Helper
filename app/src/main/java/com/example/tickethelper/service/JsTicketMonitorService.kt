package com.example.tickethelper.service

//noinspection SuspiciousImport
import android.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.app.NotificationCompat
import com.example.tickethelper.data.JsTicketMonitorDataStore
import com.example.tickethelper.util.VipTicketService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import retrofit2.HttpException
import java.io.IOException

class JsTicketMonitorService : Service() {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val CHANNEL_ID = "js_ticket_monitor"
    private val NOTIFICATION_ID = 1003
    private val TAG = "JsTicketMonitorService"

    // 标记是否主动停止服务
    private var isStoppedManually = false
    // 保存监控任务的引用
    private var monitorJob: Job? = null

    private suspend fun checkTicketStock(
        focusItem: JsTicketMonitorDataStore.FocusItem,
        appId: String?
    ): Int {
        Log.d(TAG, "开始查询${focusItem.name}的余票，appId=$appId")
        // 校验
        if (appId.isNullOrEmpty()) {
            return 0
        }
        if (focusItem.showId.isEmpty() || focusItem.sign.isEmpty()) {
            return 0
        }

        // VipTicketService
        return try {
            val vipTicketService = VipTicketService.create(appId)
            val response = vipTicketService.getVipTicketStatus(
                showId = focusItem.showId,
                sign = focusItem.sign
            )

            // 解析
            calculateTotalStock(response)
        } catch (e: HttpException) {
            // 404/500
            println("查询${focusItem.name}余票失败：HTTP错误 ${e.code()}")
            // isStoppedManually = true
            onDestroy()
            sendServiceErrorNotification("JST服务发生错误，已自动关闭，请稍后重试！")
            0
        } catch (e: IOException) {
            // 无网
            println("查询${focusItem.name}余票失败：网络异常 ${e.message}")
            // isStoppedManually = true
            onDestroy()
            sendServiceErrorNotification("JST服务发生错误，已自动关闭，请稍后重试！")
            0
        } catch (e: Exception) {
            // 未知异常
            println("查询${focusItem.name}余票失败：${e.message}")
            0
        }
    }

    private fun calculateTotalStock(response: com.example.tickethelper.util.VipTicketResponse): Int {

        Log.d(TAG, "接口原始响应：rtnCode=${response.rtnCode}, data=${response.data}")

        // 校验响应状态
        if (response.rtnCode != "10000" || response.data == null) {
            println("接口返回异常：${response.rtnMessage}")
            isStoppedManually = true
            sendServiceErrorNotification("JST服务发生错误，已自动关闭，请稍后重试！")
            return 0
        }

        var totalStock = 0
        val data = response.data

        Log.d(TAG, "共${data.showSessionModelList.size}个场次")
        // 遍历所有场次
        response.data.showSessionModelList.forEachIndexed { sessionIndex, session ->
            Log.d(TAG, "场次${sessionIndex+1}：${session.sessionName}，共${session.priceInfoModelList.size}个票价档位")
            // 遍历所有票价档位
            session.priceInfoModelList.forEachIndexed { priceIndex, priceInfo ->
                // 累加有库存的票数（stock>0计入）
                val stockValue = when (priceInfo.stock) {
                    is Int -> priceInfo.stock
                    is String -> {
                        priceInfo.stock.trim().toIntOrNull() ?: 0
                    }
                    else -> 0 // 其他类型=0
                }

                Log.d(TAG, "  票价档位${priceIndex+1}：${priceInfo.priceName}，原始stock=${priceInfo.stock}，解析后=$stockValue")

                // 累加
                if (stockValue > 0) {
                    totalStock += stockValue
                    Log.d(TAG, "  → 累加库存，当前总库存=$totalStock")
                }
            }
        }
        return totalStock
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "久事余票监控",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "久事余票监控后台服务"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun getForegroundNotification(): Notification {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("久事余票监控")
            .setContentText("后台监控中...")
            .setSmallIcon(R.drawable.ic_notification_overlay)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        // 启动前台服务（保活）
        startForeground(NOTIFICATION_ID, getForegroundNotification())
        Log.d(TAG, "JST服务已创建并启动前台服务")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 重置手动停止标记
        isStoppedManually = false
        // 启动监控任务（保存Job）
        monitorJob = scope.launch {
            startMonitor()
        }
        Log.d(TAG, "JST服务开始执行监控逻辑，每10秒查询一次余票")
        return START_STICKY // 服务被杀死后自动重启
    }

    private suspend fun startMonitor() {
        val dataStore = JsTicketMonitorDataStore(applicationContext)
        while (!isStoppedManually) { // 手动停止时退出循环
            val focusList = dataStore.focusList.first()
            val appId = dataStore.appId.first()

            focusList.forEach { item ->
                if (isStoppedManually) return@forEach // 提前退出
                val stock = checkTicketStock(item, appId)
                Log.d(TAG, "查询${item.name}结果：stock=$stock")
                if (stock > 0) {
                    Log.d(TAG, "${item.name}有票，准备添加到消息盒子")
                    val currentMessages = dataStore.messageBox.first()
                    val isDuplicate = currentMessages.any {
                        it.name == item.name &&
                                it.time > System.currentTimeMillis() - 5 * 60 * 1000
                    }

                    if (!isDuplicate) {
                        val message = JsTicketMonitorDataStore.TicketMessage(
                            name = item.name,
                            stock = stock,
                            time = System.currentTimeMillis(),
                            count = 1
                        )
                        dataStore.addTicketMessage(message)
                        Log.d(TAG, "${item.name}消息已添加到DataStore")
                        sendTicketNotification(message)
                    }
                }
            }
            delay(10000)
        }
        Log.d(TAG, "监控循环已退出")
    }

    private fun sendTicketNotification(message: JsTicketMonitorDataStore.TicketMessage) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("${message.name}有余票啦！")
            .setContentText("剩余票数：${message.stock}")
            .setSmallIcon(R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun sendServiceErrorNotification(message: String) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("JST服务错误")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        // 标记为手动停止 停止监控协程
        isStoppedManually = true
        monitorJob?.cancel() // 取消监控任务
        // 停止前台服务
        stopForeground(STOP_FOREGROUND_REMOVE)

        val intent = Intent("com.example.tickethelper.JST_SERVICE_STOPPED")
        intent.setPackage(packageName)
        val isSent = sendBroadcast(intent)
        Log.d(TAG, "服务关闭广播发送结果：$isSent")

        Log.d(TAG, "JST服务已销毁，监控任务已取消")
        // 重启服务（保活）
//        val intent = Intent(this, JsTicketMonitorService::class.java)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            startForegroundService(intent)
//        } else {
//            startService(intent)
//        }
    }

    // 手动停止Intent
    companion object {
        // Intent Action
        const val ACTION_STOP_SERVICE = "com.example.tickethelper.STOP_SERVICE"

        fun stopService(context: Context) {
            val intent = Intent(context, JsTicketMonitorService::class.java)
            intent.action = ACTION_STOP_SERVICE
            context.startService(intent)
        }
    }
}