@toc API/Requests/Cameras/CameraApi

# CameraApi #

API for retrieving information about cameras.


## Get main tree

Get all cameras the user has.

If the request was successful, you'll get the list of `VMSCameraTree` objects.

```
@GET(FLAT_TREE)
suspend fun getCamerasTree(): List<VMSCameraTree>
```


## Camera search

Get the list of cameras matching the search.

If the request was successful, you'll get the list of found `VMSCamera` objects.

```
@GET(FLAT_TREE)
suspend fun getSearchTree(@Query("search") search: String): List<VMSCamera>
```


## Get camera

Get specific camera information by camera id.

If the request was successful, you'll get a `VMSCamera` object.

```
@GET(CAMERAS_INFO)
suspend fun getCamera(@Path(ID) id: String): VMSCamera
```


## Rename camera

Rename the camera with it's id and new name.

If the request was successful, you'll get an updated `VMSCamera` object.

```
@POST(CAMERAS_RENAME)
suspend fun renameCamera(
    @Path(CAMERA) id: String,
    @Body request: VMSRenameCameraRequest
): VMSCamera
```


## Send report.

Send a report if there is something wrong with the camera.

See `VMSStatics` for the list of possible issues in `camera_issues`.

If the request was successful, you'll get an empty `ResponseBody`.

```
@POST(CAMERAS_ISSUE)
suspend fun sendReport(@Path(ISSUE_KEY) key: String, @Path(ID) id: String): ResponseBody
```

`issue_key` - id of the reported issue

`id` - id of the camera with the issue


## Get camera preview.

Get a camera preview for a specific date. You will get `.mp4` file of camera one frame.

If the request was successful, you'll get `preview` to download frame.

```
@GET(CAMERAS_PREVIEW)
suspend fun getCamerasPreview(
    @Path(ID) id: String,
    @Query("date") date: String? = null
): VMSUrlPreviewResponse
```

## VMSUrlPreviewResponse.

`id` - camera id

`date` - date of the frame camera, UTC format, not required, see `DateUtils.kt` for more details 