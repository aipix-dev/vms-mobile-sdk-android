package com.mobile.vms.player.ui

import android.content.Context
import com.mobile.vms.models.*
import com.mobile.vms.network.ApiError
import com.mobile.vms.player.helpers.*
import io.reactivex.disposables.CompositeDisposable
import java.util.Calendar

interface VMSPlayerContract {

	interface View {
		fun loadLive(url: String?)
		fun loadArchive(url: String?, moveTo: String)
		fun needDisableButtons(isNeed: Boolean = true)
		fun delayMoveCamera()
		fun handleErrorArchive(errorText: String? = null)
		fun passCameraWithRanges(camera: VMSCamera)
		fun stopTimeAndVideo()
		fun checkArchiveAfterUpdate()
		fun loadMarks(list: List<VMSEvent>)
		fun goToNearestMark(mark: VMSEvent?, direction: String)
		fun createMarkSuccess()
		fun updateEventSuccess(event: VMSEvent)
		fun loadAfterError()
		fun showOrHideProgressBar(show: Boolean, screenNumber: Int = DEFAULT_SCREEN_NUMBER)
		fun customHandlingErrors(error: ApiError)

		fun showToast(test: String)

		fun showEmptyScreen(
			needShow: Boolean,
			isLocked: Boolean = false,
			isShowTimeline: Boolean = false
		)

		fun canHandleTimeline(canHandle: Boolean)

		fun checkTimelineVisibility()
	}

	interface Presenter {
		var compositeDisposable: CompositeDisposable
		var hasCallbackErrors: Boolean

		fun detachView(view: View)
		fun attachView(view: View)
		fun destroy()
		fun getStringForLayoutByKey(key: String): String = VMSSettings.getStringForLayoutByKey(key)
		fun getLiveStream(id: String, type: String, context: Context)
		fun getArchive(camera: VMSCamera, context: Context, calendar: Calendar)
		fun moveCamera(id: String, label: String, context: Context)
		fun moveCameraHome(id: String, context: Context)
		fun getCamera(id: String, context: Context)
		fun getMarks(id: String, from: String, to: String, lastRange: Calendar, context: Context)
		fun getNearestMark(idCamera: String, from: String, direction: String)
		fun createMark(idCamera: String, createData: VMSEventCreateData, context: Context)
		fun updateMark(cameraId: Int, markId: Int, createData: VMSEventCreateData, context: Context)
		fun getArchiveLink(idCamera: String, from: String, to: String, context: Context)
	}

}