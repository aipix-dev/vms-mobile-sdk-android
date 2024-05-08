package com.mobile.vms.player.helpers

import android.annotation.SuppressLint
import android.app.*
import android.content.*
import android.content.pm.*
import android.content.res.Configuration
import android.graphics.*
import android.graphics.drawable.Drawable
import android.media.*
import android.net.*
import android.os.*
import android.os.Build.*
import android.text.*
import android.util.*
import android.view.*
import android.view.animation.*
import android.widget.*
import androidx.annotation.*
import androidx.core.app.*
import androidx.core.content.*
import com.mobile.vms.*
import com.mobile.vms.models.VMSArchiveRange
import com.mobile.vms.player.*
import com.mobile.vms.player.helpers.*
import io.reactivex.*
import java.io.*
import java.util.*

fun changeStatusBarByKeyVMS(activity: Activity, key: String) {
	activity.window?.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
	activity.window?.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS and WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
	activity.window?.statusBarColor = getColorCompat(
		activity,
		when (key) {
			KEY_SCREENSHOT_DETAIL_EXPANDED -> {
				if (VERSION.SDK_INT >= VERSION_CODES.O) {
					if (activity.resources?.configuration?.orientation == Configuration.ORIENTATION_PORTRAIT) {
						activity.window?.decorView?.systemUiVisibility =
							View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
						activity.window?.navigationBarColor =
							getColorCompat(activity, R.color.white)
					} else {
						activity.window?.navigationBarColor =
							getColorCompat(activity, R.color._131418)
					}
				}
				R.color._131418
			}

			KEY_SCREENSHOT_DETAIL_HIDDEN -> {
				activity.window?.decorView?.systemUiVisibility = 0
				if (VERSION.SDK_INT >= VERSION_CODES.O) {
					activity.window?.navigationBarColor =
						getColorCompat(activity, R.color._1F2128)
				}
				R.color._1F2128
			}

			KEY_PRIMARY_BRAND_1 -> {
				activity.window?.decorView?.systemUiVisibility = 0
				if (VERSION.SDK_INT >= VERSION_CODES.O) {
					activity.window?.navigationBarColor =
						getColorCompat(activity, R.color.colorPrimary)
				}
				R.color.colorPrimary
			}

			KEY_PRIMARY_BRAND_2 -> {
				activity.window?.decorView?.systemUiVisibility = 0
				if (VERSION.SDK_INT >= VERSION_CODES.O) {
					activity.window?.navigationBarColor =
						getColorCompat(activity, R.color.color_black_brand_2)
				}
				R.color.color_black_brand_2
			}

			KEY_INTERCOM_BG -> {
				activity.window?.decorView?.systemUiVisibility = 0
				if (VERSION.SDK_INT >= VERSION_CODES.O) {
					activity.window?.navigationBarColor =
						getColorCompat(activity, R.color.black_player_bg)
				}
				R.color.black_player_bg
			}

			KEY_INTERCOM_VISITOR_TIME -> {
				R.color.black_alpha_double
			}

			else -> R.color.white
		}
	)
}

fun isLocaleCorrect(activity: Activity): Boolean {
	val locale = if (VERSION.SDK_INT >= VERSION_CODES.N) {
		activity.resources.configuration.locales.get(0)
	} else {
		activity.resources.configuration.locale
	}
	return locale.language == Locale(VMSSettings.getChosenLanguage()).language
}

fun changeLocale(activity: Activity) {
	val res = activity.resources
	val config = res.configuration
	val locale = Locale(VMSSettings.getChosenLanguage())

	if (VERSION.SDK_INT >= VERSION_CODES.N) {
		config.setLocale(locale)
		val localeList = LocaleList(locale)
		config.setLocales(localeList)
	} else {
		config.setLocale(locale)
	}
	activity.createConfigurationContext(config)
}

@SuppressLint("MissingPermission")
fun isOnline(context: Context): Boolean {
	val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
	val netInfo = cm.activeNetworkInfo
	//should check null because in airplane mode it will be null
	return netInfo != null && netInfo.isConnected
}

fun isPortrait(context: Context?): Boolean {
	return context?.resources?.configuration?.orientation == Configuration.ORIENTATION_PORTRAIT
}

fun getChosenMarkTypes(): List<String> {
	return if (settings.hasAllEventsToShow()) VMSSettings.markTypes.map { it.name ?: "" }
	else settings.chosenEventsTypes.map { it.first }
}

fun cutUnreachableArchiveRanges(
	list: List<VMSArchiveRange>?,
	startAt: String?
): ArrayList<VMSArchiveRange> {
	if (list.isNullOrEmpty() || startAt.isNullOrEmpty()) return arrayListOf()  //if no archive ranges -> return empty list
	val newList = arrayListOf<VMSArchiveRange>()
	val startAtTime = try {   //camera start at time in seconds
		(setCalendarByDateServer(startAt).timeInMillis / 1000).toInt()
	} catch (e: Exception) {
		(Calendar.getInstance().timeInMillis / 1000).toInt()
	}
	loop@ for ((position, item) in list.withIndex()) {
		if (item.from >= startAtTime) {  //all ranges started from current are active
			newList.addAll(list.subList(position, list.size))
			break@loop
		} else {
			val itemEnd = item.from + item.duration
			if (startAtTime in item.from..itemEnd) {  //range will start from start_at
				newList.add(VMSArchiveRange(itemEnd - startAtTime, startAtTime))
			}
		}
	}
	return newList
}

fun getDrawableCompat(context: Context, id: Int): Drawable? {
	return ContextCompat.getDrawable(context, id)
}

fun getColorCompat(context: Context, id: Int): Int {
	return ContextCompat.getColor(context, id)
}

fun logSdk(tag: String, message: String) {
	if (VMSMobileSDK.isDebuggable) {
		val stackTrace = Thread.currentThread().stackTrace
		var methodName = "unknown"
		if (stackTrace.size > 3) {
			// stackTrace[0] == getStackTrace
			// stackTrace[1] == getThreadStackTrace
			// stackTrace[2] == logSdk
			methodName = stackTrace[3].methodName
		}

//		if (stackTrace.size > 4) {
//			methodName = "$methodName() ${stackTrace[4].methodName}"
//		}
//
//		if (stackTrace.size > 5) {
//			methodName = "$methodName() ${stackTrace[5].methodName}"
//		}
//
//		if (stackTrace.size > 6) {
//			methodName = "$methodName() ${stackTrace[6].methodName}"
//		}

		Log.d(tag, "$methodName: $message")
	}
}

fun createVibration(needVibration: Boolean, context: Context, vibrateTime: Long = 50L, vibrateAmplitude: Int = 10) {
	if (!needVibration) return
	val vibration = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
	val hasVibration = vibration.hasVibrator()   //is phone has vibration
	if (hasVibration) {  //if have vibration
		val hasAmplitudeControl =
			vibration.hasAmplitudeControl()    //is phone has amplitude control
		val amplitude = if (hasAmplitudeControl) vibrateAmplitude
		else VibrationEffect.DEFAULT_AMPLITUDE
		vibration.vibrate(VibrationEffect.createOneShot(vibrateTime, amplitude))
	}
}

fun String.getValidBaseUrl() = if (this.endsWith("/")) this else "$this/"

fun String.getBaseUrlShort() = if (this.endsWith("/")) this.dropLast(1) else this

fun String.getValidImageUrl(baseUrl: String) = if (this.startsWith("https:")) this else "$baseUrl$this"

