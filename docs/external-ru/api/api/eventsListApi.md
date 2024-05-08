@toc API/Requests/Cameras/EventsListApi

# EventsListApi #

API для получения всех событий пользователя.

Поля `from` и `to` - даты в формате UTC, подробнее см. в файле `DateUtils.kt`.


### Получение камер с аналитикой

Получение списка камер по страницам и, при необходимости, поиск по тем, на которых у пользователя есть аналитика.

```
@GET(CAMERAS_WITH_ANALYTICS)
suspend fun getCamerasList(
    @Query(SEARCH) search: String? = null,
    @Query(FILTER) filter: String = "analytics",
    @Query(PAGE) page: String? = "1"
): VMSPaginatedResponse<VMSCamera>
```

`search` - необходимо не менее 3 символов для получения результата

Вы можете получить постраничный список по ссылке.

```
@GET
suspend fun getCamerasList(@Url url: String): VMSPaginatedResponse<VMSCamera>
```


## События

### Получение системных событий

Получение списка системных событий.

```
@GET(EVENTS)
suspend fun getEventsSystem(
    @Query(PAGE) page: String? = "1",
    @Query(CAMERAS_ARRAY) cameras: List<Int>? = null,
    @Query(SORT) sort: String = "created_at",
    @Query(DIR) dir: String = "desc", // 'desc' by default or 'asc'
    @Query(DATE) date: String?,
    @Query(TIMEZONE) timezone: String?,
    @Query(FROM) from: String?,
    @Query(TO) to: String?,
): VMSPaginatedResponse<VMSEvent>
```

`from` - дата создания события в формате UTC, обязательно, подробнее см. в `DateUtils.kt`

`to` - дата окончания события в формате UTC, не обязательно, подробнее см. в `DateUtils.kt`


### Получение пользовательских событий

```
@GET(MARKS)
suspend fun getEventsMarks(
    @Query(PAGE) page: String? = "1",
    @Query(CAMERAS_ARRAY) cameras: List<Int>? = null,
    @Query(SORT) sort: String = "created_at",
    @Query(DIR) dir: String = "desc",// 'desc' by default or 'asc'
    @Query(DATE) date: String?,
    @Query(TIMEZONE) timezone: String?,
    @Query(FROM) from: String?,
    @Query(TO) to: String?,
): VMSPaginatedResponse<VMSEvent>
```


### Получение событий аналитики

```
@GET(ANALYTIC_CASE_EVENTS)
suspend fun getEventsAnalytic(
    @Query(PAGE) page: String? = null,
    @Query(EVENTS_ARRAY) events: List<String>? = null,
    @Query(ANALYTIC_TYPES) types: List<String>? = null,
    @Query(CAMERAS_ARRAY) cameras: List<Int>? = null,
    @Query(IDS_ARRAY) cases: List<Int>? = null,
    @Query(DIR) dir: String = "desc", // 'desc' by default or 'asc'
    @Query(DATE) date: String?,
    @Query(TIMEZONE) timezone: String?,
    @Query(FROM) from: String?,
    @Query(TO) to: String?
): VMSPaginatedResponse<VMSEvent>
```


### Получение кейсов аналитики

Получить все доступные случаи событий для текущего `event_types`. Все доступные типы событий см. в разделе `VMSStatics`.

Вы можете получить список событий по `имени` объекта `VMSEventType`.

Также если у вас есть этот список событий, то вы можете получить аналитику событий списка по `type` объекта `VMSEventType`.

```
@GET(ANALYTIC_CASE)
suspend fun getEventsAnalyticCases(@Query(TYPES_ARRAY) types: List<String>): VMSPaginatedResponse<VMSAnalyticCase>
```
