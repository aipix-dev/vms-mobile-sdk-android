package com.mobile.vms.player.helpers

import com.mobile.vms.player.helpers.VMSSettings.getStringForLayoutByKey
import com.mobile.vms.settings
import java.text.SimpleDateFormat
import java.util.*

const val KEY_SCREENSHOT_DETAIL_EXPANDED = "KEY_SCREENSHOT_DETAIL_EXPANDED"
const val KEY_SCREENSHOT_DETAIL_HIDDEN = "KEY_SCREENSHOT_DETAIL_HIDDEN"
const val KEY_INTERCOM_BG = "KEY_INTERCOM_BG"
const val KEY_INTERCOM_VISITOR_TIME = "KEY_INTERCOM_VISITOR_TIME"
const val KEY_PRIMARY_BRAND_1 = "KEY_PRIMARY_BRAND_1"
const val KEY_PRIMARY_BRAND_2 = "KEY_PRIMARY_BRAND_2"
const val ALPHA_FULL = 1f
const val ALPHA_HALF = 0.4f
const val ALPHA_ZERO = 0f
const val ACTIVE = "active" //  у камеры 2 стрима, оба активных
const val INACTIVE = "inactive" //  - у камеры все стримы не активны
const val PARTIAL = "partial" // - у камеры 1 стрим активен
const val EMPTY = "empty" // у камеры нет потоков
const val INITIAL = "initial" // - камера только создалась, статус еще не обновился (около 1 минуты)
const val HIGH = "high"
const val LOW = "low"
const val DIRECTION_PREVIOUS = "previous"
const val DIRECTION_NEXT = "next"
const val USER_STATUS_BLOCKED = "blocked"
const val USER_STATUS_INACTIVE = "inactive"
const val DEFAULT_SCREEN_NUMBER = -1
const val STATUS_TIME_START_PERIOD = "statusTimeStartPeriod"
const val STATUS_TIME_END_PERIOD = "statusTimeEndPeriod"
const val ERROR_TYPE_CONNECTION = "ERROR_TYPE_CONNECTION"
const val ERROR_TYPE_TECHNICAL = "ERROR_TYPE_TECHNICAL"
const val ERROR_TYPE_FORCE_UPDATE = "ERROR_TYPE_FORCE_UPDATE"

// will be set - BuildConfig.FLAVOR == EXTERNAL_APP_ONE || BuildConfig.FLAVOR == MYANALYTICS
fun isExternalBrandSdk() = false

const val SHOW_STREAM_LIFE = "SHOW_STREAM_LIFE" //просмотр лайф
const val SHOW_STREAM_ARCHIVE = "SHOW_STREAM_ARCHIVE" //просмотр архив
const val TAP_ARCHIVE_PAUSE = "TAP_ARCHIVE_PAUSE" // нажать поставить на паузу
const val TAP_ARCHIVE_PLAY = "TAP_ARCHIVE_PLAY" // нажать плей
const val TAP_PLAYBACK_START = "TAP_PLAYBACK_START" // нажать перемотать назад
const val TAP_PLAYBACK_END = "TAP_PLAYBACK_END" // нажать перемотать назад
const val TAP_PLAYBACK_MINUS_24H = "TAP_PLAYBACK_MINUS_24H" // нажать перемотать назад
const val TAP_PLAYBACK_MINUS_1h = "TAP_PLAYBACK_MINUS_1h" // нажать перемотать назад
const val TAP_PLAYBACK_MINUS_1M = "TAP_PLAYBACK_MINUS_1M" // нажать перемотать назад
const val TAP_PLAYBACK_MINUS_5S = "TAP_PLAYBACK_MINUS_5S" // нажать перемотать назад
const val TAP_PLAYBACK_PLUS_24H = "TAP_PLAYBACK_PLUS_24H" // нажать перемотать вперед
const val TAP_PLAYBACK_PLUS_1h = "TAP_PLAYBACK_PLUS_1h" // нажать перемотать вперед
const val TAP_PLAYBACK_PLUS_1M = "TAP_PLAYBACK_PLUS_1M" // нажать перемотать вперед
const val TAP_PLAYBACK_PLUS_5S = "TAP_PLAYBACK_PLUS_5S" // нажать перемотать вперед
const val SHOW_CALENDAR = "SHOW_CALENDAR" // показать календарь
const val SHOW_DOWNLOAD_ARCHIVE = "SHOW_DOWNLOAD_ARCHIVE" // скачать архив
const val SHOW_PLAYBACK_SPEED = "SHOW_PLAYBACK_SPEED" //скорость воспроизведения
const val TAP_PLAYBACK_SPEED_0_5x = "TAP_PLAYBACK_SPEED_0_5x" // нажать 0.5X
const val TAP_PLAYBACK_SPEED_1X = "TAP_PLAYBACK_SPEED_1X" // нажать 1X
const val TAP_PLAYBACK_SPEED_2X = "TAP_PLAYBACK_SPEED_2X" // нажать 2X
const val TAP_PLAYBACK_SPEED_4X = "TAP_PLAYBACK_SPEED_4X" // нажать 4X
const val TAP_PLAYBACK_SPEED_8X = "TAP_PLAYBACK_SPEED_8X" // нажать 8X
const val SHOW_MARK_LIST = "SHOW_MARK_LIST" // показать метки камеры
const val TAP_VIDEO_QUALITY_HIGH =
	"TAP_VIDEO_QUALITY_HIGH" // нажать переключить стрим на высокое качество
const val TAP_VIDEO_QUALITY_LOW =
	"TAP_VIDEO_QUALITY_LOW" // нажать переключить стрим на низкое качество

const val LABEL_UP = "LABEL_UP"
const val LABEL_DOWN = "LABEL_DOWN"
const val LABEL_RIGHT = "LABEL_RIGHT"
const val LABEL_LEFT = "LABEL_LEFT"
const val LABEL_RESET = "LABEL_REST"
const val LABEL_ZOOM_IN = "LABEL_ZOOM_IN"
const val LABEL_ZOOM_OUT = "LABEL_ZOOM_OUT"
const val TOAST_DURATION = 2000L
const val LONG_CLICK_DURATION = 4000L
const val LONG_CLICK_TIMEOUT = 500L
const val NOTIFICATION_ID = 1
const val GO_TO_PREVIOUS_MARK_DELAY = 5000L
const val TEN_MIN = 600000L
const val speed_X_1 = 1f

const val MOVE_LEFT_ONE_DAY = -86400 * 1000L
const val MOVE_LEFT_ONE_HOUR = -3600 * 1000L
const val MOVE_LEFT_ONE_MINUTE = -60 * 1000L
const val MOVE_LEFT_FIVE_SECONDS = -5 * 1000L
const val MOVE_RIGHT_ONE_DAY = 86400 * 1000L
const val MOVE_RIGHT_ONE_HOUR = 3600 * 1000L
const val MOVE_RIGHT_ONE_MINUTE = 60 * 1000L
const val MOVE_RIGHT_FIVE_SECONDS = 5 * 1000L

/*
convert speed float to string with cut zero after dot (0.5f -> 0.5x; 1.0f -> 1x)
 */
fun getSpeedText(speed: Float) =
	(if (speed >= speed_X_1) speed.toInt().toString() else speed.toString()) + "x"

const val TIME_FORMAT_CLIENT = "dd.MM.yyyy HH:mm:ss"
const val TIME_FORMAT_SERVER = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'"
const val TIME_FORMAT_SERVER_TIMELINE = "yyyy-MM-dd HH:mm:ss"
const val TIME_FORMAT_EXIF = "yyyy:MM:dd HH:mm:ss"
const val FULL_DATE_FORMAT = "yyyy-MM-dd-HH-mm-ss"
const val DATE_FORMAT_PERIOD = "dd MMM yyyy HH:mm:ss"
const val TIME = "HH:mm:ss"

const val EVENTS_NOT_SHOW = "EVENTS_NOT_SHOW"
const val EVENTS_SHOW_ALL = "EVENTS_SHOW_ALL"
const val RU = "ru"
const val EN = "en"
const val VIDEO_TYPE_RTSP = "rtsp"
const val VIDEO_TYPE_HLS = "hls"

fun getOffset() = TimeZone.getDefault().rawOffset // + TimeZone.getDefault().dstSavings

fun setCalendarVMS(calendar: Calendar): String {
	val sdf = SimpleDateFormat(TIME_FORMAT_CLIENT, Locale(settings.getChosenLanguage()))
	val time = calendar.time
	return sdf.format(time)
}

fun getDateFormatPeriod(calendar: Calendar): String {
	val sdf = SimpleDateFormat(DATE_FORMAT_PERIOD, Locale(settings.getChosenLanguage()))
	val time = calendar.time
	return sdf.format(time)
}

fun setTimeForCalendar(calendar: Calendar, time: String): Calendar {
	val arrayTime = time.split(":")
	calendar.set(Calendar.HOUR_OF_DAY, arrayTime[0].toInt())
	calendar.set(Calendar.MINUTE, arrayTime[1].toInt())
	calendar.set(Calendar.SECOND, arrayTime[2].toInt())
	return calendar
}

fun setCurrentDateMarkCreate(calendar: Calendar): String {
	val monthArray = arrayOf(
		getStringForLayoutByKey("january"),
		getStringForLayoutByKey("february"),
		getStringForLayoutByKey("march"),
		getStringForLayoutByKey("april"),
		getStringForLayoutByKey("may"),
		getStringForLayoutByKey("june"),
		getStringForLayoutByKey("july"),
		getStringForLayoutByKey("august"),
		getStringForLayoutByKey("september"),
		getStringForLayoutByKey("october"),
		getStringForLayoutByKey("november"),
		getStringForLayoutByKey("december")
	)
	return "${calendar.get(Calendar.DAY_OF_MONTH)} ${monthArray[calendar.get(Calendar.MONTH)]}"
}

fun getOnlyTime(calendar: Calendar): String {
	val sdf = SimpleDateFormat(TIME, Locale(settings.getChosenLanguage()))
	val time = calendar.time
	return sdf.format(time)
}

fun setCalendarServer(calendar: Calendar): String {
	val sdf = SimpleDateFormat(TIME_FORMAT_SERVER, Locale(settings.getChosenLanguage()))
	val time = calendar.time
	return sdf.format(time)
}

fun setCalendarServerToUTC(tempCalendar: Calendar): String {
	val sdf = SimpleDateFormat(TIME_FORMAT_SERVER, Locale(settings.getChosenLanguage()))
	val calendar = tempCalendar.clone() as Calendar // avoid bug with change data removing offset
	calendar.timeInMillis = calendar.timeInMillis - getOffset()
	return sdf.format(calendar.time)
}

fun getCalendarExif(): String {
	val sdf = SimpleDateFormat(TIME_FORMAT_EXIF, Locale(settings.getChosenLanguage()))
	val calendar = Calendar.getInstance(Locale(settings.getChosenLanguage()))
	val time = calendar.time
	return sdf.format(time)
}

fun setCalendarFull(calendar: Calendar): String {
	val sdf = SimpleDateFormat(FULL_DATE_FORMAT, Locale(settings.getChosenLanguage()))
	val time = calendar.time
	return sdf.format(time)
}

fun setCalendarByDateServer(date: String): Calendar {
	return try {
		val sdf = SimpleDateFormat(TIME_FORMAT_SERVER, Locale(settings.getChosenLanguage()))
		val calendar = Calendar.getInstance(Locale(settings.getChosenLanguage()))
		calendar.time = sdf.parse(date)
		calendar
	} catch (e: Exception) {
		Calendar.getInstance()
	}
}

fun setCalendarByDatePeriod(date: String): Calendar {
	return try {
		val sdf = SimpleDateFormat(DATE_FORMAT_PERIOD, Locale(settings.getChosenLanguage()))
		val calendar = Calendar.getInstance(Locale(settings.getChosenLanguage()))
		calendar.time = sdf.parse(date)
		calendar
	} catch (e: Exception) {
		Calendar.getInstance()
	}
}

fun setCalendarByDateServerFromUTC(date: String): Calendar {
	return try {
		val sdf = SimpleDateFormat(TIME_FORMAT_SERVER, Locale(settings.getChosenLanguage()))
		val calendar = Calendar.getInstance(Locale(settings.getChosenLanguage()))
		calendar.time = sdf.parse(date)
		calendar.timeInMillis = calendar.timeInMillis + getOffset()
		calendar
	} catch (e: Exception) {
		Calendar.getInstance()
	}
}

fun setCalendarByDateClientToUTC(date: String): Calendar {
	return try {
		val sdf = SimpleDateFormat(TIME_FORMAT_SERVER, Locale(settings.getChosenLanguage()))
		val calendar = Calendar.getInstance(Locale(settings.getChosenLanguage()))
		calendar.time = sdf.parse(date)
		calendar.timeInMillis = calendar.timeInMillis - getOffset()
		calendar
	} catch (e: Exception) {
		Calendar.getInstance()
	}
}

fun setCalendarByDateServerTimeLine(date: String): Calendar {
	return try {
		val sdf =
			SimpleDateFormat(TIME_FORMAT_SERVER_TIMELINE, Locale(settings.getChosenLanguage()))
		val calendar = Calendar.getInstance(Locale(settings.getChosenLanguage()))
		calendar.time = sdf.parse(date)
		calendar.timeInMillis = calendar.timeInMillis + getOffset()
		calendar
	} catch (e: Exception) {
		Calendar.getInstance()
	}
}

fun isMarkOlderStartedArchive(dateMark: String, dateCamera: String): Boolean {
	return try {
		val c1 = setCalendarByDateServer(dateMark)
		val c2 = setCalendarByDateServer(dateCamera)
		c1.time.before(c2.time)
	} catch (e: Exception) {
		false
	}
}

fun getCalendarByDateServerMark(dateMark: String, dateStartCamera: String): Calendar {
	val c1 = setCalendarByDateServer(dateMark)
	val c2 = setCalendarByDateServer(dateStartCamera)
	return if (c1.time.before(c2.time)) c2 else c1
}

fun setCalendarByDateServerMarkFull(date: String): Calendar {
	val sdf = SimpleDateFormat(FULL_DATE_FORMAT, Locale(settings.getChosenLanguage()))
	val calendar = Calendar.getInstance(Locale(settings.getChosenLanguage()))
	calendar.time = sdf.parse(date)
	return calendar
}

fun setCalendarByLastAccessRange(lastRange: Calendar, dateStartCamera: String): Calendar {
	val dateStart = setCalendarByDateServer(dateStartCamera)
	return if (lastRange.time.before(dateStart.time)) dateStart else lastRange
}

fun compareDatesCalendarWithoutTime(c1: Calendar, c2: Calendar): Boolean {
	return if (c1.get(Calendar.YEAR) != c2.get(Calendar.YEAR)) false
	else
		if (c1.get(Calendar.MONTH) != c2.get(Calendar.MONTH)) false
		else c1.get(Calendar.DAY_OF_MONTH) == c2.get(Calendar.DAY_OF_MONTH)
}

fun compareDatesServerWithoutTime(s1: String?, s2: String?): Boolean {
	return try {
		if (s1 != null && s2 != null) {
			val c1 = setCalendarByDateServer(s1)
			val c2 = setCalendarByDateServer(s2)
			compareDatesCalendarWithoutTime(c1, c2)
		} else {
			false
		}
	} catch (e: Exception) {
		false
	}
}