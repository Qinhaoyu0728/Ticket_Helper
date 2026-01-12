package com.example.tickethelper.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// AppId有效性测试工具类
object AppIdTestUtil {
    private const val TAG = "AppIdTestUtil"

    // 测试AppId是否有效（调用接口测试）
    suspend fun testAppIdValidity(appId: String, testShowId: String = "test_show_id", testSign: String = "test_sign"): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // 服务
                val vipTicketService = VipTicketService.create(appId)
                // 调用测试接口（使用测试用的showId/sign）
                val response = vipTicketService.getVipTicketStatus(
                    showId = testShowId,
                    sign = testSign
                )
                // 响应码为成功码10000且data不为null
                val successCodes = listOf("10000")
                val isValid = successCodes.contains(response.rtnCode) && response.data != null
                Log.d(TAG, "AppId测试结果：${if (isValid) "有效" else "无效"}，响应码=${response.rtnCode}")
                return@withContext isValid
            } catch (e: Exception) {
                // 网络异常/接口调用失败都视为无效
                Log.e(TAG, "AppId测试失败：${e.message}", e)
                return@withContext false
            }
        }
    }
}