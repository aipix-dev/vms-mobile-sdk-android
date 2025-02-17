@toc API/Requests/Authorization/AuthorizationApi

# AuthorizationApi #

API для входа в приложение.


## Авторизация

Войдите в свое приложение. Смотрите `VMSCaptcha`, чтобы узнать, нужна ли вам информация о капче для этого запроса.

Если вы получаете ошибку 429, это означает, что вам нужно удалить сеанс. 

В таком случае повторите этот запрос логина с параметром `sessionId`, который вы можете получить из ошибки.

Также, передавать капчу для логина в данном случае не нужно.

Для получения этой информации обратитесь к подробностям `ApiError`.

Если запрос прошел успешно, вы получите объект `VMSLoginResponse`.

```
@POST(TOKEN)
suspend fun login(@Body loginRequest: VMSLoginRequest): VMSLoginResponse
```


### VMSLoginRequest

Объект с необходимой информацией для запроса входа в систему.

`login` — логин пользователя

`password` — пароль пользователя

`session_id` — сессия, которую вы хотите заменить на новую

`captcha` — капча, введенная пользователем с изображения

`key` — ключ капчи, полученный с сервера

Есть 3 способа выполнения логина с передачей разных параметров: 

- login(login, password) - логин без капчи

- login(login, password, captcha, key) - логин используя капчу

- login(login, password, session_id) - логин используя `session_id`

## Получение капчи

Если для входа необходима капча, изначально следует сделать запрос на ее получение.

В случае успешного запроса вы получите объект `VMSCapcha`.

Обратите внимание, что существует ограничение в 20 попыток отправить запрос getCaptcha() с одного IP-адреса в течение 10-минутного окна.

```
@GET(CAPTCHA)
suspend fun getCaptcha(): VMSCaptcha
```


### VMSCaptcha

Объект, который вы получаете с сервера с необходимой информацией для входа в систему.

`key` — ключ капчи, необходимый для входа в систему с капчей

`img` — представление изображения капчи в формате base64, вы можете преобразовать полученные данные img в изображение

`ttl` — допустимое время жизни запрошенной капчи
