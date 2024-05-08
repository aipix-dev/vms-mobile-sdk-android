@toc API/Requests/Cameras/CameraEventsApi

# CameraEventsApi #

API для работы с событиями внутри плеера.

Поля `from` и `to` - даты в формате UTC, подробнее см. в файле `DateUtils.kt`


## Получение всех событий

Для получения всех событий камеры за указанный период времени и типов событий.

Для получения всех возможных типов событий смотрите `VMSStatics`.

Чтобы вернуть события всех возможных типов, укажите пустой список.

```
@GET(CAMERAS_MARKS)
suspend fun getCameraMarks(
    @Path(ID) id: String,
    @Query("from") from: String,
    @Query("to") to: String,
    @Query("types[]") types: List<String>
): List<VMSEvent>
```


## Получение ближайшего события

Получение ближайшего события, следующего или предыдущего по отношению к текущей дате, в архиве камеры.

Для получения всех возможных типов событий см. раздел `VMSStatics`.

Для получения следующего или предыдущего события необходимо прибавить или отнять 1 секунду от даты создания события.

```
@GET(CAMERAS_NEAREST_EVENT)
suspend fun getNearestEvent(
    @Path(ID) idCamera: String,
    @Query("from") from: String,
    @Query("types[]") types: List<String>,
    @Query("rewind") direction: String
): VMSNearestEvent
```

`types` - список имен массива `mark_types` объекта `VMSStatics`

`direction` - может быть `previous` или `next`


# Create, update, delete event

Для создания события с помощью VMSEventCreateData задаются имя и дата создания.

## Создание события

```
@POST(CAMERAS_EVENTS)
suspend fun createEvent(
    @Path(ID) idCamera: String,
    @Body createData: VMSEventCreateData
): VMSEvent
```

Чтобы обновить событие, задайте его имя или дату создания с помощью `VMSEventCreateData`

## Обновление события

```
@PUT(CAMERAS_ACCESS_MARK)
suspend fun updateEvent(
    @Path(ID) cameraId: Int,
    @Path(MARK_ID) eventId: Int,
    @Body createData: VMSEventCreateData
): VMSEvent
```


## Удаление события


```
@DELETE(CAMERAS_ACCESS_MARK)
	suspend fun deleteEvent(
		@Path(ID) cameraId: String,
		@Path(MARK_ID) eventId: String
	): ResponseBody
```

Вы можете создать или обновить событие, установить имя или дату создания, используя `VMSEventCreateData`

```
@Parcelize
data class VMSEventCreateData(
	@SerializedName("title") val title: String = "",
	@SerializedName("from") val from: String = "",
	@SerializedName("to") val to: String? = null
): Parcelable
```

`title` - имя события, обязательно

`from` - дата создания события в формате UTC, обязательно, подробнее см. в файле `DateUtils.kt`

`to` - дата окончания события в формате UTC, не обязательно, подробнее см. в `DateUtils.kt`
