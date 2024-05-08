@toc API/Requests/User/SessionsApi

# SessionsApi #

API для работы с различными пользовательскими сессиями.


## Получение списка сессий

Получить список различных сессий.

Если запрос прошел успешно, вы получите список объектов `VMSSession`.

```
@GET(SESSIONS_LIST)
suspend fun getSessions(): ArrayList<VMSSession>
```


## Удаление сеанса

Удаление конкретной сессии с заданным идентификатором.

Если запрос был выполнен успешно, вы получите `VMSStatusResponse`.

```
@POST(SESSIONS_ID)
suspend fun deleteSession(@Path(ID) id: String): VMSStatusResponse
```
