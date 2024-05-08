package com.mobile.vms.socket

import android.os.Parcelable
import androidx.lifecycle.*
import com.google.gson.annotations.SerializedName
import com.mobile.vms.models.*
import io.reactivex.subjects.*
import kotlinx.parcelize.Parcelize

/**
 * Models for using pusher sockets.
 */

@Parcelize
data class VMSCamerasSocket(
	@SerializedName("type") val type: String? = null,
	@SerializedName("info") val info: VMSDataX? = null
): Parcelable

@Parcelize
data class VMSEventSocket(
	@SerializedName("type") val type: String? = null,
	@SerializedName("mark") val event: VMSEvent? = null
): Parcelable

@Parcelize
data class VMSIntercomSocket(
	@SerializedName("intercom") val intercom: VMSIntercom?,
	@SerializedName("deleted_ids") val deleted_ids: ArrayList<Int>?,
	@SerializedName("id") val id: String,
	@SerializedName("type") val type: String
): Parcelable

@Parcelize
data class VMSVisitorSocket(
	@SerializedName("intercom_code") val intercom_code: VMSCodeVisitor?,
	@SerializedName("deleted_ids") val deleted_ids: ArrayList<Int>?,
	@SerializedName("id") val id: String,
	@SerializedName("type") val type: String
): Parcelable

@Parcelize
data class VMSVisitHistorySocket(
	@SerializedName("intercom_call") val intercom_call: VMSIntercomCall?,
	@SerializedName("deleted_ids") val deleted_ids: ArrayList<Int>?,
	@SerializedName("id") val id: String,
	@SerializedName("type") val type: String
): Parcelable

@Parcelize
data class VMSDataX(
	@SerializedName("attached") val attached: List<Int>,
	@SerializedName("detached") val detached: List<Int>
): Parcelable

@Parcelize
data class VMSFavoriteSocket(
	@SerializedName("type") val type: String? = null,
	@SerializedName("data") val data: VMSFavoriteData? = null,
): Parcelable

@Parcelize
data class VMSFavoriteData(
	@SerializedName("user_id") val userId: Int? = null,
	@SerializedName("camera_id") val cameraId: Int? = null,
	@SerializedName("order") val order: Int? = null,
	@SerializedName("name") val name: String? = null,
	@SerializedName("is_favorite") val isFavorite: Boolean? = null,
	@SerializedName("status") val status: String? = null,
): Parcelable

@Parcelize
data class VMSGroupSocket(
	@SerializedName("type") val type: String? = null,
	@SerializedName("data") val data: VMSGroupData? = null,
): Parcelable

@Parcelize
data class VMSGroupData(
	@SerializedName("id") val id: Int? = null,
	@SerializedName("name") val name: String? = null,
	@SerializedName("ids") val ids: List<Int>? = null,
): Parcelable

/**
 * TODO
 * Implemented: @SerializedName("download") var data: DataUrl? in 23.09.00
 * Delete:      @SerializedName("url")      val url: String?   in 24.09.00
 */
@Parcelize
data class VMSArchiveLinkSocket(
	@SerializedName("url") private val url: String? = null,
	@SerializedName("download") private var data: VMSDataUrl? = null,
): Parcelable {
	private fun isUrlEmpty() = url == null

	fun getURL(): String? = if (isUrlEmpty()) data?.url else url
}

@Parcelize
data class VMSDataUrl(@SerializedName("url") val url: String = ""): Parcelable

data class VMSLogoutSocket(@SerializedName("subject") val subject: String? = null)

@Parcelize
data class VMSErrorSocket(@SerializedName("type") val type: String? = null): Parcelable

@Parcelize
data class VMSCallSocket(
	@SerializedName("data") val data: VMSIntercomData,
	@SerializedName("type") val type: String = "",
	var notification: VMSNotificationData? = null
): Parcelable

@Parcelize
data class VMSNotificationData(
	@SerializedName("title") val title: String, @SerializedName("body") val body: String
): Parcelable

@Parcelize
data class VMSIntercomCodeNotificationSocket(
	@SerializedName("data") val data: VMSPushCodeVisitor, @SerializedName("type") val type: String
): Parcelable

@Parcelize
data class VMSIntercomData(
	@SerializedName("id") val id: String? = "",
	@SerializedName("call_id") val call_id: String,
	@SerializedName("title") val title: String,
	@SerializedName("video_url") val video_url: String?,
	@SerializedName("call_created_at") val call_created_at: String,
	@SerializedName("camera_id") val camera_id: String?,
	@SerializedName("mark_type") val markType: String? = null,
	@SerializedName("is_open_door_app") val isOpenDoorApp: Boolean? = null
): Parcelable

@Parcelize
data class VMSPushCodeVisitor(
	@SerializedName("id") val id: String? = null,
	@SerializedName("title") val title: String? = null,
	@SerializedName("code") val code: String? = null,
	@SerializedName("expired_at") val expired_at: String? = null,
	@SerializedName("is_expired") val is_expired: String? = null,
	@SerializedName("will_deleted_at") val will_deleted_at: String? = null,
	@SerializedName("created_at") val created_at: String? = null,
): Parcelable

@Parcelize
data class VMSPusherError(
	val message: String? = null,
	val error: Throwable? = null,
): Parcelable

//val vmsLogoutSocket: BehaviorSubject<VMSLogoutSocket> = BehaviorSubject.create()

val vmsLogoutSocket: MutableLiveData<VMSLogoutSocket> = MutableLiveData()
val vmsCamerasSocket: MutableLiveData<VMSCamerasSocket> = MutableLiveData()
val vmsCamerasFavoriteSocket: MutableLiveData<VMSFavoriteSocket> = MutableLiveData()
val vmsCamerasGroupsSocket: MutableLiveData<VMSCamerasSocket> = MutableLiveData()
val vmsGroupSocket: MutableLiveData<VMSGroupSocket> = MutableLiveData()
val vmsPermissionsSocket: MutableLiveData<VMSCamerasSocket> = MutableLiveData()
val vmsMarksSocket: MutableLiveData<VMSEventSocket> = MutableLiveData()
val vmsIntercomEventSocket: MutableLiveData<VMSEventSocket> = MutableLiveData()
val vmsArchiveLinkSocket: MutableLiveData<VMSArchiveLinkSocket> = MutableLiveData()
val vmsIntercomKeySocket: MutableLiveData<VMSIntercomPushEvent> = MutableLiveData()
val vmsIntercomSocket: MutableLiveData<VMSIntercomSocket> = MutableLiveData()
val vmsVisitorSocket: MutableLiveData<VMSVisitorSocket> = MutableLiveData()
val vmsVisitHistorySocket: MutableLiveData<VMSVisitHistorySocket> = MutableLiveData()
val vmsCancelCallSocket: BehaviorSubject<VMSCallSocket?> = BehaviorSubject.create()
val vmsAnalyticEventCreatedSocket: MutableLiveData<VMSEventSocket> = MutableLiveData()
val vmsErrorSocket: MutableLiveData<VMSErrorSocket> = MutableLiveData()

fun <T> LiveData<T>.observeOnce(owner: LifecycleOwner, reactToChange: (T) -> Unit): Observer<T> {
	val wrappedObserver = object: Observer<T> {
		override fun onChanged(data: T) {
			reactToChange(data)
			removeObserver(this)
			removeObservers(owner)
		}
	}

	observe(owner, wrappedObserver)
	return wrappedObserver
}

















