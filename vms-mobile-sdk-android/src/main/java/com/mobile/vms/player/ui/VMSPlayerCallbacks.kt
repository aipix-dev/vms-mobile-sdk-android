package com.mobile.vms.player.ui

import android.graphics.Bitmap
import com.mobile.vms.models.VMSCamera
import com.mobile.vms.network.ApiError
import java.util.*

interface VMSPlayerCallbackCameraEvents {
	/**
	 * To open all the events of this camera.
	 */
	fun onClickOpenEvents(data: VMSCamera)
}

interface VMSPlayerCallbackEventsTypes {
	/**
	 * To change displayed event's types.
	 */
	var chosenEventsTypes: List<Pair<String, String>>
	fun onChooseEventsTypes(data: List<Pair<String, String>>)
}

interface VMSPlayerCallbackScreenshot {
	/**
	 * To capture a screenshot of the camera displaying the current broadcast date.
	 */
	fun onClickScreenshot(
		bitmap: Bitmap,
		camera: VMSCamera,
		time: Calendar,
		state: VMSScreenState = VMSScreenState.DEFAULT
	)
}

interface VMSPlayerCallbackLogEvent {
	/**
	 * If you want to log user activity, this method provides the action's tag names to transfer
	 * to your app.
	 */
	fun onLogEvent(value: String)
}

interface VMSPlayerCallbackErrors {
	/**
	 * If you want to handle any error.
	 */
	fun onHandleErrors(error: ApiError)
}

interface VMSPlayerCallbackVideoType {
	/**
	 * To save video type in your app.
	 */
	var videoType: String
	fun onSaveVideoType(videoType: String)
}

interface VMSPlayerCallbackVideoQuality {
	/**
	 * To save video quality in your app.
	 */
	var videoQuality: String
	fun onSaveVideoQuality(videoQuality: String)
}