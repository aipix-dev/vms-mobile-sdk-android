@toc API/Requests/PlayerApi

# PlayerApi #

API для использования внутри плеера, или вы можете реализовать и настроить его в своем собственном проекте.

## Получение live потока

Получение прямой трансляции с камеры в выбранном качестве.

Если запрос прошел успешно, вы получите объект с URL для воспроизведения.

```
@GET(STREAMS)
suspend fun getStreams(
    @Path(ID) id: String,
    @Query(TYPE) type: String? = "high",
    @Query(SOURCE) source: String? = "hls"
): VMSStreamsResponse
```

`id` - идентификатор `VMSCamera`

`type` - `low` или `high`

`source` - `hls` или `rtsp`


## Получение архивного потока

Получение архивного потока с камеры.

Если запрос прошел успешно, вы получите URL для воспроизведения.

```
@GET(ARCHIVE)
suspend fun getArchive(
    @Path(ID) id: String,
    @Query(START) start: String,
): VMSStreamsResponse
```

`id` - идентификатор `VMSCamera`

`start` - дата, начиная с которой будет воспроизводиться данный архив, в формате UTC, подробнее см. в файле `DateUtils.kt`


## Получение URL для скачивания архива

Получение URL для скачивания определенной части архива камеры.

Если запрос прошел успешно, ответ вернет `VMSStatusResponse`.

Затем вы получите сокет push с объектом `VMSArchiveLinkSocket`, содержащим сгенерированный URL загрузки.

```
@GET(CAMERAS_ARCHIVE_LINK)
suspend fun getArchiveLink(
    @Path(ID) id: String,
    @Query(FROM) from: String,
    @Query(TO) to: String
): VMSStatusResponse
```

`id` - идентификатор `VMSCamera`

`from` - дата, с которой должен скачиваться данный архив, формат UTC, подробнее см. в файле `DateUtils.kt`

`to` - дата, до которой должен быть загружен архив, формат UTC, подробнее см. в файле `DateUtils.kt`

Время между `from` и `to` должно быть меньше 10 минут.


## Перемещение камеры

Перемещение камеры в указанном направлении, только для камер с PTZ.

Если запрос прошел успешно, ответ вернет `VMSStatusResponse`.

```
@POST(CAMERAS_MOVE)
suspend fun moveCamera(@Path(ID) id: String, @Body ptz: Any): VMSStatusResponse
```

`ptz` - подробнее см. раздел `PTZ.kt`

## Перемещение камеры в положение по умолчанию

Перемещение камеры в исходное положение, только для камер с PTZ.

Если запрос прошел успешно, ответ вернет `VMSStatusResponse`.

```
@POST(CAMERAS_MOVE_HOME)
suspend fun moveCameraHome(@Path(ID) id: String): VMSStatusResponse
```

`id` - идентификатор `VMSCamera`
