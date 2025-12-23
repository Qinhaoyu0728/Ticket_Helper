package com.example.tickethelper.utils

import android.app.DatePickerDialog
import android.content.Context
import android.widget.DatePicker
import java.util.*

object DatePickerHelper {
    fun showDatePicker(
        context: Context,
        initialCalendar: Calendar = Calendar.getInstance(),
        onDateSelected: (String) -> Unit
    ) {
        val year = initialCalendar.get(Calendar.YEAR)
        val month = initialCalendar.get(Calendar.MONTH)
        val day = initialCalendar.get(Calendar.DAY_OF_MONTH)

        // 创建日期选择
        val datePickerDialog = DatePickerDialog(
            context,
            { _: DatePicker, selectedYear: Int, selectedMonth: Int, selectedDay: Int ->
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

        // end to 今天
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        datePickerDialog.show()
    }
}