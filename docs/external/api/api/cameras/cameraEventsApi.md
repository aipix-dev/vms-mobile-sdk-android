@toc API/Requests/Cameras/CameraEventsApi

# CameraEventsApi #

API to work with events inside the player.

Fields `from` and `to` - dates in UTC format, see more details in `DateUtils.kt`.


## Get all events

To retrieve all camera events in a specified time period and event types.

See `VMSStatics` to get all possible event types.

To return events of all possible types, specify an empty list.

```
@GET(CAMERAS_MARKS)
suspend fun getCameraEvents(
    @Path(ID) cameraId: String,
    @Query("from") from: String,
    @Query("to") to: String,
    @Query("types[]") types: List<String>
): List<VMSEvent>
```


## Get the nearest event

Get the closest event next or previous to your current date in the camera's archive.

See `VMSStatics` to get all possible event types.

You will need to add or subtract 1 second from the event creation date to get the next or previous event.

```
@GET(CAMERAS_NEAREST_EVENT)
suspend fun getNearestEvent(
    @Path(ID) cameraId: String,
    @Query("from") from: String,
    @Query("types[]") types: List<String>,
    @Query("rewind") direction: String
): VMSNearestEvent
```

`types` - list names an array `mark_types` of `VMSStatics` object

`direction` - could be `previous` or `next`


# Create, update, delete event

To create an event using VMSEventCreateData, the name and creation date are set.

## Create event

```
@POST(CAMERAS_EVENTS)
suspend fun createEvent(
    @Path(ID) cameraId: String,
    @Body createData: VMSEventCreateData
): VMSEvent
```

To update the event, set the name or creation date using `VMSEventCreateData`.

## Update event

```
@PUT(CAMERAS_ACCESS_MARK)
suspend fun updateEvent(
    @Path(ID) cameraId: String,
    @Path(MARK_ID) eventId: String,
    @Body createData: VMSEventCreateData
): VMSEvent
```


## Delete event

```
@DELETE(CAMERAS_ACCESS_MARK)
	suspend fun deleteEvent(
		@Path(ID) cameraId: String,
		@Path(MARK_ID) eventId: String
	): ResponseBody
```


```
@Parcelize
data class VMSEventCreateData(
	@SerializedName("title") val title: String = "",
	@SerializedName("from") val from: String = "",
	@SerializedName("to") val to: String? = null
): Parcelable
```

`title` - name of event, required

`from` - created date of event in UTC format, required, see more details in `DateUtils.kt`

`to` - end date of event in UTC format, not required, see more details in `DateUtils.kt`
