package com.mobile.vms.player.timeline

import android.os.CountDownTimer

const val LOADING_DELAY = 600L

class LoadingTimer {
	private var timer: CountDownTimer? = null
	fun startTimer(action: () -> Unit) {
		timer?.cancel()
		timer = object: CountDownTimer(LOADING_DELAY, LOADING_DELAY) {
			override fun onTick(p0: Long) {}

			override fun onFinish() {
				action.invoke()
			}
		}.start()
	}

	fun stopTimer() {
		timer?.cancel()
	}
}