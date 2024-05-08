package com.mobile.vms.socket

import android.util.ArrayMap
import com.google.gson.*
import com.mobile.vms.VMSMobileSDK
import com.mobile.vms.models.*
import com.mobile.vms.player.helpers.logSdk
import com.mobile.vms.socket.custom.*
import com.pusher.client.channel.*
import com.pusher.client.connection.*
import com.pusher.client.util.HttpAuthorizer
import io.reactivex.subjects.*
import org.json.JSONObject
import java.net.URL
import kotlin.collections.set

const val PING_TIMEOUT = 25000L
const val PONG_TIMEOUT = 12500L
const val LOST_TIMEOUT = 25 // seconds to wait for reconnect
const val BEARER = "Bearer "
const val AUTHORIZATION = "Authorization"
const val BROADCASTING_AUTH = "broadcasting/auth"
const val WS_URL_ERROR = "wsurl_error"

/**
 * Used library Pusher for socket
 */
object VMSPusherApi: PrivateChannelEventListener {

	const val TAG = "VMSPusherApi"
	private val gson: Gson = GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create()
	var pusher: CustomPusher? = null
	var channel: PrivateChannel? = null
	private var channelUser: PrivateChannel? = null
	var options: CustomPusherOptions? = null
	var actionCloseRing: (event: VMSCallSocket) -> Unit = {}
	val vmsPusherErrorHandler: Subject<VMSPusherError> = PublishSubject.create()
	var baseUrl = ""
	private var socketUrl = ""
	private var socketPort = ""
	private var socketAppKey = ""
	private var versionApi = "v1"
	private val defaultPort = "443"
	var userToken = ""
	var userId = ""
	var accessTokenId = ""
	var isNeedReconnectSocket = false
	var pusherSocketId = ""
	var path: String? = null

	fun Builder(
		socketUrl1: String,
		socketAppKey1: String,
		userToken1: String,
		userId1: Int,
		accessTokenId1: String
	) {
		val url: URL
		if (socketUrl1.isEmpty()) {
			vmsPusherErrorHandler.onNext(VMSPusherError("Socket URL is empty."))
			return
		}
		val host = try {
			val http = socketUrl1.replace("wss://", "https://").replace("ws://", "http://")
			url = URL(http)
			path = if (url.path.isNullOrEmpty()) null else url.path
			logSdk(TAG, "url = $url")
			logSdk(TAG, "path = $path")
			logSdk(TAG, "url.host = ${url.host}")
			url.host
		} catch (e: Exception) {
			logSdk(TAG, "socketUrl1 = $socketUrl1")
			e.printStackTrace()
			parseSocketUrl(socketUrl1)
		}

		this.baseUrl = VMSMobileSDK.baseUrl
		this.socketUrl = host
		this.socketPort = parseSocketPort(baseUrl) // delete
		this.socketAppKey = socketAppKey1
		this.userToken = userToken1
		this.path = path ?: "/pusher"
		if (userId1 != 0) this.userId = userId1.toString()
		if (accessTokenId1.isNotEmpty()) this.accessTokenId = accessTokenId1
	}

	/**
	 * Call this method to initialize the pusher sockets.
	 */
	fun onConnect() {
		if (pusher != null || userToken.isEmpty() || socketUrl.isEmpty()) {
			logSdk(TAG, "Try to connect but didn't pass")
			return
		}
		try {
			val apiKey = socketAppKey
			options = CustomPusherOptions(hasEncrypted = true, socketUrl, socketPort)
			options?.isEncrypted = true
			options?.prefix = path // can be changed from "/ws" to "/pusher"
			options?.setHost(socketUrl)
			options?.setWsPort(socketPort.toInt())
			options?.buildUrl(apiKey)
			options?.activityTimeout = PING_TIMEOUT  //timeout for send ping message
			options?.pongTimeout = PONG_TIMEOUT  //timeout for wait pong message
			val auth = "${baseUrl}api/${versionApi}/$BROADCASTING_AUTH"

			val http = HttpAuthorizer(auth)
			val token = BEARER + userToken
			val map = ArrayMap<String, String>()
			map[AUTHORIZATION] = token
			http.setHeaders(map)
			options?.authorizer = http

			logSdk(TAG, "*** Try to connect to ")
			logSdk(TAG, "*** BaseUrl = $baseUrl ")
//			logSdk(TAG, "*** AppKey = $socketAppKey")
//			logSdk(TAG, "*** UserId = $userId ")
//			logSdk(TAG, "*** WsUrl = $socketUrl ")
//			logSdk(TAG, "*** Path = $path ")
//			logSdk(TAG, "*** Api version = $versionApi ")
//			logSdk(TAG, "*** Auth = $auth ")
//			logSdk(TAG, "accessTokenId = $accessTokenId")
//          logSdk(TAG, "*** userToken = $userToken ")

			pusher = CustomPusher(apiKey, options)
			pusher?.connect(object: ConnectionEventListener {
				override fun onConnectionStateChange(change: ConnectionStateChange) {
					//server socket exist and available -> do nothing
					logSdk(TAG, "*** State changed from: ${change.previousState} to: ${change.currentState} ***")
					if (change.currentState == ConnectionState.CONNECTED) {
						pusherSocketId = pusher?.connection?.socketId ?: ""
						logSdk(TAG, "*** State changed to *** ${change.currentState} ***")
//						logSdk(TAG, "*** SocketId *** ${pusherSocketId} ***")
					}
				}

				override fun onError(message: String?, code: String?, e: java.lang.Exception?) {
					//server socket not exist -> handle error
                    logSdk(TAG, "*** Disconnect or there was a connecting problem! \nMessage = $message \ncode = $code \nException = ${e?.printStackTrace()}")
					vmsPusherErrorHandler.onNext(VMSPusherError(message, e))

					if (isNeedReconnectSocket) {
						isNeedReconnectSocket = false
						vmsErrorSocket.postValue(VMSErrorSocket(WS_URL_ERROR))
					}
				}
			}, ConnectionState.ALL)

			val channelPrivateTokenId = "private-token.${accessTokenId}"
			val channelWithUserId = "private-user.${userId}"

			if (accessTokenId.isNotEmpty()) channel =
				pusher?.subscribePrivate(channelPrivateTokenId, this, TOKEN_PUSH, INTERCOM_PUSH)
			if (userId.isNotEmpty()) channelUser =
				pusher?.subscribePrivate(channelWithUserId, this, USER_PUSH, INTERCOM_PUSH)
			logSdk(TAG, "Finish to connecting")
		} catch (e: Exception) {
			e.message
		}
	}

	private val handleAppSocket: (args: PusherEvent) -> Unit = { args ->
		try {
			val data = args.data
			val jsonObject = JSONObject(data)
			val jsonMessage = jsonObject.getJSONObject("data")
			val type = jsonMessage.getString("type")
			val subject = jsonObject.getString("subject")
			logSdk(TAG, type)
			logSdk(TAG, data)

			when {
				LOGOUT_PUSH.equals(type, ignoreCase = true) -> {
					vmsLogoutSocket.postValue(VMSLogoutSocket(subject))
				}

				PERMISSIONS_UPDATED.equals(type, ignoreCase = true) -> {
					vmsPermissionsSocket.postValue(VMSCamerasSocket(type))
				}

				CAMERA_FAVORITE_STORE.equals(type, ignoreCase = true)
					|| CAMERA_FAVORITE_DESTROY.equals(type, ignoreCase = true) -> {
					try {
						val dataF: VMSFavoriteData = gson.fromJson(
							jsonMessage.getString("data"),
							VMSFavoriteData::class.java
						)
						vmsCamerasFavoriteSocket.postValue(VMSFavoriteSocket(type, dataF))
					} catch (e: Exception) {
						e.message
						vmsCamerasFavoriteSocket.postValue(VMSFavoriteSocket(type))
					}
				}

				GROUPS_UPDATED.equals(type, ignoreCase = true)
					|| CAMERA_GROUPS_SYNCED.equals(type, ignoreCase = true) -> {
					vmsCamerasGroupsSocket.postValue(VMSCamerasSocket(type))
				}

				GROUP_STORE.equals(type, ignoreCase = true)
					|| GROUP_DESTROY.equals(type, ignoreCase = true) -> {
					try {
						val dataF: VMSGroupData = gson.fromJson(
							jsonMessage.getString("data"),
							VMSGroupData::class.java
						)
						vmsGroupSocket.postValue(VMSGroupSocket(type, dataF))
					} catch (e: Exception) {
						e.message
						vmsGroupSocket.postValue(VMSGroupSocket(type))
					}
				}

				CAMERAS_UPDATED.equals(type, ignoreCase = true) -> {
					try {
						val dataX: VMSDataX =
							gson.fromJson(jsonMessage.getString("data"), VMSDataX::class.java)
						vmsCamerasSocket.postValue(VMSCamerasSocket(type, dataX))
					} catch (e: Exception) {
						e.message
						vmsCamerasSocket.postValue(VMSCamerasSocket(type))
					}
				}

				MARK_UPDATED.equals(type, ignoreCase = true) ||
						MARK_CREATED.equals(type, ignoreCase = true) ||
						MARK_DELETED.equals(type, ignoreCase = true) -> {
					try {
						val mark: VMSEvent = gson.fromJson(
							jsonMessage.getString("data"),
							VMSEvent::class.java
						)
						vmsMarksSocket.postValue(VMSEventSocket(type, mark))
					} catch (e: Exception) {
						e.message
						vmsMarksSocket.postValue(VMSEventSocket(type))
					}
				}

				ARCHIVE_GENERATED.equals(type, ignoreCase = true) -> {
					try {
						val event: VMSArchiveLinkSocket = gson.fromJson(
							jsonMessage.getString("data"),
							VMSArchiveLinkSocket::class.java
						)
						vmsArchiveLinkSocket.postValue(event)
					} catch (e: Exception) {
						e.message
						vmsArchiveLinkSocket.postValue(VMSArchiveLinkSocket(data = null))
					}
					// error : {"data":{"type":"archive_generated","status":"error","data":{"url":null}},"subject":null,"message":null}
				}
				ANALYTIC_CASE_FACE_EVENT_CREATED.equals(type, ignoreCase = true) -> {
					try {
						val event: VMSEvent = gson.fromJson(
							jsonMessage.getString("data"),
							VMSEvent::class.java
						)
						if (event.isIntercom == true) {
							vmsIntercomEventSocket.postValue(VMSEventSocket(type, event))
						} else {
							vmsAnalyticEventCreatedSocket.postValue(VMSEventSocket(type, event))
						}
					} catch (e: Exception) {
						e.message
					}
				}
				ANALYTIC_CASE_MOTION_DETECT_EVENT_CREATED.equals(type, ignoreCase = true) ||
					ANALYTIC_CASE_LINE_INTERSECTION_EVENT_CREATED.equals(type, ignoreCase = true) ||
					ANALYTIC_CASE_SMOKE_FIRE_EVENT_CREATED.equals(type, ignoreCase = true) ||
					ANALYTIC_CASE_LOUD_SOUND_EVENT_CREATED.equals(type, ignoreCase = true) ||
					ANALYTIC_CASE_CAMERA_OBSTACLE_EVENT_CREATED.equals(type, ignoreCase = true) ||
					ANALYTIC_CASE_LICENSE_PLATE_EVENT_CREATED.equals(type, ignoreCase = true) ||
					ANALYTIC_CASE_PERSON_COUNTING_EVENT_CREATED.equals(type, ignoreCase = true) ||
					ANALYTIC_CASE_VISITOR_COUNTING_EVENT_CREATED.equals(type, ignoreCase = true) ||
					ANALYTIC_CASE_CONTAINER_NUMBER_RECOGNITION_EVENT_CREATED.equals(type, ignoreCase = true) -> {
					try {
						val event: VMSEvent = gson.fromJson(
							jsonMessage.getString("data"),
							VMSEvent::class.java
						)
						vmsAnalyticEventCreatedSocket.postValue(VMSEventSocket(type, event))
					} catch (e: Exception) {
						e.message
					}
				}
			}
		} catch (e: Exception) {
			e.message
		}
	}

	private val handleIntercomSocket: (args: PusherEvent) -> Unit = { args ->
		try {
			val data = args.data
			val jsonObject = JSONObject(data)
			val type = jsonObject.getString("type")
			logSdk(TAG, "type = $type")
			logSdk(TAG, "data = $data")
			when {
				INTERCOM_KEY_CONFIRMED.equals(type, ignoreCase = true)
						|| INTERCOM_KEY_ERROR.equals(type, ignoreCase = true)
						|| INTERCOM_ADD_ERROR.equals(type, ignoreCase = true) -> {
					try {
						val intercomPush = gson.fromJson(data, VMSIntercomPush::class.java)
						vmsIntercomKeySocket.postValue(VMSIntercomPushEvent(intercomPush))
					} catch (e: Exception) {
						e.printStackTrace()
					}
				}

				INTERCOM_STORE.equals(type, ignoreCase = true)
						|| INTERCOM_RENAME.equals(type, ignoreCase = true)
						|| INTERCOM_UPDATE.equals(type, ignoreCase = true)
						|| INTERCOM_DELETE.equals(type, ignoreCase = true) -> {
					try {
						val event = gson.fromJson(data, VMSIntercomSocket::class.java)
						vmsIntercomSocket.postValue(event)
					} catch (e: Exception) {
						e.printStackTrace()
					}
				}

				INTERCOM_CODE_STORE.equals(type, ignoreCase = true)
						|| INTERCOM_CODE_DELETE.equals(type, ignoreCase = true) -> {
					try {
						val event = gson.fromJson(data, VMSVisitorSocket::class.java)
						vmsVisitorSocket.postValue(event)
					} catch (e: Exception) {
						e.printStackTrace()
					}
				}

				INTERCOM_CALL_STORE.equals(type, ignoreCase = true)
						|| INTERCOM_CALL_DELETE.equals(type, ignoreCase = true) -> {
					try {
						val event = gson.fromJson(data, VMSVisitHistorySocket::class.java)
						vmsVisitHistorySocket.postValue(event)
					} catch (e: Exception) {
						e.printStackTrace()
					}
				}
			}
		} catch (e: Exception) {
			e.message
		}
	}

	fun onDisconnect() {
		logSdk(TAG, "*** onDisconnect")
		if (pusher == null) return
		val channelPrivateTokenId = "private-token.${accessTokenId}"
		val channelWithUserId = "private-user.${userId}"
		pusher?.unsubscribe(channelPrivateTokenId)
		pusher?.unsubscribe(channelWithUserId)
		pusher?.disconnect()
		pusher = null
		userToken = ""
		socketUrl = ""
		logSdk(TAG, "*** Finish onDisconnect")
	}

	override fun onEvent(event: PusherEvent?) {
		event?.let {
			logSdk(TAG, "data = ${event.data} eventName = ${event.eventName} data = ${event.userId} event?.data = $it ")
			val eventCancel = try {
				gson.fromJson(it.data, VMSCallSocket::class.java)
			} catch (e: Exception) {
				null
			}
			if (eventCancel != null && eventCancel.type == INTERCOM_CANCEL) {
				vmsCancelCallSocket.onNext(eventCancel)
				actionCloseRing.invoke(eventCancel)
			} else if (it.eventName == INTERCOM_PUSH) {
				handleIntercomSocket.invoke(it)
			} else {
				handleAppSocket.invoke(it)
			}
		}
	}

	override fun onAuthenticationFailure(message: String?, e: java.lang.Exception?) {
		logSdk(TAG,
			String.format("Authentication failure due to [%s], exception was [%s]", message, e)
		)
		vmsPusherErrorHandler.onNext(VMSPusherError(message, e))
	}

	override fun onSubscriptionSucceeded(p0: String?) {
		logSdk(TAG, "onSubscriptionSucceeded $p0")
	}

	fun parseSocketUrl(name: String): String {
		val url = try {
			val array = name.split("://").last()
			if (array.contains(":")) array.split(":").first()
			else if (array.contains("/")) array.split("/").first()
			else array
		} catch (e: Exception) {
			e.message
			""
		}
		return url
	}

	fun parseSocketPort(name: String): String {
		val url = try {
			val array = name.split("://").last()
			val arraySplit = if (array.contains(":")) array.split(":").last() else defaultPort
			if (arraySplit.contains("/")) arraySplit.split("/").first() else arraySplit
		} catch (e: Exception) {
			e.message
			defaultPort
		}
		return url
	}

}