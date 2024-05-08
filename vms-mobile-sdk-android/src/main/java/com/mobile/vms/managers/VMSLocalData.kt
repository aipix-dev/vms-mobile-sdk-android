package com.mobile.vms.managers

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.mobile.vms.models.VMSEventType
import org.json.*
import java.util.*

object VMSLocalData {

	private var dataServer: JsonObject? = null
	private var eventsShowedList: List<Pair<String, String>>? = null
	private var dataStreamShowedEvents = ""
	private var gson = Gson()
	const val EVENTS_NOT_SHOW = "EVENTS_NOT_SHOW"
	const val EVENTS_SHOW_ALL = "EVENTS_SHOW_ALL"
	const val RU = "ru"
	const val EN = "en"
	var videoRates = ArrayList<Double>()
	var markTypes = ArrayList<VMSEventType>()
	var neverShowWifiForCurrentSession = false
	var enabledAudio = false
	var needVibration = true
	var neverShowWifi = false
	var videoType = "hls"

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

	var streamShowedEvents: List<Pair<String, String>>   // type, name (need both)
		get() = try {
			if (eventsShowedList == null) {
				val type = object: TypeToken<List<Pair<String, String>>>() {}.type
				eventsShowedList =
					gson.fromJson<List<Pair<String, String>>>(dataStreamShowedEvents, type)
			}
			eventsShowedList!!
		} catch (e: Exception) {
			eventsShowedList = listOf(Pair(EVENTS_SHOW_ALL, getStringForLayoutByKey("display_all")))
			eventsShowedList!!
		}
		set(value) {
			this.eventsShowedList = value
			val dataJson = gson.toJson(eventsShowedList)
			dataStreamShowedEvents = dataJson
		}

	fun hasEventsToShow() = !(streamShowedEvents.map { it.first }.contains(EVENTS_NOT_SHOW))
	fun hasAllEventsToShow() = streamShowedEvents.map { it.first }.contains(EVENTS_SHOW_ALL)
	fun isRtspVideo() = false // videoType == "rtsp"

}
