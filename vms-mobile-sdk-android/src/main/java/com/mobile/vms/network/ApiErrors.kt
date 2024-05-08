package com.mobile.vms.network

import android.os.Parcelable
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.mobile.vms.models.*
import kotlinx.parcelize.Parcelize
import okhttp3.ResponseBody
import retrofit2.HttpException
import java.net.*

/**
 * Structured info error.
 */
@Parcelize
data class ApiError(val throwable: Throwable): Parcelable {

	val info: ErrorInfo
		get() {
			if (throwable is HttpException) {
				return when (throwable.code()) {
					401 -> ErrorInfo("Unauthorised", "You are unauthorised", throwable.code())
					403 -> ErrorInfo("Forbidden", "Access forbidden", throwable.code())
					409 -> ErrorInfo("Force Update", "Need to update app", throwable.code())
					419 -> ErrorInfo("Session Expired", "You sessions expired", throwable.code())
					422 -> ErrorInfo(
						"Incorrect Data",
						"Incorrect input data: ${throwable.let { throwable.message() }}",
						throwable.code()
					)

					429 -> ErrorInfo("Request Limit", "You reached request limit", throwable.code())
					503 -> ErrorInfo(
						"Technical Error",
						"Server technical works error",
						throwable.code()
					)

					in 500..600 -> ErrorInfo(
						"Server Error",
						"Unknown server error",
						throwable.code()
					)

					else -> ErrorInfo("Unknown", "Unknown error", throwable.code())
				}
			} else if (throwable.isNetworkError()) {
				return ErrorInfo("NoConnection", "There is no connection with server", 0)
			} else return ErrorInfo("Unknown", "Unknown error", 0)
		}

	/**
	 * Structured info error.
	 */
	data class ErrorInfo(val name: String, val message: String, val statusCode: Int)
}

/**
 * Get error text by Throwable, for example to show in your logs.
 */
fun ApiError.getErrorInfoLogs(): String {
	return "Error name: ${info.name}\n" +
			"Error message: ${info.message}\n" +
			"Error statusCode: ${info.statusCode}"
}

/**
 * Get logs by Throwable when 422 status code was occurred.
 */
fun Throwable.getErrorMessage422(): String? {
	if (this is HttpException) {
		val message = StringBuilder()
		return try {
			val errorBody = this.response()?.errorBody()
			val response = errorBody?.string()
			if (response.isNullOrEmpty()) return this.message
			val obj = GsonBuilder().create().fromJson(response, VMSErrors::class.java)
			obj.errors.entries.forEach { entry ->
				var messagesCount = 0
				for (stringMessage in entry.value) {
					messagesCount++
					if (messagesCount > 1) message.append("\n")
					message.append(stringMessage)
				}
			}
			message.toString()
		} catch (e: Exception) {
			message.toString()
		}
	} else {
		return this.message
	}
}

fun HttpException.getErrorMessage419(): VMSLoginResponse? {
	val errorBody: ResponseBody? = this.response()?.errorBody()
	val response = errorBody?.string()
	val gson = GsonBuilder().create()
	val loginResponse = try {
		gson.fromJson(response, VMSLoginResponse::class.java)
	} catch (e: Exception) {
		try {
			val type = object: TypeToken<ArrayList<VMSSession>>() {}.type
			val sessions = gson.fromJson(response, type) as ArrayList<VMSSession>
			VMSLoginResponse(sessions = sessions)
		} catch (e: Exception) {
			null
		}
	} finally {
		errorBody?.close()
	}
	return loginResponse
}

/**
 * Get VMSError from error body when you need it several times.
 * Error body is cleared after first usage.
 */
fun Throwable.getVMSError(): VMSErrors? {
	if (this is HttpException) {
		return try {
			val errorBody = this.response()?.errorBody()
			val response = errorBody?.string()
			if (response.isNullOrEmpty()) return null
			GsonBuilder().create().fromJson(response, VMSErrors::class.java)
		} catch (e: Exception) {
			null
		}
	} else {
		return null
	}
}

fun VMSErrors.getErrorMessage422(): String {
	val message = StringBuilder()
	this.errors.entries.forEach { entry ->
		var messagesCount = 0
		for (stringMessage in entry.value) {
			messagesCount++
			if (messagesCount > 1) message.append("\n")
			message.append(stringMessage)
		}
	}
	return message.toString()
}

/**
 * Necessary to handle the captcha
 */
fun VMSErrors.isCaptchaNeeded() = this.errors.containsKey("captcha")

fun Throwable?.isNetworkError(): Boolean {
	return this != null && this is ConnectException || this is SocketTimeoutException || this is UnknownHostException
}
