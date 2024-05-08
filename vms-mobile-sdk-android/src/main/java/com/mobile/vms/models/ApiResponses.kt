package com.mobile.vms.models

import android.os.Parcelable
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import com.mobile.vms.player.helpers.EN
import kotlinx.parcelize.Parcelize
import java.io.Serializable


@Parcelize
data class VMSExternalAuthUrlResponse(
	@SerializedName("url") val url: String = ""
): Parcelable

@Parcelize
data class VMSLoginResponse(
	@SerializedName("access_token") val accessToken: String = "",
	@SerializedName("user") val user: VMSUser? = null,
	@SerializedName("sessions") val sessions: List<VMSSession>? = arrayListOf(),
	@SerializedName("sessions_limit") val sessionsLimit: Int? = 0,
	@SerializedName("key") val key: String? = null
): Parcelable

@Parcelize
data class VMSStreamsResponse(
	@SerializedName("fallback_url") val fallbackUrl: String? = null,
	@SerializedName("url") val url: String? = null,
	@SerializedName("move_to") val moveTo: String? = null
): Parcelable

@Parcelize
data class VMSUrlPreviewResponse(
	@SerializedName("preview") val preview: String = "",
	@SerializedName("status") val status: String? = null,
	@SerializedName("camera_id") val cameraId: Int? = null
): Parcelable

@Parcelize
data class VMSNearestEvent(
	@SerializedName("mark") val event: VMSEvent
): Parcelable

@Parcelize
class VMSStatusResponse: Parcelable {
	@SerializedName("status")
	var status: String? = null
	val isSuccess: Boolean
		get() = OK.equals(status, ignoreCase = true)

	companion object {
		val OK = "ok"
	}
}

@Parcelize
data class VMSEventCreateData(
	@SerializedName("title") val title: String = "",
	@SerializedName("from") val from: String = "",
	@SerializedName("to") val to: String? = null
): Parcelable

@Parcelize
data class VMSChildGroup(
	@SerializedName("id") val id: Int = 0,
	@SerializedName("items") val items: java.util.ArrayList<VMSCamera>? = null,
	@SerializedName("name") val name: String = "",
	@SerializedName("previews") val previews: java.util.ArrayList<VMSUrlPreviewResponse>? = null
): Parcelable


fun VMSChildGroup.toChildCamera() =
	VMSCameraTree(cameras = items, children = null, id = id, name = name, previews = previews)

fun transformToListChildCamera(arrayList: List<VMSChildGroup>): List<VMSCameraTree> {
	return arrayList.map { it.toChildCamera() }
}

data class VMSCreateGroupResponse(
	@SerializedName("id") val id: Int = 0,
	@SerializedName("items") val items: List<Any> = listOf(),
	@SerializedName("items_count") val itemsCount: Int = 0,
	@SerializedName("name") val name: String = ""
): Serializable

@Parcelize
data class VMSSimpleName(
	@SerializedName("name") var name: String
): Parcelable

@Parcelize
data class VMSGroupSync(
	@SerializedName("type") val type: String = "" // sync or async
): Parcelable

@Parcelize
data class VMSGroupSyncRequest(
	@SerializedName("groups") val groups: ArrayList<Int>
): Parcelable

@Parcelize
data class VMSUpdateGroupRequest(
	@SerializedName("name") val name: String,
	@SerializedName("items") val items: ArrayList<Int>
): Parcelable

@Parcelize
data class VMSChangePasswordRequest(
	@SerializedName("current_password") val currentPassword: String = "",
	@SerializedName("password") val password: String = "",
	@SerializedName("password_confirmation") val passwordConfirmation: String = ""
): Parcelable

@Parcelize
data class VMSVisitHistoryGroupData(
	@SerializedName("date") val date: String? = null,
	@SerializedName("history") val history: VMSIntercomCall? = null
): Parcelable

@Parcelize
data class VMSIntercomChangeTitleData(
	@SerializedName("title") val title: String
): Parcelable

@Parcelize
data class VMSIntercomSettingsDataEnabled(
    @SerializedName("is_enabled") val isEnabled: Boolean? = null,
    @SerializedName("timetable") val timetable: VMSTimetable? = null,
    @SerializedName("is_landline_sip_line_available") val isLandlineSipLineAvailable: Boolean? = null,
    @SerializedName("is_analog_line_available") val isAnalogLineAvailable: Boolean? = null,
): Parcelable

@Parcelize
data class VMSIntercomsDeleteData(
	@SerializedName("ids") val ids: ArrayList<Int>
): Parcelable

@Parcelize
data class VMSIntercomCodeData(
	@SerializedName("title") val title: String? = null,
	@SerializedName("expired_at") val expiredAt: String,
): Parcelable

@Parcelize
data class VMSVisitorsGroupData(
	@SerializedName("date") val date: String? = null,
	@SerializedName("visitor") val visitor: VMSCodeVisitor? = null
): Parcelable

@Parcelize
data class VMSVisitorAddData(
	@SerializedName("intercom") val intercom: VMSIntercom? = null,
	@SerializedName("number") val number: Int
): Parcelable

@Parcelize
data class VMSActivationCode(
	@SerializedName("code") val code: String,
	@SerializedName("expire_in_seconds") val expireInSeconds: Int
): Parcelable

@Parcelize
data class VMSIntercomFlatData(
	@SerializedName("flat") val flat: String
): Parcelable

@Parcelize
data class VMSIntercomPushEvent(@SerializedName("push") val push: VMSIntercomPush): Parcelable

@Parcelize
data class VMSIntercomPush(
	@SerializedName("intercom") val intercom: VMSIntercom,
	@SerializedName("expire_in_seconds") val expireInSeconds: Int,
	@SerializedName("id") val id: String,
	@SerializedName("type") val type: String,
	@SerializedName("error") val error: String? = ""
): Parcelable

@Parcelize
data class VMSStatics(
	@SerializedName("camera_issues") var cameraIssues: ArrayList<VMSCameraIssue>,
	@SerializedName("video_rates") var videoRates: ArrayList<Double>,
	@SerializedName("system_events") var systemEvents: ArrayList<VMSEventType>,
	@SerializedName("mark_types") var markTypes: ArrayList<VMSEventType>,
	@SerializedName("analytic_types") var analyticTypes: ArrayList<VMSEventType>, // for filter
	@SerializedName("analytic_events") var analyticEvents: ArrayList<VMSEventType>, // not use
): Parcelable

@Parcelize
data class VMSCameraIssue(
	@SerializedName("id") var id: Int,
	@SerializedName("title") val title: String
): Parcelable

@Parcelize
data class VMSErrors(
	@SerializedName("errors") val errors: MutableMap<String, List<String>> = HashMap(),
	@SerializedName("message") val message: String
): Parcelable

@Parcelize
data class VMSIntercomAnswer(
	@SerializedName("sip") val sip: String? = null,
	@SerializedName("host") val host: String? = null,
	@SerializedName("number") val number: String? = null
): Parcelable

@Parcelize
data class VMSBasicStatic(
	@SerializedName("available_locales") val availableLocales: Set<String>? = null,
	@SerializedName("is_captcha_available") val isCaptchaAvailable: Boolean? = null,
	@SerializedName("is_external_auth_enabled") val isExternalAuthEnabled: Boolean? = null,
	@SerializedName("version") val version: String? = null,
): Parcelable

@Parcelize
data class VMSCaptcha(
	@SerializedName("img") val img: String,
	@SerializedName("key") val key: String,
	@SerializedName("ttl") val ttl: Int
): Parcelable

@Parcelize
data class VMSLanguageName(
	@SerializedName("language") var language: String
): Parcelable

data class VMSTranslations(
	@SerializedName("json") var json: JsonElement? = null,
	@SerializedName("language") val language: String? = EN,
	@SerializedName("revision") val revision: Int?
)
