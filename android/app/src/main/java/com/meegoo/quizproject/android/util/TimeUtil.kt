package com.meegoo.quizproject.android.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*
import android.os.Build
import com.meegoo.quizproject.android.QuizApplication


var formatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
    .withLocale(getLocale())
    .withZone(ZoneId.systemDefault())

fun Instant?.format(): String {
    return this?.let {
        formatter.format(it)
    } ?: ""
}


private fun getLocale(): Locale? {
    val context = QuizApplication.appContext
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        context.resources.configuration.locales.get(0)
    } else {
        @Suppress("DEPRECATION")
        context.resources.configuration.locale
    }
}