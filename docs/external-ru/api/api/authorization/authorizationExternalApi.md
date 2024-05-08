@toc API/Requests/Authorization/AuthorizationExternalApi

# AuthorizationExternalApi #

API для входа в приложение через внешний API.

Вы должны понимать, как работают deeplink на Android — https://developer.android.com/training/app-links/deep-linking/.

Смотрите `VMSBasicStatic`, чтобы узнать, можете ли вы войти в систему с помощью внешнего API, используя `is_external_auth_enabled`.


## Получение внешнего URL

Изначально вам необходимо получить внешний URL для перенаправления пользователя на авторизацию.

Если запрос успешен, вы получите `url` для загрузки внешней службы.

```
@GET(EXTERNAL_AUTH_URL)
suspend fun authUrlExternal(): VMSAuthUrlExternalResponse
```

С помощью `android.webkit.WebViewClient` создайте виджет `WebView` и проведите авторизацию в веб-клиенте по `url`.

Если авторизация прошла успешно, необходимо получить `code` из url перенаправления и передать его в запрос `loginByExternalUrl`.


## Внешний вход

Войдите в свое приложение с помощью веб-клиента.

Если запрос успешен, вы получите объект `VMSLoginResponse`.

```
@POST(EXTERNAL_AUTH_CALLBACK)
suspend fun loginByExternalUrl(@Body request: VMSExternalAuthCodeRequest): VMSLoginResponse
```

Если вы получаете ошибку 419, это означает, что необходимо удалить сессию. Более подробную информацию см. в разделе `ApiError`.

Для этого повторите запрос, используя параметр `sessionId`, который вы можете получить из ошибки.

```
@POST(EXTERNAL_AUTH_CALLBACK)
suspend fun loginByExternalUrlSession(@Body request: VMSExternalAuthCodeSessionRequest): VMSLoginResponse
```


### VMSExternalAuthCodeRequest

Объект, содержащий информацию, необходимую для внешнего входа в систему. Для входа в систему необходим код.

Пример deeplink - `your_schema://your_domain/login?code=your_code`.

`code` — ваш код, необходимый для входа в систему


### VMSExternalAuthCodeSessionRequest

`key` — ключ входа, необходимый для входа в систему

`sessionId` — идентификатор сессии, который необходимо заменить при возникновении ошибки 419. Более подробная информация приведена в разделе `ApiError`
