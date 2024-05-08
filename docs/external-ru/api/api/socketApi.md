@toc API/Requests/SocketApi

# SocketApi #

API для получения информации о сокете для подключения к сокетам.


## Получение информации о сокете

Получение всей информации о сокете, необходимой для подключения.

Если запрос прошел успешно, вы получите объект `VMSSocketResponse`, чтобы установить значение `ws_url` для `VMSPusherApi`.

```
@GET(WS_URL)
suspend fun getSocketUrl(): VMSSocketResponse
```
