package com.example.demo.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.demo.App
import com.mobile.vms.VMSMobileSDK
import com.mobile.vms.models.VMSLoginRequest
import com.mobile.vms.network.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    private val _loginState = MutableLiveData<LoginState>().apply {
        value = LoginState()
    }
    val loginState: LiveData<LoginState> = _loginState
    private var client: ApiClientCoroutines? = null

    private var hasTranslationsResponse = false
    private var hasStaticsResponse = false
    private var hasStaticBasicResponse = false

    init {
        client = VMSClientApi.createServiceClientCoroutines()
        getTranslations()
        getStatics()
        getStaticBasic()
    }

    private fun getTranslations() {
        val handler = CoroutineExceptionHandler { _, _ ->
            hasTranslationsResponse = true
            checkAllDataLoaded()
        }
        CoroutineScope(Dispatchers.IO).launch(handler) {
            val response = client?.getTranslations(revision = 0)
            App.translations = response
            hasTranslationsResponse = true
            checkAllDataLoaded()
        }
    }

    private fun getStatics() {
        val handler = CoroutineExceptionHandler { _, _ ->
            hasStaticsResponse = true
            checkAllDataLoaded()
        }
        CoroutineScope(Dispatchers.IO).launch(handler) {
            val response = client?.getStatics()
            App.statics = response
            hasStaticsResponse = true
            checkAllDataLoaded()
        }
    }

    private fun getStaticBasic() {
        val handler = CoroutineExceptionHandler { _, _ ->
            hasStaticBasicResponse = true
            checkAllDataLoaded()
        }
        CoroutineScope(Dispatchers.IO).launch(handler) {
            val response = client?.getBasicStatic()
            App.basicStatic = response
            hasStaticBasicResponse = true
            checkAllDataLoaded()
        }
    }

    private fun checkAllDataLoaded() {
        if (hasTranslationsResponse && hasStaticsResponse && hasStaticBasicResponse) {
            _loginState.postValue(_loginState.value?.copyWith(isLoading = false))
        }
    }

    fun loginPressed(login: String, password: String) {
        _loginState.postValue(_loginState.value?.copyWith(isLoading = true))

        val handler = CoroutineExceptionHandler { _, throwable ->
            // if throwable is HttpException and code == 419 -> this mean that number of sessions exceeded
            val errorText = throwable.getErrorMessage422()
            _loginState.postValue(
                _loginState.value?.copyWith(isLoading = false, errorText = errorText)
            )
        }
        CoroutineScope(Dispatchers.IO).launch(handler) {
            val response = client?.login(
                VMSLoginRequest(login = login, password = password)
            )
            getSocketUrl(
                response?.accessToken ?: "",
                response?.user?.id ?: 0,
                response?.user?.accessTokenId ?: ""
            )
            App.permissions = response?.user?.permissions
            _loginState.postValue(
                _loginState.value?.copyWith(
                    isLoading = false,
                    successToken = response?.accessToken
                )
            )
        }
    }

    private fun getSocketUrl(accessToken: String, userId: Int, userTokenId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val response = client?.getSocketUrl()
            response?.let {
                VMSMobileSDK.pusherApi.Builder(
                    response.wsUrl,
                    response.appKey,
                    accessToken,
                    userId,
                    userTokenId
                )
                VMSMobileSDK.pusherApi.onConnect()
            }
        }
    }
}