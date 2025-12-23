package com.example.tickethelper.util

import okhttp3.OkHttpClient
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

private val userAgentList = listOf(
    "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) MicroMessenger/8.0.45 NetType/WIFI MiniProgramEnv/Windows WindowsWechat",
    "Mozilla/5.0 (Android 14; Mobile; rv:109.0) Gecko/109.0 Firefox/117.0 MicroMessenger/8.0.60",
    "Mozilla/5.0 (Linux; Android 13; SM-G998B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36 MicroMessenger/8.0.58"
)

val randomUserAgent = userAgentList.random()

val osTypeList = listOf("wechat_mini", "android", "ios", "wechat")
val randomOsType = osTypeList.random()

interface TicketService {
    @GET("{targetId}/sessions_dynamic_data")
    suspend fun getTicketStatus(
        @Path("targetId") targetId: String,
        @Query("src") src: String = randomOsType,
        @Query("channelId") channelId: String = "",
        @Query("terminalSrc") terminalSrc: String = "WEB",
        @Query("lang") lang: String = "en"
    ): TicketStatusResponse

    companion object {
        fun create(): TicketService {
            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val original = chain.request()
                    val request = original.newBuilder()
                        // 伪装微信小程序User-Agent
                        .header("User-Agent", randomUserAgent)
                        .header("Accept", "application/json, text/plain, */*")
                        .header("Accept-Language", "zh-CN,zh;q=0.9")
                        .header("Connection", "keep-alive")
                        .build()
                    chain.proceed(request)
                }
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl("https://ztmen.jussyun.com/cyy_gatewayapi/show/pub/v3/show/")
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return retrofit.create(TicketService::class.java)
        }
    }
}

val sessionTypeMapping = listOf("三日票", "周五票", "周六票", "周日票")

// 扩展票种详情数据类
data class TicketSessionDetail(
    val sessionId: String,
    val sessionType: String, // 票种名称（三日票/周五票等）
    val sessionStatus: String, // 余票状态
)

// 扩展目标状态数据类（替换原有的单一状态）
data class TargetTicketStatus(
    val overallStatus: String, // 整体状态（有票/缺票）
    val sessionDetails: List<TicketSessionDetail> // 各票种详情
)

// 响应数据类
data class TicketStatusResponse(
    val statusCode: Int,
    val comments: String,
    val data: TicketData
)

data class TicketData(
    val showId: String,
    val sessionVOs: List<TicketSession>
)

data class TicketSession(
    val bizShowSessionId: String,
    val sessionStatus: String
)