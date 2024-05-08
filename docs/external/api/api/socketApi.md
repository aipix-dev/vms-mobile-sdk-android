@toc API/Requests/SocketApi

# SocketApi # 

API to get socket information to connect to sockets.


## Get socket information

Get all hte socket information needed to connect.

If the request was successful, you'll get a `VMSSocketResponse` object to set `ws_url` to `VMSPusherApi`.

```
@GET(WS_URL)
suspend fun getSocketUrl(): VMSSocketResponse
```
