@toc API/Requests/CallsApi

# CallsApi #

API to work with SIP calls from intercom.

It is assumed that you know how to work with  FCM, SIP, RTC, WebSocket.

All requests have `id` of object `VMSIntercomData` from fcm data-push with event `VMSCallSocket`.


## Get call status

If you are using FCM or HMS, you can handle remote messages. 

Use this request to check the call status when you get the push `intercom_ring`.

If the request was successful, you'll get a `VMSIntercomCall` object. 

If `status == "ring"` you can call `callAnswered()`.

```
@GET(CALLS_STATUS)
suspend fun callStatus(@Path(ID) id: String): VMSIntercomCall
```


## Call answered

Use this request to tell the server that the call has been answered on the current device.

If the request was successful, you'll get a `VMSIntercomAnswer` object. Use it to start the SIP session for this call.

```
@POST(CALLS_ANSWER)
suspend fun callAnswered(@Path(ID) id: String): VMSIntercomAnswer
```


## Call canceled

Use this request to tell the server that the call has been cancelled on the current device if the call hasn't been started.

If the request was successful, you'll get a `Response<Unit>`.

```
@POST(CALLS_CANCEL)
suspend fun callCanceled(@Path(ID) id: String): Response<Unit>
```


## Call ended

Use this request to tell the server that the call has ended on the current device when the call was started.

If the request was successful, you'll get a `Response<Unit>`.

```
@POST(CALLS_END)
suspend fun callEnded(@Path(ID) id: String): Response<Unit>
```
