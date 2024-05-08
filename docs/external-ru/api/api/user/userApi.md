@toc API/Requests/User/UserApi

# UserApi #

API для получения информации о пользователе, изменения его пароля и выхода из приложения.


## Получение пользователя

Получить информацию о текущем пользователе.

Если запрос прошел успешно, то будет получен объект `VMSUser`.

```
@GET(USER_SELF)
suspend fun getUser(): VMSUser
```


## Изменение пароля

Изменение пароля текущего авторизованного пользователя.

Если запрос прошел успешно, вы получите `ResponseBody`.

```
@PUT(USER_SELF)
suspend fun savePassword(@Body group: VMSChangePasswordRequest): ResponseBody
```


### VMSChangePasswordRequest

Объект с информацией, необходимой для смены пароля.

`password` и `password_confirmation` должны совпадать, `current_password` это ваш старый пароль.


## Выход из системы

Выход из системы текущего авторизованного пользователя.

Если запрос прошел успешно, вы получите `ResponseBody`.

```
@POST(LOGOUT)
suspend fun logout(): ResponseBody
```
