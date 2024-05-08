@toc API/Requests/Cameras/FavoritesApi

# FavoritesApi #

API для работы с избранными камерами.


## Добавление камеры в избранное

Сделать камеру избранной по ее идентификатору.

В случае успешного выполнения запроса ответом будет `VMSCamera`.

```
@POST(FAVORITES_CRUD)
suspend fun createFavorite(@Path(CAMERA) id: String): VMSCamera
```


## Удаление камеры из избранного

Удаление камеры из избранного по ее идентификатору.

Если запрос прошел успешно, то ответом будет `VMSStatusResponse`.

```
@DELETE(FAVORITES_CRUD)
suspend fun deleteFavorite(@Path(CAMERA) id: String): VMSStatusResponse
```


## Получение списка избранных камер

Получить список избранных камер. Укажите страницу для запроса.

Если запрос прошел успешно, вы получите ответ с постраничным списком камер.

```
@GET(FAVORITES)
suspend fun getFavorites(@Query(PAGE) page: String? = "1"): VMSPaginatedResponse<VMSCamera>
```

