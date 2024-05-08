@toc API/Requests/Cameras/GroupApi

# GroupApi #

Api для работы с группами камер.


## Получение списка групп

Получить список групп камер. Укажите страницу для запроса.

Если запрос прошел успешно, вы получите ответ с постраничным списком групп.

```
@GET(GROUPS)
suspend fun getGroups(@Query(PAGE) page: String? = "1"): VMSPaginatedResponse<VMSChildGroup>
```


## Создание группы

Создать новую группу камер с указанным именем. Изначально группа пуста.

В случае успешного выполнения запроса ответом будет  `VMSCreateGroupResponse`.

```
@POST(GROUPS)
suspend fun createGroup(@Body name: VMSSimpleName): VMSCreateGroupResponse
```


## Переименование группы

Переименовать конкретную группу по ее идентификатору с новым именем.

В случае успешного выполнения запроса ответом будет `VMSChildGroup`.

```
@PUT(GROUP_CRUD)
suspend fun renameGroup(@Path(ID) id: String, @Body name: VMSSimpleName): VMSChildGroup
```


## Удаление группы

Удаление конкретной группы по ее идентификатору.

Если запрос прошел успешно, ответом будет `VMSStatusResponse`.

```
@DELETE(GROUP_CRUD)
suspend fun deleteGroup(@Path(ID) id: String): VMSStatusResponse
```


## Обновление группы

Обновление группы с указанной информацией.

В случае успешного выполнения запроса ответом будет `VMSChildGroup`.

```
@PUT(GROUP_CRUD)
suspend fun updateGroup(@Path(ID) id: String, @Body group: VMSUpdateGroupRequest): VMSChildGroup
```

`id` — идентификатор группы для указания необходимой группы


### VMSUpdateGroupRequest

Объект, содержащий информацию, необходимую для обновления группы.

`name` — новое имя группы; если вы не хотите менять имя группы - задайте в этом параметре старое имя

`items` — список идентификаторов камер, которые необходимо добавить в данную группу


## Синхронизация групп

Этот запрос синхронизирует указанную камеру со всеми группами пользователей.

В запросе требуется список групп, к которым будет принадлежать данная камера. 

Камера будет удалена из других групп.

В случае успешного выполнения запроса ответом будет объект `VMSGroupSync'.

```
@POST(GROUPS_SYNC)
suspend fun syncGroups(@Path(ID) id: String, @Body groups: VMSGroupSyncRequest): VMSGroupSync
```

`id` — укажите камеру по ее идентификатору

`groups` - указать список идентификаторов групп, в которых будет отображаться камера (камера будет добавлена в группу, если ранее ее там не было)


### VMSGroupSync

Информация о синхронизации групп на стороне сервера.

```
@Parcelize
data class VMSGroupSync(
	@SerializedName("type") val type: String = ""
): Parcelable
```

`sync` — вы получите этот тип, если синхронизация была выполнена

`async` — вы получите этот тип, если у пользователя более 50 групп, серверный запрос будет выполняться асинхронно.

По завершении асинхронного процесса вы получите сообщение от сокета, которое можно обработать. Более подробная информация приведена в разделе `VMSPusherApi`.
