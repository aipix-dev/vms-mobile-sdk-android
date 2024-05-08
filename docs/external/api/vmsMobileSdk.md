@toc API/Initialization

# VMSMobileSDK initialization #

Main entry point for establishing a connection between the application and the server.


## Initialization

For easy initialisation, you need to set 3 required parameters to the `VMSMobileSDK.Builder`:

- application: Application - import android.app.*
- baseUrl: String - url of your server
- uuid: String - uuid of your device

```
import VMSMobileSDK

VMSMobileSDK.Builder(
    application: Application,
    baseUrl: String,
    uuid: String,
    language: String,
).apply {
     // If you have access token 
     if (your_user_token.isNotEmpty()) {
         VMSMobileSDK.userToken = your_user_token
     }
 }
```


### Language

It is necessary to support localization from our backend. See the `available_locales` field from the `VMSBasicStatic` object.


### Access token

For authorised user you need to set user's access token. You need to set access token after successful login to use it in other requests. 

Set `userToken` from `VMSMobileSDK`:

```kotlin
VMSMobileSDK.userToken = your_user_token
```

We support only 2 requests without token:

- getTranslations(language: String, revision: Int);
- getStaticBasic();