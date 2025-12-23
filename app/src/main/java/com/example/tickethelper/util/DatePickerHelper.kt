package com.example.tickethelper.utils

import android.app.DatePickerDialog
import android.content.Context
import android.widget.DatePicker
import java.util.*

object DatePickerHelper {
    // 显示原生日期选择对话框
    fun showDatePicker(
        context: Context,
        initialCalendar: Calendar = Calendar.getInstance(),
        onDateSelected: (String) -> Unit // 回调：返回 yyyy-MM-dd 格式
    ) {
        val year = initialCalendar.get(Calendar.YEAR)
        val month = initialCalendar.get(Calendar.MONTH)
        val day = initialCalendar.get(Calendar.DAY_OF_MONTH)

        // 创建原生日期选择对话框
        val datePickerDialog = DatePickerDialog(
            context,
            { _: DatePicker, selectedYear: Int, selectedMonth: Int, selectedDay: Int ->
                // 格式化日期为 yyyy-MM-dd（月份+1，因为原生月份从0开始）
                val formattedDate = String.format(
                    Locale.getDefault(),
                    "%04d-%02d-%02d",
                    selectedYear,
                    selectedMonth + 1,
                    selectedDay
                )
                onDateSelected(formattedDate)
            },
            year,
            month,
            day
        )

        // 限制最大可选日期为今天（可选）
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        datePickerDialog.show()
    }
}