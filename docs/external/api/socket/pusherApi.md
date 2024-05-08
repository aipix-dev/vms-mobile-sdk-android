@toc API/Socket/VMSPusherApi

# VMSPusherApi #

Uses com.pusher:pusher-java-client:2.0.2() version with some fixes for VMSMobileSDK purposes.

We supported minSdk 28 - 8 Android. Latest version of pusher - 2.4.4 - for Android 7 and above (coming soon).

https://github.com/pusher/pusher-websocket-java


## Initialization VMSPusherApi.kt

```
VMSMobileSDK.pusherApi.Builder(
    socketUrl: String,
    appKey: String,
    userToken: String,
    userId: Int,
    accessTokenId: String)
```

`socketUrl` - the specified base URL, the socket URL to connect to the WebSocket, see `SocketApi` for how to get this information

`appKey` - app key to connect to WebSocket, see `SocketApi` for how to get this information

`userToken` - token of the current user

`userId` - id of the current user

`accessTokenId` - access token id of the current user


## VMSPusherApi singleton object

`onConnect()` - use this function to connect to the WebSocket (if you have authorization and open the application or after login)

`onDisconnect()` - use this function to disconnect from the WebSocket (if you have authorization and close the application or after logout)


## VMSSocketEvents.kt

`vmsLogoutSocket` - when the user was logged out of the system (e.g. the session was deleted)
`vmsCamerasSocket` - when the list of cameras was updated for this user
`vmsCamerasFavoriteSocket` - when the camera is added or removed from the favorites
`vmsCamerasGroupsSocket` - when the list of camera groups was updated for this user
`vmsPermissionsSocket` - when the list of permissions was updated for this user
`vmsMarksSocket` - when the camera events were created, updated, deleted
`vmsIntercomEventSocket` - when the intercom events were created
`vmsArchiveLinkSocket` - when archive download URL is successfully generated
`vmsIntercomKeySocket` - when the intercom key is confirmed or errors have occurred
`vmsIntercomSocket` - when the intercom is stored, renamed, updated, deleted
`vmsVisitorSocket` - when the code of new visitor was added or deleted
`vmsVisitHistorySocket` - when the call was added or deleted for the call history
`vmsCancelCallSocket` - when you get the cancel socket for the close ring intercom
`vmsAnalyticEventCreatedSocket` - when analytic event was added
`vmsErrorSocket` - when the socket error happens and need to call `getSocketUrl()`, if the request is completed successfully, then restart pusher-socket and reconnect using this code:

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

`vmsPusherErrorHandler` — implement this `BehaviorSubject` to receive errors from pusher with a `VMSPusherError` object