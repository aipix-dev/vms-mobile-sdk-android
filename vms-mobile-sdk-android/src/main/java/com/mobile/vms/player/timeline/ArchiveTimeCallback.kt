package com.mobile.vms.player.timeline

import com.mobile.vms.models.VMSEvent
import java.util.*

interface ArchiveTimeCallback {

	var previousNavigatedMarkFrom: Calendar?
	var previousNavigatedMarkTime: Calendar?
	fun stopTimeArchive(isShowTime: Boolean)
	fun startHandlerAndChangeDateArchive(d: Calendar)
	fun slideTimeLineAndPassDateArchive(d: Calendar)
	fun clickMark(mark: VMSEvent)
	fun onLongClickMark(mark: VMSEvent, centerX: Float, screenWidth: Float)
	fun onLongClick(centerX: Float, calendarClick: Calendar, isConfigChange: Boolean = false)
	fun vibrate(time: Long = 150L)
	fun playCameraArchive()
	fun zoomOut()
}