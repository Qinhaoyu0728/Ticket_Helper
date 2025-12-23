package com.example.tickethelper.data

import android.annotation.SuppressLint
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class AutoRefreshConfig(
    val enabled: Boolean = false,
    val showOverallStatus: Boolean = true, // 是否显示整体状态标签（默认开启）
    val listStyle: String = "two_column" // 样式
)