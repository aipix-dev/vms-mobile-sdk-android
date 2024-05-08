@toc API/Requests/Cameras/EventsListApi

# EventsListApi #

API to retrieve all user events.

Fields `from` and `to` - dates in UTC format, see `DateUtils.kt` for details.


### Get cameras with analytics

Get a list of cameras by page and if necessary search where the user has analytics on.

```
@GET(CAMERAS_WITH_ANALYTICS)
suspend fun getCamerasList(
    @Query(SEARCH) search: String? = null,
    @Query(FILTER) filter: String = "analytics",
    @Query(PAGE) page: String? = "1"
): VMSPaginatedResponse<VMSCamera>
```

`search` - you need at least 3 symbols to get result

You can get paginated list by url.

```
@GET
suspend fun getCamerasList(@Url url: String): VMSPaginatedResponse<VMSCamera>
```


## Events

### Get system events

Get a list of system events.

```
@GET(EVENTS)
suspend fun getEventsSystem(
    @Query(PAGE) page: String? = "1",
    @Query(CAMERAS_ARRAY) cameras: List<Int>? = null,
    @Query(SORT) sort: String,
    @Query(DIR) dir: String,
    @Query(DATE) date: String? = null,
    @Query(TIMEZONE) timezone: String? = null,
    @Query(FROM) from: String? = null,
    @Query(TO) to: String? = null,
): VMSPaginatedResponse<VMSEvent>
```

`from` - creation date of the event in UTC format, required, see more details in `DateUtils.kt`

`to` - end date of the event in UTC format, not required, see more details in `DateUtils.kt`


### Get user's events

```
@GET(MARKS)
suspend fun getEventsMarks(
    @Query(PAGE) page: String? = "1",
    @Query(CAMERAS_ARRAY) cameras: List<Int>? = null,
    @Query(SORT) sort: String,
    @Query(DIR) dir: String,
    @Query(DATE) date: String? = null,
    @Query(TIMEZONE) timezone: String? = null,
    @Query(FROM) from: String? = null,
    @Query(TO) to: String? = null,
): VMSPaginatedResponse<VMSEvent>
```


### Get analytics events

```
@GET(ANALYTIC_CASE_EVENTS)
suspend fun getEventsAnalytic(
    @Query(PAGE) page: String? = "1",
    @Query(EVENTS_ARRAY) events: List<String>? = null,
    @Query(ANALYTIC_TYPES) types: List<String>? = null,
    @Query(CAMERAS_ARRAY) cameras: List<Int>? = null,
    @Query(IDS_ARRAY) cases: List<Int>? = null,
    @Query(DIR) dir: String,
    @Query(DATE) date: String? = null,
    @Query(TIMEZONE) timezone: String? = null,
    @Query(FROM) from: String? = null,
    @Query(TO) to: String? = null,
): VMSPaginatedResponse<VMSEvent>
```


### Get analytics cases

Get all available event cases for the current `event_types`. See `VMSStatics` for all available event types.

You can get list event cases by `name` of `VMSEventType` object. 

Also if you have this list event cases then you can get list events analytics by `type` of `VMSEventType` object.

```
@GET(ANALYTIC_CASE)
suspend fun getEventsAnalyticCases(@Query(TYPES_ARRAY) types: List<String>): VMSPaginatedResponse<VMSAnalyticCase>
```
