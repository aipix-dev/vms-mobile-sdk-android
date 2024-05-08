package com.mobile.vms.player.helpers

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import com.mobile.vms.models.*

@SuppressLint("MissingPermission")
fun hasMobileConnection(activity: Activity): Boolean {
	return try {
		val cm = activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
		val activeNetwork = cm.activeNetworkInfo
		if (activeNetwork != null) {
			activeNetwork.type == ConnectivityManager.TYPE_MOBILE
		} else false
	} catch (e: Exception) {
		false
	}
}

