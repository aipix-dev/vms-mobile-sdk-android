@toc API/Инициализация

# Инициализация VMSMobileSDK #

Основная точка входа для установления соединения между приложением и сервером.


## Инициализация

Для удобства инициализации вы должны установить 3 обязательных параметра для `VMSMobileSDK.Builder`:

- application: Application — импорт android.app.*
- baseUrl: String — url вашего сервера
- uuid: String — uuid вашего сервера

```
import VMSMobileSDK

VMSMobileSDK.Builder(
    application: Application,
    baseUrl: String,
    uuid: String,
    language: String,
).apply {
     // Если у вас есть токен доступа
     if (your_user_token.isNotEmpty()) {
         VMSMobileSDK.userToken = your_user_token
     }
 }
```


### Язык

Необходимо поддерживать локализацию из нашего бэкенда. Смотрите поле `available_locales` из объекта `VMSBasicStatic`.


### Токен доступа

Для авторизованного пользователя необходимо установить его токен доступа. Необходимо установить токен доступа после успешного входа в систему для использования в других запросах.

Установите `userToken` из `VMSMobileSDK`:

```kotlin
VMSMobileSDK.userToken = your_user_token
```

Мы поддерживаем только 2 запроса без токена:

- getTranslations(language: String, revision: Int);
- getStaticBasic();