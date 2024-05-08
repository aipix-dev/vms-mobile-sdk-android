package com.mobile.vms.socket.custom

import com.google.gson.Gson
import com.mobile.vms.socket.VMSPusherApi
import com.pusher.client.connection.*
import com.pusher.client.connection.impl.InternalConnection
import com.pusher.client.connection.websocket.WebSocketListener
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONObject
import java.net.*
import java.util.*
import java.util.concurrent.*
import java.util.logging.*
import javax.net.ssl.SSLException

data class DataPushConnecting(
	val activity_timeout: Long,
	val socket_id: String
)

internal class CustomWebSocketConnection(
	url: String?,
	activityTimeout: Long,
	pongTimeout: Long,
	maxReconnectionAttempts: Int,
	maxReconnectionGap: Int,
	proxy: Proxy,
	factory: CustomFactory
): InternalConnection, WebSocketListener {
	private val factory: CustomFactory
	private val activityTimer: ActivityTimer
	private val eventListeners: MutableMap<ConnectionState, MutableSet<ConnectionEventListener>> =
		ConcurrentHashMap()
	private val webSocketUri: URI
	private val proxy: Proxy
	private val maxReconnectionAttempts: Int
	private val maxReconnectionGap: Int

	@Volatile
	private var state = ConnectionState.DISCONNECTED
	private var underlyingConnection: CustomWebSocketClientWrapper? = null
	private var socketId: String? = ""
	private var reconnectAttempts = 0

	/* Connection implementation */
	override fun connect() {
		factory.queueOnEventThread {
			if (state == ConnectionState.DISCONNECTED) {
				tryConnecting()
			}
		}
	}

	private fun tryConnecting() {
		try {
			underlyingConnection = factory
				.newWebSocketClientWrapper(webSocketUri, proxy, this@CustomWebSocketConnection)
			updateState(ConnectionState.CONNECTING)
			underlyingConnection?.connect()
		} catch (e: SSLException) {
			sendErrorToAllListeners("Error connecting over SSL", null, e)
		}
	}

	override fun disconnect() {
		factory.queueOnEventThread { // disconnect even if state CONNECTING
			// need when can't connect and reopen app -> previous connecting should be close
			if (state != ConnectionState.DISCONNECTING && state != ConnectionState.DISCONNECTED) {
				updateState(ConnectionState.DISCONNECTING)
				underlyingConnection!!.close()
			}
		}
	}

	override fun bind(state: ConnectionState, eventListener: ConnectionEventListener) {
		eventListeners[state]!!.add(eventListener)
	}

	override fun unbind(state: ConnectionState, eventListener: ConnectionEventListener): Boolean {
		return eventListeners[state]!!.remove(eventListener)
	}

	override fun getState(): ConnectionState {
		return state
	}

	/* InternalConnection implementation detail */
	override fun sendMessage(message: String) {
		factory.queueOnEventThread {
			try {
				if (state == ConnectionState.CONNECTED) {
//                    Log.d("VMS_SDK", "CustomWebSocketConnection. sendMessage = $message")
					underlyingConnection!!.send(message)
				} else {
//                    Log.d("VMS_SDK", "Cannot send a message while in $state state")
					sendErrorToAllListeners(
						"Cannot send a message while in $state state",
						null,
						null
					)
				}
			} catch (e: Exception) {
//                Log.e("VMS_SDK", "An exception occurred while sending message [$message]")
				sendErrorToAllListeners(
					"An exception occurred while sending message [$message]",
					null,
					e
				)
			}
		}
	}

	override fun getSocketId(): String {
		return socketId!!
	}

	/* implementation detail */
	private fun updateState(newState: ConnectionState) {
//        log.fine("State transition requested, current [$state], new [$newState]")
		val change = ConnectionStateChange(state, newState)
		state = newState
		val interestedListeners: MutableSet<ConnectionEventListener> = HashSet()
		interestedListeners.addAll(eventListeners[ConnectionState.ALL]!!)
		interestedListeners.addAll(eventListeners[newState]!!)
		for (listener in interestedListeners) {
			factory.queueOnEventThread { listener.onConnectionStateChange(change) }
		}
	}

	private fun handleEvent(event: String?, wholeMessage: String) {
		if (event!!.startsWith(INTERNAL_EVENT_PREFIX)) {
			handleInternalEvent(event, wholeMessage)
		} else {
			factory.channelManager.onMessage(event, wholeMessage)
		}
	}

	private fun handleInternalEvent(event: String?, wholeMessage: String) {
		if (event == "pusher:connection_established") {
			handleConnectionMessage(wholeMessage)
		} else if (event == "pusher:error") {
			handleError(wholeMessage)
		}
	}

	private fun handleConnectionMessage(message: String) {
		val jsonObject = GSON.fromJson<Map<*, *>>(message, MutableMap::class.java)
		val dataString = jsonObject["data"] as String?
		val dataMap = GSON.fromJson<Map<*, *>>(dataString, MutableMap::class.java)
		socketId = dataMap["socket_id"] as String?
		if (state != ConnectionState.CONNECTED) {
			updateState(ConnectionState.CONNECTED)
		}
		reconnectAttempts = 0
	}

	private fun handleError(wholeMessage: String) {
		val json = GSON.fromJson<Map<*, *>>(wholeMessage, MutableMap::class.java)
		val data = json["data"]
		val dataMap: Map<*, *>?
		dataMap = if (data is String) {
			GSON.fromJson<Map<*, *>>(
				data as String?,
				MutableMap::class.java
			)
		} else {
			data as Map<*, *>?
		}
		val message = dataMap!!["message"] as String?
		val codeObject = dataMap["code"]
		var code: String? = null
		if (codeObject != null) {
			code = Math.round((codeObject as Double?)!!).toString()
		}
		sendErrorToAllListeners(message, code, null)
	}

	private fun sendErrorToAllListeners(message: String?, code: String?, e: Exception?) {
		val allListeners: MutableSet<ConnectionEventListener> = HashSet()
		for (listenersForState in eventListeners.values) {
			allListeners.addAll(listenersForState)
		}
		for (listener in allListeners) {
			factory.queueOnEventThread { listener.onError(message, code, e) }
		}
	}

	/* WebSocketListener implementation */
	override fun onOpen(handshakedata: ServerHandshake) {
		// TODO: log the handshake data
	}

	override fun onMessage(message: String) {
//        Log.e("VMS_SDK", "CustomWebSocketConnection. onMessage = $message")
		activityTimer.activity()
		factory.queueOnEventThread {
			val map: Map<String, String?> =
				GSON.fromJson<Map<String, String>>(message, MutableMap::class.java)
			val event = map["event"]
			if (map["data"] != null) {
//                Log.d("VMS_SDK", "CustomWebSocketConnection. message = $message")
				val jsonObject = JSONObject(message)
//                Log.d("VMS_SDK", "CustomWebSocketConnection. jsonObject = $jsonObject")
				val jsonMessage = jsonObject.getString("data")
//                Log.d("VMS_SDK", "CustomWebSocketConnection. jsonMessage = $jsonMessage")
				val data: DataPushConnecting =
					GSON.fromJson(jsonMessage, DataPushConnecting::class.java)
//                Log.d("VMS_SDK", "CustomWebSocketConnection. data = $data")
				val time =
					(data.activity_timeout * 1000L) - 5000 // need to set minus 5 second for cause bad connection
				if (time >= 6000) {
					VMSPusherApi.options?.activityTimeout = time
//                    Log.d("VMS_SDK", "CustomWebSocketConnection. activityTimeout = $time")
					if (time / 2 >= 1000) {
						VMSPusherApi.options?.pongTimeout = time / 2
//                        Log.e("VMS_SDK", "CustomWebSocketConnection. pongTimeout = ${time / 2}")
					}
				}
			}
			handleEvent(event, message)
		}
	}

	override fun onClose(code: Int, reason: String, remote: Boolean) {
		if (state == ConnectionState.DISCONNECTED || state == ConnectionState.RECONNECTING) {
			log.warning(
				"Received close from underlying socket when already disconnected." + "Close code ["
						+ code + "], Reason [" + reason + "], Remote [" + remote + "]"
			)
			return
		}
//        Log.e(
//            "VMS_SDK",
//            "CustomWebSocketConnection. onClose. shouldReconnect = " + shouldReconnect(code)
//        )
		if (!shouldReconnect(code)) {
			updateState(ConnectionState.DISCONNECTING)
		}

		//Reconnection logic
		if (state == ConnectionState.CONNECTED || state == ConnectionState.CONNECTING) {
			if (reconnectAttempts < maxReconnectionAttempts) {
				tryReconnecting()
			} else {
				updateState(ConnectionState.DISCONNECTING)
				cancelTimeoutsAndTransitonToDisconnected()
			}
			return
		}
		if (state == ConnectionState.DISCONNECTING) {
			cancelTimeoutsAndTransitonToDisconnected()
		}
	}

	private fun tryReconnecting() {
		reconnectAttempts++
//        Log.e(
//            "VMS_SDK",
//            "CustomWebSocketConnection. tryReconnecting. reconnectAttempts = $reconnectAttempts"
//        )
		updateState(ConnectionState.RECONNECTING)
		factory.timers.schedule({ //            activityTimer.cancelTimeouts();  //add close old timers
			underlyingConnection!!.removeWebSocketListener()
			tryConnecting()
		}, maxReconnectionGap.toLong(), TimeUnit.SECONDS) //set reconnection gap
	}

	// Received error codes 4000-4099 indicate we shouldn't attempt reconnection
	// https://pusher.com/docs/pusher_protocol#error-codes
	private fun shouldReconnect(code: Int): Boolean {
		return code < 4000 || code >= 4100
	}

	private fun cancelTimeoutsAndTransitonToDisconnected() {
		activityTimer.cancelTimeouts()
		factory.queueOnEventThread {
			updateState(ConnectionState.DISCONNECTED)
			factory.shutdownThreads()
		}
		reconnectAttempts = 0
	}

	override fun onError(ex: Exception) {
		factory.queueOnEventThread { // Do not change connection state as Java_WebSocket will also
			// call onClose.
			// See:
			// https://github.com/leggetter/pusher-java-client/issues/8#issuecomment-16128590
			// updateState(ConnectionState.DISCONNECTED);
			sendErrorToAllListeners("An exception was thrown by the websocket", null, ex)
		}
	}

	private inner class ActivityTimer(
		private val activityTimeout: Long,
		private val pongTimeout: Long
	) {
		private var pingTimer: Future<*>? = null
		private var pongTimer: Future<*>? = null

		/**
		 * On any activity from the server - Cancel pong timeout - Cancel
		 * currently ping timeout and re-schedule
		 */
		@Synchronized
		fun activity() {
			cancelTimeouts()
			pingTimer = factory.timers.schedule({
				log.fine("Sending ping")
				sendMessage(PING_EVENT_SERIALIZED)
				schedulePongCheck()
			}, activityTimeout, TimeUnit.MILLISECONDS)
		}

		/**
		 * Cancel any pending timeouts, for example because we are disconnected.
		 */
		@Synchronized
		fun cancelTimeouts() {
			if (pingTimer != null) {
				pingTimer!!.cancel(false)
			}
			if (pongTimer != null) {
				pongTimer!!.cancel(false)
			}
		}

		/**
		 * Called when a ping is sent to await the response - Cancel any
		 * existing timeout - Schedule new one
		 */
		@Synchronized
		private fun schedulePongCheck() {
			if (pongTimer != null) {
				pongTimer!!.cancel(false)
			}
			pongTimer = factory.timers.schedule({
				log.fine("Timed out awaiting pong from server - disconnecting")
				underlyingConnection!!.removeWebSocketListener()
				disconnect()

				// Proceed immediately to handle the close
				// The WebSocketClient will attempt a graceful WebSocket shutdown by exchanging the close frames
				// but may not succeed if this disconnect was called due to pong timeout...
				onClose(-1, "Pong timeout", false)
			}, pongTimeout, TimeUnit.MILLISECONDS)
		}
	}

	companion object {
		private val log = Logger.getLogger(
			CustomWebSocketConnection::class.java.name
		)
		private val GSON = Gson()
		private const val INTERNAL_EVENT_PREFIX = "pusher:"
		private const val PING_EVENT_SERIALIZED = "{\"event\": \"pusher:ping\"}"
	}

	init {
		webSocketUri = URI(url)
		activityTimer = ActivityTimer(activityTimeout, pongTimeout)
		this.maxReconnectionAttempts = maxReconnectionAttempts
		this.maxReconnectionGap = maxReconnectionGap
		this.proxy = proxy
		this.factory = factory
		for (state in ConnectionState.values()) {
			eventListeners[state] = Collections.newSetFromMap(ConcurrentHashMap())
		}
	}
}