package com.mobile.vms.network

import android.os.Build
import android.text.TextUtils
import com.google.gson.*
import com.mobile.vms.*
import com.mobile.vms.network.converter.ArrayAdapterFactory
import com.mobile.vms.player.helpers.*
import com.mobile.vms.socket.VMSPusherApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.*
import javax.net.ssl.*
import javax.security.cert.CertificateException

const val TIMEOUT_READ = 30000
const val TIMEOUT_CONNECT = 60000
const val APP_JSON = "application/json"
const val USER_AGENT = "User-Agent"
const val X_Socket_ID = "X-Socket-ID"
const val CONTENT_TYPE = "Content-Type"
const val ACCEPT = "Accept"
const val HL = "hl"
const val X_CLIENT_KEY = "X-Client"
const val X_CLIENT_VERSION = "X-Version"
const val X_UUID = "X-UUID"
const val BEARER = "Bearer "
const val X_CLIENT_VALUE = "android"
const val AUTHORIZATION = "Authorization"
const val VERSION_SDK = "23.12.1.0"
fun userAgent() =
	Build.MANUFACTURER + " / " + Build.MODEL + " / " + Build.VERSION.RELEASE + " / " + VERSION_SDK + " / Android"

/**
 * Used library Retrofit for REST
 */
object VMSClientApi {

	var baseUrl: String = ""
		set(value) {
			field = value.getValidBaseUrl()
		}
	var uuid: String = ""
	var userToken: String = ""
	var language: String? = null

	fun Builder(baseUrl: String, uuid: String, lang: String?): VMSClientApi {
		VMSClientApi.baseUrl = baseUrl
		VMSClientApi.uuid = uuid
		language = lang ?: EN
		return this
	}

	fun getOkHttpClient(): OkHttpClient {
		val trustAllCerts: Array<TrustManager> = arrayOf(
			object: X509TrustManager {
				@Throws(CertificateException::class)
				override fun checkClientTrusted(
					chain: Array<X509Certificate?>?,
					authType: String?
				) {
				}

				@Throws(CertificateException::class)
				override fun checkServerTrusted(
					chain: Array<X509Certificate?>?,
					authType: String?
				) {
				}

				override fun getAcceptedIssuers(): Array<X509Certificate> {
					return arrayOf()
				}

			}
		)
		val builder = OkHttpClient.Builder()

		val sslContext: SSLContext =
			SSLContext.getInstance(if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) "SSL" else "TLS")
		sslContext.init(null, trustAllCerts, SecureRandom())
		// Create an ssl socket factory with our all-trusting manager
		val sslSocketFactory: SSLSocketFactory = sslContext.socketFactory
		builder.sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
		return builder
			.hostnameVerifier { _, _ -> true }
			.connectTimeout(TIMEOUT_CONNECT.toLong(), TimeUnit.MILLISECONDS)
			.readTimeout(TIMEOUT_READ.toLong(), TimeUnit.MILLISECONDS)
			.writeTimeout(TIMEOUT_READ.toLong(), TimeUnit.MILLISECONDS)
			.addNetworkInterceptor { chain ->
				var response = chain.proceed(chain.request())
				if (response.code == 301) {
					response = response.newBuilder().code(308).build()
				}
				response
			}
			.addInterceptor(HttpLoggingInterceptor().setLevel(if (VMSMobileSDK.isDebuggable) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE))
			.addInterceptor { chain ->
				val original = chain.request()
				val requestBuilder = original.newBuilder()
				val token = BEARER + userToken
				val socketId = VMSPusherApi.pusherSocketId
				if (!TextUtils.isEmpty(userToken)) requestBuilder.header(AUTHORIZATION, token)
				if (!TextUtils.isEmpty(uuid)) requestBuilder.header(X_UUID, uuid)
				if (!TextUtils.isEmpty(socketId)) requestBuilder.header(X_Socket_ID, socketId)
				requestBuilder.header(USER_AGENT, userAgent())
				requestBuilder.header(X_CLIENT_VERSION, VERSION_SDK)
				requestBuilder.header(X_CLIENT_KEY, X_CLIENT_VALUE)
				requestBuilder.header(CONTENT_TYPE, APP_JSON)
				requestBuilder.header(ACCEPT, APP_JSON)
				requestBuilder.header(HL, language ?: EN)
				requestBuilder.method(original.method, original.body)
				logSdk(
					"VMSClientApi", "Headers\n" +
							"AUTHORIZATION = ${ token} \n" +
							"X_UUID = ${ uuid} \n" +
							"X_Socket_ID = ${ socketId} \n" +
							"USER_AGENT = ${ userAgent()} \n" +
							"X_CLIENT_VERSION = ${ VERSION_SDK} \n" +
							"X_CLIENT_KEY = ${ X_CLIENT_VALUE} \n" +
							"CONTENT_TYPE = ${ APP_JSON} \n" +
							"ACCEPT = ${ APP_JSON} \n" +
							"HL = ${ language ?: EN} \n"
				)
				chain.proceed(requestBuilder.build())
			}
			.retryOnConnectionFailure(true)
			.build()
	}

	fun createServiceClientObservable(url: String? = null): ApiClientObservable {
		if(url != null) baseUrl = url
		val gson = GsonBuilder()
			.serializeNulls()
			.setLenient()
			.registerTypeAdapterFactory(ArrayAdapterFactory())
			.setDateFormat("yyyy-MM-dd HH:mm:ss")
			.create()
		return provideRetrofit(gson).create(ApiClientObservable::class.java)
	}

	fun createServiceClientCoroutines(url: String? = null): ApiClientCoroutines {
		if(url != null) baseUrl = url
		val gson = GsonBuilder()
			.serializeNulls()
			.setLenient()
			.registerTypeAdapterFactory(ArrayAdapterFactory())
			.setDateFormat("yyyy-MM-dd HH:mm:ss")
			.create()
		return provideRetrofit(gson).create(ApiClientCoroutines::class.java)
	}

	fun provideRetrofit(gson: Gson): Retrofit {
		val okHttpClient = getOkHttpClient()
		return Retrofit.Builder()
			.addCallAdapterFactory(RxJava2CallAdapterFactory.create())
			.addConverterFactory(ScalarsConverterFactory.create()) // this line cut quotes
			.addConverterFactory(GsonConverterFactory.create(gson))
			.client(okHttpClient)
			.baseUrl(baseUrl)
			.build()
	}

}