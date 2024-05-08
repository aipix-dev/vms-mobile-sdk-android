@toc API/Requests/Authorization/AuthorizationExternalApi

# AuthorizationExternalApi #

API to login to the app via an external API.

You should understand how deep linking works on Android - https://developer.android.com/training/app-links/deep-linking/

Refer to `VMSBasicStatic` to determine if you can login with an external API using `is_external_auth_enabled`.


## Get external URL

Initially, you need to obtain an external url to redirect the user for authorisation.

If the request was successful, you'll receive a `url` to load the external service.

```
@GET(EXTERNAL_AUTH_URL)
suspend fun authUrlExternal(): VMSAuthUrlExternalResponse
```

Use `android.webkit.WebViewClient` to create `WebView` widget and make authorisation in your web client using `url`.

If your authorisation was successful, you should get `code` from the redirect url and pass it to the `loginByExternalUrl` request.


## External login

Login to your application using a web client.

If the request was successful, you'll receive a `VMSLoginResponse` object.

```
@POST(EXTERNAL_AUTH_CALLBACK)
suspend fun loginByExternalUrl(@Body request: VMSExternalAuthCodeRequest): VMSLoginResponse
```

If you get a 419 error, it means you need to delete the session. See `ApiError` for more details.

To do this, repeat the request using the `sessionId` parameter that you can get from the error.

```
@POST(EXTERNAL_AUTH_CALLBACK)
suspend fun loginByExternalUrlSession(@Body request: VMSExternalAuthCodeSessionRequest): VMSLoginResponse
```


### VMSExternalAuthCodeRequest

Object containing information required for external login. You need a code to be able to login.

Deeplink example - `your_schema://your_domain/login?code=your_code`.

`code` - your code needed to login


### VMSExternalAuthCodeSessionRequest

`key` - key required to login

`sessionId` - session id to replace if you get a 419 error. See `ApiError` for more details.
