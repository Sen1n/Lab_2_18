package com.example.privatbankcurrencies

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateUtils {

    private const val PATTERN = "dd.MM.yyyy"

    fun todayString(): String {
        val formatter = SimpleDateFormat(PATTERN, Locale.US)
        return formatter.format(Date())
    }

    fun isValidDate(dateStr: String): Boolean {
        if (!Regex("""^\d{2}\.\d{2}\.\d{4}$""").matches(dateStr)) return false
        val formatter = SimpleDateFormat(PATTERN, Locale.US).apply {
            isLenient = false
        }
        return try {
            formatter.parse(dateStr) != null
        } catch (e: ParseException) {
            false
        }
    }

    fun millisToDateString(millis: Long): String {
        val formatter = SimpleDateFormat(PATTERN, Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return formatter.format(Date(millis))
    }

    fun dateStringToMillis(dateStr: String): Long? {
        val formatter = SimpleDateFormat(PATTERN, Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
            isLenient = false
        }
        return try {
            formatter.parse(dateStr)?.time
        } catch (e: ParseException) {
            null
        }
    }
}