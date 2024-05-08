@toc API/ApiError

# ApiError.kt #

При возникновении ошибки вы получите сообщение `ApiError(val throwable: Throwable)` с подробной информацией.

Дополнительную информацию смотрите в `ApiErrors.kt`.


`ErrorType` — отображает общую информацию об ошибке

`name` — имя ошибки

`message` — информационное сообщение, отправленное с сервера

`statusCode` — код статуса ошибки, если ошибка пришла с сервера


Используйте наш метод логирования `logSdk()`, чтобы показать подробную информацию об ошибке:

`logSdk("Throwable", "ApiError: ${ApiError(it).getErrorInfoLogs()}")`


### Типы ошибок

Необходимо обработать коды состояния запросов 419 и 422.

Например, как обрабатывать ошибки входа в систему:

```kotlin
fun handleErrors(error: ApiError) {
	when (error.info.statusCode) {
		419 -> {
			val response = (error.throwable as HttpException).getErrorMessage419()
			if (response != null) {
				// In this case you have limited sessions and you should to remove one
				// to login with new session
			} else {
				// your common handling error
			}
		}

		422 -> {
			val text = error.throwable.getErrorMessage422()
			logSdk("Error422","Error info: $text")

			if (error.throwable.isCaptchaNeeded()) {
				// If has captcha error then you should to update your captcha
				getCaptcha()
			} else {
				// your common handling error
			}
		}

		else -> {
			// your common handling errors
		}
	}
}
```

### О кодах состояния

#### 401 — Unauthorised, вам следует выйти из системы
#### 403 — Forbidden, когда у вас не было разрешения на вызов запроса
#### 409 — Force Update, когда вам нужно обновить версию SDK
#### 419 — Session Expired, следует удалить какой-то сеанс для входа в систему
#### 422 — Incorrect Data, вызовите `Throwable.getErrorMessage422()`, чтобы получить текстовую ошибку
#### 429 — Request Limit, когда произошло слишком много запросов
#### 503 — Technical Error, вы можете показать собственное сообщение об ошибке или экран
#### 500 — Server Error, вы можете показать собственное сообщение об ошибке или экран
#### Unknown error — неизвестная ошибка
#### No internet connection — нет соединения