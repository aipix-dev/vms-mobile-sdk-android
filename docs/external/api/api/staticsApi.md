@toc API/Requests/StaticsApi

# StaticsApi #

API to get basic information from your server and send possible tokens to the server.


## Get all translations.

Get translations of given language and from given revision.

If the request was successful, you'll get a `VMSTranslations` object.

This request doesn't require a token.

```
@GET(DICTIONARY)
suspend fun getTranslations(
    @Query(LANGUAGE) language: String? = EN,
    @Query(REVISION) revision: Int
): VMSTranslations
```

`language` - by default `en`, other available languages can be obtained from `available_locales` of the `VMSBasicStatic` object

`revision` - number of revision from which you'll get changes in translations; set to `0` to get all translations


## Static

Retrieve the static information required for the application.

If the request was successful, you'll get a `VMSStatic` object.

This request requires a token.

```
@GET(STATIC)
suspend fun getStatics(): VMSStatics
```


### VMSStatic

Object you get from the server with information you need to perform some functionality.

`camera_issues` - types of camera problem reports

`video_rates` - video rates available for player

`mark_types` - available types of user events

`system_events` - system events available to user

`analytic_types` - analytic event types available to user

`analytic_events` - event types of analytic cases available to user


## Basic static

Get the information you need to proceed with the login.

If the request was successful, you'll get a `VMSBasicStatic` object.

This request doesn't require a token.

```
@GET(STATIC_BASIC)
suspend fun getBasicStatic(): VMSBasicStatic
```


### VMSBasicStatic

Object you get from server with information you need to login properly.

`is_captcha_available` - if `true` - you need to show captcha information for login

`is_external_auth_enabled` - if `true` - you can login with external service

`version` - current version of backend server

`available_locales` - available locale translations


## Tokens

### FCM(for Google pushes)

Send the FCM token to the server if you are using firebase.

If the request was successful, you'll get a `Response<ResponseBody>` object.

```
@PUT(DEVICE)
suspend fun sendFcmToken(@Body fcmRequest: VMSFcmRequest): Response<ResponseBody>
```


### HMS(for Huawei pushes)

Send the HMS token to the server if you are using the Huawei Push Kit.

If the request was successful, you'll get a `Response<ResponseBody>` object.

```
@PUT(DEVICE)
suspend fun sendHuaweiToken(@Body fcmRequest: VMSHuaweiRequest): Response<ResponseBody>
```
