@toc API/Requests/Cameras/CameraApi

# CameraApi #

API для получения информации о камерах.


## Получение главного дерева

Получение всех камер, имеющихся у пользователя.

Если запрос прошел успешно, вы получите список объектов `VMSCameraTree`.

```
@GET(FLAT_TREE)
suspend fun getCamerasTree(): List<VMSCameraTree>
```


## Поиск камеры

Получение список камер, соответствующих запросу.

Если запрос прошел успешно, вы получите список найденных объектов `VMSCamera`.

```
@GET(FLAT_TREE)
suspend fun getSearchTree(@Query("search") search: String): List<VMSCamera>
```


## Получение камеры

Получение информации о конкретной камере по ее идентификатору.

Если запрос прошел успешно, вы получите объект `VMSCamera`.

```
@GET(CAMERAS_INFO)
suspend fun getCamera(@Path(ID) id: String): VMSCamera
```


## Переименование камеры

Переименование камеры с указанием ее идентификатора и нового имени.

Если запрос прошел успешно, вы получите обновленный объект `VMSCamera`.

```
@POST(CAMERAS_RENAME)
suspend fun renameCamera(
    @Path(CAMERA) id: String,
    @Body request: VMSRenameCameraRequest
): VMSCamera
```


## Отправить отчет

Отправить отчет, если с камерой что-то не так.

Смотрите `VMSStatics`, чтобы получить список возможных проблем в `camera_issues`.

Если запрос прошел успешно, вы получите пустой `ResponseBody`.

```
@POST(CAMERAS_ISSUE)
suspend fun sendReport(@Path(ISSUE_KEY) key: String, @Path(ID) id: String): ResponseBody
```

`issue_key` — идентификатор проблемы, о которой сообщается

`id` — идентификатор камеры, с которой возникла проблема


## Получение превью камеры

Получить предварительный просмотр камеры за определенную дату. Вы получите файл `.mp4` с одним кадром камеры.

Если запрос прошел успешно, вы получите `preview` для загрузки кадра.

```
@GET(CAMERAS_PREVIEW)
suspend fun getCamerasPreview(
    @Path(ID) id: String,
    @Query("date") date: String? = null
): VMSUrlPreviewResponse
```


## VMSUrlPreviewResponse

`id` - идентификатор камеры

`date` - дата камеры кадра, UTC формат, не обязательный, подробнее см. в файле `DateUtils.kt`.