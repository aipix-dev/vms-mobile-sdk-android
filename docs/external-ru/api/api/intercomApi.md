@toc API/Requests/IntercomApi

# IntercomApi #

API для работы с домофонами.

Поля `from` и `to` - даты в формате UTC, подробнее см. в файле `DateUtils.kt`.


## Получение списка домофонов

Получение списка домофонов. Укажите страницу для запроса.

Если запрос прошел успешно, вы получите постраничный ответ из списка домофонов.

```
@GET(INTERCOM_LIST)
suspend fun getIntercomsList(@Query(PAGE) page: String? = "1"): VMSPaginatedResponse<VMSIntercom>
```


## Получение списка кодов домофона

Получение списка кодов домофона. Укажите страницу для запроса.

Если запрос прошел успешно, вы получите постраничный ответ из списка кодов.

```
@GET(INTERCOM_CODES)
suspend fun getIntercomCodesList(@Query(PAGE) page: String? = "1"): VMSPaginatedResponse<VMSCodeVisitor>

```


## Получение списка звонков

Получение списка звонков. Укажите страницу для запроса.

Если запрос прошел успешно, вы получите постраничный ответ из списка звонков.

```
@GET(INTERCOM_CALLS)
suspend fun getCallsList(@Query(PAGE) page: String): VMSPaginatedResponse<VMSClientApi>
```


## Задать настройки домофона

Изменить параметры настроек конкретного домофона по его идентификатору.

Если запрос прошел успешно, вы получите обновленный домофон.

```
@PATCH(INTERCOM_PATCH)
suspend fun changeIntercomSettings(
    @Path(ID) id: String,
    @Body data: VMSIntercomSettingsDataEnabled
): VMSIntercom
```

`id` - intercom id

`@Body data: VMSIntercomSettingsDataEnabled` - data objects:
```
@Parcelize
data class VMSIntercomSettingsDataEnabled(
	@SerializedName("is_enabled") val is_enabled: Boolean? = true,
	@SerializedName("timetable") val timetable: VMSTimetable
): Parcelable
```

`timetable` - расписание домофонных звонков; звонки будут приниматься только в соответствии с выбранным временем

Если вы хотите отключить домофон, используйте этот запрос, чтобы прекратить прием звонков с домофона для текущего устройства:

```
@PATCH(INTERCOM_PATCH)
suspend fun setIntercomDisabled(
    @Path(ID) id: String,
    @Body data: VMSIntercomSettingsDataDisabled = VMSIntercomSettingsDataDisabled()
): VMSIntercom
```

`id` - идентификатор домофона

`@Body data: VMSIntercomSettingsDataDisabled` - объект данных

```
@Parcelize
data class VMSIntercomSettingsDataDisabled(
    @SerializedName("is_enabled") val is_enabled: Boolean? = false,
): Parcelable
```


### VMSTimetable

Расписание домофонных звонков.

Расписание может быть задано двумя способами:

- по дням
- по интервалам

Одновременная установка обоих параметров невозможна.

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
`type` - может иметь значение `monday`, `tuesday`, `wednesday`, `thursday`, `friday`, `saturday`, `sunday`, `same_every_day`

`from` - может иметь значение `00:00:00` но должно быть меньше параметра `to`

`to` - could be `23:59:59` но должно быть больше параметра `from`


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

`from` - дата в формате UTC, но должна быть меньше параметра `to`

`to` - дата в формате UTC, но должна быть больше параметра `from`


## Открытие двери

Открыть дверь домофона.

Если запрос прошел успешно, ответом будет `Response<Unit>`.

```
@POST(INTERCOM_OPEN_DOOR)
suspend fun openDoor(@Path(ID) id: String): Response<Unit>
```


## Создание кода

Создать код для физического открытия двери.

Если запрос прошел успешно, вы получите объект `VMSVisitor`.

```
@POST(INTERCOM_ID_CODES)
suspend fun createCode(
    @Path(ID) id: String,
    @Body data: VMSIntercomCodeData
): VMSCodeVisitor
```

`id` — идентификатор домофона

```
@Parcelize
data class VMSIntercomCodeData(
@SerializedName("title") val title: String?,
@SerializedName("expired_at") val expired_at: String,
): Parcelable
```

`title` — название для нового кода

`expired_at` - дата, до которой действует данный код, формат UTC, подробнее см. в файле `DateUtils.kt`


## Удаление кодов домофона

Удаление ненужных кодов домофонов.

Если запрос прошел успешно, ответом будет `Response<Unit>`.

```
@HTTP(method = DELETE, path = INTERCOM_CODES, hasBody = true)
suspend fun deleteVisitors(@Body data: VMSIntercomsDeleteData): Response<Unit>
```

`@Body data: VMSIntercomsDeleteData` - объект данных

```
@Parcelize
data class VMSIntercomsDeleteData(
@SerializedName("ids") val ids: ArrayList<Int>
): Parcelable
```

`ids` - список с идентификаторами каждого удаляемого `VMSCodeVisitor`.


## Удаление звонков

Удаление ненужных звонков.

Если запрос прошел успешно, ответом будет `Response<Unit>`.

```
@HTTP(method = DELETE, path = INTERCOM_CALLS, hasBody = true)
suspend fun deleteCalls(@Body data: VMSIntercomsDeleteData): Response<Unit>
```

`ids` -  список с идентификаторами каждой удаляемой `VMSVisitHistory`


## Get analytics files

Получить список файлов аналитики. Укажите страницу для запроса.

Если запрос прошел успешно, вы получите постраничный ответ с файлами аналитики.

```
@GET(INTERCOM_FILES)
suspend fun getAnalyticsFiles(
    @Path(INTERCOM_PATH) intercom: String,
    @Query(PAGE) page: String,
): VMSPaginatedResponse<VMSAnalyticFile>
```


## Create analytic file

Создайте файл аналитики, используя `MultipartBody.Part.createFormData` из Retrofit, который возвращает `MultipartBody.Part`.

Укажите имя для запроса. Тип указывать не стоит, так как в мобильном клиенте всегда будет указан тип `face_resource`.

Если запрос прошел успешно, вы получите постраничный ответ из аналитического файла.

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

Вы можете изменить имя файла аналитики.

Если запрос прошел успешно, вы получите обновленный файл аналитики.

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

Удалить аналитический файл или файлы можно по id, который можно передать в `resources`.

Если запрос прошел успешно, вы получите удаленный файл аналитики.

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

Получить список событий домофона. Укажите страницу для запроса.

Если запрос прошел успешно, вы получите постраничный ответ о событиях домофона.

```
@GET(INTERCOM_EVENTS)
suspend fun getIntercomEvents(@Query(PAGE) page: String? = "1"): VMSPaginatedResponse<VMSEvent>
```

