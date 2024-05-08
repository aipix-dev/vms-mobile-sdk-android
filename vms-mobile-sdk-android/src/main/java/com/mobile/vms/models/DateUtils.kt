package com.mobile.vms.models

import java.text.SimpleDateFormat
import java.util.*

/**
 * This date format use on backend
 * If you need to convert utc date to local date use `getLocalDateFromUtc()`
 */
const val TIME_FORMAT_SERVER_UTC = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'"

fun String.getLocalDateFromUtc(): String {
	return try {
		val sdf = SimpleDateFormat(TIME_FORMAT_SERVER_UTC, Locale.getDefault())
		val offset: Int = TimeZone.getDefault().rawOffset
		val calendar = Calendar.getInstance(Locale.getDefault())
		calendar.time = sdf.parse(this)!!
		calendar.timeInMillis = calendar.timeInMillis + offset
		return sdf.format(calendar.time)
	} catch (e: Exception) {
		this
	}
}