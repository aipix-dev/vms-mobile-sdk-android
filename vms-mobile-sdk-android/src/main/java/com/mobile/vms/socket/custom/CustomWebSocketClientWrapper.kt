package com.mobile.vms.socket.custom

import com.mobile.vms.socket.LOST_TIMEOUT
import com.pusher.client.connection.websocket.WebSocketListener
import org.java_websocket.handshake.ServerHandshake
import java.io.IOException
import java.net.*
import java.security.*
import javax.net.ssl.*

internal class CustomWebSocketClientWrapper(
	uri: URI,
	proxy: Proxy?,
	webSocketListener: WebSocketListener?
): CustomWebSocketClient(uri) {
	private var webSocketListener: WebSocketListener?
	override fun onOpen(handshakedata: ServerHandshake) {
		if (webSocketListener != null) {
			webSocketListener!!.onOpen(handshakedata)
		}
	}

	override fun onMessage(message: String) {
		if (webSocketListener != null) {
			webSocketListener!!.onMessage(message)
		}
	}

	override fun onClose(code: Int, reason: String, remote: Boolean) {
		if (webSocketListener != null) {
			webSocketListener!!.onClose(code, reason, remote)
		}
	}

	override fun onError(ex: Exception) {
		if (webSocketListener != null) {
			webSocketListener!!.onError(ex)
		}
	}

	/**
	 * Removes the WebSocketListener so that the underlying WebSocketClient doesn't expose any listener events.
	 */
	fun removeWebSocketListener() {
		webSocketListener = null
	}

	companion object {
		private const val WSS_SCHEME = "wss"
	}

	init {
		connectionLostTimeout = LOST_TIMEOUT
		if (uri.scheme == WSS_SCHEME) {
			socket = try {
				val sslContext = SSLContext.getInstance("TLS")
				sslContext.init(null, null, null) // will use java's default
				// key and trust store which
				// is sufficient unless you
				// deal with self-signed
				// certificates
				val factory = sslContext.socketFactory // (SSLSocketFactory)
				// SSLSocketFactory.getDefault();
				factory.createSocket()
			} catch (e: IOException) {
				throw SSLException(e)
			} catch (e: NoSuchAlgorithmException) {
				throw SSLException(e)
			} catch (e: KeyManagementException) {
				throw SSLException(e)
			}
		}
		this.webSocketListener = webSocketListener
		setProxy(proxy)
	}
}