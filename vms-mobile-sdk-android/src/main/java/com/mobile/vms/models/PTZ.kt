package com.mobile.vms.models

import com.google.gson.annotations.SerializedName

data class PTZ_Up(val up: Boolean = true)

data class PTZ_Down(val down: Boolean = true)

data class PTZ_Right(val right: Boolean = true)

data class PTZ_Left(val left: Boolean = true)

data class PTZ_Zoom_in(
	@SerializedName("zoom-in")
	val zoomIn: Boolean = true
)

data class PTZ_Zoom_out(
	@SerializedName("zoom-out")
	val zoomOut: Boolean = true
)
