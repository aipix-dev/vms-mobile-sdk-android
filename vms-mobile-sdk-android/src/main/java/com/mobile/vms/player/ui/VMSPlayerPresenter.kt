package com.mobile.vms.player.ui

import android.content.Context
import com.mobile.vms.models.*
import com.mobile.vms.network.*
import com.mobile.vms.player.helpers.*
import com.mobile.vms.player.ui.VMSPlayerContract.View
import com.mobile.vms.settings
import io.reactivex.disposables.CompositeDisposable
import retrofit2.HttpException
import java.net.*
import java.util.*

class VMSPlayerPresenter: VMSPlayerContract.Presenter {

	override var compositeDisposable = CompositeDisposable()
	override var hasCallbackErrors = false
	var view: View? = null
	val TAG = VMSPlayerPresenter::class.java.simpleName
	private var isArchiveDateOutOfRange = false

	private fun getApiClient(): ApiClientObservable = VMSClientApi.createServiceClientObservable()

	override fun detachView(view: View) {
		this.view = null
	}

	override fun attachView(view: View) {
		this.view = view
	}

	override fun destroy() = compositeDisposable.clear()

	override fun getLiveStream(id: String, type: String, context: Context) {
		view?.let {
			view?.showOrHideProgressBar(true)
			getApiClient().getStreams(id, type, settings.videoType)
				.subsIoObsMain()
				.subscribe({ result ->
					view?.loadLive(result.url)
				}) { error ->
					view?.stopTimeAndVideo()
					if (hasCallbackErrors) {
						view?.customHandlingErrors(ApiError(error))
					} else {
						if (error is HttpException && error.code() == 404) {
							view?.showEmptyScreen(needShow = true)
						} else {
							didReceiveError(view!!, error, context) { view?.loadAfterError() }
						}
					}
				}?.addTo(compositeDisposable)
		}
	}

	override fun getArchive(
		camera: VMSCamera,
		context: Context,
		calendar: Calendar
	) {
		if (camera.getStartAtLocal().isEmpty()) {
			view?.handleErrorArchive()
			return
		}
		view?.let {
			val startTime =
				camera.startAt?.let { if (it.isNotEmpty()) setCalendarByDateServerFromUTC(it).timeInMillis else 0 }
					?: 0

			val settingTime = calendar.clone() as Calendar
			val cursorTime = settingTime.timeInMillis
			var dateForClient = setCalendarServer(calendar)
			var dateForServer = setCalendarServerToUTC(calendar)
			val currentTime = Calendar.getInstance()
			logSdk(TAG, "***** dateForClient  $dateForClient")
			logSdk(TAG, "***** dateForServer  $dateForServer")
			logSdk(TAG, "***** lastAvailableRange  ${Calendar.getInstance().time}")
			if (cursorTime <= startTime) {
				dateForClient = camera.getStartAtLocal()
				dateForServer = camera.startAt ?: ""
			} else if (cursorTime < currentTime.timeInMillis) {
			} else {
				// must never come here
			}
			val id = if (camera.id != 0) camera.id.toString() else { // STREAM_ID // for test
				view?.handleErrorArchive()
				return
			}
			logSdk(TAG, "***** dateForClient       $dateForClient")
			logSdk(TAG, "***** dateForServer       $dateForServer")
			view?.showOrHideProgressBar(true)
			getApiClient().getArchive(id, dateForServer)
				.subsIoObsMain()
				.subscribe({ result ->
					view?.loadArchive(result.url ?: result.fallbackUrl, dateForClient)
				}) { error ->
					// Handle errors without custom logic
					if (error is ConnectException || error is SocketTimeoutException || error is UnknownHostException) {
						view?.stopTimeAndVideo()  //stop timer and block controls
						view?.canHandleTimeline(canHandle = false)    //bun handle timeline clicks
						didReceiveError(view!!, error, context) { view?.loadAfterError() }
					} else if (error is HttpException && error.code() == 422) {
						// 422 mean this date no more available in archive -> need load new camera data
						isArchiveDateOutOfRange = true
						getCamera(camera.id.toString(), context)
					} else {
						view?.showOrHideProgressBar(false)
						val errorText: String? = if (error is HttpException && error.code() == 400) {
							// 400 mean restrictions for archive
							error.getErrorMessage422()
						} else {
							null
						}
						view?.handleErrorArchive(errorText)
					}
					view?.checkTimelineVisibility()
				}?.addTo(compositeDisposable)
		}
	}

	override fun moveCamera(id: String, label: String, context: Context) {
		view?.let {
			val ptz = when (label) {
				LABEL_UP -> PTZ_Up()
				LABEL_DOWN -> PTZ_Down()
				LABEL_RIGHT -> PTZ_Right()
				LABEL_LEFT -> PTZ_Left()
				LABEL_ZOOM_IN -> PTZ_Zoom_in()
				LABEL_ZOOM_OUT -> PTZ_Zoom_out()
				else -> {
					moveCameraHome(id, context)
					return@let
				}
			}

			view?.showOrHideProgressBar(true)
			getApiClient().moveCamera(id, ptz)
				.subsIoObsMain()
				.subscribe({
					view?.delayMoveCamera()
				}) { error ->
					if (hasCallbackErrors) {
						view?.customHandlingErrors(ApiError(error))
					} else {
						didReceiveError(view!!, error, context) { moveCamera(id, label, context) }
					}
				}?.addTo(compositeDisposable)
		}
	}

	override fun moveCameraHome(id: String, context: Context) {
		view?.let {
			view?.showOrHideProgressBar(true)
			getApiClient().moveCameraHome(id)
				.subsIoObsMain()
				.subscribe({
					view?.delayMoveCamera()
				}) { error ->
					if (hasCallbackErrors) {
						view?.customHandlingErrors(ApiError(error))
					} else {
						didReceiveError(view!!, error, context) { moveCameraHome(id, context) }
					}
				}?.addTo(compositeDisposable)
		}
	}

	override fun getCamera(id: String, context: Context) {
		view?.let {
			view?.showOrHideProgressBar(true)
			getApiClient().getCamera(id)
				.subsIoObsMain()
				.subscribe({
					view?.passCameraWithRanges(it)
					if (isArchiveDateOutOfRange) {
						isArchiveDateOutOfRange = false
						getArchive(it, context, setCalendarByDateServer(it.getStartAtLocal()))
					}
				}) { error ->
					if (hasCallbackErrors) {
						view?.customHandlingErrors(ApiError(error))
					} else {
						if (error is HttpException && error.code() == 404) {
							view?.showEmptyScreen(needShow = true)
						} else {
							didReceiveError(view!!, error, context) { getCamera(id, context) }
						}
					}
				}?.addTo(compositeDisposable)
		}
	}

	override fun getMarks(
		id: String,
		from: String,
		to: String,
		lastRange: Calendar,
		context: Context
	) {
		val calendarCurrent = setCalendarByDateClientToUTC(setCalendarServer(lastRange))
		val calendarTo = setCalendarByDateClientToUTC(to)
		val fromServer = setCalendarServer(setCalendarByDateClientToUTC(from))
		val toServer =
			setCalendarServer(if (calendarTo.timeInMillis < calendarCurrent.timeInMillis) calendarTo else calendarCurrent)
		logSdk(TAG, "from            = $from       to = $to")
		logSdk(TAG, "fromServer      = $fromServer toServer = $toServer")
		logSdk(TAG, "calendarCurrent = ${calendarCurrent.time} ")
		logSdk(TAG, "is = ${calendarTo.timeInMillis < calendarCurrent.timeInMillis} ")

		view?.let {
			getApiClient().getCameraEvents(id, fromServer, toServer, getChosenMarkTypes())
				.subsIoObsMain()
				.subscribe({
					view?.loadMarks(it)
				}) { error ->
					if (hasCallbackErrors) {
						view?.customHandlingErrors(ApiError(error))
					} else {
						didReceiveError(view!!, error, context) {
							getMarks(
								id,
								fromServer,
								toServer,
								lastRange,
								context
							)
						}
					}
				}?.addTo(compositeDisposable)
		}
	}

	override fun getNearestMark(idCamera: String, from: String, direction: String) {
		view?.let {
			getApiClient().getNearestEvent(
				idCamera,
				from,
				getChosenMarkTypes(),
				direction
			)
				.subsIoObsMain()
				.subscribe({ data ->
					view?.goToNearestMark(data.event, direction)
				}) {
					if (hasCallbackErrors) {
						view?.customHandlingErrors(ApiError(it))
					} else {
						view?.showToast(getStringForLayoutByKey("err_common"))
					}
				}?.addTo(compositeDisposable)
		}
	}

	override fun createMark(idCamera: String, createData: VMSEventCreateData, context: Context) {
		view?.let {
			view?.showOrHideProgressBar(true)
			logSdk(TAG, "createMark from = ${createData.from}, to = ${createData.to}")
			getApiClient().createEvent(idCamera, createData)
				.subsIoObsMain()
				.subscribe({
					view?.showOrHideProgressBar(false)
					view?.createMarkSuccess()
				}) {
					view?.showOrHideProgressBar(false)
					if (hasCallbackErrors) {
						view?.customHandlingErrors(ApiError(it))
					} else {
						view?.showToast(getStringForLayoutByKey("mark_created_fail"))
					}
				}?.addTo(compositeDisposable)
		}
	}

	override fun updateMark(
		cameraId: Int,
		markId: Int,
		createData: VMSEventCreateData,
		context: Context
	) {
		view?.let {
			view?.showOrHideProgressBar(true)
			getApiClient().updateEvent(cameraId.toString(), markId.toString(), createData)
				.subsIoObsMain()
				.subscribe({
					view?.showOrHideProgressBar(false)
					view?.updateEventSuccess(it)
				}) {
					view?.showOrHideProgressBar(false)
					if (hasCallbackErrors) {
						view?.customHandlingErrors(ApiError(it))
					} else {
						view?.showToast(getStringForLayoutByKey("mark_updated_fail"))
					}
				}?.addTo(compositeDisposable)
		}
	}

	override fun getArchiveLink(idCamera: String, from: String, to: String, context: Context) {
		view?.let {
			view?.showOrHideProgressBar(true)
			val fromTime = setCalendarServerToUTC(setCalendarByDateServer(from))
			val toTime = setCalendarServerToUTC(setCalendarByDateServer(to))
			getApiClient().getArchiveLink(idCamera, fromTime, toTime)
				.subsIoObsMain()
				.subscribe({
					view?.showOrHideProgressBar(false)
					if (it.isSuccess) {
						view?.showToast(getStringForLayoutByKey("SHELL_ARCHIVE_PROCESS"))
					} else {
						view?.showToast(getStringForLayoutByKey("err_common"))
					}
				}) {
					view?.showOrHideProgressBar(false)
					if (hasCallbackErrors) {
						view?.customHandlingErrors(ApiError(it))
					}
				}?.addTo(compositeDisposable)
		}
	}

}