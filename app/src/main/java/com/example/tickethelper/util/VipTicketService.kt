package com.example.tickethelper.util

// 国内久事APP渠道

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
        fun create(): VipTicketService {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://jsapp.jussyun.com/jiushi-ticket/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return retrofit.create(VipTicketService::class.java)
        }
    }
}

// 响应数据类
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