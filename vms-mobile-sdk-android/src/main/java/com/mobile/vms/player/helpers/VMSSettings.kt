package com.mobile.vms.player.helpers

import com.google.gson.*
import com.mobile.vms.models.VMSEventType
import org.json.*
import java.util.*

object VMSSettings {

	private var dataServer: JsonObject? = null
	var videoRates = ArrayList<Double>()
	var markTypes = ArrayList<VMSEventType>()
	var neverShowWifiForCurrentSession = false
	var enabledAudio = false
	var needVibration = true
	var neverShowWifi = false
	var videoType = VIDEO_TYPE_RTSP
	var videoQuality = HIGH
	var chosenEventsTypes: List<Pair<String, String>> =
		listOf(Pair(EVENTS_SHOW_ALL, getStringForLayoutByKey("display_all")))

	fun getStringForLayoutByKey(key: String) = getJsonTranslations().getByKeyOrKeySave(key) ?: ""

	fun saveTranslations(json: JsonObject?) {
		try {
			if (json == null || json.toString().isEmpty()) return
			val mergedObj = JSONObject()
			var key: String

			if (getJsonTranslations() != null) {
				//write all existed translations from prefs
				val jsonObjectOld = JSONObject(getJsonTranslations().toString())
				val i2: Iterator<*> = jsonObjectOld.keys()
				while (i2.hasNext()) {
					key = i2.next() as String
					mergedObj.put(key, jsonObjectOld.get(key))
				}
			}

			//write or rewrite new translations from server
			val jsonObjectNew = JSONObject(json.toString())
			val i1: Iterator<*> = jsonObjectNew.keys()
			while (i1.hasNext()) {
				key = i1.next() as String
				mergedObj.put(key, jsonObjectNew.get(key))
			}

			val lineMerged = mergedObj.toString()
			val gsonObject = JsonParser().parse(lineMerged) as JsonObject
			dataServer = gsonObject
		} catch (e: JSONException) {
			e.printStackTrace()
		}
	}

	private fun getJsonTranslations(): JsonObject? {
		try {
			if (dataServer == null) {
				dataServer = JsonObject()
			}
			return dataServer
		} catch (e: Exception) {
			e.printStackTrace()
		}
		return null
	}

	private fun JsonObject?.getByKeyOrKeySave(key: String): String? {
		return try {
			this?.get(key)?.asString ?: if (key.contains("_")) "" else key
		} catch (e: Exception) {
			null
		}
	}

	fun getChosenLanguage(): String {
		return when (Locale.getDefault().language) {
			"ru" -> RU
			else -> EN
		}
	}

	fun hasEventsToShow() = !(chosenEventsTypes.map { it.first }.contains(EVENTS_NOT_SHOW))
	fun hasAllEventsToShow() = chosenEventsTypes.map { it.first }.contains(EVENTS_SHOW_ALL)
	fun isRtspVideo() = videoType == VIDEO_TYPE_RTSP

}
