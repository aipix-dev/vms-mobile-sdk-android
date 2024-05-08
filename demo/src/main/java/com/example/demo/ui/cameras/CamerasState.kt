package com.example.demo.ui.cameras

import com.mobile.vms.models.VMSCamera

class CamerasState(
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val cameras: ArrayList<VMSCamera>? = null
)

fun CamerasState.copyWith(
    isLoading: Boolean? = null,
    isError: Boolean? = null,
    cameras: ArrayList<VMSCamera>? = null
): CamerasState {
    return CamerasState(
        isLoading = isLoading ?: this.isLoading,
        isError = isError ?: this.isError,
        cameras = cameras
    )
}