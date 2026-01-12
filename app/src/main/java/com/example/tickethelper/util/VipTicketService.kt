package com.example.tickethelper.util

// 国内久事APP渠道 --> AppTicketScreen

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.QueryMap

interface VipTicketService {
    @GET("ticket/v2-get/getShowSessionNew")
    suspend fun getVipTicketStatus(
        @Query("inWhite") inWhite: Boolean = false,
        @Query("os_type") osType: String = "wechat_mini",
        @Query("showId") showId: String,
        @Query("sign") sign: String
    ): VipTicketResponse

    companion object {
        fun create(appId: String?): VipTicketService {
            val interceptor = Interceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                // app_id header
                appId?.takeIf { it.isNotEmpty() }?.let {
                    requestBuilder.addHeader("app_id", it)
                }
                chain.proceed(requestBuilder.build())
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl("https://jsapp.jussyun.com/jiushi-ticket/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()
            return retrofit.create(VipTicketService::class.java)
        }
    }
}

// 响应
data class VipTicketResponse(
    val data: VipTicketData?,
    val rtnCode: String,
    val rtnMessage: String
)

data class VipTicketData(
    val fastStartTime: Long,
    val lastEndTime: Long,
    val showSessionModelList: List<ShowSessionModel>
)

data class ShowSessionModel(
    val beginTime: String,
    val endTime: String,
    val sessionName: String,
    val priceInfoModelList: List<PriceInfoModel>
)

data class PriceInfoModel(
    val priceName: String,
    val price: Int,
    val stock: Int,
    val sessionStatus: String
)