@toc Player/VMSPlayerController

# PlayerFragment #


## Initialization

```
PlayerFragment.newInstance(data: VMSPlayerData)
```


## VMSPlayerData

Create fragment using method `newInstance(...)`:

```
VMSPlayerFragment.newInstance(
    VMSPlayerData(
        val camera: VMSCamera,
        val listCameras: ArrayList<VMSCamera>?,
        val event: VMSEvent?,
        val jsonTranslations: com.google.gson.JsonObject,
        val videoRates: ArrayList<Double>,
        val markTypes: ArrayList<VMSEventType>,
        val permissions: List<VMSPermission>,
        val allowVibration: Boolean,
        val screenState: VMSScreenState = VMSScreenState.DEFAULT
    )
)
```

`camera` - the camera you want to open the player for

`listCameras` - if this parameter is set, you can swipe through the player to change the camera displayed

`event` - if set, you can open the archive at a specific time

`jsonTranslations` - a dictionary of translations required within the player; see `StaticsApi` for details

`videoRates` - video speed options available for setting in the archive stream

`markTypes` - marker type options available for setting in the archive settings to be displayed on the timeline

`permissions` - list of permissions required for the player to work properly

`allowVibration` - the default is `true`; set to `false` if you don't want to use vibration when scrolling the timeline

You can set these fields based on what you get from the server.


## VMSPlayerCallbacks

The PlayerFragment uses callbacks:


## VMSPlayerCallbackCameraEvents

The callback must indicate user action to open screen camera events.

`onClickOpenEvents(data: VMSCamera)`


## VMSPlayerCallbackEventsTypes

Variable to pass the types of events displayed within the player.

`chosenEventsTypes: ArrayList<Pair<String, String>>`

The callback must store the types of the chosen events.

`onChooseEventsTypes(data: List<Pair<String, String>>)`


## VMSPlayerCallbackScreenshot

Take a screenshot of the currently displayed camera on the current broadcast date.

`onClickScreenshot(bitmap: Bitmap, camera: VMSCamera, time: Calendar, state: VMSScreenState = VMSScreenState.DEFAULT)`


## VMSPlayerCallbackLogEvent

If you want to log user activity, this callback method provides the action names to pass to your application.

`onLogEvent(value: String)`


## VMSPlayerCallbackErrors

If you want to handle api player errors by yourself, you must implement this interface.

Otherwise all api player errors will be handled within the player by this sdk.

`onHandleErrors(value: ApiError)`


## VMSPlayerCallbackVideoType

If you want to store video type in your application, you need to implement this interface.

`videoType: String` - it needs to pass to player screen

`onSaveVideoType(videoType: String)` - it needs to override in your app


## VMSPlayerCallbackVideoQuality

If you want to save video quality in your application, you need to implement this interface.

`videoQuality: String` - it needs to pass to player screen

`onSaveVideoQuality(videoQuality: String)` - it needs to override in your app


## VMSScreenState

The PlayerFragment has three states with different behaviours.

`DEFAULT` - default case to use this screen with full options

`INTERCOM_PHOTO` - show only live stream and screenshot action

`ARCHIVE` - show archive stream only with all available actions