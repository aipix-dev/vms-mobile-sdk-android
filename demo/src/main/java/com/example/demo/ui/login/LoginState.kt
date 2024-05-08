package com.example.demo.ui.login

data class LoginState(
    val isLoading: Boolean = true,
    val errorText: String? = null,
    val successToken: String? = null
)

fun LoginState.copyWith(
    isLoading: Boolean? = null,
    errorText: String? = null,
    successToken: String? = null
): LoginState {
    return LoginState(
        isLoading = isLoading ?: this.isLoading,
        errorText = errorText,
        successToken = successToken
    )
}