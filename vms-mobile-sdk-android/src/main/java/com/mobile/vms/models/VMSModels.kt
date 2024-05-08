package com.mobile.vms.models

import android.os.Parcelable
import com.mobile.vms.player.ui.VMSScreenState
import kotlinx.parcelize.*

@Parcelize
data class VMSPlayerData(
	val camera: VMSCamera,
	val list: ArrayList<VMSCamera>? = null,
	val event: VMSEvent? = null,
	val jsonTranslations: @RawValue com.google.gson.JsonObject? = null,
	val videoRates: ArrayList<Double>,
	val markTypes: ArrayList<VMSEventType>,
	val permissions: List<VMSPermission>,
	val allowVibration: Boolean = true,
	val screenState: VMSScreenState = VMSScreenState.DEFAULT,
): Parcelable
