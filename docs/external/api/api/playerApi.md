@toc API/Requests/PlayerApi

# PlayerApi #

API to use within the player, or you can implement and customise it in your own project.

## Get live stream

Get camera live stream in selected quality.

If the request was successful, you'll get an object with a URL to play.

```
@GET(STREAMS)
suspend fun getStreams(
    @Path(ID) id: String,
    @Query(TYPE) type: String? = "high",
    @Query(SOURCE) source: String? = "hls"
): VMSStreamsResponse
```

`id` - `VMSCamera` id

`type` - `low` or `high`

`source` - `hls` or `rtsp`


## Get archive stream

Get camera archive stream.

If the request was successful, you'll get object with url to play.

```
@GET(ARCHIVE)
suspend fun getArchive(
    @Path(ID) id: String,
    @Query(START) start: String,
): VMSStreamsResponse
```

`id` - `VMSCamera` id

`start` - a date from which to play this archive, in UTC format, see `DateUtils.kt` for details


## Get url to download archive

Get URL to download a specific part of the camera archive.

If the request was successful, you'll get a `VMSStatusResponse`.

You'll then receive a socket push with a `VMSArchiveLinkSocket` object containing the generated download url.

```
@GET(CAMERAS_ARCHIVE_LINK)
suspend fun getArchiveLink(
    @Path(ID) id: String,
    @Query(FROM) from: String,
    @Query(TO) to: String
): VMSStatusResponse
```

`id` - `VMSCamera` id

`from` - a date from which this archive should download, UTC format, see `DateUtils.kt` for details

`to` - a date to which this archive should download, UTC format, see more details in `DateUtils.kt`

The time between `from` and `to` should be less than 10 minutes.


## Move camera

Move camera in specified direction, only for cameras with PTZ.

If the request was successful, you'll get a `VMSStatusResponse`.

```
@POST(CAMERAS_MOVE)
suspend fun moveCamera(@Path(ID) id: String, @Body ptz: Any): VMSStatusResponse
```

`id` - `VMSCamera` id

`ptz` - see `PTZ.kt` for more details


## Move camera to default

Move camera to home position, only for cameras with PTZ.

If the request was successful, you'll get a `VMSStatusResponse`.

```
@POST(CAMERAS_MOVE_HOME)
suspend fun moveCameraHome(@Path(ID) id: String): VMSStatusResponse
```

`id` - `VMSCamera` id
