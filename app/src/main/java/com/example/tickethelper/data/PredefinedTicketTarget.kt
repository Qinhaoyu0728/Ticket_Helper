// data/PredefinedTicketTarget.kt
package com.example.tickethelper.data

// 内置抢票目标模型
data class PredefinedTicketTarget(
    val category: String, // 类目名称
    val targetId: String, // 对应的ID
    val description: String // 描述（可选）
)