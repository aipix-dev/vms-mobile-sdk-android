@toc API/Requests/CallsApi

# CallsApi #

API для работы с SIP-вызовами от домофона.

Предполагается, что вы умеете работать с FCM, SIP, RTC, WebSocket.

Все запросы имеют `id` объекта `VMSIntercomData` из fcm data-push с событием `VMSCallSocket`.


## Получение статуса звонка

Если вы используете FCM или HMS, вы можете работать с удаленными сообщениями. 

Используйте этот запрос, чтобы проверить статус вызова, когда вы получите пуш `intercom_ring`.

Если запрос прошел успешно, вы получите объект `VMSIntercomCall`.

Если `status == "ring"`, то вам необходимо вызвать `callAnswered()`.

```
@GET(CALLS_STATUS)
suspend fun callStatus(@Path(ID) id: String): VMSIntercomCall
```


## Вызов отвечен

С помощью этого запроса можно сообщить серверу, что на текущем устройстве был получен ответ на вызов.

Если запрос прошел успешно, то будет получен объект `VMSIntercomAnswer`. Используйте его для начала SIP-сессии для данного вызова.

```
@POST(CALLS_ANSWER)
suspend fun callAnswered(@Path(ID) id: String): VMSIntercomAnswer
```


## Вызов отменен

Используйте этот запрос, чтобы сообщить серверу об отмене вызова на текущем устройстве, если вызов не был начат.

Если запрос прошел успешно, вы получите `Response<Unit>`.

```
@POST(CALLS_CANCEL)
suspend fun callCanceled(@Path(ID) id: String): Response<Unit>
```


## Звонок окончен

Используйте этот запрос, чтобы сообщить серверу о том, что на текущем устройстве, с которого был начат вызов, вызов завершился.

Если запрос прошел успешно, вы получите `Response<Unit>`.

```
@POST(CALLS_END)
suspend fun callEnded(@Path(ID) id: String): Response<Unit>
```
