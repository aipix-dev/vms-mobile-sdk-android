@toc API/ApiError

# ApiError.kt #

If an error occurs, you will get an `ApiError(val throwable: Throwable)` with detailed information. 

See more in `ApiErrors.kt`.


`ErrorInfo` - shows general error information

`name` - name of the error

`message` - information message sent from the server

`statusCode` - status code of the error, if the error came from the server


You can try to log errors using our `logSdk` to show detailed information about errors:

`logSdk("Throwable", "ApiError: ${ApiError(it).getErrorInfoLogs()}")`


### Error types

You should handle 419 and 422 request status codes.

For example, how to handle login errors:

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

### About status codes

#### 401 - case Unauthorised, you should do logout user
#### 403 - case Forbidden, when you didn't have permission to invoke request
#### 409 - case Force Update, when you need to update version sdk
#### 419 - case Session Expired, should remove any session to login, call `HttpException.getErrorMessage419()` to get `sessions` from `VMSLoginResponse`
#### 422 - case Incorrect Data, call `Throwable.getErrorMessage422()` to get text error 
#### 429 - case Request Limit, when too many requests occurred
#### 503 - case Technical Error, you can show custom error message or screen
#### 500 - case Server Error, you can show custom error message or screen
#### Unknown error - case unknown
#### No internet connection - case no connection