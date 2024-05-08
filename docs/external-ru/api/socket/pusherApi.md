@toc API/Socket/VMSPusherApi

# VMSPusherApi #

Использует версию com.pusher:pusher-java-client:2.0.2 с некоторыми исправлениями для целей VMSMobileSDK.

Мы поддерживаем minSdk 28 – 8 Android. Версия 2.4.4 для Android 7 и выше (скоро появится).

https://github.com/pusher/pusher-websocket-java


## Инициализация VMSPusherApi.kt

```
VMSMobileSDK.pusherApi.Builder(
    socketUrl: String,
    appKey: String,
    userToken: String,
    userId: Int,
    accessTokenId: String)
```

`socketUrl` — указанный базовый URL, URL сокета для подключения к WebSocket, смотрите `SocketApi`

`appKey` — ключ приложения для подключения к WebSocket, смотрите в разделе  `SocketApi`

`userToken` — токен текущего пользователя

`userId` — идентификатор текущего пользователя

`accessTokenId` — идентификатор токена доступа текущего пользователя


## Одноэлементный объект VMSPusherApi

`onConnect()` — используйте эту функцию для подключения к WebSocket (при наличии авторизации и открытии приложения или после входа в систему)

`onDisconnect()` — используйте эту функцию для отключения от WebSocket (при наличии авторизации и закрытии приложения или после выхода из системы)


## VMSSocketEvents.kt

`vmsLogoutSocket` — когда пользователь вышел из системы (например, сеанс был удален)
`vmsCamerasSocket` — когда список камер был обновлен для этого пользователя
`vmsCamerasFavoriteSocket` — когда камера добавляется или удаляется из избранного
`vmsCamerasGroupsSocket` — когда для этого пользователя был обновлен список групп камер
`vmsPermissionsSocket` — когда список разрешений был обновлен для этого пользователя
`vmsMarksSocket` — в случае создания, обновления, удаления меток/событий камеры
`vmsIntercomEventSocket` - когда были созданы события домофона
`vmsArchiveLinkSocket` — когда URL загрузки архива был сгенерирован успешно
`vmsIntercomKeySocket` — при подтверждении нажатия ключа домофона или возникновении ошибок
`vmsIntercomSocket` — при сохранении, переименовании, обновлении, удалении домофона
`vmsVisitorSocket` — при добавлении или удаления кода нового посетителя
`vmsVisitHistorySocket` — когда звонок был добавлен или удален из истории звонков
`vmsCancelCallSocket` — при получении сокета отмены для домофона по замкнутому вызову
`vmsAnalyticEventCreatedSocket` - когда было добавлено событие аналитики
`vmsErrorSocket` — при возникновении ошибки сокета, необходимо вызвать `getSocketUrl()`, когда запрос будет выполнен успешно, то перезапустите сокет и повторно подключитесь, используя этот код:

```kotlin
VMSMobileSDK.pusherApi.Builder(
	socketUrl,
	appKey,
	userToken,
	userId,
	accessTokenId,
).apply {
	VMSMobileSDK.pusherApi.isNeedReconnectSocket = false
	VMSMobileSDK.pusherApi.onConnect()
}
```


## VMSPusherError.kt

`vmsPusherErrorHandler` — реализуйте этот `BehaviorSubject` для получения ошибок от pusher с объектом `VMSPusherError`
