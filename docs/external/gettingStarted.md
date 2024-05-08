@toc Getting started

# Instruction of implementation VMSMobileSDK #

### Android example, written in Kotlin

NOTE: You must have at least:

- minSdk = 28
- compileSdk = 34
- buildToolsVersion = '34.0.0'



### Step 1.

Initialize the VMSMobileSDK in your Application class within the onCreate() method as shown below.

```kotlin
// ... your code
override fun onCreate() {
	VMSMobileSDK.Builder(
		application = this, // required
		baseUrl = "https://your_backend_baseurl", // required
		uuid = "your stored uuid", // required, see more https://developer.android.com/reference/java/util/UUID
		language = "en" // not required, by default "en", see the `available_locales` field from the `VMSBasicStatic` object
	).apply {
		if (your_user_token.isNotEmpty()) {
			// This code will be executed only for authorized user, look at step 2
			VMSMobileSDK.userToken = your_user_token
		}
	}
// ... your code
}
```

* The UUID can be obtained using the following method:

```kotlin
val uuid = Secure.getString(context.contentResolver, Secure.ANDROID_ID)
```

You can then save your application using any method you prefer.


### Step 2.

Use the `createServiceClientCoroutines()` function to create service client APIs and make requests.

We use the "Retrofit" library to help you create a service client API as shown below:

```kotlin
fun createServiceClient(url: String): ApiClientObservable = VMSClientApi.createServiceClientObservable(url)

fun createServiceClient(url: String): ApiClientCoroutines = VMSClientApi.createServiceClientCoroutines(url)
```

You can use this service client API to make requests. For example, you can make the following 2 requests without an access token:

`getTranslations(language: String, revision: Int)` - fetches a JSON containing a dictionary that can be used in your application; it is also used in `VMSPlayerFragment`

See more in `staticsApi.md` about the `language` and `revision` parameters.

`getStaticBasic()` - useful if your application requires access to captcha and external authentication via a website

```kotlin
// ... your code

//example observable:
fun getTranslations(language: String, revision: Int) {
	val apiClient: ApiClientObservable = VMSClientApi.createServiceClient()
	apiClient?.getTranslations(language, revision)
		?.subsIoObsMain()
		?.subscribe({ jsonTranslations ->
			// ... your code to store jsonTranslations in your app
		}) {
			// ... insert your error handling code here
			// Refer to the ApiErrors.kt SDK file to retrieve error codes or messages
		}?.addTo(compositeDisposable)
}
fun getStaticBasic() {
	val apiClient: ApiClientObservable = VMSClientApi.createServiceClientObservable()
	apiClient?.getStaticBasic()
		.subsIoObsMain()
		.subscribe({ result ->
			// ... your code to store basic static data
			prefs.isCaptchaAvailable = result.is_captcha_available == true
			prefs.isExternalAuthEnabled = result.is_external_auth_enabled == true
		}, { error ->
			// ... insert your error handling code here
			// Refer to the ApiErrors.kt SDK file to retrieve error codes or messages
		}).addTo(compositeDisposable)
}

//example coroutine:
fun getTranslations(language: String, revision: Int) {
	CoroutineScope(Dispatchers.IO).launch(CoroutineExceptionHandler { _, _ ->
		// ... insert your error handling code here
		// Refer to the ApiErrors.kt SDK file to retrieve error codes or messages
	}) {
		val result = VMSClientApi.createServiceClientCoroutines().getTranslations(language, revision)
		// ... your code to store jsonTranslations in your app
	}
}
fun getStaticBasic() {
	CoroutineScope(Dispatchers.IO).launch(CoroutineExceptionHandler { _, _ ->
		// ... insert your error handling code here
		// Refer to the ApiErrors.kt SDK file to retrieve error codes or messages
	}) {
		// ... your code to store basic static data, such as SharedPreference
		val result = VMSClientApi.createServiceClientCoroutines().getBasicStatic()
		prefs.isCaptchaAvailable = result.is_captcha_available == true
		prefs.isExternalAuthEnabled = result.is_external_auth_enabled == true
	}
}

```


### Step 3.

Auth

Use login() with `login` and `password` as mandatory parameters.

From the resulting response, extract the user data to set in the SDK.

```kotlin
fun login() {
	val apiClient: ApiClientObservable = VMSClientApi.createServiceClientObservable()
	apiClient?.login()
		?.subsIoObsMain()
		?.subscribe({ response ->
			VMSMobileSDK.userToken = responseaccessToken// required, need to be set for auth-user to secure requests
		}) {
			// ... your code to handle errors
			// check SDK file ApiErrors.kt to get error code or message
		}?.addTo(compositeDisposable)
}
```


### Step 4.

Use getStatics() after successful login.

`getStatics()` - get static data for your app

```kotlin
fun getStatics() {
	val apiClient: ApiClientObservable? = VMSClientApi.createServiceClient()
	apiClient?.getStatics()
		.subsIoObsMain()
		.subscribe({ result ->
			// ... your code to store static data
			localData.videoRates = result.video_rates
		}, { error ->
			// ... insert your error handling code here
			// Refer to the ApiErrors.kt SDK file to retrieve error codes or messages
		}).addTo(compositeDisposable)
}
```


### Step 5.

Pusher sockets:

`getSocketUrl()` - for using our pusher sockets, perform this function to acquire the socket URL

```kotlin
// ... your code

// Execute this request post after successful login.
fun getSocketUrl() {
	apiManager.getSocketUrl()
		.subsIoObsMain()
		.subscribe({ response ->
			if (response.ws_url.isNotEmpty()) {

				VMSMobileSDK.pusherApi.Builder(
					response.ws_url, // required, websocket url from 
					response.app_key, // required, app key from 
					accessToken, // required, from response of `login()`
					user.id.toString(), // required, from response of `login()`
					user.access_token_id, // required, from response of `login()`
				)
				VMSMobileSDK.pusherApi.onConnect() // connect to pusher socket
			}
		}, { _ ->
			// ... insert your error handling code here
			// Refer to the ApiErrors.kt SDK file to retrieve error codes or messages
		}).addTo(compositeDisposable)
}
```


### Step 6.

If you need to use FCM for intercoms, make sure you send the fcm token to our backend.

Refer to the Firebase documentation to set up your project: https://firebase.google.com/docs/cloud-messaging/android/client?hl=en

```kotlin
// ... your code in FCM service class
fun setTokenToServer() {
	if (!TextUtils.isEmpty(userStorage.token)) {
		apiManager?.sendDeviceToken(prefs.tokenFCM)?.subsIoObsMain()?.subscribe({
			//do nothing
		}, {
			//do nothing
		})?.addTo(compositeDisposable)
	}
}
```


### Step 7.

To retrieve the list of cameras, make the following requests:

`getCamerasTree()` - get all cameras available to the user account

`getFavorites()` - get all favorite cameras, if any have been added

`getGroups()` - get all camera group, if any have been created


### Step 8.

Player screen.

Use the following code snippet to display the player screen:

```kotlin
// ... your code
fun openPlayerScreen(): VMSPlayerFragment {
	val data = VMSPlayerData(
		camera, // required, camera can be got from the `getCamerasTree()` response
		listOfCameras, // not required, can be got from the `getCamerasTree()` response; set if you want to be able to swipe between cameras
		event, // not required, set if you want to open camera archive with definite time from this `VMSEvent`
		jsonTranslations, // required, can be got from `getTranslations(language: String, revision: Int)` response
		videoRates, // required, can be got from `getStatics()` response
		markTypes, // required, can be got from `getStatics()` response
		permissions, // required, can be got from `login()` response 
		allowVibration, // not required, default is `true`, set to `false` if you don't want to use vibration when scrolling the timeline 
	)
	val fragment = VMSPlayerFragment.newInstance(data) // create fragment with VMSPlayerData
	fragment.callbackCameraEventsMLD.postValue(callbackCameraEvents) // required, if you want to set callback action in camera screen
	return fragment
}
val callbackCameraEvents: VMSPlayerCallbackCameraEvents = object: VMSPlayerCallbackCameraEvents {
	override fun onClickOpenEvents(data: VMSCamera) {
		// ... your code to open events screen
	}
}
```

See more details in `playerFragment.md`


### Step 9.

Subscribe to events.

```kotlin
// For example, you can subscribe to vmsCamerasSocket in the `onCreate()` function in your Activity.
vmsCamerasSocket.observe(this) {
	// Here you will find all the information you need to update your cameras.
}
// Send event to update cameras if it needs
vmsCamerasSocket.postValue(VMSCamerasSocket("cameras_updated"))
```


For a fuller understanding instruction, please refer to our comprehensive documentation.
