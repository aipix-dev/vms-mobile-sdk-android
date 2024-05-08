@toc Player/VMSPlayerController

# PlayerFragment #


## Инициализация

```
PlayerFragment.newInstance(data: VMSPlayerData)
```


## VMSPlayerData

Создать фрагмент, используя метод `newInstance(...)`:

```
newInstance(
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

`camera` — камера, для которой вы хотите открыть плеер

`listCameras` — если этот параметр установлен, то можно пролистывать плеер свайпом, чтобы изменить отображаемую камеру

`event` — если задан этот параметр, то можно открыть архив в определенное время

`jsonTranslations` — словарь переводов, необходимых для работы плеера; подробнее см. раздел `StaticsApi`.

`videoRates` — параметры скорости видео, доступные для установки в архивном потоке

`markTypes` — параметры типов меток, доступные для установки в настройках архива, для отображения на временной шкале

`permissions` — список разрешений, необходимых для корректной работы плеера

`allowVibration` — по умолчанию `true`; установите значение `false`, если вы не хотите использовать вибрацию при прокрутке временной шкалы

Вы можете установить эти поля, основываясь на данных, полученных от сервера.


## VMSPlayerCallbacks

В PlayerFragment используются обратные вызовы:


## VMSPlayerCallbackCameraEvents

Обратный вызов должен указывать на действие пользователя по открытию экрана событий камеры.

`onClickOpenEvents(data: VMSCamera)`


## VMSPlayerCallbackEventsTypes

Переменная для передачи отображаемых типов событий внутри проигрывателя.

`chosenEventsTypes: ArrayList<Pair<String, String>>`

Обратный вызов должен хранить типы выбранных событий.

`onChooseEventsTypes(data: List<Pair<String, String>>)`


## VMSPlayerCallbackScreenshot

Делает снимок отображаемой в данный момент камеры с текущей датой трансляции.

`onClickScreenshot(bitmap: Bitmap, camera: VMSCamera, time: Calendar, state: VMSScreenState = VMSScreenState.DEFAULT)`


## VMSPlayerCallbackLogEvent

Если вы хотите регистрировать активность пользователя, этот метод обратного вызова предоставляет имена действий, которые будут переданы в ваше приложение.

`onLogEvent(value: String)`


## VMSPlayerCallbackErrors

Если вы хотите самостоятельно обрабатывать ошибки API-плеера, вам следует реализовать этот интерфейс.

В противном случае все ошибки API-плеера будут обрабатываться внутри плеера с помощью этого SDK.

`onHandleErrors(value: ApiError)`


## VMSPlayerCallbackVideoType

Если вы хотите сохранить тип видео в своем приложении, вы должны реализовать этот интерфейс.

`videoType: String` - он должен передаваться на экран проигрывателя

`onSaveVideoType(videoType: String)` - он должен быть переопределен в вашем приложении


## VMSPlayerCallbackVideoQuality

Если вы хотите сохранить качество видео в своем приложении, вы должны реализовать этот интерфейс.

`videoQuality: String` - он должен передаваться на экран проигрывателя

`onSaveVideoQuality(videoQuality: String)` - он должен быть переопределен в вашем приложении


## VMSScreenState

PlayerFragment имеет три состояния с разным поведением.

`DEFAULT` — вариант по умолчанию для использования этого экрана с полными опциями

`INTERCOM_PHOTO` — показывать только прямую трансляцию и действие скриншота

`ARCHIVE` - показывать только поток архива со всеми доступными в нем действиями