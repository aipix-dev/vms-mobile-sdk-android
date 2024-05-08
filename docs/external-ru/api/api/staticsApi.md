@toc API/Requests/StaticsApi

# StaticsApi #

API для получения основной информации от вашего сервера и отправки возможных токенов на сервер.


## Получение всех переводов

Получение переводов данного языка из конкретной версии.

Если запрос прошел успешно, вы получите объект `VMSTranslationObject`.

Для этого запроса токен не требуется.

```
@GET(DICTIONARY)
suspend fun getTranslations(
    @Query(LANGUAGE) language: String? = EN,
    @Query(REVISION) revision: Int
): VMSTranslations
```

`language` - по умолчанию `en`, другие доступные языки можно получить из `available_locales` объекта `VMSBasicStatic`

`revision` - номер ревизии, из которой вы получите изменения в переводах; установите значение `0`, чтобы получить все переводы


## Статика

Получение статической информации, необходимой для работы приложения.

Если запрос прошел успешно, вы получите объект `VMSStatic`.

Для этого запроса требуется токен.

```
@GET(STATIC)
suspend fun getStatics(): VMSStatics
```


### VMSStatic

Объект, получаемый от сервера с информацией, необходимой для выполнения некоторой функциональности.

`camera_issues` — типы отчетов о проблемах с камерой

`video_rates` — скорости видео, доступные для плеера

`mark_types` — доступные типы пользовательских событий

`system_events` — системные события, доступные пользователю

`analytic_types` — типы событий аналитики, доступные пользователю

`analytic_events` — типы событий кейсов аналитики, доступные пользователю


## Базовая статика

Получение информации, необходимой для продолжения входа в систему.

Если запрос прошел успешно, вы получите объект `VMSBasicStatic`.

Для этого запроса нужен токен.

```
@GET(STATIC_BASIC)
suspend fun getBasicStatic(): VMSBasicStatic
```


### VMSBasicStatic

Объект, который вы получаете с сервера, с информацией, необходимой для правильного входа в систему.

`is_captcha_available` — если `true`,  то для входа в систему необходимо отображать информацию о капче

`is_external_auth_enabled` — если `true`, то можно осуществлять вход с помощью внешнего сервиса

`version` — текущая версия бэкенд-сервера

`available_locales` - доступные языковые переводы


## Токены

### FCM (для пушей Google)

Отправьте токен FCM на сервер, если вы используете firebase.

Если запрос прошел успешно, вы получите объект `Response<ResponseBody>`.

```
@PUT(DEVICE)
suspend fun sendFcmToken(@Body fcmRequest: VMSFcmRequest): Response<ResponseBody>
```


### HMS (для пушей Huawei)

Отправьте токен HMS на сервер, если вы используете Huawei Push Kit.

Если запрос прошел успешно, вы получите объект `Response<ResponseBody>`.

```
@PUT(DEVICE)
suspend fun sendHuaweiToken(@Body fcmRequest: VMSHuaweiRequest): Response<ResponseBody>
```
