package com.example.demo.ui.cameras

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mobile.vms.models.VMSCamera
import com.mobile.vms.network.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CamerasViewModel : ViewModel() {

    private val _camerasState = MutableLiveData<CamerasState>().apply {
        value = CamerasState()
    }
    val camerasState: LiveData<CamerasState> = _camerasState
    var client: ApiClientCoroutines? = null

    init {
        client = VMSClientApi.createServiceClientCoroutines()
        getCameras()
    }

    private fun getCameras() {
        _camerasState.postValue(_camerasState.value?.copyWith(isLoading = true))

        val handler = CoroutineExceptionHandler { _, _ ->
            _camerasState.postValue(
                _camerasState.value?.copyWith(isLoading = false, isError = true)
            )
        }
        CoroutineScope(Dispatchers.IO).launch(handler) {
            val response = client?.getCamerasTree()
            val cameras = arrayListOf<VMSCamera>()
            response?.let { parentCameras ->
                for (group in parentCameras.listIterator()) {
                    group.cameras?.let { childCameras ->
                        cameras.addAll(childCameras)
                    }
                }
            }
            _camerasState.postValue(
                _camerasState.value?.copyWith(
                    isLoading = false,
                    isError = false,
                    cameras = cameras
                )
            )
        }
    }
}