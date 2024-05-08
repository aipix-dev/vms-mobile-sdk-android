@toc API/Requests/WidgetApi

# WidgetApi #

API для получения информации с сервера для виджетов устройства.


### Получение списка камер

Получение списка подробной информации о камерах с указанными идентификаторами.

Если запрос прошел успешно, вы получите список объектов `VMSCamera`.

```
@GET(CAMERAS_EXISTED)
suspend fun getWidgetCameras(@Query(IDS_ARRAY) shows: ArrayList<Int>): ArrayList<VMSCamera>
```


### Получение превью камеры

Получите предварительный просмотр камеры. Вы получите файл `.mp4` камеры по одному кадру.

Если запрос был успешным, вы получите URL для загрузки кадра.

```
@GET(CAMERAS_PREVIEW)
suspend fun getCamerasPreview(
    @Path(ID) id: String,
    @Query(DATE) date: String? = null
): VMSUrlPreviewResponse
```


### Получение списка домофонов

Получить список подробной информации о домофонах с указанными идентификаторами.

Если запрос прошел успешно, вы получите список объектов `VMSIntercom`.

```
@GET(INTERCOM_EXISTED)
suspend fun getWidgetIntercoms(@Query(IDS_ARRAY) shows: ArrayList<Int>): ArrayList<VMSIntercom>
```
