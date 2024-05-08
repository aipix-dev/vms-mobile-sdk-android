package com.mobile.vms.models

import com.google.gson.annotations.SerializedName

data class VMSLoginRequest(
	val login: String,
	val password: String,
	@SerializedName("session_id") val sessionId: String? = null,
	val captcha: String? = null,
	val key: String? = null,
)

data class VMSFcmRequest(@SerializedName("fcm_token") val fcmToken: String)

data class VMSHuaweiRequest(@SerializedName("huawei_token") val huaweiToken: String)

data class VMSLogoutRequest(private val uuid: String)

data class VMSExternalAuthCodeRequest(
	val code: String
)

data class VMSExternalAuthCodeSessionRequest(
	val key: String,
	@SerializedName("session_id") val sessionId: String
)

data class VMSRenameCameraRequest(val name: String)

data class VMSAddBridgeRequest(
	@SerializedName("name") val name: String,
	@SerializedName("mac") val mac: String?,
	@SerializedName("serial_number") val serialNumber: String?,
)

data class VMSRenameBridgeRequest(
	@SerializedName("name") val name: String,
)