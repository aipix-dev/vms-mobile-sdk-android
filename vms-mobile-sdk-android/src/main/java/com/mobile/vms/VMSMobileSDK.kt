package com.mobile.vms

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager.*
import android.os.Build.*
import com.mobile.vms.network.VMSClientApi
import com.mobile.vms.player.helpers.*
import com.mobile.vms.socket.VMSPusherApi

val settings: VMSSettings by lazy {
	VMSMobileSDK.settings
}

/**
 * Main class for initialization sdk to use client api, pusher api, hls & rtsp player.
 */
object VMSMobileSDK {

	var instance: VMSMobileSDK = this
	lateinit var app: Application
	lateinit var pusherApi: VMSPusherApi
	lateinit var clientApi: VMSClientApi
	lateinit var settings: VMSSettings
	var isVisible: Boolean = false

	var baseUrl: String = ""
		set(value) {
			field = value
			if (VMSMobileSDK::clientApi.isInitialized) clientApi.baseUrl = value
			if (VMSMobileSDK::pusherApi.isInitialized) pusherApi.baseUrl = value
		}

	var userToken: String = ""
		set(value) {
			field = value
			if (VMSMobileSDK::clientApi.isInitialized) clientApi.userToken = value
			if (VMSMobileSDK::pusherApi.isInitialized) pusherApi.userToken = value
		}

	var userId: String = ""
		set(value) {
			field = value
			if (VMSMobileSDK::pusherApi.isInitialized) pusherApi.userId = value
		}

	var accessTokenId: String = ""
		set(value) {
			field = value
			if (VMSMobileSDK::pusherApi.isInitialized) pusherApi.accessTokenId = value
		}

	var language: String = EN
		set(value) {
			field = value
			if (VMSMobileSDK::clientApi.isInitialized) clientApi.language = value
		}

	fun Builder(
		application: Application,
		baseUrl: String,
		uuid: String,
		language: String? = null
	) {
		this.app = application
		this.language = language ?: EN
		val url = baseUrl.getValidBaseUrl()
		VMSMobileSDK.baseUrl = url
		clientApi = VMSClientApi.Builder(url, uuid, language)
		pusherApi = VMSPusherApi
		settings = VMSSettings
	}

	@JvmStatic
	internal val isDebuggable: Boolean by lazy {
		isDebuggableApp(app)
	}

	private fun isDebuggableApp(app: Application): Boolean {
		var debuggable = false
		try {
			val appInfo = if (VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
				app.packageManager
					.getApplicationInfo(app.packageName, ApplicationInfoFlags.of(0))
			} else {
				app.packageManager.getApplicationInfo(app.packageName, GET_META_DATA)
			}
			debuggable = 0 != appInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE
		} catch (e: NameNotFoundException) {
			e.printStackTrace()
			/* debuggable variable will remain false */
		}
		return debuggable
	}

}