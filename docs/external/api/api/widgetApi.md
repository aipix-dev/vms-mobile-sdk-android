@toc API/Requests/WidgetApi

# WidgetApi #

API to get information from server for device widgets.


### Get list of cameras

Get a list of detailed information about cameras with specified ids.

If the request was successful, you'll get a list of `VMSCamera` objects.

```
@GET(CAMERAS_EXISTED)
suspend fun getWidgetCameras(@Query(IDS_ARRAY) shows: ArrayList<Int>): ArrayList<VMSCamera>
```


### Get camera preview

Get camera preview. You will receive an `.mp4` file of camera one frame at a time.

If the request was successful, you'll get the url to download the frame.

```
@GET(CAMERAS_PREVIEW)
suspend fun getCamerasPreview(
    @Path(ID) id: String,
    @Query(DATE) date: String? = null
): VMSUrlPreviewResponse
```


### Get list of intercoms

Get a list of detailed information about intercoms with specified ids.

If the request was successful, you'll get a list of `VMSIntercom` objects.

```
@GET(INTERCOM_EXISTED)
suspend fun getWidgetIntercoms(@Query(IDS_ARRAY) shows: ArrayList<Int>): ArrayList<VMSIntercom>
```
