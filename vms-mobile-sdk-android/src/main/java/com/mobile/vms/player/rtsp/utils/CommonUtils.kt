package com.mobile.vms.player.rtsp.utils

import kotlinx.coroutines.*

fun CoroutineScope.launchInMain(delayMillis: Long = 0, block: suspend CoroutineScope.() -> Unit) {
	this.launch {
		if (delayMillis > 0) delay(delayMillis)
		withContext(Dispatchers.Main) {
			block()
		}
	}
}