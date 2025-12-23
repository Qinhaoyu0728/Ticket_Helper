package com.example.tickethelper.data

import android.annotation.SuppressLint
import kotlinx.serialization.Serializable
import java.util.UUID

@SuppressLint("UnsafeOptInUsageError")
@Serializable // 标记为可序列化
data class TicketTarget(
    val id: String = UUID.randomUUID().toString(),
    val targetId: String,
    val category: String,
    val createdAt: Long = System.currentTimeMillis()
)