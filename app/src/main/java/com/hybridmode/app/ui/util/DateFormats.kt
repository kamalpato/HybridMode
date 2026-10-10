package com.hybridmode.app.ui.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateFormats {
    private val dateTime = SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.getDefault())
    private val dateOnly = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    fun dateTime(epochMs: Long): String = dateTime.format(Date(epochMs))

    fun date(epochMs: Long): String = dateOnly.format(Date(epochMs))
}
