@toc API/Requests/IntercomApi

# IntercomApi #

API for intercom manipulation.

Fields `from` and `to` - dates in UTC format, see more details in `DateUtils.kt`.


## Get intercoms list

Get list of intercoms. Specify the page for the request.

If the request was successful, you'll get paginated response with the list of intercoms.

```
@GET(INTERCOM_LIST)
suspend fun getIntercomsList(@Query(PAGE) page: String? = "1"): VMSPaginatedResponse<VMSIntercom>
```


## Get intercom codes list

Get list of intercom codes. Specify the page for the request.

If the request was successful, you'll get paginated response with a list of codes.

```
@GET(INTERCOM_CODES)
suspend fun getIntercomCodesList(@Query(PAGE) page: String? = "1"): VMSPaginatedResponse<VMSCodeVisitor>

```


## Get calls list

Get list of intercom calls. Specify the page for the request.

If the request was successful, you'll get paginated response of calls list.

```
@GET(INTERCOM_CALLS)
suspend fun getCallsList(@Query(PAGE) page: String? = "1"): VMSPaginatedResponse<VMSClientApi>
```


## Set intercom settings

Change settings parameters for specific intercom by it's id.

If the request was successful, you will receive updated intercom.

```
@PATCH(INTERCOM_PATCH)
suspend fun changeIntercomSettings(
    @Path(ID) id: String,
    @Body data: VMSIntercomSettingsDataEnabled
): VMSIntercom
```

`id` - intercom id

`@Body data: VMSIntercomSettingsDataEnabled` - data object:
```
@Parcelize
data class VMSIntercomSettingsDataEnabled(
	@SerializedName("is_enabled") val is_enabled: Boolean? = true,
	@SerializedName("timetable") val timetable: VMSTimetable
): Parcelable
```

`timetable` - Intercom call schedule; calls will only be received according to the selected time

If you want to disable intercom, use this command to stop receiving intercom calls for the current device:

```
@PATCH(INTERCOM_PATCH)
suspend fun setIntercomDisabled(
    @Path(ID) id: String,
    @Body data: VMSIntercomSettingsDataDisabled = VMSIntercomSettingsDataDisabled()
): VMSIntercom
```

`id` - intercom id

`@Body data: VMSIntercomSettingsDataDisabled` - data object

```
@Parcelize
data class VMSIntercomSettingsDataDisabled(
    @SerializedName("is_enabled") val is_enabled: Boolean? = false,
): Parcelable
```


### VMSTimetable

Intercom call schedule.

The schedule can be set in two ways:

- by days
- by intervals

You cannot set both parameters at the same time.

```
@Parcelize
data class VMSTimetable(
	@SerializedName("days") val days: ArrayList<VMSTimetableDay>? = null,
	@SerializedName("intervals") val intervals: ArrayList<VMSTimetableInterval>? = null
): Parcelable
```

```
@Parcelize
data class VMSTimetableDay(
	@SerializedName("type") val type: String,
	@SerializedName("from") val from: String?,
	@SerializedName("to") val to: String?
): Parcelable {
	fun getDateFrom() = from?.getLocalDateFromUtc() ?: ""

	fun getDateTo() = to?.getLocalDateFromUtc() ?: ""
}
```
`type` - could be `monday`, `tuesday`, `wednesday`, `thursday`, `friday`, `saturday`, `sunday`, `same_every_day` 

`from` - could be `00:00:00` but should be less than the `to` parameter

`to` - could be `23:59:59` but should be more than the `from` parameter


```
@Parcelize
data class VMSTimetableInterval(
    @SerializedName("from") val from: String? = "",
    @SerializedName("to") val to: String? = ""
): Parcelable {
    fun getDateFrom() = from?.getLocalDateFromUtc() ?: ""

	fun getDateTo() = to?.getLocalDateFromUtc() ?: ""
}
```

`from` - date in UTC format, but should be less than the `to` parameter

`to` - date in UTC format, but should be more than the `from` parameter


## Open door

Open the intercom door.

If the request was successful, you'll get a `Response<Unit>`.

```
@POST(INTERCOM_OPEN_DOOR)
suspend fun openDoor(@Path(ID) id: String): Response<Unit>
```


## Create code

Create a code to physically open the door.

If the request was successful, you'll get `VMSVisitor` object.

```
@POST(INTERCOM_ID_CODES)
suspend fun createCode(
    @Path(ID) id: String,
    @Body data: VMSIntercomCodeData
): VMSCodeVisitor
```

`id` - intercom id

```
@Parcelize
data class VMSIntercomCodeData(
    @SerializedName("title") val title: String?,
    @SerializedName("expired_at") val expired_at: String,
): Parcelable
```

`title` - the name for new code

`expired_at` - date until which this code is valid, UTC format, see `DateUtils.kt` for details


## Delete intercom codes

Delete intercom codes you don't need anymore.

If the request was successful, you'll get a `Response<Unit>`.

```
@HTTP(method = DELETE, path = INTERCOM_CODES, hasBody = true)
suspend fun deleteVisitors(@Body data: VMSIntercomsDeleteData): Response<Unit>
```

`@Body data: VMSIntercomsDeleteData` - data object

```
@Parcelize
data class VMSIntercomsDeleteData(
    @SerializedName("ids") val ids: ArrayList<Int>
): Parcelable
```

`ids` - list with id of each `VMSCodeVisitor` to be deleted

## Delete calls

Deleting unnecessary calls.

If the request was successful, you'll get a `Response<Unit>`.

```
@HTTP(method = DELETE, path = INTERCOM_CALLS, hasBody = true)
suspend fun deleteCalls(@Body data: VMSIntercomsDeleteData): Response<Unit>
```

`ids` - list with id of each `VMSVisitHistory` that be deleted


## Get analytics files

Get list of analytics files. Specify the page for the request.

If the request was successful, you'll get a paginated response of analytics files.

```
@GET(INTERCOM_FILES)
suspend fun getAnalyticsFiles(
    @Path(INTERCOM_PATH) intercom: String,
    @Query(PAGE) page: String,
): VMSPaginatedResponse<VMSAnalyticFile>
```


## Create analytic file

Create an analytic file using `MultipartBody.Part.createFormData` from Retrofit that returns `MultipartBody.Part`.

Specify name for the request. You shouldn't specify a type because the mobile client will always have a `face_resource` type.

If the request was successful, you'll get paginated response from the analytic file.

```
@Multipart
@POST(INTERCOM_FILES)
suspend fun createAnalyticFile(
    @Part image: MultipartBody.Part,
    @Part(NAME) name: String,
    @Part(TYPE) type: String = "face_resource",
): VMSAnalyticFile
```


## Update analytic file

You can change the name of the analytic file.

If the request was successful, you'll get an updated analytic file.

```
@POST(ANALYTIC_GROUP_FILES)
suspend fun updateAnalyticFile(
    @Path(INTERCOM_PATH) intercom: String,
    @Body data: IntercomAnalyticFileName,
): VMSAnalyticFile

data class IntercomAnalyticFileName(
	var name: String
): Serializable
```


## Delete analytic file/files

You can delete an analytic file or files by id, which you can pass in `resources`.

If the request was successful, you'll get a deleted analytic file.

```
@HTTP(method = "DELETE", path = INTERCOM_FILES, hasBody = true)
suspend fun deleteAnalyticFile(
    @Path(INTERCOM_PATH) intercom: String,
    @Body body: IntercomAnalyticFileData,
): VMSAnalyticFile

data class IntercomAnalyticFileData(
	var resources: List<String>,
	var force: Boolean = true
): Serializable
```


## Get intercom events

Get list of intercom events. Specify the page for the request.

If the request was successful, you'll get a paginated response of intercom events.

```
@GET(INTERCOM_EVENTS)
suspend fun getIntercomEvents(@Query(PAGE) page: String? = "1"): VMSPaginatedResponse<VMSEvent>
```

