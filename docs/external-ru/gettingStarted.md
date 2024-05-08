@toc Getting started

# Инструкция по реализации VMSMobileSDK #

### Примеры для Android, написанные на языке Kotlin.

ПРИМЕЧАНИЕ: У вас должно быть как минимум:

- minSdk = 28
- compileSdk = 34
- buildToolsVersion = '34.0.0'



### Step 1.

Инициализируйте VMSMobileSDK в классе наследнике Application в методе onCreate(), как показано ниже.

```kotlin
// ... ваш код
override fun onCreate() {
	VMSMobileSDK.Builder(
		application = this, // требуется
		baseUrl = "https://your_backend_baseurl", // требуется
		uuid = "your stored uuid", // требуется, см. подробнее https://developer.android.com/reference/java/util/UUID
		language = "en" // не требуется, по умолчанию "en", cмотрите поле `available_locales` из объекта `VMSBasicStatic`
	).apply {
		if (your_user_token.isNotEmpty()) {
			// Данный код будет выполняться только для авторизованного пользователя, смотрите шаг 2
			VMSMobileSDK.userToken = your_user_token
		}
	}
// ... ваш код
}
```

* UUID можно получить с помощью следующего метода:

```kotlin
val uuid = Secure.getString(context.contentResolver, Secure.ANDROID_ID)
```

После этого вы можете сохранить приложение любым удобным для вас способом.


### Step 2.

С помощью функции `createServiceClientCoroutines()` создаем API клиента сервиса и выполняем запросы.

Для создания API клиента сервиса мы используем библиотеку "Retrofit", как показано ниже:

```kotlin
fun createServiceClient(url: String): ApiClientObservable = VMSClientApi.createServiceClientObservable(url)

fun createServiceClient(url: String): ApiClientCoroutines = VMSClientApi.createServiceClientCoroutines(url)
```

Вы можете использовать этот API клиента сервиса для выполнения запросов. Например, без токена доступа можно выполнить следующие 2 запроса:

`getTranslations(language: String, revision: Int)` - получает JSON, содержащий словарь, который может быть использован в вашем приложении; он также используется в `VMSPlayerFragment`

Подробнее о параметрах `language` и `revision` см. в файле `staticsApi.md`.

`getStaticBasic()` - полезен, если ваше приложение требует доступа к капче и внешней аутентификации через веб-сайт

```kotlin
// ... ваш код

//пример observable:
fun getTranslations(language: String, revision: Int) {
	val apiClient: ApiClientObservable = VMSClientApi.createServiceClient()
	apiClient?.getTranslations(language, revision)
		?.subsIoObsMain()
		?.subscribe({ jsonTranslations ->
			// ... ваш код для хранения jsonTranslations в вашем приложении
		}) {
			// ... ваш код обработки ошибок
			// Обратитесь к SDK-файлу ApiErrors.kt для получения кодов или сообщений об ошибках
		}?.addTo(compositeDisposable)
}
fun getStaticBasic() {
	val apiClient: ApiClientObservable = VMSClientApi.createServiceClientObservable()
	apiClient?.getStaticBasic()
		.subsIoObsMain()
		.subscribe({ result ->
			// ... ваш код для хранения основных статических данных
			prefs.isCaptchaAvailable = result.is_captcha_available == true
			prefs.isExternalAuthEnabled = result.is_external_auth_enabled == true
		}, { error ->
			// ... ваш код обработки ошибок
			// Обратитесь к SDK-файлу ApiErrors.kt для получения кодов или сообщений об ошибках
		}).addTo(compositeDisposable)
}

//пример coroutine:
fun getTranslations(language: String, revision: Int) {
	CoroutineScope(Dispatchers.IO).launch(CoroutineExceptionHandler { _, _ ->
		// ... ваш код обработки ошибок
		// Обратитесь к SDK-файлу ApiErrors.kt для получения кодов или сообщений об ошибках
	}) {
		val result = VMSClientApi.createServiceClientCoroutines().getTranslations(language, revision)
		// ... ваш код для хранения jsonTranslations в вашем приложении
	}
}
fun getStaticBasic() {
	CoroutineScope(Dispatchers.IO).launch(CoroutineExceptionHandler { _, _ ->
        // ... ваш код обработки ошибок
		// Обратитесь к SDK-файлу ApiErrors.kt для получения кодов или сообщений об ошибках
	}) {
		// ... ваш код для хранения основных статических данных
		val result = VMSClientApi.createServiceClientCoroutines().getBasicStatic()
		prefs.isCaptchaAvailable = result.is_captcha_available == true
		prefs.isExternalAuthEnabled = result.is_external_auth_enabled == true
	}
}

```


### Step 3.

Авторизация

Используйте функцию login() с обязательными параметрами `login` и `password`.

Из полученного ответа извлеките данные пользователя для установки в SDK.

```kotlin
fun login() {
    val apiClient: ApiClientObservable = VMSClientApi.createServiceClientObservable()
    apiClient?.login()
        ?.subsIoObsMain()
        ?.subscribe({ response ->
            VMSMobileSDK.userToken = responseaccessToken// требуется, должен быть установлен для авторизованного пользователя для защиты запросов
        }) {
            // ... ваш код обработки ошибок
            // Обратитесь к SDK-файлу ApiErrors.kt для получения кодов или сообщений об ошибках
        }?.addTo(compositeDisposable)
}
```


### Step 4.

Use getStatics() after successful login.

`getStatics()` - get static data for your app

```kotlin
fun getStatics() {
    val apiClient: ApiClientObservable = VMSClientApi.createServiceClient()
    apiClient?.getStatics()
        .subsIoObsMain()
        .subscribe({ result ->
            // ... ваш код для хранения статичных данных
            localData.videoRates = result.video_rates
        }, { error ->
            // ... ваш код обработки ошибок
            // Обратитесь к SDK-файлу ApiErrors.kt для получения кодов или сообщений об ошибках
        }).addTo(compositeDisposable)
}
```


### Step 5.

Pusher сокеты:

`getSocketUrl()` - для использования наших pusher-сокетов выполните эту функцию, чтобы получить URL сокета

```kotlin
// ... ваш код

// Выполните этот запрос после успешного входа в систему.
fun getSocketUrl() {
    apiManager.getSocketUrl()
        .subsIoObsMain()
        .subscribe({ response ->
            if (response.ws_url.isNotEmpty()) {
                
                VMSMobileSDK.pusherApi.Builder(
                            response.ws_url, // обязательно, url websocket  
                            response.app_key, // обязательно, ключ приложения
                            accessToken, // обязательно, из ответа `login()`
                            user.id.toString(), // обязательно, из ответа `login()`
                            user.access_token_id, // требуется, из ответа `login()`
                        )
                VMSMobileSDK.pusherApi.onConnect() // подключение к сокету pusher
            }
        }, { _ ->
            // ... ваш код обработки ошибок
            // Обратитесь к SDK-файлу ApiErrors.kt для получения кодов или сообщений об ошибках
        }).addTo(compositeDisposable)
}
```


### Step 6.

Если вам необходимо использовать FCM для домофонов, убедитесь, что вы отправили токен fcm в наш бэкенд.

Для настройки проекта обратитесь к документации Firebase: https://firebase.google.com/docs/cloud-messaging/android/client?hl=en.

```kotlin
// ... ваш код в FCM сервис класс
fun setTokenToServer() {
		if (!TextUtils.isEmpty(userStorage.token)) {
			apiManager?.sendDeviceToken(prefs.tokenFCM)?.subsIoObsMain()?.subscribe({
				//ничего не делать
			}, {
			    //ничего не делать
			})?.addTo(compositeDisposable)
		}
	}
```


### Step 7.

Чтобы получить список камер, выполните следующие запросы:

`getCamerasTree()` - получение всех камер, доступных для учетной записи пользователя

`getFavorites()` - получить все избранные камеры, если таковые были добавлены

`getGroups()` - получить все группы камер, если таковые были созданы


### Step 8.

Экран плеера.

Для открытия экрана плеера используйте следующий фрагмент кода:

```kotlin
// ... ваш код
fun openPlayerScreen(): VMSPlayerFragment {
	val data = VMSPlayerData(
		camera, // требуется, камера может быть получена из ответа `getCamerasTree()`
		listOfCameras, // не требуется, камера может быть получена из ответа `getCamerasTree()`; устанавливается, если требуется возможность перелистывания между камерами
		event, // не требуется, устанавливается, если необходимо открывать архив камер с определенным временем из данного `VMSEvent`
		jsonTranslations, // требуется, можно получить из ответа `getTranslations(language: String, revision: Int)`
		videoRates, // требуется, можно получить из ответа `getStatics()`
		markTypes, // требуется, может быть получено из ответа `getStatics()`
		permissions, // требуется, может быть получен из ответа `login()`
		allowVibration, // не требуется, по умолчанию `true`, установите значение `false`, если не хотите использовать вибрацию при прокрутке временной шкалы 
	)
	val fragment = VMSPlayerFragment.newInstance(data) // создать фрагмент с VMSPlayerData
	fragment.callbackCameraEventsMLD.postValue(callbackCameraEvents) // требуется, если необходимо задать действия обратного вызова на экране камеры
    return fragment
}
val callbackCameraEvents: VMSPlayerCallbackCameraEvents = object: VMSPlayerCallbackCameraEvents {
	override fun onClickOpenEvents(data: VMSCamera) {
		// ... ваш код чтобы октрыть экран событий камеры
	}
}
```

Более подробную информацию можно найти в файле `playerFragment.md`


### Step 9.

Подписка на события.

```kotlin
// Например, вы можете подписаться на vmsCamerasSocket в функции `onCreate()` в вашей Activity.
vmsCamerasSocket.observe(this) {
	//  Здесь вы найдете всю информацию, необходимую для обновления камер.
}
// Отправляем событие для обновления камер, если оно необходимо
vmsCamerasSocket.postValue(VMSCamerasSocket("cameras_updated"))
```


Для более полного понимания инструкции обратитесь к нашей исчерпывающей документации.