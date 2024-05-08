package com.mobile.vms.socket.custom

import com.pusher.client.PusherOptions

class CustomPusherOptions(
	private val hasEncrypted: Boolean, // now always encrypted
	private val socketUrl: String,
	private val socketPort: String
): PusherOptions() {

	var prefix: String? = null

	companion object {
		var URI_SUFFIX = "?client=java-client&protocol=5&version=$LIB_VERSION"
	}

	override fun buildUrl(apiKey: String?): String {
		val buildUrlCustom = String.format(
			"%s://%s:%s%sapp/%s%s",
			"wss",
			socketUrl,
			socketPort,
			if (this.prefix != null) this.prefix + "/" else "",
			apiKey,
			URI_SUFFIX
		)

		return if (hasEncrypted) buildUrlCustom else super.buildUrl(apiKey)
	}

}