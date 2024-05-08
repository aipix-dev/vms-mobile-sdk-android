@toc API/Requests/Cameras/FavoritesApi

# FavoritesApi #

API for manipulation with favorite cameras.


## Make camera favorite

Make camera favorite by it's id.

If the request was successful, you'll get a `VMSCamera`.

```
@POST(FAVORITES_CRUD)
suspend fun createFavorite(@Path(CAMERA) id: String): VMSCamera
```


## Remove camera from favorites

Remove camera from favorites by it's id.

If the request was successful, you'll get a `VMSStatusResponse`.

```
@DELETE(FAVORITES_CRUD)
suspend fun deleteFavorite(@Path(CAMERA) id: String): VMSStatusResponse
```


## Get list of favorite cameras

Get list of favorite cameras. Specify the page for the request.

If the request was successful, you'll receive a response with a paginated list of cameras.

```
@GET(FAVORITES)
suspend fun getFavorites(@Query(PAGE) page: String? = "1"): VMSPaginatedResponse<VMSCamera>
```

