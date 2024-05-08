@toc API/Requests/Authorization/AuthorizationApi

# AuthorizationApi #

API to login to the app.


## Login

Login to your application. Refer to `VMSBasicStatic` to determine if captcha information is needed for this request.

If you receive a 419 error, it indicates that you need to delete the session.

In that case, repeat the login request with the `sessionId` parameter, which you can obtain from the error.

You shouldn't pass captcha for repeating login.

For more details on this, see the `ApiError` information.

If the request was successful, you'll receive the `VMSLoginResponse` object.

```
@POST(TOKEN)
suspend fun login(@Body loginRequest: VMSLoginRequest): VMSLoginResponse
```

### VMSLoginRequest

An object containing the necessary information for a login request.

`login` - user's login

`password` - user's password

`session_id` - session you want to replace with the new one

`captcha` - captcha user entered from image

`key` - captcha key received from server

We have 3 cases when you can make a login and pass specific parameters:

- login(login, password) - login without captcha

- login(login, password, captcha, key) - login with captcha

- login(login, password, session_id) - login with session id


## Get captcha

If captcha is required for the login, you should initially make a request to obtain it.

Upon a successful request, you will receive a `VMSCaptcha` object.

Note that there is a limit of 20 attempts to send a getCaptcha() request from a single IP address within a 10-minute window.

```
@GET(CAPTCHA)
suspend fun getCaptcha(): VMSCaptcha
```


### VMSCaptcha

Object you receive from the server with the required captcha information for login.

`key` - captcha key needed to login with captcha

`img` - base64 representation of the captcha image, you can convert the received img data into an image

`ttl` - valid time to live of the requested captcha
