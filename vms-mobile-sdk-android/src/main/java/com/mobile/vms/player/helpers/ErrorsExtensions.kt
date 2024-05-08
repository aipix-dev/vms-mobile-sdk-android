package com.mobile.vms.player.helpers

import android.content.Context
import com.mobile.vms.VMSMobileSDK
import com.mobile.vms.network.*
import com.mobile.vms.player.helpers.VMSSettings.getStringForLayoutByKey
import com.mobile.vms.player.ui.VMSPlayerContract.View
import com.mobile.vms.socket.*
import retrofit2.HttpException
import java.net.*
import javax.net.ssl.SSLHandshakeException

fun didReceiveError(
	view: View,
	it: Throwable,
	context: Context,
	screenNumber: Int = DEFAULT_SCREEN_NUMBER,
	action: (() -> Unit)?
) {
	view.showOrHideProgressBar(false, screenNumber)
	val isTechnicalWorkError = (it as? HttpException)?.code() == 503
	if (action != null && (!isOnline(context) || isTechnicalWorkError)) {
		SingletonErrorHandler.instance.callbackList.add(action) // pass action-request when lost connection
	}
	try {
		val showErrorScreen = { _: String ->
			view.showToast(getStringForLayoutByKey("err_no_connection"))
		}

		when (it) {
			is ConnectException -> showErrorScreen.invoke(ERROR_TYPE_CONNECTION)
			is SocketTimeoutException -> showErrorScreen.invoke(ERROR_TYPE_CONNECTION)
			is UnknownHostException -> showErrorScreen.invoke(ERROR_TYPE_CONNECTION)
			is SSLHandshakeException -> showErrorScreen.invoke(ERROR_TYPE_TECHNICAL)
			is HttpException -> {
				logSdk("ApiError", "HttpException: ${ApiError(it).getErrorInfoLogs()}")
				when (it.code()) {
					401 -> {
						vmsLogoutSocket.postValue(VMSLogoutSocket(subject = getStringForLayoutByKey("session_completed")))
					}

					409 -> showErrorScreen.invoke(ERROR_TYPE_FORCE_UPDATE)
					419 -> action?.invoke()
					422 -> {
						view.showToast(it.getErrorMessage422().let {
							if (it.isNullOrEmpty()) getStringForLayoutByKey("err_422") else it
						})
					}

					429 -> view.showToast(getStringForLayoutByKey("err_429"))
					503 -> showErrorScreen.invoke(ERROR_TYPE_TECHNICAL)
					in 500..502, 504, 505 -> if (VMSMobileSDK.isDebuggable) view.showToast("Server error ${it.code()}, stackTrace: $it, cause: ${it.cause}") else view.showToast(
						getStringForLayoutByKey("err_common")
					)

					else -> view.showToast(getStringForLayoutByKey("err_common"))
				}
			}
		}
	} catch (e: Exception) {
		e.message
	}
}

class SingletonErrorHandler private constructor() {

	fun doOnError() {
		// do non view related stuff
		// like a network call or something
		if (callbackList.isNotEmpty()) {
//            logsdk {"callbackList size = ${callbackList.size}"}
			callbackList.forEach {
				try {
					it.invoke()
				} catch (e: Exception) {
					e.printStackTrace()
				}
			}
			callbackList.clear()
			isSearchRequestAlreadySaved = false
		}
	}

	companion object {
		val instance by lazy { SingletonErrorHandler() }
	}

	var callbackList = ArrayList<(() -> Unit)>()
	var isSearchRequestAlreadySaved = false

}



