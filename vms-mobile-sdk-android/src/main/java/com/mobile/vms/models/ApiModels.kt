package com.mobile.vms.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.mobile.vms.VMSMobileSDK
import com.mobile.vms.player.helpers.*
import kotlinx.parcelize.Parcelize
import java.io.Serializable

class VMSPaginatedResponse<T>(
	@SerializedName("data") val `data`: List<T>? = null,
	@SerializedName("links") val links: VMSLinks,
	@SerializedName("meta") val meta: VMSMeta
)

@Parcelize
data class VMSMeta(
	@SerializedName("current_page") val currentPage: Int = 0,
	@SerializedName("from") val from: Int = 0,
	@SerializedName("last_page") val lastPage: Int = 0,
	@SerializedName("path") val path: String = "",
	@SerializedName("per_page") val perPage: Int = 0,
	@SerializedName("to") val to: Int = 0,
	@SerializedName("total") val total: Int = 0
): Parcelable

@Parcelize
data class VMSLinks(
	@SerializedName("first") val first: String? = "",
	@SerializedName("last") val last: String? = "",
	@SerializedName("next") val next: String? = null,
	@SerializedName("prev") val prev: String? = null
): Parcelable

@Parcelize
data class VMSCamera(
	@SerializedName("id") val id: Int = 0,
	@SerializedName("status") val status: String? = null,
	@SerializedName("type") val type: String? = null,
	@SerializedName("start_at") val startAt: String? = "",
	@SerializedName("created_at") val createdAt: String? = null,
	@SerializedName("name") var name: String? = null,
	@SerializedName("pretty_name") val prettyName: String? = null,
	@SerializedName("pretty_text") val prettyText: String? = null,
	@SerializedName("short_address") val shortAddress: String? = null,
	@SerializedName("address") val address: String? = null,
	@SerializedName("full_address") val fullAddress: String? = null,
	@SerializedName("streams") val streams: List<VMSStream>? = null,
	@SerializedName("services") val services: VMSServices? = null,
	@SerializedName("group") val group: String? = null,
	@SerializedName("has_sound") var hasSound: Boolean? = null,
	@SerializedName("is_favorite") var isFavorite: Boolean? = null,
	@SerializedName("is_bridge") var isBridge: Boolean? = null,
	@SerializedName("user_status") var userStatus: String = "active",
	@SerializedName("archive_ranges") val archiveRanges: List<VMSArchiveRange>? = null,
	@SerializedName("ip") val ip: String? = null,
	@SerializedName("is_restricted_live") val isRestrictedLive: Boolean? = null,
	@SerializedName("is_restricted_archive") val isRestrictedArchive: Boolean? = null,
	open var isChosen: Boolean = false, // to save state for filters
): Parcelable, VMSAnalyticTypeProtocol {
	override fun getIdOfType(): Int = id

	override fun typeName(): String = prettyName ?: name ?: ""

	override fun typeTitle(): String = name ?: ""

	override fun typeFor(): String = type ?: ""

	fun getStartAtLocal() = startAt?.getLocalDateFromUtc() ?: ""

	override fun setIsChosen(isChosen: Boolean) {
		this.isChosen = isChosen
	}
}

@Parcelize
data class VMSSession(
	@SerializedName("id") val id: String,
	@SerializedName("online") val online: Boolean,
	@SerializedName("last") val last: Int?,
	@SerializedName("user_agent") val userAgent: String?,
	@SerializedName("client") val client: String?,
	@SerializedName("ip") val ip: String?,
	@SerializedName("is_current") val isCurrent: Boolean = false
): Parcelable

@Parcelize
data class VMSUser(
	@SerializedName("id") val id: Int = 0,
	@SerializedName("login") val login: String = "",
	@SerializedName("name") val name: String = "",
	@SerializedName("parent_id") val parentId: Int? = null,
	@SerializedName("permissions") val permissions: List<VMSPermission> = listOf(),
	@SerializedName("has_legals") val hasLegals: Boolean = false,
	@SerializedName("created_at") val createdAt: String = "",
	@SerializedName("updated_at") val updatedAt: String = "",
	@SerializedName("access_token_id") val accessTokenId: String? = null,
	@SerializedName("can_update_password") val canUpdatePassword: Boolean = false
): Parcelable

@Parcelize
data class VMSPermission(
	@SerializedName("description") val description: String? = "",
	@SerializedName("display_name") val displayName: String? = "",
	@SerializedName("id") val id: Int? = 0,
	@SerializedName("name") val name: String? = "",
	@SerializedName("user_type") val userType: String? = "",
	@SerializedName("group") val group: String? = ""
): Parcelable

@Parcelize
data class VMSSocketResponse(
	@SerializedName("ws_url") val wsUrl: String,
	@SerializedName("app_key") val appKey: String,
	@SerializedName("port") val port: Int,
	@SerializedName("host") val host: String,
	@SerializedName("path") val path: String,
	@SerializedName("is_encrypted") val isEncrypted: String,
): Parcelable

@Parcelize
class VMSCameraTree(
	@SerializedName("cameras") val cameras: java.util.ArrayList<VMSCamera>? = null,
	@SerializedName("children") val children: java.util.ArrayList<VMSCameraTree>? = null, // here
	@SerializedName("previews") val previews: java.util.ArrayList<VMSUrlPreviewResponse>? = null, // here
	@SerializedName("id") val id: Int = 0,
	@SerializedName("name") val name: String = "",
	@SerializedName("main_name") val mainName: String = "",
	@SerializedName("sub_main") val subMain: String? = null,
	@SerializedName("items_count") val itemsCount: String? = null
): Parcelable {
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as VMSCameraTree

		if (cameras != other.cameras) return false
		if (children != other.children) return false
		if (previews != other.previews) return false
		if (id != other.id) return false
		if (name != other.name) return false
		if (mainName != other.mainName) return false
		if (subMain != other.subMain) return false
		if (itemsCount != other.itemsCount) return false

		return true
	}

	override fun hashCode(): Int {
		var result = cameras?.hashCode() ?: 0
		result = 31 * result + (children?.hashCode() ?: 0)
		result = 31 * result + (previews?.hashCode() ?: 0)
		result = 31 * result + id
		result = 31 * result + name.hashCode()
		result = 31 * result + mainName.hashCode()
		result = 31 * result + (subMain?.hashCode() ?: 0)
		result = 31 * result + (itemsCount?.hashCode() ?: 0)
		return result
	}
}

@Parcelize
data class VMSStream(
	@SerializedName("id") val id: Int? = null,
	@SerializedName("type") val type: String? = null,
	@SerializedName("camera_id") val cameraId: Int? = null,
	@SerializedName("is_preview_from_server") val isPreviewFromServer: Boolean? = null,
	@SerializedName("is_archive_from_server") val isArchiveFromServer: Boolean? = null,
	@SerializedName("status") val status: String? = null,
	@SerializedName("has_sound") val hasSound: Boolean? = null,
	@SerializedName("width") val width: String? = null,
	@SerializedName("height") val height: String? = null,
	@SerializedName("video_codec") val videoCodec: String? = null,
	@SerializedName("audio_codec") val audioCodec: String? = null,
): Parcelable

@Parcelize
data class VMSServices(
	@SerializedName("motion_detect") val motionDetect: Boolean? = null,
	@SerializedName("ptz") val ptz: Boolean? = null
): Parcelable

@Parcelize
data class VMSIntercom(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("title") var title: String? = null,
    @SerializedName("is_online") var isOnline: Boolean? = null,
    @SerializedName("department") val department: Int? = null,
    @SerializedName("address") val address: String? = null,
    @SerializedName("camera") val camera: VMSCamera? = null,
    @SerializedName("available_answer_devices") var availableAnswerDevices: List<String>? = null,    // devices for answering
    @SerializedName("is_enabled") var isEnabled: Boolean? = null,    // mobile device
    @SerializedName("timetable") var timetable: VMSTimetable? = null,
    @SerializedName("is_landline_sip_line_available") var isLandlineSipLineAvailable: Boolean? = null,    // line phone device
    @SerializedName("is_analog_line_available") var isAnalogLineAvailable: Boolean? = null,    // analog device
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("is_analytic_available") val isAnalyticAvailable: Boolean? = null,
    @SerializedName("is_open_door_key") val isOpenDoorKey: Boolean? = null,    // open door from key
    @SerializedName("is_open_door_code") val isOpenDoorCode: Boolean? = null,    // codes for visitors tab
    @SerializedName("is_open_door_app") val isOpenDoorApp: Boolean? = null,    // open door from button inside app
    @SerializedName("is_open_door_face") val isOpenDoorFace: Boolean? = null,    // intercom events tab and photo for opening
): Parcelable

@Parcelize
data class VMSTimetable(
	@SerializedName("days") val days: ArrayList<VMSTimetableDay> = ArrayList(), // set arraylist by default if you set interval
	@SerializedName("intervals") val intervals: ArrayList<VMSTimetableInterval>? = null
): Parcelable

@Parcelize
data class VMSTimetableDay(
	@SerializedName("type") val type: String,
	@SerializedName("from") val from: String?,
	@SerializedName("to") val to: String?
): Parcelable

@Parcelize
data class VMSTimetableInterval(
	@SerializedName("from") val from: String? = "",
	@SerializedName("to") val to: String? = ""
): Parcelable

@Parcelize
data class VMSIntercomCall(
	@SerializedName("id") val id: Int,
	@SerializedName("status") val status: String? = null,
	@SerializedName("type") val type: String? = null,
	@SerializedName("mark_type") val markType: String? = null,
	@SerializedName("created_at") val createdAt: String? = null,
	@SerializedName("intercom") val intercom: VMSIntercom? = null,
	@SerializedName("started_at") val startedAt: String? = null,
	@SerializedName("ended_at") val endedAt: String? = null
): Parcelable {

	fun getFromLocal() = createdAt?.getLocalDateFromUtc() ?: ""
}

//@Parcelize // VMSVisitHistory
//data class VMSIntercomCall(
//	@SerializedName("status") val status: String? = null,
//): Parcelable

@Parcelize
data class VMSCodeVisitor(
	@SerializedName("code") val code: String? = null,
	@SerializedName("created_at") val createdAt: String? = null,
	@SerializedName("expired_at") val expiredAt: String,
	@SerializedName("id") val id: Int,
	@SerializedName("intercom") val intercom: VMSIntercom? = null,
	@SerializedName("title") val title: String,
	@SerializedName("is_expired") val isExpired: Boolean,
	@SerializedName("will_deleted_at") val willDeletedAt: String,
): Parcelable {
	fun getFromLocal() = createdAt?.getLocalDateFromUtc() ?: ""
}

@Parcelize
data class VMSArchiveRange(
	@SerializedName("duration") val duration: Int,
	@SerializedName("from") val from: Int
): Parcelable

@Parcelize
open class VMSEvent(
	@SerializedName("id") open val id: Int? = null,
	@SerializedName("type") open val type: String? = null, // types: mark, motion_detect_smtp
	@SerializedName("created_at") open val createdAt: String? = null,
	@SerializedName("camera") open var camera: VMSCamera? = null,
	@SerializedName("camera_id") open val cameraId: Int? = null,
	@SerializedName("title") open val title: String? = null,
	@SerializedName("from") open val from: String? = null,
	@SerializedName("name") open val name: String? = null,
	@SerializedName("to") open val to: String? = null,
	@SerializedName("can_delete") open val canDelete: Boolean = false,
	@SerializedName("preview_url") open val previewUrl: String? = null,
	@SerializedName("type_pretty") open val typePretty: String? = null, // name analytic case
	@SerializedName("user") val user: VMSUser? = null,
	//analytic cases
	@SerializedName("analytic_group") val analyticGroup: VMSAnalyticGroup? = null,
	@SerializedName("analytic_case") val analyticCase: VMSAnalyticCase? = null, // include reactions and types of events - here color Face not recognize
	@SerializedName("analytic_file") val analyticFile: VMSAnalyticFile? = null, // detect photo face - there are 2 photos
	@SerializedName("crop") val crop: String? = null, // second photo and for sign of car
	@SerializedName("event") val event: VMSAnalyticEvent? = null, // second field color
	@SerializedName("similarity") val similarity: Double? = null, // percent
	@SerializedName("rect") val rect: Int? = null, // count of people
	@SerializedName("uuid") val uuid: String? = null,
	@SerializedName("is_intercom") val isIntercom: Boolean? = null,
	@SerializedName("container_code") val containerCode: String? = null, // case container recognition
): Parcelable, VMSAnalyticTypeProtocol {
	open fun getFromLocal() = from?.getLocalDateFromUtc() ?: getCreatedAtLocal()
	open fun getCreatedAtLocal() = createdAt?.getLocalDateFromUtc() ?: ""

	override fun getIdOfType(): Int = id ?: 0

	override fun typeName(): String = name ?: ""

	override fun typeTitle(): String = title ?: typeName()

	override fun typeFor(): String = type ?: ""

	override fun setIsChosen(isChosen: Boolean) {}
}

@Parcelize
data class VMSAnalyticGroup(
	@SerializedName("id") val id: Int? = null,
	@SerializedName("name") val name: String? = null,
	@SerializedName("type") val type: String? = null,
	@SerializedName("type_pretty") val typePretty: String? = null,
	@SerializedName("uuid") val uuid: String? = null,
	@SerializedName("created_at") val createdAt: String? = null,
	@SerializedName("updated_at") val updatedAt: String? = null
): Parcelable

@Parcelize
data class VMSAnalyticFile(
	@SerializedName("id") val id: Int,
	@SerializedName("name") val name: String = "",
	@SerializedName("type") val type: String,
	@SerializedName("type_pretty") val typePretty: String = "",
	@SerializedName("uuid") val uuid: String,
	@SerializedName("body") val body: String,
	@SerializedName("url") val url: String = "",
	@SerializedName("created_at") val createdAt: String,
	@SerializedName("updated_at") val updatedAt: String,
): Parcelable {
	fun getValidUrl() = url.getValidImageUrl(VMSMobileSDK.baseUrl.getBaseUrlShort())
}

@Parcelize
data class VMSAnalyticEvent(
	@SerializedName("analytic_type") val analyticType: String? = null,
	@SerializedName("color") val color: String? = null,
	@SerializedName("description") val description: String? = null,
	@SerializedName("id") val id: Int? = null,
	@SerializedName("name") val name: String? = null,
	@SerializedName("type_pretty") val typePretty: String? = null,
): Parcelable

@Parcelize
data class VMSAnalyticCase(
	@SerializedName("available_events") val availableEvents: List<VMSEventType>,
	@SerializedName("cameras") val cameras: List<VMSCamera>? = null,
	@SerializedName("color") val color: String? = null,
	@SerializedName("created_at") val createdAt: String? = null,
	@SerializedName("id") val id: Int? = null,
	@SerializedName("is_fails_exists") val isFailsExists: Boolean? = null,
	@SerializedName("status") val status: String? = null,
	@SerializedName("title") val title: String? = null,
	@SerializedName("type") val type: String? = null,
	@SerializedName("type_pretty") val typePretty: String? = null,
	@SerializedName("updated_at") val updatedAt: String? = null,
): Parcelable, VMSAnalyticTypeProtocol {
	override fun getIdOfType(): Int = id ?: 0

	override fun typeName(): String = typePretty ?: ""

	override fun typeTitle(): String = title ?: typeName()

	override fun typeFor(): String = type ?: ""

	override fun setIsChosen(isChosen: Boolean) {}
}

@Parcelize
data class VMSCoordinate(
	@SerializedName("x") val x: Double,
	@SerializedName("y") val y: Double
): Parcelable

@Parcelize
data class VMSEventType(
	@SerializedName("id") val id: Int? = null,
	@SerializedName("type") val type: String? = null, // for all events
	@SerializedName("name") val name: String? = null, // for all events
	@SerializedName("title") val title: String? = null, // main name instead description
	@SerializedName("color") val color: String? = null,
	@SerializedName("description") val description: String? = null, // will remove
	@SerializedName("analytic_type") val analyticType: String? = null,
	var isChosen: Boolean = false,
): Parcelable, VMSAnalyticTypeProtocol {
	override fun getIdOfType(): Int = id ?: 0

	override fun typeName(): String = name ?: typeFor()

	override fun typeTitle(): String = title ?: description ?: typeName()

	override fun typeFor(): String = type ?: ""

	override fun setIsChosen(isChosen: Boolean) {
		this.isChosen = isChosen
	}
}

@Parcelize
data class VMSBridge(
	@SerializedName("id") val id: Int? = null,
	@SerializedName("name") val name: String? = null,
	@SerializedName("uuid") val uuid: String? = null,
	@SerializedName("serial_number") val serialNumber: String? = null,
	@SerializedName("mac") val mac: String? = null,
	@SerializedName("status") val status: String? = null,
	@SerializedName("is_online") val isOnline: Boolean? = null,
	@SerializedName("version") val version: String? = null,
	@SerializedName("cameras_count") val camerasCount: Int? = null,
	@SerializedName("last_updated_at") val lastUpdatedAt: String? = null,
	@SerializedName("created_at") val createdAt: String? = null,
	@SerializedName("updated_at") val updatedAt: String? = null,
	@SerializedName("storages") val storages: List<VMSBridgeStorage>? = null,
): Parcelable

@Parcelize
data class VMSBridgeStorage(
	@SerializedName("id") val id: Int? = null,
	@SerializedName("path") val path: String? = null,
	@SerializedName("usage") val usage: Double? = null,
	@SerializedName("capacity") val capacity: Double? = null,
): Parcelable

interface VMSAnalyticTypeProtocol {
	fun getIdOfType(): Int
	fun typeName(): String
	fun typeTitle(): String
	fun typeFor(): String
	fun setIsChosen(isChosen: Boolean)
}

data class IntercomAnalyticFileData(
	var resources: ArrayList<String>,
	var force: Boolean = true
): Serializable

data class IntercomAnalyticFileName(
	var name: String
): Serializable
