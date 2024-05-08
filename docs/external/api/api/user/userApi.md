@toc API/Requests/User/UserApi

# UserApi #

API to retrieve user information, change their password and log them out of the application.

## Get user

Get current user information.

If the request was successful, you'll get a `VMSUser` object.

```
@GET(USER_SELF)
suspend fun getUser(): VMSUser
```


## Change password

Change the password of the current authorised user.

If the request was successful, you'll get a `ResponseBody`.

```
@PUT(USER_SELF)
suspend fun savePassword(@Body group: VMSChangePasswordRequest): ResponseBody
```


### VMSChangePasswordRequest

Object containing the information needed to change the password.

`password` and `password_confirmation` should match, `current_password` is your old password.


## Logout

Logout current authorized user.

If the request was successful, you'll get `ResponseBody`.

```
@POST(LOGOUT)
suspend fun logout(): ResponseBody
```
