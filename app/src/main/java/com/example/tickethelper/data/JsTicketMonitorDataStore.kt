package com.example.tickethelper.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "js_ticket_monitor"
)

class JsTicketMonitorDataStore(private val context: Context) {
    // 关注列表
    private val KEY_FOCUS_LIST = stringSetPreferencesKey("focus_list")
    // AppId
    private val KEY_APP_ID = stringPreferencesKey("app_id")
    // 消息盒子缓存
    private val KEY_MESSAGE_BOX = stringSetPreferencesKey("message_box")
    // 存储重复次数（Name -> Count）
    private val KEY_MESSAGE_COUNT = stringSetPreferencesKey("message_count")
    private val KEY_APP_ID_VALID = booleanPreferencesKey("app_id_valid")
    private val KEY_APP_ID_TESTED = booleanPreferencesKey("app_id_tested") // 是否测试过

    // 关注对象数据类
    data class FocusItem(
        val name: String,
        val showId: String,
        val sign: String,
        val info: String
    )

    // 消息数据类
    data class TicketMessage(
        val name: String,
        val stock: Int,
        val time: Long,
        val count: Int = 1
    )

    // 获取关注列表
    val focusList: Flow<List<FocusItem>> = context.appSettingsDataStore.data
        .map { prefs: Preferences ->
            val focusSet = prefs[KEY_FOCUS_LIST] ?: emptySet()
            val focusItems = mutableListOf<FocusItem>()
            focusSet.forEach { str ->
                try {
                    val parts = str.split("|")
                    if (parts.size == 4) {
                        focusItems.add(FocusItem(parts[0], parts[1], parts[2], parts[3]))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            focusItems
        }

    // 保存关注列表
    suspend fun saveFocusList(list: List<FocusItem>) {
        context.appSettingsDataStore.edit { prefs ->
            prefs[KEY_FOCUS_LIST] = list.map { "${it.name}|${it.showId}|${it.sign}|${it.info}" }.toSet()
        }
    }

    // 获取AppId
    val appId: Flow<String?> = context.appSettingsDataStore.data
        .map { prefs -> prefs[KEY_APP_ID] }

    // 保存AppId
    suspend fun saveAppId(appId: String) {
        context.appSettingsDataStore.edit { prefs ->
            prefs[KEY_APP_ID] = appId
            prefs[KEY_APP_ID_TESTED] = false // 新保存AppId后标记为未测试
        }
    }

    // AppId脱敏显示
    fun maskAppId(originalAppId: String?): String {
        if (originalAppId.isNullOrEmpty()) {
            return ""
        }
        val length = originalAppId.length
        // 保留前5位 + 后5位，中间用*填充
        return when {
            length <= 10 -> {
                // 长度<=10 直接显示
                originalAppId
            }
            else -> {
                val prefix = originalAppId.substring(0, 5)
                val suffix = originalAppId.substring(length - 5, length)
                val starCount = length - 10
                val stars = "*".repeat(starCount)
                "$prefix$stars$suffix"
            }
        }
    }

    // AppId有效性状态相关方法
    val appIdValid: Flow<Boolean> = context.appSettingsDataStore.data
        .map { prefs -> prefs[KEY_APP_ID_VALID] ?: false }

    val appIdTested: Flow<Boolean> = context.appSettingsDataStore.data
        .map { prefs -> prefs[KEY_APP_ID_TESTED] ?: false }

    suspend fun saveAppIdValidStatus(isValid: Boolean) {
        context.appSettingsDataStore.edit { prefs ->
            prefs[KEY_APP_ID_VALID] = isValid
            prefs[KEY_APP_ID_TESTED] = true // 标记为已测试
        }
    }

    // 获取消息盒子
    val messageBox: Flow<List<TicketMessage>> = context.appSettingsDataStore.data
        .map { prefs: Preferences ->
            // 读取次数
            val countMap = mutableMapOf<String, Int>()
            val countSet = prefs[KEY_MESSAGE_COUNT] ?: emptySet()
            countSet.forEach { str ->
                val parts = str.split("|")
                if (parts.size == 2) {
                    val name = parts[0]
                    val count = parts[1].toIntOrNull() ?: 1
                    countMap[name] = count
                }
            }

            // 读取所有消息（不去重 保留全部）
            val messageList = mutableListOf<TicketMessage>()
            val messageSet = prefs[KEY_MESSAGE_BOX] ?: emptySet()
            messageSet.forEach { str ->
                try {
                    val parts = str.split("|")
                    if (parts.size == 3) {
                        val name = parts[0].ifEmpty { "未知赛事" }
                        val stock = parts[1].toIntOrNull() ?: 0
                        val time = parts[2].toLongOrNull() ?: 0
                        val count = countMap[name] ?: 1

                        if (stock > 0) {
                            messageList.add(
                                TicketMessage(
                                    name = name,
                                    stock = stock,
                                    time = time,
                                    count = count
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 倒序
            messageList.sortedByDescending { it.time }
        }

    // 添加消息到盒子
    suspend fun addTicketMessage(message: TicketMessage) {
        if (message.name.isEmpty() || message.stock <= 0) {
            Log.w("DataStore", "消息校验失败：name=${message.name}, stock=${message.stock}，不存储")
            return
        }

        context.appSettingsDataStore.edit { prefs ->
            // 更新重复次数
            val countMap = mutableMapOf<String, Int>()
            val countSet = prefs[KEY_MESSAGE_COUNT] ?: emptySet()
            countSet.forEach { str ->
                val parts = str.split("|")
                if (parts.size == 2) {
                    val name = parts[0]
                    val count = parts[1].toIntOrNull() ?: 1
                    countMap[name] = count
                }
            }
            countMap[message.name] = countMap.getOrDefault(message.name, 0) + 1
            val newCountSet = mutableSetOf<String>()
            countMap.forEach { (name, count) ->
                newCountSet.add("$name|$count")
            }
            prefs[KEY_MESSAGE_COUNT] = newCountSet

            // 存储消息
            val messageStr = "${message.name}|${message.stock}|${message.time}"
            // 获取原有消息集合
            val existingMessages = prefs[KEY_MESSAGE_BOX] ?: emptySet()
            // 构建新
            val newMessages = mutableSetOf<String>().apply {
                addAll(existingMessages)
                add(messageStr)
            }
            // 先排序再去重
            val uniqueMessages = newMessages.mapNotNull { str ->
                val parts = str.split("|")
                if (parts.size == 3) {
                    Triple(parts[0], parts[1], parts[2].toLongOrNull() ?: 0)
                } else null
            }.groupBy { it.first } // 按赛事名称分组
                .mapValues { (_, list) -> list.maxByOrNull { it.third } } // 取每组最新的
                .values.filterNotNull()
                .map { "${it.first}|${it.second}|${it.third}" } // 转回字符串
                .toSet()

            // 保存
            prefs[KEY_MESSAGE_BOX] = uniqueMessages // 或 newMessages

            Log.d("DataStore", "消息追加成功：$messageStr，当前消息总数=${uniqueMessages.size}")
        }
    }

    // 清空消息盒子
    suspend fun clearMessageBox() {
        context.appSettingsDataStore.edit { prefs ->
            prefs.remove(KEY_MESSAGE_BOX)
            prefs.remove(KEY_MESSAGE_COUNT)
        }
    }

    // 内置关注对象
    val defaultFocusItems = listOf(
        FocusItem(
            name = "A看台",
            showId = "6931332204da960001241231",
            sign = "RTJGQTY1QkY5N0Q0NUM1MzM4RkEwNDk0Q0I5MjcxMTk=",
            info = "2026赛季F1上海站 A看台"
        ),
        FocusItem(
            name = "B看台",
            showId = "693132e14996310001244821",
            sign = "N0M1MDBBMTBDQkNCOEM4Njk1REVBMDVDNTU4ODQxMTM=",
            info = "2026赛季F1上海站 B看台"
        ),
        FocusItem(
            name = "H看台",
            showId = "69315292499631000125952f",
            sign = "MzBCMTNDRjdFNTBDQjdDOTEwREExM0NGQTZCM0MwMzc=",
            info = "2026赛季F1上海站 H看台"
        ),
        FocusItem(
            name = "K看台",
            showId = "693152ad4996310001259691",
            sign = "RUI0N0ZDNUU1QjgzNzBEMTY3QzQyRUM5QjQ4QkMyMzk=",
            info = "2026赛季F1上海站 K看台"
        ),
        FocusItem(
            name = "E看台",
            showId = "693152c604da960001255ee6",
            sign = "MUU2NkIxNkNGNjA5NDg4NTg2RENFQTJEQjE3NjRBMzY=",
            info = "2026赛季F1上海站 E看台"
        ),
        FocusItem(
            name = "C/F/J草地看台",
            showId = "6931535304da960001256176",
            sign = "NjVDMTNFRDY2MTU0NUU0QUUzQTlGM0U3MTZDMzlCOTg=",
            info = "2026赛季F1上海站 C/F/J草地看台"
        ),
        FocusItem(
            name = "F1×迪士尼限量联名套票",
            showId = "6932f256499631000135691a",
            sign = "RTUwMTJEOUI3MTRGOUI0NzEwNzRBQzI4ODFBMzQzQTg=",
            info = "2026赛季F1上海站 迪士尼联名套票"
        ),
        FocusItem(
            name = "铂金体验之旅",
            showId = "693147634996310001251e16",
            sign = "NzE0QkI3MEQ3MEIzRkY5MzZGREVGMTQwQzYwRjIxQjE=",
            info = "2026赛季F1上海站 铂金体验之旅"
        ),
        FocusItem(
            name = "A铂金看台",
            showId = "6931340149963100012455d5",
            sign = "QTU4RDhGQzkwQUVFNUYzRDMyMzc5MjQ1NTQ3NjNDM0I=",
            info = "2026赛季F1上海站 A铂金看台"
        ),
        FocusItem(
            name = "E看台车迷应援区",
            showId = "6931533e49963100012599e9",
            sign = "NkVEMUMwOURCNzBGODM0NDNCOTVGNDgzQTE3RkE2OTc=",
            info = "2026赛季F1上海站 E看台车迷应援区"
        ),
        FocusItem(
            name = "T1 Club",
            showId = "6937d3ce04da96000168c45e",
            sign = "NEQ0MTExNjk4NEQxN0IyNEJEN0VDMUY5RjU0OEUzRUU=",
            info = "2026赛季F1上海站T1 Club"
        ),
        FocusItem(
            name = "T16 Club",
            showId = "6932f40a499631000135814b",
            sign = "Njg3NUUxNDRFRjVCMDhFNURGRUREMzNGMkMwQzVDRTc=",
            info = "2026赛季F1上海站T16 Club"
        )
    )
}