@toc API/Requests/User/SessionsApi

# SessionsApi #

API to manipulate different user sessions.


## Get sessions list

Get the list of different sessions.

If the request was successful, you'll receive the list of `VMSSession` object.

```
@GET(SESSIONS_LIST)
suspend fun getSessions(): ArrayList<VMSSession>
```


## Delete session

Delete a specific session with a given id.

If the request was successful, you'll receive a `VMSStatusResponse`.

```
@POST(SESSIONS_ID)
suspend fun deleteSession(@Path(ID) id: String): VMSStatusResponse
```
