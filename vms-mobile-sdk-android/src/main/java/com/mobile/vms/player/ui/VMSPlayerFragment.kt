package com.mobile.vms.player.ui

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.*
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.*
import android.os.*
import android.os.Build.*
import android.util.*
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.*
import androidx.core.view.*
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.MutableLiveData
import androidx.media3.common.*
import androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.mobile.vms.*
import com.mobile.vms.R
import com.mobile.vms.databinding.SdkFragmentExoPlayerBinding
import com.mobile.vms.models.*
import com.mobile.vms.network.*
import com.mobile.vms.player.adapters.*
import com.mobile.vms.player.helpers.*
import com.mobile.vms.player.rtsp.*
import com.mobile.vms.player.strategyplayer.*
import com.mobile.vms.player.strategyplayer.PlayerProtocol.PlaybackState.*
import com.mobile.vms.player.timeline.*
import com.mobile.vms.player.zoom.ShowCameraControllerCallback
import com.mobile.vms.socket.*
import java.net.ConnectException
import java.text.ParseException
import java.util.*
import java.util.concurrent.atomic.*
import kotlin.Pair

enum class VMSScreenState {
	DEFAULT, // usual case to use this screen with Live And Archive
	INTERCOM_PHOTO, // unusual case to show only Live and action screenshot
	SHOW_CHOSEN_MARKS // unusual case to show marks of chosen types
}

/**
 * This is a screen with a player that plays streams of HLS and RTSP.
 */
class VMSPlayerFragment: VMSBaseBindingFragment<SdkFragmentExoPlayerBinding>(),
	VMSPlayerContract.View, ShowCameraControllerCallback,
	View.OnClickListener, LiveTimeCallback, ArchiveTimeCallback {

	private val TAG = "PlayerFragment"
	override fun getLayoutId() = R.layout.sdk_fragment_exo_player

	private lateinit var presenter: VMSPlayerContract.Presenter
	lateinit var player: PlayerVms
	private var state = VMSScreenState.DEFAULT

	var videoCodec = "h264"
	private var camera = VMSCamera()
	private var permissions: List<VMSPermission> = ArrayList<VMSPermission>()
	private var archiveRanges = ArrayList<VMSArchiveRange>()
	internal lateinit var adapter: ScreenSlidePagerAdapter
	internal var cameraList = ArrayList<VMSCamera>()
	private val exoPageChangeListener = ExoPageChangeListener()
	private var lastPage = 0
	private var camerasSize = 0
	private var statusStreamHighActive = false
	private var statusStreamLowActive = false
	private var timeDatePicker = ""
	private var timeForUpdateTimerSeconds = 1000L
	private var cameraId = 0
	private var playerPosition = 0
	private var lastFragment: VMSPlayerPageFragment? = null
	private val STREAM_ID = "10"
	private var bottomViewSpeeds: BottomSheetBehavior<LinearLayout>? = null
	private var bottomViewQuality: BottomSheetBehavior<LinearLayout>? = null
	private var bottomViewMore: BottomSheetBehavior<LinearLayout>? = null
	private var bottomViewDownloadArchive: BottomSheetBehavior<LinearLayout>? = null
	private var bottomViewMarks: BottomSheetBehavior<LinearLayout>? = null
	private var bottomVideoPlayerProtocol: BottomSheetBehavior<LinearLayout>? = null
	private var bottomViewDatePicker: BottomSheetBehavior<LinearLayout>? = null
	private var bottomViewTimePicker: BottomSheetBehavior<LinearLayout>? = null
	private var isSecondPopupShow = false
	private var isAnimationShow = false
	private var isPopupSpeedsShow = false
	private var isPopupQualityShow = false
	private var isPopupMoreShow = false
	private var isPopupEventsShow = false
//	private var isPopupVideoPlayerProtocol = false
	private var isPopupDownloadArchiveShow = false
	private var isPopupDatePickerShow = false
	private var isPopupTimePickerShow = false
	private var currentLiveCalendar: Calendar? = null
	var currentDateCursor: Calendar? = null
	private var calendarStartPeriod: Calendar? = null
	private var calendarEndPeriod: Calendar? = null
	private var secondsTimerHandler = Handler(Looper.getMainLooper())
	private var secondsTimerHandlerLive = Handler(Looper.getMainLooper())
	private var secondsTimerHandlerSlide = Handler(Looper.getMainLooper())
	private var isSlideHandlerRun = false
	private var isPlayerStateEnd = false
	private var isMoveRight = false
	private var adapterSpeeds: SpeedAdapter? = null
	private var adapterMore: MoreAdapter? = null
	private var adapterMarks: EventsAdapter? = null
	private var adapterVideoPlayerProtocol: VideoPlayerProtocolAdapter? = null // hidden
	private var adapterQuality: QualityAdapter? = null
	private val listInactiveStreamDurations = ArrayList<ArrayList<Long>>()
	private var hasPTZ = false
	private var ptzModeEnabled = false
	private var hasShowArchive = true
	private var hasSavePreview = true
	private var hasMarksToShow = false
	private var hasMarksToCreate = false
	private var hasMarksToUpdate = false
	private var hasDownloadArchive = false
	private var countFailedLoads = 0    //counter of fail load stream (for possible bug on backend)
	private var lastClickedTimeOnMark: Calendar? =
		null   //save time when click or double click on mark
	private var needShowMarkHint = false    //need for show/hide controls
	private var absentMarks = false    //need for show/hide controls
	private var markFrom: String? = null
	private var isToastAlreadyShown = false
	private var isLastPlayerLive = true //if last stream was live (used for back to fragment)
	private var videoWidthPortrait = 0
	private var videoHeightPortrait = 0
	private var videoWidthLand = 0
	private var videoHeightLand = 0
	private val markCreateViewWidth = 292.toPx() //view width in px for land orientation
	private val markCreateViewHeight = 220.toPx() //view height in px
	private val markCursorWidthToCenter = 22.toPx() //half width of cursor view
	private var centerCreateMark = 0f    //x coordinate of long click
	private var dateFromDatePicker = Calendar.getInstance() //date selected from date picker
	private val currentCameraMarks = mutableListOf<String>()  //mark names for current camera
	private var counterForMarkCreateHint = 1 //for create mark create name hint
	private var isBackPressedFromTimePicker = false //if there is back pressed when time picker open
	private var changedMark: VMSEvent? = null   //mark that will be updated
	var updateEventFlag: Boolean =
		false    // indicate that we need back to previous screen without clear all back stack
	private var counterStatesOfBuffering =
		0 // if player loop states from STATE_READY to STATE_BUFFERING - need stop player and reinstall it
	private val MAX_COUNTER_BUFFERED = 3
	private var isNeedAddTimeToArchive = false  //for correct time in archive
	private var correctTimeArchive: Calendar? = null
	private var needLoadNewMarks = false
	private val hintMarginBottom = 104.toPx() //if haven't access for resources (for elvis operator)
	private var savedMarkName: String? = null //for save entered mark name when rotate screen
	private var isVibrateNow = false    //is phone vibrate now
	private var forcePlayVideo = false  //for play when create mark view shown
	private var lastToastTime: Calendar? = null   //save time when start show toast
	private var needShowToastHint = false    //need for show/hide controls
	private var statusTimePickerDialog = "" // statusTimeStartPeriod, statusTimeEndPeriod
	private val receiverChangingTimeZone = object: BroadcastReceiver() {
		override fun onReceive(context: Context?, intent: Intent) {
			val action = intent.action
			if (action == Intent.ACTION_TIME_CHANGED || action == Intent.ACTION_TIMEZONE_CHANGED) {
				val currentOffset = Calendar.getInstance().timeZone.rawOffset
				if (currentDateCursor?.timeZone?.rawOffset != currentOffset) {
					currentDateCursor?.timeZone?.rawOffset = currentOffset
				}
			}
		}
	}
	private val JAVA_SECOND_FOR_SPEED = 960L // This need to avoid minor measurement error for timer

	private var hasPermissionsEvents = false
	private var errorType =
		"" // todo ERROR_TYPE_CONNECTION, ERROR_TYPE_TECHNICAL  //save error visibility for handle it when go screenshot detail and back
	private var callbackCameraEvents: VMSPlayerCallbackCameraEvents? = null
	private var callbackEventsTypes: VMSPlayerCallbackEventsTypes? = null
	private var callbackScreenshot: VMSPlayerCallbackScreenshot? = null
	private var callbackLogEvent: VMSPlayerCallbackLogEvent? = null
	private var callbackErrors: VMSPlayerCallbackErrors? = null
	private var callbackVideoType: VMSPlayerCallbackVideoType? = null
	private var callbackVideoQuality: VMSPlayerCallbackVideoQuality? = null
	private var timer = LoadingTimer()   // for archive loading delay
	private var playerOpenTime = Calendar.getInstance()
	private var tempQuality: String? = null

	private fun getArchive(date: Calendar) {
		timer.startTimer {
			if (isVisible) presenter.getArchive(camera, requireContext(), date)
		}
		showEmptyScreen(needShow = false)
	}

	private fun getProtocolPlayer() = PlayerRtsp(requireActivity() as AppCompatActivity)
//		if (!settings.isRtspVideo() || !isLive)
//			PlayerHls(requireContext())
//		else
//			PlayerRtsp(requireActivity())

	private fun updatePlayerView() {
		if (isVisible) {
//			getCurrentFragment()?.binding?.vmsPlayerView?.isGoneVMS(false)
//			Handler(Looper.getMainLooper()).postDelayed({
//				if (isVisible) {
//					getCurrentFragment()?.binding?.vmsPlayerView?.postInvalidate()
//				}
//			}, 2000)
			getCurrentFragment()?.binding?.vmsPlayerView?.postInvalidate()
		}
	}

	private fun registerReceivers() {
		val filter = IntentFilter()
		filter.addAction(Intent.ACTION_TIMEZONE_CHANGED)
		filter.addAction(Intent.ACTION_TIME_CHANGED)
		activity?.registerReceiver(receiverChangingTimeZone, filter)
	}

	companion object {
		var isLive = true
		var isHideButtons = false
		var isCreateMarkViewShown = false
		val callbackCameraEventsMLD = MutableLiveData<VMSPlayerCallbackCameraEvents>()
		val callbackEventsTypesMLD = MutableLiveData<VMSPlayerCallbackEventsTypes>()
		val callbackScreenshotMLD = MutableLiveData<VMSPlayerCallbackScreenshot>()
		val callbackLogEventMLD = MutableLiveData<VMSPlayerCallbackLogEvent>()
		val callbackErrorsMLD = MutableLiveData<VMSPlayerCallbackErrors>()
		val callbackVideoTypeMLD = MutableLiveData<VMSPlayerCallbackVideoType>()
		val callbackVideoQualityMLD = MutableLiveData<VMSPlayerCallbackVideoQuality>()

		fun newInstance(data: VMSPlayerData) = VMSPlayerFragment().apply {
			camera = data.camera
			cameraList = data.list ?: arrayListOf(camera)
			playerPosition = cameraList.indexOfFirst { it.id == camera.id }
			data.event?.let { event ->
				event.id?.let { changedMark = event }  //changed mark exist only if has id
				markFrom = event.getFromLocal()
			}
			cameraId = cameraList[playerPosition].id
			camerasSize = cameraList.size

			settings.videoRates = data.videoRates
			settings.markTypes = data.markTypes
			settings.saveTranslations(data.jsonTranslations)
			permissions = data.permissions
			settings.needVibration = data.allowVibration
			state = data.screenState

			callbackCameraEventsMLD.observe(this) {
				callbackCameraEvents = it
			}
			callbackEventsTypesMLD.observe(this) {
				settings.chosenEventsTypes = it.chosenEventsTypes
				adapterMarks?.chosenList = ArrayList(it.chosenEventsTypes)
				callbackEventsTypes = it
			}
			callbackScreenshotMLD.observe(this) {
				callbackScreenshot = it
			}
			callbackLogEventMLD.observe(this) {
				callbackLogEvent = it
			}
			callbackErrorsMLD.observe(this) {
				callbackErrors = it
				presenter.hasCallbackErrors = callbackErrors != null // user do handling errors
			}
//			callbackVideoTypeMLD.observe(this) {
//				callbackVideoType = it
//				settings.videoType = it.videoType
//				player = PlayerVms(getProtocolPlayer())
//				handleBottomViewVideoPlayerProtocol()
//				initVideoPlayerProtocolAdapter()
//			}
			callbackVideoQualityMLD.observe(this) {
				settings.videoQuality = it.videoQuality
				adapterQuality?.innerQuality = getQualityStream()
				callbackVideoQuality = it
			}
			vmsPermissionsSocket.observe(this) {
				Handler(Looper.getMainLooper()).postDelayed({
					if (isVisible) {
						initBottomViewMore()
						initEventsTypesAdapter()
					}
				}, 2000)
			}
			vmsCamerasSocket.observe(this) {
				if (it.info != null && it.info.detached.isNotEmpty()) {
					val detachedList = it.info.detached
					if (detachedList.contains(cameraId)) {
						showToast(getStringForLayoutByKey("camera_not_available"))
						Handler(Looper.getMainLooper()).postDelayed({ //delay for update cameras fragment
							if (isVisible)
								requireActivity().onBackPressedDispatcher.onBackPressed() //close player fragment for this camera
						}, 2000)
					} else {
						getCamera(cameraId)
					}
				}
			}
			vmsMarksSocket.observe(this) { it ->
				if (it.event != null) {   //mark have to be not null
					it.event.getFromLocal().let { from ->
						if (hasMarksToShow && hasPermissionsEvents) {
							val markDate = setCalendarByDateServer(from)
							val startTimeline = binding.timeLine.getLeftTimelineCalendar()
							val endTimeline = binding.timeLine.getRightTimelineCalendar()
							if (startTimeline != null && endTimeline != null &&
								markDate.after(startTimeline) && endTimeline.after(markDate)
							) {
								getMarks()   //update marks on timeline if new mark gets into current timeline
							}
						}
					}
				}
			}

		}
	}

	private fun handlePlayerScreenStates() {
		when (state) {
			VMSScreenState.INTERCOM_PHOTO -> {
				binding.viewTopBarNavigation.frArchive.isGoneVMS(true)
				binding.ivMoreLive.isGoneVMS(true)
				binding.ivPtz.isGoneVMS(true)
				binding.ivScreenshotLive.isGoneVMS(true)
			}

			else -> {
				// do nothing
			}
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		createVibration(settings.needVibration, requireContext())

		presenter = VMSPlayerPresenter()
		registerReceivers()
		changeStatusBarByKeyVMS(requireActivity(), KEY_SCREENSHOT_DETAIL_HIDDEN)
		binding.viewEmptyScreenSdk.emptyPicture.setImageDrawableCompatVMS(
			requireContext(),
			if (!isExternalBrandSdk()) R.drawable.icon_empty_screen_nsc else R.drawable.icon_empty_screen
		)
		presenter.attachView(this)
		isHideButtons = false

		binding.timeLine.isHapticFeedbackEnabled = true    //enable haptic feedback for timeline
		binding.viewTopBarNavigation.txtCameraNameTopBar.text = camera.name
		binding.tvTime.text = setCalendarVMS(Calendar.getInstance())

//        tv_time_top_bar_live?.text = setCalendar(currentLiveCalendar!!)
//        tv_time_top_bar_archive?.text = setCalendar(correctTimeArchive!!)

//        tv_time_top_bar_live?.text = setCalendar(Calendar.getInstance())
//        tv_time_top_bar_archive?.text = setCalendar(Calendar.getInstance())

		initViewPager()
		player = PlayerVms(getProtocolPlayer())

		binding.viewTopBarNavigation.imgBackTopBar.setOnClickListener(this)
		binding.viewBgForBottomSheetPlayer.setSafeOnClickListenerVMS {
			if (isPopupMoreShow) {
				setStateBehaviorMore(BottomSheetBehavior.STATE_HIDDEN)
			} else if (isPopupSpeedsShow) {
				setStateBehaviorSpeeds(BottomSheetBehavior.STATE_HIDDEN)
			} else if (isPopupQualityShow) {
				setStateBehaviorQuality(BottomSheetBehavior.STATE_HIDDEN)
			} else if (isPopupEventsShow) {
				setStateBehaviorEvents(BottomSheetBehavior.STATE_HIDDEN)
			}/* else if (isPopupVideoPlayerProtocol) {
				setStateBehaviorVideoPlayerProtocol(BottomSheetBehavior.STATE_HIDDEN)
			}*/ else if (isPopupDownloadArchiveShow) {
				if (isPopupDatePickerShow) setStateBehaviorDatePicker(BottomSheetBehavior.STATE_HIDDEN)
				else setStateBehaviorDownloadArchive(BottomSheetBehavior.STATE_HIDDEN)
			} else if (isPopupDatePickerShow) {
				setStateBehaviorDatePicker(BottomSheetBehavior.STATE_HIDDEN)
			}
		}
		binding.viewBgForBottomSheetTimePicker.setSafeOnClickListenerVMS {
			setStateBehaviorTimePicker(BottomSheetBehavior.STATE_HIDDEN, isOnBackPressed = true)
			if (!isPopupDownloadArchiveShow && !isCreateMarkViewShown) { //if create mark view not open (just playing video on player)
				//after back pressed from time picker -> open date picker
				setStateBehaviorDatePicker(BottomSheetBehavior.STATE_EXPANDED)
			}
		}
		binding.tvMarkDate.setSafeOnClickListenerVMS { onClickCalendar() }
		binding.viewSheetBottomDatePicker.dateDone.setSafeOnClickListenerVMS {
			dateFromDatePicker.timeInMillis = binding.viewSheetBottomDatePicker.datePicker.date
			when {
				isCreateMarkViewShown -> {
					//if create mark view shown -> set date and load archive
					currentDateCursor?.let {
						setCalendarByDatePicker(it)
						//invalidate timeline
						binding.timeLine.setCursorByClickMoveButton(it)
						playCameraArchive()
					}
				}

				isPopupDownloadArchiveShow -> {
					if (statusTimePickerDialog == STATUS_TIME_END_PERIOD) {
						setCalendarByDatePicker(calendarEndPeriod!!)
						setTimeForCalendar(calendarEndPeriod!!, getOnlyTime(calendarEndPeriod!!))
						binding.viewSheetBottomDownloadArchive.tvEndPeriodFull.text =
							getDateFormatPeriod(calendarEndPeriod!!)
					} else {
						setCalendarByDatePicker(calendarStartPeriod!!)
						setTimeForCalendar(
							calendarStartPeriod!!,
							getOnlyTime(calendarStartPeriod!!)
						)
						binding.viewSheetBottomDownloadArchive.tvStartPeriodFull.text =
							getDateFormatPeriod(calendarStartPeriod!!)
					}
				}

				else -> {
					//if create mark view not shown (just playing video) -> open time picker
					setStateBehaviorTimePicker(BottomSheetBehavior.STATE_EXPANDED)
				}
			}
			setStateBehaviorDatePicker(BottomSheetBehavior.STATE_HIDDEN)
		}
		binding.tvMarkTime.setSafeOnClickListenerVMS {
			setStateBehaviorTimePicker(BottomSheetBehavior.STATE_EXPANDED)
		}
		binding.viewSheetBottomTimePicker.timeDone.setSafeOnClickListenerVMS {
			currentDateCursor?.let {
				if (isCreateMarkViewShown || isPopupDownloadArchiveShow) {
					// do nothing
				} else {
					//if create mark view not shown (mean come from date picker) -> set date from date picker
					setCalendarByDatePicker(it)
				}
				//set time from time picker
				fun action(calendar: Calendar): () -> Unit = {
					calendar.set(
						Calendar.HOUR_OF_DAY,
						binding.viewSheetBottomTimePicker.timeHh.value
					)
					calendar.set(Calendar.MINUTE, binding.viewSheetBottomTimePicker.timeMm.value)
					calendar.set(Calendar.SECOND, binding.viewSheetBottomTimePicker.timeSs.value)
					logSdk(TAG, "action was called!!!")
				}

				if (!isPopupDownloadArchiveShow) {
					action(it).invoke()
					binding.timeLine.setCursorByClickMoveButton(it) //invalidate timeline
					playCameraArchive()
				} else {
					when (statusTimePickerDialog) {
						STATUS_TIME_START_PERIOD -> {
							action(calendarStartPeriod!!).invoke()
							binding.viewSheetBottomDatePicker.tvTimePeriod.text =
								getOnlyTime(calendarStartPeriod!!)
						}

						STATUS_TIME_END_PERIOD -> {
							action(calendarEndPeriod!!).invoke()
							binding.viewSheetBottomDatePicker.tvTimePeriod.text =
								getOnlyTime(calendarEndPeriod!!)
						}
					}
				}
			}
			setStateBehaviorTimePicker(BottomSheetBehavior.STATE_HIDDEN)
		}
		binding.cancelButton.setSafeOnClickListenerVMS {
			closeCreateMarkView(needPlayArchive = changedMark == null)   //close mark create view
			if (changedMark != null) {
				updateEventFlag = true
				requireActivity().onBackPressedDispatcher.onBackPressed()   //if mark update -> close player
			}
		}
		binding.doneToCreateButton.setSafeOnClickListenerVMS {
			if (binding.etMarkTitle.text.trim().isEmpty()) {  //trim to bun only spaces in title
				//if mark title is empty -> show toast
				showToast(getStringForLayoutByKey("mark_empty_title"))
			} else {
				val markData = VMSEventCreateData(
					title = binding.etMarkTitle.text.toString(),
					from = setCalendarServerToUTC(currentDateCursor!!)
				)
				if (changedMark != null) {
					//update existed mark - was crash
					changedMark?.id?.let { it1 ->
						presenter.updateMark(
							cameraId,
							it1,
							markData,
							requireContext()
						)
					}
				} else {
					//create new mark
					presenter.createMark(
						cameraId.toString(),
						markData,
						requireContext()
					)
				}
			}
		}
		binding.moveToPreviousMark.setSafeOnClickListenerVMS {
			currentDateCursor?.let {
				val currentTime =
					if (previousNavigatedMarkFrom == null) {
						currentDateCursor!!
					} else {
						val previous = previousNavigatedMarkFrom!!.clone() as Calendar
						previous.apply { timeInMillis -= 1000 }
					}
				presenter.getNearestMark(
					cameraId.toString(), setCalendarServerToUTC(currentTime), DIRECTION_PREVIOUS
				)
			}
		}
		binding.moveToNextMark.setSafeOnClickListenerVMS {
			if (binding.viewEmptyScreenSdk.emptyScreen.isVisible) return@setSafeOnClickListenerVMS   // hide click for archive unavailable error
			currentDateCursor?.let {
				val currentTime =
					if (previousNavigatedMarkFrom == null) {
						currentDateCursor!!
					} else {
						val next = previousNavigatedMarkFrom!!.clone() as Calendar
						next.apply { timeInMillis += 1000 }
					}
				presenter.getNearestMark(
					cameraId.toString(), setCalendarServerToUTC(currentTime), DIRECTION_NEXT
				)
			}
		}

		needDisableButtons(false)
		handleBottomViewMore()  //first
		initBottomViewMore()    //second
		handleBottomViewQuality() // first
		handleBottomViewSpeeds() // first
		initSpeedAdapter() // second
		handleBottomViewEventsTypes() // first
		initEventsTypesAdapter() // second
//		handleBottomViewVideoPlayerProtocol() // first
//		initVideoPlayerProtocolAdapter() // second
		handleBottomViewDownloadArchive()
		handleBottomViewDatePicker()
		handleBottomViewTimePicker()
		setDatePickerListener()
		setTimePickerListeners()
		updateUserPermissions()
		handlePlayerScreenStates()
		if (markFrom.isNullOrEmpty()) {  //start new fragment and haven't mark
			isLive = true
		} else {    //start new fragment and have mark
			checkMarkStartEndArchive(markFrom!!)
			isLive = false
			showLiveArchiveUI()
			binding.viewTopBarNavigation.frLive.isGoneVMS(true)
		}
		SingletonErrorHandler.instance.doOnError()  //send requests after reopen screen
	}

	fun isHandleBackPressed(): Boolean {
		var isHandledHere = true
		if (isPopupMoreShow) {
			setStateBehaviorMore(BottomSheetBehavior.STATE_HIDDEN)
		} else if (isPopupSpeedsShow) {
			setStateBehaviorSpeeds(BottomSheetBehavior.STATE_HIDDEN)
		} else if (isPopupQualityShow) {
			setStateBehaviorQuality(BottomSheetBehavior.STATE_HIDDEN)
		} else if (isPopupEventsShow) {
			setStateBehaviorEvents(BottomSheetBehavior.STATE_HIDDEN)
		}/* else if (isPopupVideoPlayerProtocol) {
			setStateBehaviorVideoPlayerProtocol(BottomSheetBehavior.STATE_HIDDEN)
		}*/ else if (isPopupDownloadArchiveShow) {
			if (isPopupTimePickerShow) setStateBehaviorTimePicker(
				BottomSheetBehavior.STATE_HIDDEN, isOnBackPressed = true
			)
			else if (isPopupDatePickerShow) setStateBehaviorDatePicker(
				BottomSheetBehavior.STATE_HIDDEN
			)
			else setStateBehaviorDownloadArchive(BottomSheetBehavior.STATE_HIDDEN)
		} else if (isPopupDatePickerShow) {
			setStateBehaviorDatePicker(BottomSheetBehavior.STATE_HIDDEN)
		} else if (isPopupTimePickerShow) {
			setStateBehaviorTimePicker(BottomSheetBehavior.STATE_HIDDEN, isOnBackPressed = true)
			if (!isCreateMarkViewShown) {
				//if create mark view not open (just playing video on player)
				//after back pressed from time picker -> open date picker
				setStateBehaviorDatePicker(BottomSheetBehavior.STATE_EXPANDED)
			}
		} else if (isCreateMarkViewShown) {
			closeCreateMarkView(needPlayArchive = changedMark == null)
			if (changedMark != null) {
				//if mark update (need back to previous screen)
				requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
				isHandledHere = false
			} else {
				//if mark create -> close mark create view (stay at the same screen)
				showOrHideProgressBar(false)
			}
		} else if (!isAnimationShow) {
			requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
			isHandledHere = false
		}
		return isHandledHere
	}

	fun getCamera(cameraId: Int) {
		presenter.getCamera(cameraId.toString(), requireContext())
	}

	private fun initViewPager() {
		binding.viewPagerExo.setShowCameraControllerCallback(this)
		binding.viewPagerExo.removeOnPageChangeListener(exoPageChangeListener)
		adapter = ScreenSlidePagerAdapter(childFragmentManager)
		binding.viewPagerExo.adapter = adapter
		binding.viewPagerExo.currentItem = playerPosition
		binding.viewPagerExo.addOnPageChangeListener(exoPageChangeListener)
		lastPage = playerPosition
	}

	inner class ScreenSlidePagerAdapter(fm: FragmentManager):
		androidx.fragment.app.FragmentStatePagerAdapter(fm) {
		override fun getCount(): Int = camerasSize

		override fun getItem(position: Int): androidx.fragment.app.Fragment {
			return VMSPlayerPageFragment().newInstance(position)
		}
	}

	private fun getCurrentFragment(): VMSPlayerPageFragment? {
		return try {
			val index = binding.viewPagerExo.currentItem
			val currentFragment =
				adapter.instantiateItem(binding.viewPagerExo, index) as VMSPlayerPageFragment
			childFragmentManager.executePendingTransactions()
			currentFragment.binding //check if fragment already has binding to use
			currentFragment
		} catch (e: Exception) {
			null
		}
	}

	private fun getCurrentItem(): Int {
		return binding.viewPagerExo.currentItem
	}

	inner class ExoPageChangeListener: androidx.viewpager.widget.ViewPager.OnPageChangeListener {
		override fun onPageScrolled(
			position: Int,
			positionOffset: Float,
			positionOffsetPixels: Int
		) {
		}

		override fun onPageSelected(position: Int) {
			showDialogCheckWifi { swipePage(position) }
		}

		override fun onPageScrollStateChanged(state: Int) {}
	}

	private fun swipePage(position: Int) {
		showEmptyScreen(needShow = false)
		binding.timeLine.setMarks(arrayListOf())    // clear marks from previous camera
		tempQuality = null
		listInactiveStreamDurations.clear() //clear inactive ranges when swipe to another camera
		countFailedLoads = 0
		counterForMarkCreateHint = 1
		counterStatesOfBuffering = 0
		SingletonErrorHandler.instance.callbackList.clear()
		camera = cameraList[position]
		playerPosition = position
		cameraId = cameraList[position].id
		if (!isLive) playerOpenTime = Calendar.getInstance()   // update archive end time
		getCamera(cameraId)
		binding.viewTopBarNavigation.txtCameraNameTopBar.text = camera.name

		updateUserPermissions()
		binding.viewPtz.mainPtz.isGoneVMS(true)
		binding.ivPtz.setColorFilter(
			getColorCompat(requireContext(), R.color.gray_icon),
			PorterDuff.Mode.MULTIPLY
		)
		val index = binding.viewPagerExo.currentItem
		lastFragment = if (index > lastPage) {
			adapter.instantiateItem(binding.viewPagerExo, index - 1) as VMSPlayerPageFragment
		} else {
			adapter.instantiateItem(binding.viewPagerExo, index + 1) as VMSPlayerPageFragment
		}
		childFragmentManager.executePendingTransactions()
//		lastFragment?.setSound(0f) // delete it cause we need save sound for all players
		lastFragment?.binding?.vmsPlayerView?.isGoneVMS(true)
		lastPage = index
		player.stop()
		if (isOnline(requireContext())) needDisableButtons(false)

		if (adapterSpeeds?.innerSpeedX != speed_X_1) {
			//reset speed to 1x on swipe to another camera
			setSpeed(speed_X_1)
		}
		initSpeedAdapter()
	}

	override fun loadAfterError() {
		showDialogCheckWifi { mainLoadViews() }
	}

	private fun showDialogCheckWifi(action: () -> Unit) {
		val isMobileConnection = hasMobileConnection(requireActivity())
		if (isMobileConnection && !settings.neverShowWifi && !settings.neverShowWifiForCurrentSession) {
			//show dialog check wifi
			VMSSettingsDialogFragment().also {
				it.onClickFirstButton = {
					requireActivity().onBackPressedDispatcher.onBackPressed()
				}
				it.onClickSecondButton = {
					VMSMobileSDK.settings.neverShowWifiForCurrentSession = true
					action.invoke()
				}
			}.show(childFragmentManager, "settings")
		} else {
			action.invoke()
		}
	}

	override fun passCameraWithRanges(camera: VMSCamera) {
//        // Log.d("camera", "camera= ${camera}")
		showOrHideProgressBar(false)
		this.camera = camera
		archiveRanges = cutUnreachableArchiveRanges(
			camera.archiveRanges,
			camera.getStartAtLocal()
		)
		// update because camera in list can contain not all the necessary data
		// and it becomes available only after get response with new camera
		saveInactiveRange()
		updateUserPermissions()
		if (camera.userStatus == USER_STATUS_BLOCKED) {
			disableArchiveIfNeed()
			showEmptyScreen(needShow = true, isLocked = true)
		} else if (isLive && camera.isRestrictedLive == true) {
			if (archiveRanges.isNotEmpty()) initServerDateTime(camera)
			showEmptyScreen(needShow = true)
		} else if (!isLive && camera.isRestrictedArchive == true && archiveRanges.isEmpty()) {
			disableArchiveIfNeed()
			showEmptyScreen(needShow = true)
		} else if (camera.status == INACTIVE || camera.status == EMPTY || camera.status == INITIAL) {
			//if camera inactive
			if (archiveRanges.isNotEmpty()) {
				//inactive and have archive
				initServerDateTime(camera)
				if (isLive) {
					//show archive if live are showing now
					showToast(getStringForLayoutByKey("inactive_camera_live"))
					onShowArchive()
				} else {
					//load archive with last available range
					if (markFrom.isNullOrEmpty()) {
						getArchiveTimeClient()
					} else {
						if (changedMark != null) checkMarkStartEndArchive(markFrom!!)
						getArchive(currentDateCursor!!)
						// todo check below mb better
//                        presenter.getArchive(camera, getMainActivity(), currentDateCursor!!, getEndOfArchive(1))}
					}
				}
			} else {
				//inactive and haven't archive
				if (isLive) {
					//if live -> show empty screen
					disableArchiveIfNeed()
					showEmptyScreen(needShow = true)
				} else {
					//if archive -> handle error archive (go live and show empty screen)
					showEmptyScreen(needShow = false)
					handleErrorArchive()
				}
			}
		} else {
			//camera active
			//show/hide views for fast clicking live-archive
			showLiveArchiveUI()
			if (!isLive && archiveRanges.isEmpty()) {
				disableArchiveIfNeed()
				showEmptyScreen(needShow = true)
			} else {
				if (archiveRanges.isNotEmpty()) initServerDateTime(camera)
				mainLoadViews()
			}
		}
//        Log.e(
//            "ARCHIVE_RANGES",
//            "camera start_at = ${setCalendarServer(setCalendarByDateServer(camera.getStartAtLocal()))}, size = ${archiveRanges.size}"
//        )
//        for (range in archiveRanges) {
//            Log.e("ARCHIVE_RANGES", "start = ${setCalendarServer(Calendar.getInstance().apply { timeInMillis = range.from * 1000L })}")
//            Log.e("ARCHIVE_RANGES", "end = ${setCalendarServer(Calendar.getInstance().apply { timeInMillis = (range.from + range.duration) * 1000L })}")
//            Log.e("ARCHIVE_RANGES", "-------------------------------------------------------------------")
//        }
	}

	private fun mainLoadViews() {
		showOrHideProgressBar(show = true) // show
		if (isLive) getLiveStream() else currentDateCursor?.let { playCameraArchive() }
	}

	private fun disableArchiveIfNeed() {
		val needShow = camera.getStartAtLocal().isNotEmpty() && archiveRanges.isNotEmpty()
		if (!isLive) {
			binding.timeLine.isGoneVMS(!needShow)
			binding.viewCursor.isGoneVMS(!needShow)
			binding.viewBottomBarNavigation.constrBottomBarNav.isGoneVMS(!needShow)
			if (settings.hasEventsToShow()) {
				showHideNearestEventButtons(!needShow)
			}
		}
	}

	private fun showHideNearestEventButtons(isGone: Boolean) {
		binding.leftButtonFrame.isGoneVMS(isGone)
		binding.rightButtonFrame.isGoneVMS(isGone)
	}

	private fun showHideNearestEventButtonsSmooth(isGone: Boolean, maxAlpha: Float) {
		binding.leftButtonFrame.isGoneSmooth(isGone, maxAlpha)
		binding.rightButtonFrame.isGoneSmooth(isGone, maxAlpha)
	}

	override fun showEmptyScreen(needShow: Boolean, isLocked: Boolean, isShowTimeline: Boolean) {
//		logSdk(TAG, "showEmptyScreen needShow = $needShow")
		if (needShow) {
			showOrHideProgressBar(show = false) // hide
			if (isShowTimeline) {
				checkTimelineVisibility()
			} else {
				hideTimeLine()
			}
			stopAndHideAllTimers()
			binding.lnrBottomActions.isGoneVMS(true)
			binding.ivSound.isGoneVMS(true)
			getCurrentFragment()?.binding?.vmsPlayerView?.isGoneVMS(true)
			Handler(Looper.getMainLooper()).postDelayed({
				if (isVisible && binding.viewEmptyScreenSdk.emptyScreen.isVisible) {
					//avoid show exo player on empty screen live after fast click
					getCurrentFragment()?.binding?.vmsPlayerView?.isGoneVMS(true)
				}
			}, 1000)
			Handler(Looper.getMainLooper()).postDelayed({
				if (isVisible && binding.viewEmptyScreenSdk.emptyScreen.isVisible) {
					//avoid show exo player on empty screen live after fast click or long time response camera stream video
					getCurrentFragment()?.binding?.vmsPlayerView?.isGoneVMS(true)
				}
			}, 3000)
		}
		val titleMessage = getStringForLayoutByKey(
			when {
				isLocked -> "camera_locked"
				isLive && camera.isRestrictedLive == true -> "restricted_live_error_short"
				!isLive && camera.isRestrictedArchive == true && archiveRanges.isEmpty() -> "restricted_archive_error"
				else -> "inactive_camera_title"
			}
		)
		binding.viewEmptyScreenSdk.emptyTitle.text = titleMessage
		binding.viewEmptyScreenSdk.emptyTitle.setTextColorCompatVMS(requireContext(), R.color.white)
		binding.viewEmptyScreenSdk.emptyMes.text = getStringForLayoutByKey("inactive_camera_msg")
		binding.viewEmptyScreenSdk.emptyMes.setTextColorCompatVMS(requireContext(), R.color._ACAFB8)
		binding.viewEmptyScreenSdk.emptyScreen.isGoneVMS(!needShow)
	}

	private fun stopAndHideAllTimers() {
		stopTimeLive()
		stopTimeArchive(isShowTime = false)
		binding.viewTopBarNavigation.tvTimeTopBarLive.isGoneVMS(true)
		binding.viewTopBarNavigation.tvTimeTopBarArchive.isGoneVMS(true)
	}

	private fun hideTimeLine() {
		stopTimeArchive(isShowTime = false)  //stop timer
		binding.viewCursor.isGoneVMS(true)
		binding.timeLine.isGoneVMS(true)
		binding.viewBottomBarNavigation.constrBottomBarNav.isGoneVMS(true)
		showHideNearestEventButtons(true)
		binding.tvTime.isGoneVMS(true)
		binding.timeLine.clear()
	}

	private var lastClickTime: Long = 0

	override fun onClick(v: View?) {
		if (System.currentTimeMillis() - lastClickTime < 500) return
		lastClickTime = System.currentTimeMillis()

		when (v?.id) {
			R.id.imgBackTopBar -> requireActivity().onBackPressedDispatcher.onBackPressed()
			R.id.imgPlay -> onPressPlayOrPause()
			R.id.ivScreenshot -> onClickScreenShot()
			R.id.ivScreenshotLive -> onClickScreenShot()
			R.id.frLive -> if (!isLive) onShowLive()
			R.id.frArchive -> if (isLive) {
				playerOpenTime = Calendar.getInstance()    // update archive end time
				binding.timeLine.lastAvailableTime = getEndOfArchive(0)  // update end time in timeline view
				onShowArchive()
			}

			R.id.ivSound -> onToggleSound()
			R.id.ivSoundArchive -> onToggleSound()
			R.id.moveLeft -> onClickMoveStart()
			R.id.moveRight -> onClickMoveEnd()
			R.id.moveLeftOneDay -> onClickMoveLeftOrRight(
				true,
				MOVE_LEFT_ONE_DAY,
				"-24 ${getStringForLayoutByKey("hours")}"
			)

			R.id.moveLeftOneHour -> onClickMoveLeftOrRight(
				true,
				MOVE_LEFT_ONE_HOUR,
				"-1 ${getStringForLayoutByKey("hour")}"
			)

			R.id.moveLeftOneMinute -> onClickMoveLeftOrRight(
				true,
				MOVE_LEFT_ONE_MINUTE,
				"-1 ${getStringForLayoutByKey("minute")}"
			)

			R.id.moveLeftFiveSeconds -> onClickMoveLeftOrRight(
				true,
				MOVE_LEFT_FIVE_SECONDS,
				"-5 ${getStringForLayoutByKey("seconds")}"
			)

			R.id.moveRightOneDay -> onClickMoveLeftOrRight(
				false,
				MOVE_RIGHT_ONE_DAY,
				"+24 ${getStringForLayoutByKey("hours")}"
			)

			R.id.moveRightOneHour -> onClickMoveLeftOrRight(
				false,
				MOVE_RIGHT_ONE_HOUR,
				"+1 ${getStringForLayoutByKey("hour")}"
			)

			R.id.moveRightOneMinute -> onClickMoveLeftOrRight(
				false,
				MOVE_RIGHT_ONE_MINUTE,
				"+1 ${getStringForLayoutByKey("minute")}"
			)

			R.id.moveRightFiveSeconds -> onClickMoveLeftOrRight(
				false,
				MOVE_RIGHT_FIVE_SECONDS,
				"+5 ${getStringForLayoutByKey("seconds")}"
			)

			R.id.ivMoreLive -> setStateBehaviorMore(BottomSheetBehavior.STATE_EXPANDED)
			R.id.ivMoreArchive -> setStateBehaviorMore(BottomSheetBehavior.STATE_EXPANDED)
			R.id.imgCalendar -> onClickCalendar()
			R.id.tvStartPeriodFull -> onClickCalendar(isShowPeriod = true, STATUS_TIME_START_PERIOD)
			R.id.tvEndPeriodFull -> onClickCalendar(isShowPeriod = true, STATUS_TIME_END_PERIOD)
			R.id.tvTimePeriod -> setStateBehaviorTimePicker(BottomSheetBehavior.STATE_EXPANDED)
			R.id.btDownloadArchive -> onClickDownloadArchive()
			R.id.ivPtz -> showHidePTZ()
			R.id.ivPtzUp -> onClickPtz(LABEL_UP)
			R.id.ivPtzDown -> onClickPtz(LABEL_DOWN)
			R.id.ivPtzRight -> onClickPtz(LABEL_RIGHT)
			R.id.ivPtzLeft -> onClickPtz(LABEL_LEFT)
			R.id.ivPtzZoomIn -> onClickPtz(LABEL_ZOOM_IN)
			R.id.ivPtzZoomOut -> onClickPtz(LABEL_ZOOM_OUT)
			R.id.ivPtzReset -> onClickPtz(LABEL_RESET)
		}
	}

	private fun onClickDownloadArchive() {
		setStateBehaviorDownloadArchive(BottomSheetBehavior.STATE_HIDDEN)
		logSdk(TAG, "calendarStartPeriod = ${calendarStartPeriod!!.time}")
		logSdk(TAG, "calendarEndPeriod = ${calendarEndPeriod!!.time}")
		when {
			calendarEndPeriod!!.timeInMillis - calendarStartPeriod!!.timeInMillis > TEN_MIN -> {
				showToast(getStringForLayoutByKey("archive_interval_error"))
			}

			calendarStartPeriod!!.timeInMillis >= calendarEndPeriod!!.timeInMillis -> {
				showToast(getStringForLayoutByKey("archive_format_error"))
			}

			else -> {
				val from = setCalendarServer(calendarStartPeriod!!)
				calendarEndPeriod?.let {
					val to = setCalendarServer(it)
					setStateBehaviorDownloadArchive(BottomSheetBehavior.STATE_HIDDEN)
					presenter.getArchiveLink(
						cameraId.toString(),
						from,
						to,
						requireContext()
					)
				}
			}
		}
	}

	private fun showHidePTZ() {
		if (hasPTZ) {
			binding.ivPtz.setColorFilter(
				getColorCompat(
					requireContext(),
					if (!binding.viewPtz.mainPtz.isVisible) R.color.colorPrimaryDark else R.color.gray_icon
				),
				PorterDuff.Mode.MULTIPLY
			)
			binding.viewPtz.mainPtz.isGoneVMS(binding.viewPtz.mainPtz.isVisible)
			ptzModeEnabled = binding.viewPtz.mainPtz.isVisible
		}
	}

	private fun onClickPtz(label: String) {
		presenter.moveCamera(cameraId.toString(), label, requireContext())
	}

	var isPortrait = true

	override fun onConfigurationChanged(newConfig: Configuration) {
		super.onConfigurationChanged(newConfig)
		logSdk(TAG, "onConfigurationChanged")
		getCurrentFragment()?.binding?.vmsPlayerView?.visibility = View.INVISIBLE
		binding.markHint.isGoneVMS(true) //hide hint
		counterStatesOfBuffering = 0
		val newOrientation = newConfig.orientation
		isPortrait = newOrientation == Configuration.ORIENTATION_PORTRAIT
		val isCurrentItem = this.getCurrentItem() == playerPosition
		if (!isCurrentItem) {
			player.sound = 0f // disable sound for neighboring cameras in fragments
		}
		val scale = resources.displayMetrics.density
		val params = FrameLayout.LayoutParams((180 * scale).toInt(), (228 * scale).toInt())
		params.gravity = if (isPortrait) {
			params.setMargins(0, 0, 0, (8 * scale).toInt())
			Gravity.CENTER_HORIZONTAL
		} else {
			params.setMargins(0, 0, (58 * scale).toInt(), (8 * scale).toInt())
			Gravity.END
		}
		binding.viewPtz.mainPtz.layoutParams = params

		if (!isCreateMarkViewShown) {
			if (isLive) {
				isLive = true
				getLiveStream() // avoid play sound in neighboring fragments
			} else {
				currentDateCursor?.let {
					getArchive(it)
					binding.timeLine.clear()
					Handler(Looper.getMainLooper()).postDelayed({
						binding.timeLine.clear()
						binding.timeLine.setCursor(currentDateCursor)
					}, 2000)
				}
			}
		} else {
//			player.startPlayer()
			updatePlayerView()
		}
		showHideBottomButtons()
		handleLinearOrientation(isPortrait)
		// Log.d("newOrientation", newOrientation.toString())
//        getCurrentFragment()?.binding?.simpleExoPlayerView?.visibility = View.INVISIBLE
		handleLastElementWidthHeight(isPortrait)
	}

	fun showHideNavigationViews() {
		if (!isAnimationShow) {
			isAnimationShow = true
			isHideButtons = binding.viewTopBarNavigation.lnrTopBar.isVisible
			val isEmptyScreen = binding.viewEmptyScreenSdk.emptyScreen.isVisible
			binding.viewTopBarNavigation.lnrTopBar.isGoneSmooth(isHideButtons) // always
			if (isLive && !isEmptyScreen) binding.lnrBottomActions.isGoneSmooth(isHideButtons)
			if (isLive && ptzModeEnabled && !isEmptyScreen) binding.viewPtz.mainPtz.isGoneSmooth(
				isHideButtons
			)
			val needHide = camera.getStartAtLocal().isEmpty()
			val canHandleViewsInArchive =
				!isLive && countFailedLoads < 3 && !(!binding.viewCursor.isVisible && needHide)
						&& (!isEmptyScreen || binding.viewEmptyScreenSdk.emptyTitle.text == getStringForLayoutByKey(
					"err_archive_unavailable"
				))
			if (canHandleViewsInArchive && !isCreateMarkViewShown) binding.viewCursor.isGoneSmooth(
				isHideButtons || needHide
			)
			if (canHandleViewsInArchive) binding.timeLine.isGoneSmooth(isHideButtons || needHide)
			val needHalfAlpha =
				activity != null && !requireActivity().isDestroyed && !requireActivity().isFinishing && isVisible && !isDetached && errorType.isNotEmpty()  // todo change logic
			if (canHandleViewsInArchive) binding.viewBottomBarNavigation.constrBottomBarNav.isGoneSmooth(
				isHideButtons || needHide, if (needHalfAlpha) ALPHA_HALF else ALPHA_FULL
			)
			if (canHandleViewsInArchive && hasMarksToShow && settings.hasEventsToShow()) {
				showHideNearestEventButtonsSmooth(
					isHideButtons || needHide,
					if (needHalfAlpha) ALPHA_HALF else ALPHA_FULL
				)
			}
			if (!isLive && needShowToastHint) binding.tvToastHint.isGoneSmooth(isHideButtons || needHide)
			if (!isLive && needShowMarkHint) binding.markHint.isGoneSmooth(isHideButtons || needHide)
			if (!isLive && isCreateMarkViewShown) binding.cursorCreateMark.isGoneSmooth(
				isHideButtons || needHide
			)
			if (!isEmptyScreen) checkAudioToShowImage(isControls = true)
			if (!isLive && isCreateMarkViewShown) {
				//resize player when create mark view shown and show/hide navigation
				if (isPortrait(requireContext())) {
					//for portrait orientation
					handleLongClickPortrait(isHideButtons || needHide)
				} else {
					//for land orientation
					handleLongClickLand(isHideButtons || needHide)
				}
			}
			Handler(Looper.getMainLooper()).postDelayed({ isAnimationShow = false }, ANIM_DURATION)
		}
	}

	private var secondTickTimerRunnableLive: Runnable = object: Runnable {
		override fun run() {
			if (isVisible) {
				currentLiveCalendar?.timeInMillis = currentLiveCalendar?.timeInMillis!!.plus(1000)
				binding.viewTopBarNavigation.tvTimeTopBarLive.text = setCalendarVMS(
					if (currentLiveCalendar != null) {
						currentLiveCalendar!!   //set time from metadata
					} else {
						Calendar.getInstance()  //set current time
					}
				)
				secondsTimerHandlerLive.postDelayed(this, timeForUpdateTimerSeconds)
			} else {
				logSdk(TAG, "kill runnable")
			}
		}
	}

	private var secondTickTimerRunnable: Runnable = object: Runnable {
		override fun run() {
			if (isVisible) {
				if (currentDateCursor == null || isLive
					|| (activity != null && !requireActivity().isDestroyed && !requireActivity().isFinishing && isVisible && !isDetached
							&& errorType.isNotEmpty())  // todo change logic
					|| !player.playWhenReady
				) return

				if (isNeedAddTimeToArchive && correctTimeArchive != null) {
					//if correct time didn't handled on start archive -> we handle it there
					currentDateCursor?.timeInMillis = correctTimeArchive!!.timeInMillis
					isNeedAddTimeToArchive = false
				}
				currentDateCursor?.timeInMillis = currentDateCursor?.timeInMillis!!.plus(1000)
				if (!handleArchiveTimeChanged()) return
				secondsTimerHandler.postDelayed(this, timeForUpdateTimerSeconds)
			}
		}
	}

	private fun handleArchiveTimeChanged(): Boolean {
		setDateAfterInactiveRange(isRunnableCheck = true)
		if (player.playbackState == Player.STATE_ENDED /*&& !isRtspVideoUrl()*/) {
			//bug with mediaserver - player set to STATE_ENDED when get in inactive range
			getArchive(currentDateCursor!!)
		}
		val newArchiveTime = setCalendarVMS(currentDateCursor!!)
		if (binding.viewTopBarNavigation.tvTimeTopBarArchive.text != newArchiveTime) {
			binding.timeLine.setCursor(currentDateCursor)
			binding.viewTopBarNavigation.tvTimeTopBarArchive.text = newArchiveTime

			val needShow = camera.getStartAtLocal().isNotEmpty()
			val isVisible = binding.viewTopBarNavigation.lnrTopBar.isVisible && needShow && !isLive
			if (!binding.viewCursor.isVisible && isVisible && !isCreateMarkViewShown)
				binding.viewCursor.isGoneVMS(false)
			if (!binding.viewBottomBarNavigation.constrBottomBarNav.isVisible && isVisible) {
				binding.viewBottomBarNavigation.constrBottomBarNav.isGoneVMS(false)
				if (hasMarksToShow && settings.hasEventsToShow()) {
					showHideNearestEventButtons(false)
				}
			}
			checkTimelineVisibility()
			if (hasMarksToShow && needLoadNewMarks && binding.timeLine.bitmap != null && binding.timeLine.leftDate != null) {
				//get marks on every new load url and have bitmap and leftdate to calculate timeline borders
				getMarks()
				needLoadNewMarks = false
			}
			listenChangeMovingButton()
		}
		if (currentDateCursor?.after(getEndOfArchive()) == true) {
			stopTimeAndVideo()
			return false    // mean break handling
		}
		return true
	}

	private var secondTickTimerRunnableSlide: Runnable = object: Runnable {
		//for timer during scroll
		override fun run() {
			if (isVisible) {
				if (currentDateCursor == null || !isSlideHandlerRun
					|| (activity != null && !requireActivity().isDestroyed && !requireActivity().isFinishing && isVisible && !isDetached
							&& errorType.isNotEmpty()) // todo change logic
				) return

				currentDateCursor!!.timeInMillis = currentDateCursor!!.timeInMillis.plus(1000)
				binding.viewTopBarNavigation.tvTimeTopBarArchive.text =
					setCalendarVMS(currentDateCursor!!)

				secondsTimerHandlerSlide.postDelayed(this, timeForUpdateTimerSeconds)
			}
		}
	}

	private fun setDateAfterInactiveRange(isRunnableCheck: Boolean = false) {
		//check and set currentDateCursor and timeline after inactive range
		try {
			currentDateCursor?.let { date ->
				if (listInactiveStreamDurations.isNotEmpty()) {
					for ((index, item) in listInactiveStreamDurations.withIndex()) {
						if (item[0] <= date.timeInMillis && date.timeInMillis <= item[1]) { // *possible transition in timeline
							if (index != listInactiveStreamDurations.lastIndex) {
								// inactive range at start or middle
								binding.timeLine.clear()
								currentDateCursor!!.timeInMillis = item[1].plus(1000)
								// Log.d("currentDateCur*", ": ${currentDateCursor?.time}")
								binding.timeLine.setCursor(currentDateCursor)
								if (isRunnableCheck) getArchive(currentDateCursor!!)
								break
							} else {
								// inactive range at the end
								// for isRunnableCheck -> it means end of archive and runnable stops
								// else -> we load archive after it's end -> set correct time before load
								if (!isRunnableCheck) {
									binding.timeLine.clear()
									currentDateCursor!!.timeInMillis = item[0].minus(1000)
									binding.timeLine.setCursor(currentDateCursor)
								}
								break
							}
						}
					}
				}
			}
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}

	override fun onResume() {
		super.onResume()
		requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
		showHideBottomButtons()
		val currentOrientation = requireContext().resources.configuration.orientation
		handleLinearOrientation(isPortrait = currentOrientation == Configuration.ORIENTATION_PORTRAIT)
		getCamera(cameraId)
		if (activity != null && !requireActivity().isDestroyed &&  !requireActivity().isFinishing && isVisible && !isDetached) {
			changeStatusBarByKeyVMS(requireActivity(), KEY_SCREENSHOT_DETAIL_HIDDEN)
		}
	}

	override fun onStop() {
		timer.stopTimer()
		player.stop() //to pause a video because now our stream is not in focus
		requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
		super.onStop()
	}

	override fun onDestroyView() {
		binding.timeLine.clear()
		stopTimeLive()
		stopTimeArchive(false)
		stopTimeSlide()
		super.onDestroyView()
	}

	override fun onDestroy() {
		stopTimeArchive(false)
		player.release()
		if (::presenter.isInitialized) {
			presenter.detachView(this)
			presenter.destroy()
		}
		try {
			requireContext().unregisterReceiver(receiverChangingTimeZone)
		} catch (e: Exception) {
			e.printStackTrace()
		}
		super.onDestroy()
	}

	fun getErrorScreen() {
		stopTimeAndVideo()
	}

	override fun checkArchiveAfterUpdate() {
		if (!hasShowArchive && !isLive) {
			onShowLive()
		}
	}

	fun updateUserPermissions() {
		val itemPTZ: VMSPermission? = permissions.find { it.name == "ptz" }
		val previousPermissionToIndex = hasMarksToShow
		hasPTZ = camera.services?.ptz ?: false && itemPTZ != null
		hasShowArchive =
			permissions.find { it.name == "archives-show" } != null && state != VMSScreenState.INTERCOM_PHOTO
		hasSavePreview = permissions.find { it.name == "archives-preview-download" } != null
		hasMarksToShow = permissions.find { it.name == "marks-index" } != null
		hasMarksToCreate = permissions.find { it.name == "marks-store" } != null
		hasMarksToUpdate = permissions.find { it.name == "marks-update" } != null
		hasDownloadArchive = permissions.find { it.name == "archives-download" } != null
		val hasPermissionSystem = permissions.find { it.name == "camera-events-index" } != null
		// todo new permission introduced in 23.09, can be removed in 24.09 (first names from pair)
		val hasPermissionAnalytic =
			permissions.find { it.name == "analytic-cases-index" || it.name == "analytic" } != null
		hasPermissionsEvents = hasMarksToShow || hasPermissionSystem || hasPermissionAnalytic

		binding.ivPtz.isGoneVMS(!hasPTZ)
		binding.viewTopBarNavigation.frArchive.isGoneVMS(!hasShowArchive)
		if (!isLive && !hasShowArchive) {
			//if play archive and permission removed -> show toast and go to live
			showToast(getStringForLayoutByKey("err_archive_permission_deny"))
			onShowLive()
		}
		binding.ivScreenshotLive.isGoneVMS(!hasSavePreview)
		binding.viewBottomBarNavigation.ivScreenshot.isGoneVMS(!hasSavePreview)
		if (previousPermissionToIndex != hasMarksToShow && binding.timeLine.getLeftTimelineCalendar() != null && binding.timeLine.getRightTimelineCalendar() != null) {
			//only if permission changed and have timeline drawn
			if (camera.getStartAtLocal().isNotEmpty() && hasMarksToShow) {
				getMarks()
				binding.leftButtonFrame.alpha =
					ALPHA_FULL   //set alpha because it can be set to 0f in isGoneSmooth
				binding.rightButtonFrame.alpha =
					ALPHA_FULL   //set alpha because it can be set to 0f in isGoneSmooth
				showHideNearestEventButtons(isHideButtons || !settings.hasEventsToShow())
			} else {
				loadMarks(emptyList())
				showHideNearestEventButtons(true)
			}
		}
		if (isCreateMarkViewShown) {
			//if marks create view shown
			if (changedMark != null) {
				//if marks update
				if (!hasMarksToShow || !hasMarksToUpdate) {
					//if no permission for show or update marks -> close player
					requireActivity().onBackPressedDispatcher.onBackPressed()
				}
			} else {
				//if mark create
				if (!hasMarksToShow || !hasMarksToCreate) {
					//if no permission for show or create marks -> close mark create view
					closeCreateMarkView(needPlayArchive = true)
				}
			}
		}
		updateOpenedBottomSheet()
	}

	fun getMarks() {
		if (settings.hasEventsToShow()) {
			binding.timeLine.getLeftTimelineDate()?.let { leftDate ->
				binding.timeLine.getRightTimelineDate()
					?.let { rightDate ->
						//get marks only for current timeline (+1h on the end)
						presenter.getMarks(
							cameraId.toString(),
							leftDate,
							rightDate,
							getEndOfArchive(0),
							requireContext()
						)
					}
			}
		}
	}

	//for update open bottom sheet when permission changed
	private fun updateOpenedBottomSheet() {
		if (binding.viewSheetBottomMore.viewBottomMore.isVisible) { //open bottom sheet more
			adapterMore?.updatePermission(
				hasMarksToShow && !isCreateMarkViewShown, hasDownloadArchive, isLive
			)  //update and notify adapter
		} else if (binding.viewSheetBottomEventsPlayer.viewBottomEventsPlayer.isVisible && !hasMarksToShow) { //open bottom sheet marks and permission removed
			bottomViewMarks?.state = BottomSheetBehavior.STATE_HIDDEN   //hide bottom sheet
		}
	}

	private fun saveInactiveRange() {
		listInactiveStreamDurations.clear()
		if (archiveRanges.isNotEmpty()) {
			val firstRangeStart = archiveRanges.first().from.toLong() * 1000
			camera.getStartAtLocal().let { archiveStart ->
				val archiveStartInMillis = setCalendarByDateServer(archiveStart).timeInMillis
				if (firstRangeStart > archiveStartInMillis) {
					val startInactiveStreamDuration = ArrayList<Long>()
					startInactiveStreamDuration.add(archiveStartInMillis)
					startInactiveStreamDuration.add(firstRangeStart)
					listInactiveStreamDurations.add(startInactiveStreamDuration)
				}
			}
			for ((index, item) in archiveRanges.withIndex()) {
				if (index != 0) {
					val preItem = archiveRanges[index - 1]
					var preRangeEnd = (preItem.from + preItem.duration).toLong()
					var currentRangeStart = item.from.toLong()
					preRangeEnd *= 1000 // to millis
					currentRangeStart *= 1000 // to millis
					val inactiveStreamDuration = ArrayList<Long>()
					inactiveStreamDuration.add(preRangeEnd)
					inactiveStreamDuration.add(currentRangeStart)
					listInactiveStreamDurations.add(inactiveStreamDuration)
				}
			}
			if (camera.isRestrictedArchive == true) {
				// add last range only if archive is restricted
				val lastRangeEnd =
					(archiveRanges.last().from + archiveRanges.last().duration).toLong() * 1000
				val archiveEndInMillis = playerOpenTime.timeInMillis
				if (archiveEndInMillis > lastRangeEnd) {
					val endInactiveStreamDuration = ArrayList<Long>()
					endInactiveStreamDuration.add(lastRangeEnd)
					endInactiveStreamDuration.add(archiveEndInMillis)
					listInactiveStreamDurations.add(endInactiveStreamDuration)
				}
			}
		}
	}

	override fun stopTimeLive() {
		secondsTimerHandlerLive.removeCallbacks(this.secondTickTimerRunnableLive)
		secondsTimerHandlerLive.removeCallbacksAndMessages(null)
	}

	override fun startHandlerAndChangeDateLive() {
		binding.viewTopBarNavigation.tvTimeTopBarLive.isGoneVMS(false)
		binding.viewTopBarNavigation.tvTimeTopBarArchive.isGoneVMS(true)
		stopTimeLive()
		secondsTimerHandlerLive.postDelayed(secondTickTimerRunnableLive, timeForUpdateTimerSeconds)
	}

	override fun stopTimeArchive(isShowTime: Boolean) {
		if (isShowTime) {
			showEmptyScreen(needShow = false)
			binding.markHint.isGoneVMS(true)
			binding.tvToastHint.isGoneVMS(true)
			needShowToastHint = false
			binding.tvTime.isGoneVMS(false)
		}
		secondsTimerHandler.removeCallbacks(this.secondTickTimerRunnable)
		secondsTimerHandler.removeCallbacksAndMessages(null)
	}

	override var previousNavigatedMarkFrom: Calendar? = null //save time of navigated mark
	override var previousNavigatedMarkTime: Calendar? = null //save time of navigated mark

	override fun vibrate(time: Long) {
		if (settings.needVibration && activity != null && !requireActivity().isDestroyed && !requireActivity().isFinishing && isVisible && !isDetached && !isVibrateNow) {
			isVibrateNow = true
			//perform haptic feedback (vibration) ignore phone and user settings
			binding.timeLine.performHapticFeedback(
				HapticFeedbackConstants.VIRTUAL_KEY,
				HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING or HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
			)
			Handler(Looper.getMainLooper()).postDelayed(
				{ isVibrateNow = false },
				time
			)    //150ms delay before next vibration
		}
	}

	override fun zoomOut() {
		needLoadNewMarks = true
	}

	private fun stopTimeSlide() {
		isSlideHandlerRun = false
		secondsTimerHandlerSlide.removeCallbacks(this.secondTickTimerRunnableSlide)
		secondsTimerHandlerSlide.removeCallbacksAndMessages(null)
	}

	override fun startHandlerAndChangeDateArchive(d: Calendar) {
		logSdk(TAG, "startHandlerAndChangeDateArchive = ${d.time}")
		stopTimeSlide()
		binding.viewTopBarNavigation.tvTimeTopBarLive.isGoneVMS(true)
		binding.viewTopBarNavigation.tvTimeTopBarArchive.isGoneVMS(false)
		if (isOnline(requireContext())) {
			try {
				stopTimeArchive(false)
				if (!isCreateMarkViewShown) {
					if (player.playWhenReady) binding.viewBottomBarNavigation.imgPlay.setImageResource(
						R.drawable.bt_pause
					)
				} else {
					setDoneCreateMark(false) // to avoid create wrong time for mark when scroll
				}
				binding.tvTime.isGoneVMS(true)
				isLive = false //set date from swipe timeline, check for end of archive only here
				currentDateCursor = d.clone() as Calendar
				if (currentDateCursor?.after(getEndOfArchive()) == true) {
					currentDateCursor =
						getEndOfArchive()   // prevent loading after last archive range
				}
				if (!player.isRtsp()) secondsTimerHandler.postDelayed(
					secondTickTimerRunnable,
					timeForUpdateTimerSeconds
				)

				//if statusStreamHighActive == true -> archive being recorded now
				val current = if (statusStreamHighActive || archiveRanges.isNotEmpty()) {
					//access current time
					Calendar.getInstance().apply { timeInMillis -= 1000 }
				} else {
					//access last available range
					getEndOfArchive()
				}
				when {
					current.timeInMillis <= currentDateCursor!!.timeInMillis -> {
						currentDateCursor!!.timeInMillis = current.timeInMillis - 1000
					}
					// when we load start archive, we send request with camera start_at
					// on response we get time from metadata and it can be less then start_at, so remove this check
					/*beginningOfArchive > currentDateCursor!!.timeInMillis -> {
                        currentDateCursor!!.timeInMillis = beginningOfArchive
                        onClickMoveLeft()
                    }*/
				}
			} catch (e: Exception) {
				e.message
			}
		}
	}

	override fun slideTimeLineAndPassDateArchive(d: Calendar) {
		if (!isCreateMarkViewShown && !isSlideHandlerRun) {
			isSlideHandlerRun = true
			secondsTimerHandlerSlide.postDelayed(
				secondTickTimerRunnableSlide,
				timeForUpdateTimerSeconds
			)
		}
		binding.tvTime.text = setCalendarVMS(d)
	}

	override fun clickMark(mark: VMSEvent) {
		mark.getFromLocal().let { from ->
			try {
				//revert params after long click
				val layoutParams = CoordinatorLayout.LayoutParams(
					CoordinatorLayout.LayoutParams.WRAP_CONTENT,
					CoordinatorLayout.LayoutParams.WRAP_CONTENT
				).apply {
					gravity = (Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
					marginStart = 0
					marginEnd = 0
					bottomMargin = hintMarginBottom
				}
				binding.markHint.layoutParams = layoutParams

				if (currentDateCursor != null) {
					checkMarkStartEndArchive(from)
					getArchive(currentDateCursor!!)
				}
				binding.markHintTitle.text = mark.title
				binding.markHintTime.text = setCalendarVMS(setCalendarByDateServer(from))
				binding.markHint.alpha =
					1f   //set alpha because it can be set to 0f in isGoneSmooth
				binding.markHintTime.isGoneVMS(false)   //can be gone by show error message
				binding.markHint.isGoneVMS(false)    //show hint
				needShowMarkHint = true

				Handler(Looper.getMainLooper()).postDelayed({
					//if haven't another clicks on mark before || haven't another clicks on marks during the TIME_FOR_SHOW_MARK_HINT
					if (lastClickedTimeOnMark == null || Calendar.getInstance().timeInMillis - lastClickedTimeOnMark!!.timeInMillis >= TOAST_DURATION) {
						binding.markHint.isGoneVMS(true) //hide hint
						needShowMarkHint = false
					}
				}, TOAST_DURATION)
				previousNavigatedMarkFrom = setCalendarByDateServer(from)//save time navigated mark
				previousNavigatedMarkTime = Calendar.getInstance()
				Handler(Looper.getMainLooper()).postDelayed({
					//if haven't another clicks on marks during 5s rule
					previousNavigatedMarkTime?.let {
						if (Calendar.getInstance().timeInMillis - it.timeInMillis >= GO_TO_PREVIOUS_MARK_DELAY) {
							previousNavigatedMarkTime = null
							previousNavigatedMarkFrom = null
						}
					}
				}, GO_TO_PREVIOUS_MARK_DELAY)
			} catch (e: Exception) {
				e.message
			}
		}
	}

	override fun onLongClickMark(mark: VMSEvent, centerX: Float, screenWidth: Float) {
		logSdk(TAG, "onLongClickMark = ${mark.getFromLocal()}")

		mark.getFromLocal().let { from ->
			try {
				binding.markHintTitle.text = mark.title
				binding.markHintTime.text = setCalendarVMS(setCalendarByDateServer(from))

				binding.markHint.measure(0, 0)
				val viewWidth = binding.markHint.measuredWidth
				if (centerX <= screenWidth / 2) {
					//view on first half of the screen -> set marginStart
					var marginStartX = centerX - viewWidth / 2
					if (marginStartX < 0) marginStartX = 0f
					val layoutParams = CoordinatorLayout.LayoutParams(
						CoordinatorLayout.LayoutParams.WRAP_CONTENT,
						CoordinatorLayout.LayoutParams.WRAP_CONTENT
					).apply {
						gravity = (Gravity.BOTTOM or Gravity.START)
						marginStart = marginStartX.toInt()
						marginEnd = 0
						bottomMargin = hintMarginBottom
					}
					binding.markHint.layoutParams = layoutParams
				} else {
					//view on last half of the screen -> set marginEnd
					var marginEndY = screenWidth - centerX - viewWidth / 2
					if (marginEndY < 0) marginEndY = 0f
					val layoutParams = CoordinatorLayout.LayoutParams(
						CoordinatorLayout.LayoutParams.WRAP_CONTENT,
						CoordinatorLayout.LayoutParams.WRAP_CONTENT
					).apply {
						gravity = (Gravity.BOTTOM or Gravity.END)
						marginStart = 0
						marginEnd = marginEndY.toInt()
						bottomMargin = hintMarginBottom
					}
					binding.markHint.layoutParams = layoutParams
				}

				binding.markHint.alpha =
					1f   //set alpha because it can be set to 0f in isGoneSmooth
				binding.markHint.isGoneVMS(false)    //show hint
				needShowMarkHint = true
				lastClickedTimeOnMark = Calendar.getInstance()    //save time when show mark
				Handler(Looper.getMainLooper()).postDelayed({
					//if haven't another clicks on mark before || haven't another clicks on marks during the TIME_FOR_SHOW_MARK_HINT
					if (lastClickedTimeOnMark == null || Calendar.getInstance().timeInMillis - lastClickedTimeOnMark!!.timeInMillis >= LONG_CLICK_DURATION) {
						binding.markHint.isGoneVMS(true) //hide hint
						needShowMarkHint = false
					}
				}, LONG_CLICK_DURATION)
			} catch (e: Exception) {
				e.message
			}
		}
	}

	override fun onLongClick(centerX: Float, calendarClick: Calendar, isConfigChange: Boolean) {
		var isWithinActiveStream = false    //ban create mark on inactive stream range
		for ((index, item) in archiveRanges.withIndex()) {
			val from = item.from * 1000L
			//if it last archive range and HD stream active -> set it duration until current time
			val to =
				if (index == archiveRanges.lastIndex && camera.isRestrictedArchive != true && (statusStreamHighActive || archiveRanges.isNotEmpty())) playerOpenTime.timeInMillis
				else (item.from + item.duration) * 1000L
			if (calendarClick.timeInMillis in from..to) {
				isWithinActiveStream = true
				break
			}
		}
		//if create new mark -> we have video height and width
		//if update existed mark -> long click only after player ready, so we have video height and width
		//if rotate screen -> we can haven't video height and width
		//so if it's create or update mark -> set new values for variables: videoWidth and videoHeight
		//if it's rotate screen -> we set variables in closeMarkCreateView method and just take them
		val isHaveVideoFormat = player.height != 0 && player.width != 0
		//if main activity exist
		//if long click on available archive range or it's update mark or it's reopen after rotate screen
		//if have permissions to index (show) and create or update mark
		//if have video height and width or change screen orientation
		if (activity != null && !requireActivity().isDestroyed && !requireActivity().isFinishing && isVisible && !isDetached
			&& (isWithinActiveStream || changedMark != null || isConfigChange)
			&& hasMarksToShow && (hasMarksToCreate || (changedMark != null && hasMarksToUpdate))

		) {
			resetCurrentElementZoom()   //reset zoom when show mark create view (zoom banned)
			isCreateMarkViewShown = true
			enableOrDisableButtonsInModeMark(false)
			forcePlayVideo = false
			centerCreateMark = centerX
			logSdk(TAG, "**calendarClick 1 = ${calendarClick.time}")
			val currentDateCenter = currentDateCursor!!.clone() as Calendar
			currentDateCursor = calendarClick.clone() as Calendar
			logSdk(TAG, "**calendarClick 2  = ${currentDateCursor!!.time}")

			setDateTimeMarkCreate()

			if (changedMark != null && !isConfigChange) {
				//if update mark and not a rotate -> set time and stop timer
				markFrom?.let {
					binding.viewTopBarNavigation.tvTimeTopBarArchive.text =
						setCalendarVMS(setCalendarByDateServer(it))
				}
				if (player.isRtsp()) {
					// need time to start new thread and invalidate surface
					// stop video only after start playing
					stopTimeArchive(false)
				} else {
					stopTimeAndVideo()
				}
			} else {
				//if create mark or config change -> load chosen date
				playCameraArchive()
			}
			binding.viewCursor.isGoneVMS(true)
			showHideNearestEventButtons(true)
			if (settings.needVibration && changedMark == null && !isConfigChange) {    //if vibration enabled, not edit mark, not rotate screen
				binding.timeLine.performHapticFeedback(
					HapticFeedbackConstants.VIRTUAL_KEY,
					HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING or HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
				)    //vibrate once
			}
			setNameMarkCreate()
			if (isPortrait(requireContext())) {
				//for portrait orientation
				//set new values for variables: videoWidthPortrait and videoHeightPortrait (see comments above)
				videoWidthPortrait = requireActivity().getWindowWidth()
				videoHeightPortrait = if (isHaveVideoFormat) {
					videoWidthPortrait * player.height / player.width
				} else {
					videoWidthPortrait * 9 / 16
				}
				handleLongClickPortrait(isHideButtons)
			} else {
				//for land orientation
				//set new values for variables: videoWidthLand and videoHeightLand (see comments above)
				videoHeightLand = requireActivity().getWindowHeight()
				videoWidthLand = if (isHaveVideoFormat) {
					videoHeightLand * player.width / player.height
				} else {
					videoHeightLand * 16 / 9
				}
				getCurrentFragment()?.binding?.mainFrameExo?.setLayoutParamsCustom(
					COORDINATOR,
					requireActivity().getWindowWidth() - markCreateViewWidth,
					CoordinatorLayout.LayoutParams.WRAP_CONTENT,
					layoutGravity = Gravity.START
				)
				//set negative margin for centering player video after resize
				getCurrentFragment()?.binding?.vmsPlayerView?.setMargins(
					-(markCreateViewWidth / 2),
					0,
					0,
					0
				)
				handleLongClickLand(isHideButtons)
			}
			binding.tvTime.setMargins(0, 0, 0, 130.toPx())
			binding.tvToastHint.setMargins(0, 0, 0, 130.toPx())
			binding.cursorCreateMark.setMargins(
				(centerX.toInt() - markCursorWidthToCenter),
				0,
				0,
				52.toPx()
			)
			binding.cursorCreateMark.alpha =
				1f  //set alpha because it can be set to 0f in isGoneSmooth
			binding.cursorCreateMark.isGoneVMS(isHideButtons)
			binding.newMarkInputView.isGoneSmooth(false)

			if (changedMark == null && !isConfigChange) {
				//animate only for create new mark
				setDoneCreateMark(false)
				val animateTime = calendarClick.timeInMillis - currentDateCenter.timeInMillis
				ValueAnimator.ofFloat(0f, 1f).apply {
					duration = ANIM_DURATION
					addUpdateListener { update ->
						val calendar = Calendar.getInstance().apply {
							timeInMillis =
								currentDateCenter.timeInMillis + (animateTime * (update.animatedValue as Float)).toLong()
						}
						binding.timeLine.setCursorByClickMoveButton(calendar)
					}
					start()
				}
				ValueAnimator.ofFloat(
					centerX,
					requireActivity().getWindowWidth().toFloat() / 2
				).apply {
					duration = ANIM_DURATION
					addUpdateListener { update ->
						binding.cursorCreateMark.setMargins(
							((update.animatedValue as Float).toInt() - markCursorWidthToCenter),
							0,
							0,
							52.toPx()
						)
						binding.cursorCreateMark.invalidate()
					}
					start()
				}
			}
		}
	}

	private fun setDoneCreateMark(isEnabled: Boolean) {
		binding.doneToCreateButton.isEnabled = isEnabled
		binding.doneToCreateButton.isClickable = isEnabled
		binding.doneToCreateButton.setBackgroundResource(if (isEnabled) R.drawable.bg_blue_round_6dp else R.drawable.bg_gray_round_6dp)
	}

	private fun handleLongClickPortrait(isHide: Boolean) {
		val topViewHeight = 76.toPx()
		val topMarginMarkCreateView = 14.toPx()
		val bottomMarginMarkCreateView = 130.toPx()
		getCurrentFragment()?.binding?.mainFrameExo?.setLayoutParamsCustom(
			COORDINATOR,
			CoordinatorLayout.LayoutParams.WRAP_CONTENT,
			CoordinatorLayout.LayoutParams.WRAP_CONTENT,
			Gravity.TOP,
			TOP,
			if (isHide) 0 else topViewHeight
		)
		val topMargin = if (isHide) videoHeightPortrait + topMarginMarkCreateView
		else topViewHeight + videoHeightPortrait + topMarginMarkCreateView
		getCurrentFragment()?.binding?.vmsPlayerView?.setMargins(0, 0, 0, 0)
		binding.newMarkInputView.setMargins(
			0,
			topMargin,
			0,
			if (isHide) 0 else bottomMarginMarkCreateView
		)
	}

	private fun handleLongClickLand(isHide: Boolean) {
		val windowHeight = requireActivity().getWindowHeight()
		val topMarginView =
			if (isHide) 6.toPx() else 50.toPx() //44dp topView and 6dp topMargin for markCreateView
		val cutPadding =
			if (isHide || windowHeight - 76.toPx() - 92.toPx() >= markCreateViewHeight) 0 else 4
		binding.newMarkInputView.setLayoutParamsCustom(
			FRAME,
			markCreateViewWidth,
			FrameLayout.LayoutParams.MATCH_PARENT,
			Gravity.END,
			TOP,
			topMarginView
		)
		setPaddingsCreateMarkView(cutPadding = cutPadding)
	}

	private fun setPaddingsCreateMarkView(cutPadding: Int) {
		binding.etMarkTitle.setCustomPaddingMarkTitle(cutPadding)
		binding.llMarkDate.setCustomPaddingMarkDate(cutPadding)
		binding.doneToCreateButton.setCustomPaddingMarkButtons(cutPadding)
		binding.cancelButton.setCustomPaddingMarkButtons(cutPadding)
	}

	private fun setNameMarkCreate() {
		if (savedMarkName != null) {
			//if have saved mark name -> set it
			binding.etMarkTitle.setText(savedMarkName!!)
			savedMarkName = null  //reset saved mark name to null
		} else {
			//haven't saved mark name -> not configuration change
			if (changedMark != null) {
				//update existed mark
				binding.etMarkTitle.setText(changedMark!!.title)
			} else {
				//create new mark
				val line = getStringForLayoutByKey("mark_new_title")
				val nameHint =
					if (counterForMarkCreateHint == 1) line else "$line $counterForMarkCreateHint"
				if (!currentCameraMarks.contains(nameHint)) {
					binding.etMarkTitle.setText(nameHint)
				} else {
					counterForMarkCreateHint++
					setNameMarkCreate()
				}
			}
		}
	}

	private fun setDateTimeMarkCreate(calendar: Calendar? = null) {
		logSdk(TAG, "**calendarClick 1 = ${currentDateCursor?.time}")
		val c = calendar ?: currentDateCursor
		c?.let {
			binding.viewTopBarNavigation.tvTimeTopBarArchive.text = setCalendarVMS(it)
			if (isCreateMarkViewShown) {
				binding.tvMarkDate.text = setCurrentDateMarkCreate(it)
				binding.tvMarkTime.text = getOnlyTime(it)
			}
		}
	}

	private fun closeCreateMarkView(needPlayArchive: Boolean) {
		if (!isCreateMarkViewShown) return
		if (activity != null && !requireActivity().isDestroyed &&  !requireActivity().isFinishing && isVisible && !isDetached) {
			isCreateMarkViewShown = false
			enableOrDisableButtonsInModeMark(true)
			resetCurrentElementZoom()
			binding.cursorCreateMark.isGoneVMS(true)
			binding.viewCursor.isGoneVMS(isHideButtons)
			if (!isHideButtons && hasMarksToShow && settings.hasEventsToShow()) {
				showHideNearestEventButtons(false)
			}

			if (needPlayArchive) {
				if (player.playbackState == STATE_IDLE.state && !isLive) {
					//for bug on Xiaomi Redmi note 4 (Redmi 4x) - 7 (7.1) android
					playCameraArchive()
				} else {
					// need to start playing after close mark create view
					if (player.isRtsp()) player.play() else player.startPlayer()
				}
			}
			binding.viewBottomBarNavigation.imgPlay.setImageResource(R.drawable.bt_pause)
			binding.tvTime.setMargins(0, 0, 0, hintMarginBottom)
			binding.tvToastHint.setMargins(0, 0, 0, hintMarginBottom)

			binding.newMarkInputView.isGoneVMS(true)
			val width: Int
			val height: Int
			if (isPortrait(requireContext())) {
				if (videoWidthPortrait != 0 && videoHeightPortrait != 0) {
					width = videoWidthPortrait
					height = videoHeightPortrait
				} else {
					//if haven't width or height - calculate them from video data
					width = requireActivity().getWindowWidth() //match parent
					height = width * player.height / player.width
					videoWidthPortrait = width
					videoHeightPortrait = height
				}
			} else {
				if (videoWidthLand != 0 && videoHeightLand != 0) {
					width = videoWidthLand
					height = videoHeightLand
				} else {
					//if haven't width or height - calculate them from video data
					height = requireActivity().getWindowHeight()   //match parent
					width = height * player.width / height
					videoWidthLand = width
					videoHeightLand = height
				}
			}
			getCurrentFragment()?.binding?.vmsPlayerView?.setLayoutParamsCustom(
				FRAME,
				width,
				height
			)
			binding.newMarkInputView.setLayoutParamsCustom(
				FRAME,
				FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
			)
			setPaddingsCreateMarkView(cutPadding = 0)
			getCurrentFragment()?.binding?.mainFrameExo?.setLayoutParamsCustom(
				COORDINATOR,
				CoordinatorLayout.LayoutParams.WRAP_CONTENT,
				CoordinatorLayout.LayoutParams.WRAP_CONTENT,
				layoutGravity = Gravity.CENTER
			)
		}
	}

	override fun createMarkSuccess() {
		closeCreateMarkView(needPlayArchive = true)
		showHideNearestEventButtons(isHideButtons || !settings.hasEventsToShow())
		if (hasMarksToShow) getMarks()
		showToast(getStringForLayoutByKey("mark_created"))
	}

	override fun updateEventSuccess(event: VMSEvent) {
		closeCreateMarkView(needPlayArchive = false)
		vmsMarksSocket.postValue(VMSEventSocket(null, event))
		updateEventFlag = true
		requireActivity().onBackPressedDispatcher.onBackPressed()
	}

	override fun stopTimeAndVideo() {
		if (isLive) {
			stopTimeLive()
		} else {
			stopTimeArchive(false)
			player.stop()
			binding.viewBottomBarNavigation.imgPlay.setImageResource(R.drawable.bt_play)
			binding.tvTime.isGoneVMS(true)
		}
	}

	override fun canHandleTimeline(canHandle: Boolean) {
		binding.timeLine.canSwipeTimeline = canHandle
	}

	override fun checkTimelineVisibility() {
		val isVisible =
			binding.viewTopBarNavigation.lnrTopBar.isVisible && binding.viewBottomBarNavigation.constrBottomBarNav.isVisible && camera.getStartAtLocal()
				.isNotEmpty() && !isLive
		if (isVisible && (!binding.timeLine.isVisible || binding.timeLine.alpha != ALPHA_FULL || !binding.viewCursor.isVisible || binding.viewCursor.alpha != ALPHA_FULL)) {
			// bug with fast clicking with isGoneSmooth -> need set alpha to 1
			binding.timeLine.alpha = ALPHA_FULL
			binding.viewCursor.alpha = ALPHA_FULL
			binding.timeLine.isGoneVMS(false)
			binding.viewCursor.isGoneVMS(isCreateMarkViewShown)
		}
	}

	private fun initServerDateTime(camera: VMSCamera) {
		try {
			//Import part : x.0 for float number
			if (markFrom.isNullOrEmpty() && !isLive && currentDateCursor == null) currentDateCursor =
				Calendar.getInstance()
			binding.timeLine.markFrom = markFrom
			binding.timeLine.init(
				this,
				camera,
				archiveRanges,
				presenter.compositeDisposable,
				getEndOfArchive(0)
			)
		} catch (e: ParseException) {
			e.printStackTrace()
		}
	}

	fun getLiveStream() {
		checkAllStreams()
		val id = if (cameraId != 0) cameraId.toString() else STREAM_ID // STREAM_ID if need for test
		if (statusStreamHighActive && (tempQuality ?: settings.videoQuality).equals(HIGH, true)) {
			//if hd stream active and chosen quality is high
			presenter.getLiveStream(id, HIGH, requireContext())
		} else if (statusStreamLowActive) { //if sd stream active
			//choose hd stream, but it is not active -> set quality to sd and reset quality adapter
			presenter.getLiveStream(id, LOW, requireContext()) //get sd stream
		} else if (statusStreamHighActive) {
			//when no sd stream, but have hd -> set quality to hd, reset quality adapter and get hd stream
			presenter.getLiveStream(id, HIGH, requireContext())
		} else {    //hd and sd streams are not active
			showEmptyScreen(needShow = true)
		}
		initQualityAdapter()
		initSpeedAdapter() // need for change speed if different codec
	}

	private fun checkAudioToShowImage(isControls: Boolean? = null): Boolean {
		if (player.isRtsp()) {
			if (player.isSoundAvailable) {
				if (player.speedX != 1.0f) {
					binding.viewBottomBarNavigation.ivSoundArchive.visibility = View.INVISIBLE
					return false
				}
				val iv = if (isLive) binding.ivSound else binding.viewBottomBarNavigation.ivSoundArchive
				if (isLive) {
					if (isControls == null) iv.isGoneVMS(isHideButtons)
					else iv.isGoneSmooth(isHideButtons)
				} else {
					iv.visibility = if (!isHideButtons) View.VISIBLE else View.INVISIBLE
				}
				iv.setOnClickListener(this)
				iv.setImageResource(if (settings.enabledAudio) R.drawable.ic_sound_small else R.drawable.ic_sound_small_hide)
			} else {
				binding.viewBottomBarNavigation.ivSoundArchive.visibility = View.INVISIBLE
				binding.ivSound.isGoneVMS(true)
			}
			return player.isSoundAvailable
		} else {
			val streams = camera.streams
			if (!streams.isNullOrEmpty()) {
				val iv =
					if (isLive) binding.ivSound else binding.viewBottomBarNavigation.ivSoundArchive
				if (streams.size == 1) {
					val hasSound = streams[0].hasSound ?: false
					if (isLive) {
						//if no sound or swipe between cameras - show/hide without animation
						if (!hasSound || isControls == null) iv.isGoneVMS(!hasSound || isHideButtons)
						//have sound and come from show/hide navigation - show/hide with animation
						else iv.isGoneSmooth(isHideButtons)
					} else {
						iv.visibility =
							if (hasSound && !isHideButtons) View.VISIBLE else View.INVISIBLE
					}
					iv.setOnClickListener(if (hasSound) this else null)
					iv.setImageResource(if (settings.enabledAudio) R.drawable.ic_sound_small else R.drawable.ic_sound_small_hide)
					// Log.d("streamQuality_1", "$hasSound")
					return hasSound
				} else {
					if (!isLive) {
						val hasSoundHigh =
							camera.streams?.find { it.type == HIGH }?.hasSound ?: false
						iv.visibility =
							if (hasSoundHigh && !isHideButtons) View.VISIBLE else View.INVISIBLE
						iv.setOnClickListener(if (hasSoundHigh) this else null)
						iv.setImageResource(if (settings.enabledAudio) R.drawable.ic_sound_small else R.drawable.ic_sound_small_hide)
						// Log.d("streamQuality_HIGH", "$hasSoundHigh")
						return hasSoundHigh
					}
					if (settings.videoQuality == HIGH && statusStreamHighActive) {
						val hasSoundHigh =
							camera.streams?.find { it.type == HIGH }?.hasSound ?: false
						if (isLive) {
							if (!hasSoundHigh || isControls == null) iv.isGoneVMS(!hasSoundHigh || isHideButtons)
							else iv.isGoneSmooth(isHideButtons)
						} else {
							iv.visibility =
								if (hasSoundHigh && !isHideButtons) View.VISIBLE else View.INVISIBLE
						}
						iv.setOnClickListener(if (hasSoundHigh) this else null)
						iv.setImageResource(if (settings.enabledAudio) R.drawable.ic_sound_small else R.drawable.ic_sound_small_hide)
						// Log.d("streamQuality_HIGH", "$hasSoundHigh")
						return hasSoundHigh
					} else if (settings.videoQuality == LOW && statusStreamLowActive) {
						val hasSoundLow = camera.streams?.find { it.type == LOW }?.hasSound ?: false
						if (isLive) {
							if (!hasSoundLow || isControls == null) iv.isGoneVMS(!hasSoundLow || isHideButtons)
							else iv.isGoneSmooth(isHideButtons)
						} else {
							iv.visibility =
								if (hasSoundLow && !isHideButtons) View.VISIBLE else View.INVISIBLE
						}
						iv.setOnClickListener(if (hasSoundLow) this else null)
						iv.setImageResource(if (settings.enabledAudio) R.drawable.ic_sound_small else R.drawable.ic_sound_small_hide)
						// Log.d("streamQuality_LOW", "$hasSoundLow")
						return hasSoundLow
					} else {
						binding.viewBottomBarNavigation.ivSoundArchive.visibility = View.INVISIBLE
						binding.ivSound.isGoneVMS(true)
						return false
					}
				}
			} else {
				binding.viewBottomBarNavigation.ivSoundArchive.visibility = View.INVISIBLE
				binding.ivSound.isGoneVMS(true)
				return false
			}
		}
	}

	private fun onToggleSound() {
		player.sound = if (settings.enabledAudio) 0f else 1f
		settings.enabledAudio = !settings.enabledAudio
		val iv = if (isLive) binding.ivSound else binding.viewBottomBarNavigation.ivSoundArchive
		iv.setImageResource(if (settings.enabledAudio) R.drawable.ic_sound_small else R.drawable.ic_sound_small_hide)
	}

	private fun checkAllStreams() {
		if (camera.streams != null) {
			when (camera.streams!!.size) {
				2 -> {
					if (camera.streams!![0].type == HIGH) {
						statusStreamHighActive = camera.streams!![0].status == ACTIVE
						statusStreamLowActive = camera.streams!![1].status == ACTIVE
						// Log.d("camera_streams", "size == 2, streams[0].type == HIGH")
					} else {
						statusStreamHighActive = camera.streams!![1].status == ACTIVE
						statusStreamLowActive = camera.streams!![0].status == ACTIVE
						// Log.d("camera_streams", "size == 2, streams[0].type == LOW")
					}
				}

				1 -> {
					if (camera.streams!![0].type == HIGH) {
						statusStreamHighActive = camera.streams!![0].status == ACTIVE
						statusStreamLowActive = false
						// Log.d("camera_streams", "size == 1, streams[0].type == HIGH")
					} else {
						statusStreamLowActive = camera.streams!![0].status == ACTIVE
						statusStreamHighActive = false
						// Log.d("camera_streams", "size == 1, streams[0].type == LOW")
					}
				}

				else -> {
					//no active streams
					statusStreamHighActive = false
					statusStreamLowActive = false
					return
				}
			}
		} else {
			//no active streams
			statusStreamHighActive = false
			statusStreamLowActive = false
			return
		}
	}

	private fun showToastIfCurrentItem(message: String) {
		val isCurrentItem = this.getCurrentItem() == playerPosition
		if (activity != null && !requireActivity().isDestroyed && !requireActivity().isFinishing && isVisible && !isDetached && isCurrentItem) {
			binding.tvToastHint.text = message
			binding.tvToastHint.alpha =
				ALPHA_FULL   //set alpha because it can be set to 0f in isGoneSmooth
			binding.tvToastHint.isGoneVMS(false)
			needShowToastHint = true
			lastToastTime = Calendar.getInstance()    //save time when show toast
			Handler(Looper.getMainLooper()).postDelayed({
				//if didn't show another toast during the TOAST_DURATION
				if (activity != null && !requireActivity().isDestroyed && !requireActivity().isFinishing && isVisible && !isDetached && isCurrentItem) {
					if (Calendar.getInstance().timeInMillis - lastToastTime!!.timeInMillis >= TOAST_DURATION) {
						binding.tvToastHint.isGoneVMS(true) //hide hint
						needShowToastHint = false
					}
				}
			}, TOAST_DURATION)
		}
	}

	fun initBottomViewMore() {
		adapterMore = MoreAdapter()
		adapterMore?.apply {
			onClick = { name: String ->
				when (name) {
					getStringForLayoutByKey("more_live_list") -> {
						closeCreateMarkView(needPlayArchive = false)
						changedMark = null
						isLastPlayerLive = isLive
						callbackLogEvent?.onLogEvent(SHOW_MARK_LIST)
						callbackCameraEvents?.onClickOpenEvents(camera)
					}

					getStringForLayoutByKey("more_live_quality") -> {
						setStateBehaviorQuality(BottomSheetBehavior.STATE_EXPANDED)
					}

					getStringForLayoutByKey("speeds_title") -> {
						setStateBehaviorSpeeds(BottomSheetBehavior.STATE_EXPANDED)
					}

					getStringForLayoutByKey("events") -> {
						setStateBehaviorEvents(BottomSheetBehavior.STATE_EXPANDED)
					}

					getStringForLayoutByKey("download_archive") -> {
						setStateBehaviorDownloadArchive(BottomSheetBehavior.STATE_EXPANDED)
					}

//					getStringForLayoutByKey("video_playback_protocol") -> {
//						setStateBehaviorVideoPlayerProtocol(BottomSheetBehavior.STATE_EXPANDED)
//					}
				}
				setStateBehaviorMore(BottomSheetBehavior.STATE_HIDDEN)
			}
		}
		binding.viewSheetBottomMore.rvMorePlayer.setHasFixedSize(true)
		binding.viewSheetBottomMore.rvMorePlayer.layoutManager =
			androidx.recyclerview.widget.LinearLayoutManager(requireContext())
		binding.viewSheetBottomMore.rvMorePlayer.adapter = adapterMore
	}

	private fun handleBottomViewMore() {
		bottomViewMore = BottomSheetBehavior.from(binding.viewSheetBottomMore.viewBottomMore)
		bottomViewMore?.addBottomSheetCallback(object: BottomSheetBehavior.BottomSheetCallback() {
			override fun onSlide(p0: View, p1: Float) {
			}

			@SuppressLint("SwitchIntDef")
			override fun onStateChanged(p0: View, p1: Int) {
				when (p1) {
					BottomSheetBehavior.STATE_HIDDEN -> {
						isPopupMoreShow = false
						binding.viewSheetBottomMore.viewBottomMore.visibility = View.INVISIBLE
						if (!isSecondPopupShow) {
							binding.viewBgForBottomSheetPlayer.isGoneVMS(true)
							if (activity != null && !requireActivity().isDestroyed &&  !requireActivity().isFinishing && isVisible && !isDetached) {
								changeStatusBarByKeyVMS(
									requireActivity(),
									KEY_SCREENSHOT_DETAIL_HIDDEN
								)
							}
						}
					}
				}
			}
		})
		binding.viewSheetBottomMore.viewBottomMore.post {
			bottomViewMore?.setState(BottomSheetBehavior.STATE_HIDDEN)
		}
	}

	private fun getQualityStream(): String {
		val currentQuality = tempQuality ?: settings.videoQuality
		val qualityResult = when {
			currentQuality == LOW && !statusStreamLowActive -> HIGH
			currentQuality == HIGH && !statusStreamHighActive -> LOW
			else -> currentQuality
		}
		return qualityResult
	}

	private fun setStateBehaviorMore(state: Int) {
		when (state) {
			BottomSheetBehavior.STATE_EXPANDED -> {
				isPopupMoreShow = true
				adapterMore?.apply {
					updateAdapter(
						hasPermissionsEvents,
						hasMarksToShow && !isCreateMarkViewShown,
						hasDownloadArchive /*&& !isArchiveRecordingShown*/, // todo check if need
						isLive,
						getQualityStream(),
						statusStreamHighActive && statusStreamLowActive,
						player.speedX,
						this@VMSPlayerFragment.state == VMSScreenState.DEFAULT
					)
				}
				binding.viewSheetBottomMore.viewBottomMore.isGoneVMS(false)
				binding.viewBgForBottomSheetPlayer.isGoneVMS(false)
				if (activity != null && !requireActivity().isDestroyed &&  !requireActivity().isFinishing && isVisible && !isDetached) {
					changeStatusBarByKeyVMS(requireActivity(), KEY_SCREENSHOT_DETAIL_EXPANDED)
				}
			}
		}
		binding.viewSheetBottomMore.viewBottomMore.post { bottomViewMore?.setState(state) }
	}

	private fun initQualityAdapter() {
		adapterQuality = null
		adapterQuality = QualityAdapter(requireContext())
		adapterQuality?.apply {
			innerQuality = getQualityStream()
			onClick = {
				countFailedLoads = 0
				if (statusStreamHighActive && statusStreamLowActive) {
					Dialogs.showSimpleAlertDialog(
						requireContext(),
						getStringForLayoutByKey("apply_video_quality_settings_confirmation"),
						okText = getStringForLayoutByKey("yes"),
						okAction = { chooseQuality(it, isTempQuality = false) },
						cancelText = getStringForLayoutByKey("no"),
						cancelAction = { chooseQuality(it, isTempQuality = true) }
					)
				} else {
					// do nothing
					//Log.d("onClickHdOrSd", "active 1 stream")
				}
				setStateBehaviorQuality(BottomSheetBehavior.STATE_HIDDEN)
			}
		}
		binding.viewSheetBottomQuality.rvQuality.setHasFixedSize(true)
		binding.viewSheetBottomQuality.rvQuality.layoutManager =
			androidx.recyclerview.widget.LinearLayoutManager(requireContext())
		binding.viewSheetBottomQuality.rvQuality.adapter = adapterQuality
	}

	private fun chooseQuality(newQuality: String, isTempQuality: Boolean) {
		val tag = if (newQuality == LOW) TAP_VIDEO_QUALITY_LOW else TAP_VIDEO_QUALITY_HIGH
		callbackLogEvent?.onLogEvent(tag)
		if (isTempQuality) {
			tempQuality = newQuality
		} else {
			tempQuality = null
			settings.videoQuality = newQuality
			callbackVideoQuality?.onSaveVideoQuality(settings.videoQuality)
		}
		getLiveStream()
		initSpeedAdapter()
	}

	private fun handleBottomViewQuality() {
		bottomViewQuality =
			BottomSheetBehavior.from(binding.viewSheetBottomQuality.viewBottomQuality)
		bottomViewQuality?.addBottomSheetCallback(object:
			BottomSheetBehavior.BottomSheetCallback() {
			override fun onSlide(p0: View, p1: Float) {
			}

			@SuppressLint("SwitchIntDef")
			override fun onStateChanged(p0: View, p1: Int) {
				when (p1) {
					BottomSheetBehavior.STATE_HIDDEN -> {
						isPopupQualityShow = false
						isSecondPopupShow = false
						binding.viewSheetBottomQuality.viewBottomQuality.visibility = View.INVISIBLE
						binding.viewBgForBottomSheetPlayer.isGoneVMS(true)
						if (activity != null && !requireActivity().isDestroyed &&  !requireActivity().isFinishing && isVisible && !isDetached) {
							changeStatusBarByKeyVMS(requireActivity(), KEY_SCREENSHOT_DETAIL_HIDDEN)
						}
					}
				}
			}
		})
		binding.viewSheetBottomQuality.viewBottomQuality.post {
			bottomViewQuality?.setState(BottomSheetBehavior.STATE_HIDDEN)
		}
	}

	private fun setStateBehaviorQuality(state: Int) {
		when (state) {
			BottomSheetBehavior.STATE_EXPANDED -> {
				isPopupQualityShow = true
				isSecondPopupShow = true
				binding.viewSheetBottomMore.viewBottomMore.post {
					bottomViewMore?.setState(BottomSheetBehavior.STATE_HIDDEN)
				}
				binding.viewSheetBottomQuality.viewBottomQuality.isGoneVMS(false)
			}
		}
		binding.viewSheetBottomQuality.viewBottomQuality.post { bottomViewQuality?.setState(state) }
	}

	private fun fetchNameVideoCodec() {
		val quality = getQualityStream() // could be only HIGH or LOW
		videoCodec = camera.streams?.find { it.type == quality }?.videoCodec ?: "h264" // could be only 264 or 265
	}

	private fun initSpeedAdapter() {
		fetchNameVideoCodec()
		adapterSpeeds = null
		adapterSpeeds = SpeedAdapter(player.speedX, videoCodec)
		adapterSpeeds?.apply {
			setList(settings.videoRates)
			onClick = { speed: Float ->
				setSpeed(speed)
				setStateBehaviorSpeeds(BottomSheetBehavior.STATE_HIDDEN)
			}
		}
		binding.viewSheetBottomList.rvBottomList.setHasFixedSize(true)
		binding.viewSheetBottomList.rvBottomList.layoutManager =
			androidx.recyclerview.widget.LinearLayoutManager(requireContext())
		binding.viewSheetBottomList.rvBottomList.adapter = adapterSpeeds
	}

	private fun handleBottomViewSpeeds() {
		bottomViewSpeeds = BottomSheetBehavior.from(binding.viewSheetBottomList.viewBottomList)
		bottomViewSpeeds?.addBottomSheetCallback(object:
			BottomSheetBehavior.BottomSheetCallback() {
			override fun onSlide(p0: View, p1: Float) {
			}

			@SuppressLint("SwitchIntDef")
			override fun onStateChanged(p0: View, p1: Int) {
				when (p1) {
					BottomSheetBehavior.STATE_HIDDEN -> {
						isPopupSpeedsShow = false
						isSecondPopupShow = false
						binding.viewSheetBottomList.viewBottomList.visibility = View.INVISIBLE
						binding.viewBgForBottomSheetPlayer.isGoneVMS(true)
						if (activity != null && !requireActivity().isDestroyed &&  !requireActivity().isFinishing && isVisible && !isDetached) {
							changeStatusBarByKeyVMS(requireActivity(), KEY_SCREENSHOT_DETAIL_HIDDEN)
						}
//                        // Log.d("onStateChanged", "$p1 = STATE_HIDDEN")
					}
				}
			}
		})
		binding.viewSheetBottomList.viewBottomList.post {
			bottomViewSpeeds?.setState(BottomSheetBehavior.STATE_HIDDEN)
		}
	}

	private fun setStateBehaviorSpeeds(state: Int) {
		callbackLogEvent?.onLogEvent(SHOW_PLAYBACK_SPEED)
		when (state) {
			BottomSheetBehavior.STATE_EXPANDED -> {
				isPopupSpeedsShow = true
				isSecondPopupShow = true
				binding.viewSheetBottomMore.viewBottomMore.post {
					bottomViewMore?.setState(BottomSheetBehavior.STATE_HIDDEN)
				}
				binding.viewSheetBottomList.viewBottomList.isGoneVMS(false)
			}
		}
		binding.viewSheetBottomList.viewBottomList.post { bottomViewSpeeds?.setState(state) }
	}

	fun initEventsTypesAdapter() {
		adapterMarks = EventsAdapter().apply {
			this@VMSPlayerFragment.callbackEventsTypes?.chosenEventsTypes?.let {
				chosenList = ArrayList(settings.chosenEventsTypes)
			}
		}
		binding.viewSheetBottomEventsPlayer.rvEventsPlayer.setHasFixedSize(true)
		binding.viewSheetBottomEventsPlayer.rvEventsPlayer.layoutManager =
			androidx.recyclerview.widget.LinearLayoutManager(requireContext())
		binding.viewSheetBottomEventsPlayer.rvEventsPlayer.adapter = adapterMarks
		binding.viewSheetBottomEventsPlayer.tvEventsContinue.setSafeOnClickListenerVMS {
			adapterMarks?.chosenList?.let { list -> settings.chosenEventsTypes = list }
			handleMarksVisibilityChange()
			if (hasMarksToShow) getMarks()
			setStateBehaviorEvents(BottomSheetBehavior.STATE_HIDDEN)
			callbackEventsTypes?.onChooseEventsTypes(ArrayList(settings.chosenEventsTypes))
		}
	}

	private fun handleBottomViewEventsTypes() {
		bottomViewMarks =
			BottomSheetBehavior.from(binding.viewSheetBottomEventsPlayer.viewBottomEventsPlayer)
		bottomViewMarks?.addBottomSheetCallback(object: BottomSheetBehavior.BottomSheetCallback() {
			override fun onSlide(p0: View, p1: Float) {
			}

			@SuppressLint("SwitchIntDef")
			override fun onStateChanged(p0: View, p1: Int) {
				when (p1) {
					BottomSheetBehavior.STATE_HIDDEN -> {
						isPopupEventsShow = false
						isSecondPopupShow = false
						binding.viewSheetBottomEventsPlayer.viewBottomEventsPlayer.visibility =
							View.INVISIBLE
						binding.viewBgForBottomSheetPlayer.isGoneVMS(true)
						if (activity != null && !requireActivity().isDestroyed &&  !requireActivity().isFinishing && isVisible && !isDetached) {
							changeStatusBarByKeyVMS(requireActivity(), KEY_SCREENSHOT_DETAIL_HIDDEN)
						}
					}
				}
			}
		})
		binding.viewSheetBottomEventsPlayer.viewBottomEventsPlayer.post {
			bottomViewMarks?.setState(BottomSheetBehavior.STATE_HIDDEN)
		}
	}

	private fun setStateBehaviorEvents(state: Int) {
		when (state) {
			BottomSheetBehavior.STATE_EXPANDED -> {
				isPopupEventsShow = true
				isSecondPopupShow = true
				binding.viewSheetBottomMore.viewBottomMore.post {
					bottomViewMore?.setState(BottomSheetBehavior.STATE_HIDDEN)
				}
				binding.viewSheetBottomEventsPlayer.viewBottomEventsPlayer.isGoneVMS(false)
			}
		}
		binding.viewSheetBottomEventsPlayer.viewBottomEventsPlayer.post {
			bottomViewMarks?.setState(state)
		}
	}

//	private fun initVideoPlayerProtocolAdapter() {
//		adapterVideoPlayerProtocol = VideoPlayerProtocolAdapter(settings.videoType).apply {
//			onClick = {
//				settings.videoType = it
//				callbackVideoType?.onSaveVideoType(settings.videoType)
//				setStateBehaviorVideoPlayerProtocol(BottomSheetBehavior.STATE_HIDDEN)
//				getLiveStream()
//			}
//		}
//		binding.viewSheetBottomVideoPlayerProtocol.rvVideoPlayerProtocol.setHasFixedSize(true)
//		binding.viewSheetBottomVideoPlayerProtocol.rvVideoPlayerProtocol.layoutManager =
//			androidx.recyclerview.widget.LinearLayoutManager(requireContext())
//		binding.viewSheetBottomVideoPlayerProtocol.rvVideoPlayerProtocol.adapter =
//			adapterVideoPlayerProtocol
//	}

//	private fun handleBottomViewVideoPlayerProtocol() {
//		bottomVideoPlayerProtocol =
//			BottomSheetBehavior.from(binding.viewSheetBottomVideoPlayerProtocol.viewBottomVideoPlayerProtocol)
//		bottomVideoPlayerProtocol?.addBottomSheetCallback(object:
//			BottomSheetBehavior.BottomSheetCallback() {
//			override fun onSlide(p0: View, p1: Float) {
//			}
//
//			@SuppressLint("SwitchIntDef")
//			override fun onStateChanged(p0: View, p1: Int) {
//				when (p1) {
//					BottomSheetBehavior.STATE_HIDDEN -> {
//						isPopupVideoPlayerProtocol = false
//						isSecondPopupShow = false
//						binding.viewSheetBottomVideoPlayerProtocol.viewBottomVideoPlayerProtocol.visibility =
//							View.INVISIBLE
//						binding.viewBgForBottomSheetPlayer.isGoneVMS(true)
//						if (activity != null && !requireActivity().isDestroyed &&  !requireActivity().isFinishing && isVisible && !isDetached) {
//							changeStatusBarByKeyVMS(requireActivity(), KEY_SCREENSHOT_DETAIL_HIDDEN)
//						}
//					}
//				}
//			}
//		})
//		binding.viewSheetBottomVideoPlayerProtocol.viewBottomVideoPlayerProtocol.post {
//			bottomVideoPlayerProtocol?.setState(BottomSheetBehavior.STATE_HIDDEN)
//		}
//	}

//	private fun setStateBehaviorVideoPlayerProtocol(state: Int) {
//		when (state) {
//			BottomSheetBehavior.STATE_EXPANDED -> {
//				isPopupVideoPlayerProtocol = true
//				isSecondPopupShow = true
//				binding.viewSheetBottomMore.viewBottomMore.post {
//					bottomVideoPlayerProtocol?.setState(BottomSheetBehavior.STATE_HIDDEN)
//				}
//				binding.viewSheetBottomVideoPlayerProtocol.viewBottomVideoPlayerProtocol.isGoneVMS(
//					false
//				)
//			}
//		}
//		binding.viewSheetBottomVideoPlayerProtocol.viewBottomVideoPlayerProtocol.post {
//			bottomVideoPlayerProtocol?.setState(state)
//		}
//	}

	private fun handleBottomViewDownloadArchive() {
		bottomViewDownloadArchive =
			BottomSheetBehavior.from(binding.viewSheetBottomDownloadArchive.viewBottomDownloadArchive)
		bottomViewDownloadArchive?.addBottomSheetCallback(object:
			BottomSheetBehavior.BottomSheetCallback() {
			override fun onSlide(p0: View, p1: Float) {
			}

			@SuppressLint("SwitchIntDef")
			override fun onStateChanged(p0: View, p1: Int) {
				when (p1) {
					BottomSheetBehavior.STATE_HIDDEN -> {
						isPopupDownloadArchiveShow = false
						isSecondPopupShow = false
						binding.viewSheetBottomDownloadArchive.viewBottomDownloadArchive.visibility =
							View.INVISIBLE
						binding.viewBgForBottomSheetPlayer.isGoneVMS(true)
						if (activity != null && !requireActivity().isDestroyed &&  !requireActivity().isFinishing && isVisible && !isDetached) {
							changeStatusBarByKeyVMS(requireActivity(), KEY_SCREENSHOT_DETAIL_HIDDEN)
						}
//                        play() // todo - it need?
					}
				}
			}
		})
		binding.viewSheetBottomDownloadArchive.viewBottomDownloadArchive.post {
			bottomViewDownloadArchive?.setState(BottomSheetBehavior.STATE_HIDDEN)
		}
	}

	private fun setStateBehaviorDownloadArchive(state: Int) {
		callbackLogEvent?.onLogEvent(SHOW_DOWNLOAD_ARCHIVE)
		when (state) {
			BottomSheetBehavior.STATE_EXPANDED -> {
				closeCreateMarkView(needPlayArchive = false)
				pause()
				val timeEndOfRange = getEndOfArchive().timeInMillis
				currentDateCursor?.let {
					calendarStartPeriod =
						Calendar.getInstance().apply { timeInMillis = it.timeInMillis }
				}
				calendarEndPeriod = Calendar.getInstance()
					.apply { timeInMillis = calendarStartPeriod!!.timeInMillis + TEN_MIN }
				logSdk(TAG, "time2 = ${calendarEndPeriod?.time}")
				if (calendarEndPeriod!!.timeInMillis > timeEndOfRange) {
					calendarEndPeriod =
						Calendar.getInstance().apply { timeInMillis = timeEndOfRange }
					logSdk(TAG, "time3 = ${calendarEndPeriod?.time}")
				}

				binding.viewSheetBottomDownloadArchive.tvStartPeriodFull.text =
					getDateFormatPeriod(calendarStartPeriod!!)
				binding.viewSheetBottomDownloadArchive.tvEndPeriodFull.text =
					getDateFormatPeriod(calendarEndPeriod!!)

				isPopupDownloadArchiveShow = true
				isSecondPopupShow = true
				binding.viewSheetBottomMore.viewBottomMore.post {
					bottomViewMore?.setState(BottomSheetBehavior.STATE_HIDDEN)
				}
				binding.viewSheetBottomDownloadArchive.viewBottomDownloadArchive.isGoneVMS(false)
			}
		}
		binding.viewSheetBottomDownloadArchive.viewBottomDownloadArchive.post {
			bottomViewDownloadArchive?.setState(state)
		}
	}

	private fun handleMarksVisibilityChange() {
		if (!isLive) {
			val marksIsNotVisible = !settings.hasEventsToShow()
			showHideNearestEventButtons(marksIsNotVisible)
			if (marksIsNotVisible) binding.markHint.isGoneVMS(true)
		}
	}

	private fun handleBottomViewDatePicker() {
		bottomViewDatePicker =
			BottomSheetBehavior.from(binding.viewSheetBottomDatePicker.viewBottomDatePicker)
		bottomViewDatePicker?.addBottomSheetCallback(object:
			BottomSheetBehavior.BottomSheetCallback() {
			override fun onSlide(p0: View, p1: Float) {
			}

			@SuppressLint("SwitchIntDef")
			override fun onStateChanged(p0: View, p1: Int) {
				when (p1) {
					BottomSheetBehavior.STATE_HIDDEN -> {
						isPopupDatePickerShow = false
						binding.viewSheetBottomDatePicker.viewBottomDatePicker.visibility =
							View.INVISIBLE
						if (!isPopupDownloadArchiveShow && !isPopupTimePickerShow && activity != null && !requireActivity().isDestroyed &&  !requireActivity().isFinishing && isVisible && !isDetached) {
							binding.viewBgForBottomSheetPlayer.isGoneVMS(true)
							changeStatusBarByKeyVMS(requireActivity(), KEY_SCREENSHOT_DETAIL_HIDDEN)
						}
					}
				}
			}
		})
		binding.viewSheetBottomDatePicker.viewBottomDatePicker.post {
			bottomViewDatePicker?.setState(BottomSheetBehavior.STATE_HIDDEN)
		}
	}

	private fun setStateBehaviorDatePicker(state: Int, isShowPeriod: Boolean = false) {
		when (state) {
			BottomSheetBehavior.STATE_EXPANDED -> {
				binding.viewSheetBottomDatePicker.tvTimeTitle.isGoneVMS(!isShowPeriod)
				binding.viewSheetBottomDatePicker.tvTimePeriod.isGoneVMS(!isShowPeriod)

				binding.viewSheetBottomDatePicker.tvTimeTitle.text =
					if (statusTimePickerDialog == STATUS_TIME_END_PERIOD) {
						getStringForLayoutByKey("download_archive_end")
					} else {
						getStringForLayoutByKey("download_archive_start")
					}

				if (isShowPeriod) {
					binding.viewSheetBottomDatePicker.tvTimePeriod.text = getOnlyTime(
						if (statusTimePickerDialog == STATUS_TIME_START_PERIOD) calendarStartPeriod!!
						else calendarEndPeriod!!
					)
				}
				logSdk(TAG, "isShowPeriod = $isShowPeriod")
				isPopupDatePickerShow = true
				setDatePicker()
				binding.viewSheetBottomDatePicker.viewBottomDatePicker.isGoneVMS(false)
				if (activity != null && !requireActivity().isDestroyed &&  !requireActivity().isFinishing && isVisible && !isDetached) {
					if (!isPopupDownloadArchiveShow) {
						binding.viewBgForBottomSheetPlayer.isGoneVMS(false)
						changeStatusBarByKeyVMS(requireActivity(), KEY_SCREENSHOT_DETAIL_EXPANDED)
					}
					//if land orientation -> scroll date picker view to bottom to correct display
					if (!isPortrait(requireContext())) binding.viewSheetBottomDatePicker.scrDatePicker.post {
						binding.viewSheetBottomDatePicker.scrDatePicker.fullScroll(View.FOCUS_DOWN)
					}
				}
			}
		}
		binding.viewSheetBottomDatePicker.viewBottomDatePicker.post {
			bottomViewDatePicker?.setState(state)
		}
	}

	private fun handleBottomViewTimePicker() {
		bottomViewTimePicker =
			BottomSheetBehavior.from(binding.viewSheetBottomTimePicker.viewBottomTimePicker)
		bottomViewTimePicker?.addBottomSheetCallback(object:
			BottomSheetBehavior.BottomSheetCallback() {
			override fun onSlide(p0: View, p1: Float) {
			}

			@SuppressLint("SwitchIntDef")
			override fun onStateChanged(p0: View, p1: Int) {
				when (p1) {
					BottomSheetBehavior.STATE_HIDDEN -> {
						isPopupTimePickerShow = false
						binding.viewSheetBottomTimePicker.viewBottomTimePicker.visibility =
							View.INVISIBLE
						if (activity != null && !requireActivity().isDestroyed &&  !requireActivity().isFinishing && isVisible && !isDetached) {
							binding.viewBgForBottomSheetTimePicker.isGoneVMS(true)
							if (isPopupDownloadArchiveShow) binding.viewBgForBottomSheetPlayer.isGoneVMS(
								false
							)
							if (!isPopupDownloadArchiveShow && !isBackPressedFromTimePicker || isCreateMarkViewShown) {
								changeStatusBarByKeyVMS(
									requireActivity(),
									KEY_SCREENSHOT_DETAIL_HIDDEN
								)
							}
						}
					}
				}
			}
		})
		binding.viewSheetBottomTimePicker.viewBottomTimePicker.post {
			bottomViewTimePicker?.setState(BottomSheetBehavior.STATE_HIDDEN)
		}
	}

	private fun setStateBehaviorTimePicker(state: Int, isOnBackPressed: Boolean = false) {
		isBackPressedFromTimePicker = isOnBackPressed
		when (state) {
			BottomSheetBehavior.STATE_EXPANDED -> {
				isPopupTimePickerShow = true
				setMinMaxTimePicker(needSetValue = true)
				binding.viewSheetBottomTimePicker.viewBottomTimePicker.isGoneVMS(false)
				binding.viewBgForBottomSheetPlayer.isGoneVMS(true)
				binding.viewBgForBottomSheetTimePicker.isGoneVMS(false) //for show above date picker
				if (isCreateMarkViewShown && activity != null && !requireActivity().isDestroyed &&  !requireActivity().isFinishing && isVisible && !isDetached) {
					changeStatusBarByKeyVMS(requireActivity(), KEY_SCREENSHOT_DETAIL_EXPANDED)
				}
			}

			BottomSheetBehavior.STATE_HIDDEN -> {
				binding.viewBgForBottomSheetTimePicker.isGoneVMS(true)
				if (isPopupDownloadArchiveShow) binding.viewBgForBottomSheetPlayer.isGoneVMS(false)
			}
		}
		binding.viewSheetBottomTimePicker.viewBottomTimePicker.post {
			bottomViewTimePicker?.setState(state)
		}
	}

	private fun setDatePicker() {
		val startDate = setCalendarByDateServer(camera.getStartAtLocal())
		val endDate = getEndOfArchive()
		binding.viewSheetBottomDatePicker.datePicker.maxDate = endDate.timeInMillis
		binding.viewSheetBottomDatePicker.datePicker.date = (when (statusTimePickerDialog) {
			STATUS_TIME_START_PERIOD -> calendarStartPeriod!!
			STATUS_TIME_END_PERIOD -> calendarEndPeriod!!
			else -> currentDateCursor!!
		}).timeInMillis
		binding.viewSheetBottomDatePicker.datePicker.minDate =
			startDate.timeInMillis   //set min date after all, otherwise -> bug and show 1900 year
	}

	private fun setMinMaxTimePicker(needSetValue: Boolean) {
		var minH = 0
		var maxH = 23
		var minM = 0
		var maxM = 59
		var minS = 0
		var maxS = 59
		val startDate = setCalendarByDateServer(camera.getStartAtLocal())
		val lastDate = getEndOfArchive()
		// if mark create view shown -> get chosen date from currentDateCursor (we just set time),
		// else -> get chosen date from date picker (we set date before time and must use it)
		val chosenDate = setCalendarServer(
			if (isCreateMarkViewShown) {
				if (currentDateCursor == null || currentDateCursor!!.after(lastDate)) {
					lastDate
				} else {
					currentDateCursor!!
				}
			} else if (statusTimePickerDialog.isEmpty()) {
				dateFromDatePicker
			} else if (statusTimePickerDialog == STATUS_TIME_END_PERIOD) {
				calendarEndPeriod!!
			} else {
				calendarStartPeriod!!
			}
		)
		// for check min day - compare chosen date and start camera date
		val isMinDay = compareDatesServerWithoutTime(chosenDate, camera.getStartAtLocal())
		// for check max day - compare chosen date and last available archive date
		val isMaxDay = compareDatesServerWithoutTime(chosenDate, setCalendarServer(lastDate))
		val compareH =
			if (needSetValue) setCalendarByDateServer(chosenDate).get(Calendar.HOUR_OF_DAY)
			else binding.viewSheetBottomTimePicker.timeHh.value
		val compareM = if (needSetValue) setCalendarByDateServer(chosenDate).get(Calendar.MINUTE)
		else binding.viewSheetBottomTimePicker.timeMm.value
		if (isMinDay) {
			minH = startDate.get(Calendar.HOUR_OF_DAY)
			if (minH == compareH) {
				minM = startDate.get(Calendar.MINUTE)
				if (minM == compareM) {
					minS = startDate.get(Calendar.SECOND)
				}
			}
		}
		if (isMaxDay) {
			maxH = lastDate.get(Calendar.HOUR_OF_DAY)
			if (maxH == compareH) {
				maxM = lastDate.get(Calendar.MINUTE)
				if (maxM == compareM) {
					maxS = lastDate.get(Calendar.SECOND)
				}
			}
		}
		binding.viewSheetBottomTimePicker.timeHh.minValue = minH
		binding.viewSheetBottomTimePicker.timeHh.maxValue = maxH
		binding.viewSheetBottomTimePicker.timeMm.minValue = minM
		binding.viewSheetBottomTimePicker.timeMm.maxValue = maxM
		binding.viewSheetBottomTimePicker.timeSs.minValue = minS
		binding.viewSheetBottomTimePicker.timeSs.maxValue = maxS

		if (needSetValue) {
			//set new value only when open time picker
			val calendar = setCalendarByDateServer(chosenDate)
			// use plus(1) and than programmatically scroll back for correct displaying numbers with one digit
			binding.viewSheetBottomTimePicker.timeHh.value =
				calendar.get(Calendar.HOUR_OF_DAY).plus(1)
			binding.viewSheetBottomTimePicker.timeMm.value = calendar.get(Calendar.MINUTE).plus(1)
			binding.viewSheetBottomTimePicker.timeSs.value = calendar.get(Calendar.SECOND).plus(1)
			simulateScroll(binding.viewSheetBottomTimePicker.timeHh)
			simulateScroll(binding.viewSheetBottomTimePicker.timeMm)
			simulateScroll(binding.viewSheetBottomTimePicker.timeSs)
		}
	}

	private fun simulateScroll(picker: NumberPicker) {
		val downEvent =
			MotionEvent.obtain(SystemClock.uptimeMillis(), 1, MotionEvent.ACTION_DOWN, 0f, 1f, 0)
		picker.dispatchTouchEvent(downEvent)

		val moveEvent =
			MotionEvent.obtain(SystemClock.uptimeMillis(), 1, MotionEvent.ACTION_MOVE, 0f, 0f, 0)
		picker.dispatchTouchEvent(moveEvent)

		val upEvent =
			MotionEvent.obtain(SystemClock.uptimeMillis(), 1, MotionEvent.ACTION_UP, 0f, 0f, 0)
		picker.dispatchTouchEvent(upEvent)
	}

	private fun setDatePickerListener() {
		//save pick date to date picker
		binding.viewSheetBottomDatePicker.datePicker.setOnDateChangeListener { view, year, month, dayOfMonth ->
			val calendar = Calendar.getInstance()
			calendar.set(Calendar.YEAR, year)
			calendar.set(Calendar.MONTH, month)
			calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
			view.date = calendar.timeInMillis
			if (statusTimePickerDialog.isNotEmpty()) {
				dateFromDatePicker.timeInMillis = binding.viewSheetBottomDatePicker.datePicker.date
				setCalendarByDatePicker(if (statusTimePickerDialog == STATUS_TIME_START_PERIOD) calendarStartPeriod!! else calendarEndPeriod!!)
				checkStartEndArchiveTime()
			}
		}
	}

	private fun checkStartEndArchiveTime() {
		if (statusTimePickerDialog == STATUS_TIME_START_PERIOD) {
			if (calendarStartPeriod!!.after(getEndOfArchive())) {
				calendarStartPeriod!!.timeInMillis = getEndOfArchive().timeInMillis
			} else if (setCalendarByDateServer(camera.getStartAtLocal()).after(calendarStartPeriod!!)) {
				calendarStartPeriod!!.timeInMillis =
					setCalendarByDateServer(camera.getStartAtLocal()).timeInMillis
			}
		} else {
			if (calendarEndPeriod!!.after(getEndOfArchive())) {
				calendarEndPeriod!!.timeInMillis = getEndOfArchive().timeInMillis
			} else if (setCalendarByDateServer(camera.getStartAtLocal()).after(calendarEndPeriod!!)) {
				calendarEndPeriod!!.timeInMillis =
					setCalendarByDateServer(camera.getStartAtLocal()).timeInMillis
			}
		}
		binding.viewSheetBottomDatePicker.tvTimePeriod.text = getOnlyTime(
			if (statusTimePickerDialog == STATUS_TIME_START_PERIOD) calendarStartPeriod!!
			else calendarEndPeriod!!
		)
	}

	private fun setTimePickerListeners() {
		//set double digit format number (i.e. 0 -> 00)
		binding.viewSheetBottomTimePicker.timeHh.setFormatter { value ->
			return@setFormatter String.format("%02d", value)
		}
		binding.viewSheetBottomTimePicker.timeMm.setFormatter { value ->
			return@setFormatter String.format("%02d", value)
		}
		binding.viewSheetBottomTimePicker.timeSs.setFormatter { value ->
			return@setFormatter String.format("%02d", value)
		}
		//listeners for display correct max min time on pickers
		binding.viewSheetBottomTimePicker.timeHh.setOnValueChangedListener { _, _, _ ->
			setMinMaxTimePicker(needSetValue = false)
		}
		binding.viewSheetBottomTimePicker.timeMm.setOnValueChangedListener { _, _, _ ->
			setMinMaxTimePicker(needSetValue = false)
		}
		// don't need listener for seconds, because it doesn't affect anything
	}

	private fun setCalendarByDatePicker(calendar: Calendar) {
		calendar.set(Calendar.YEAR, dateFromDatePicker.get(Calendar.YEAR))
		calendar.set(Calendar.MONTH, dateFromDatePicker.get(Calendar.MONTH))
		calendar.set(Calendar.DAY_OF_MONTH, dateFromDatePicker.get(Calendar.DAY_OF_MONTH))
		logSdk(TAG, "set calendar = ${calendar.time} ")
	}

	override fun goToNearestMark(mark: VMSEvent?, direction: String) {
		if (mark == null || setCalendarByDateServer(camera.getStartAtLocal()).after(
				setCalendarByDateServer(mark.getFromLocal())
			)
		) {
			//revert params after long click
			val layoutParams = CoordinatorLayout.LayoutParams(
				CoordinatorLayout.LayoutParams.WRAP_CONTENT,
				CoordinatorLayout.LayoutParams.WRAP_CONTENT
			).apply {
				gravity = (Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
				marginStart = 0
				marginEnd = 0
				bottomMargin = hintMarginBottom
			}
			binding.markHint.layoutParams = layoutParams

			//show error message
			binding.markHintTitle.text =
				getStringForLayoutByKey(if (mark != null) "older_marks_not_available" else if (direction == DIRECTION_PREVIOUS) "no_older_marks" else "no_newer_marks")
			absentMarks = true
			binding.markHintTime.isGoneVMS(true)
			binding.markHint.alpha =
				ALPHA_FULL   //set alpha because it can be set to 0f in isGoneSmooth
			binding.markHint.isGoneVMS(false)    //show hint
			needShowMarkHint = true
			lastClickedTimeOnMark = Calendar.getInstance()    //save time when show mark
			Handler(Looper.getMainLooper()).postDelayed({
				//if haven't another clicks on mark before || haven't another clicks on marks during the TIME_FOR_SHOW_MARK_HINT
				if (isVisible) {
					if (lastClickedTimeOnMark == null || Calendar.getInstance().timeInMillis - lastClickedTimeOnMark!!.timeInMillis >= TOAST_DURATION) {
						binding.markHint.isGoneVMS(true) //hide hint
						needShowMarkHint = false
					}
				}
			}, TOAST_DURATION)
		} else {
			absentMarks = false
			clickMark(mark) //navigate to mark

			// set date and init to zoom each mark from cluster
			binding.timeLine.markFrom = mark.getFromLocal()
			binding.timeLine.init(
				this,
				camera,
				archiveRanges,
				presenter.compositeDisposable,
				getEndOfArchive(0)
			)
		}
	}

	override fun needDisableButtons(isNeed: Boolean) {
		val listener = if (isNeed)
			null
		else
			this
		binding.ivScreenshotLive.setOnClickListener(listener)
		binding.viewTopBarNavigation.frLive.setOnClickListener(listener)
		binding.ivSound.setOnClickListener(listener)
		binding.viewBottomBarNavigation.ivSoundArchive.setOnClickListener(listener)
		binding.viewTopBarNavigation.frArchive.setOnClickListener(listener)
		binding.viewBottomBarNavigation.imgCalendar.setOnClickListener(listener)
		binding.viewSheetBottomDownloadArchive.tvStartPeriodFull.setOnClickListener(listener)
		binding.viewSheetBottomDownloadArchive.tvEndPeriodFull.setOnClickListener(listener)
		binding.viewSheetBottomDatePicker.tvTimePeriod.setOnClickListener(listener)
		binding.viewSheetBottomDownloadArchive.btDownloadArchive.setOnClickListener(listener)
		binding.viewBottomBarNavigation.moveLeftOneDay.setOnClickListener(listener)
		binding.viewBottomBarNavigation.moveLeftOneHour.setOnClickListener(listener)
		binding.viewBottomBarNavigation.moveLeftOneMinute.setOnClickListener(listener)
		binding.viewBottomBarNavigation.moveLeftFiveSeconds.setOnClickListener(listener)
		binding.viewBottomBarNavigation.moveRightOneDay.setOnClickListener(listener)
		binding.viewBottomBarNavigation.moveRightOneHour.setOnClickListener(listener)
		binding.viewBottomBarNavigation.moveRightOneMinute.setOnClickListener(listener)
		binding.viewBottomBarNavigation.moveRightFiveSeconds.setOnClickListener(listener)
		binding.ivMoreLive.setOnClickListener(listener)
		binding.viewBottomBarNavigation.ivMoreArchive.setOnClickListener(listener)
		binding.viewPtz.ivPtzUp.setOnClickListener(listener)
		binding.viewPtz.ivPtzDown.setOnClickListener(listener)
		binding.viewPtz.ivPtzRight.setOnClickListener(listener)
		binding.viewPtz.ivPtzLeft.setOnClickListener(listener)
		binding.viewPtz.ivPtzZoomIn.setOnClickListener(listener)
		binding.viewPtz.ivPtzZoomOut.setOnClickListener(listener)
		binding.viewPtz.ivPtzReset.setOnClickListener(listener)
		binding.ivPtz.setOnClickListener(listener)
		binding.viewBottomBarNavigation.imgPlay.setOnClickListener(listener)
		binding.viewBottomBarNavigation.moveLeft.setOnClickListener(listener)
		binding.viewBottomBarNavigation.moveRight.setOnClickListener(listener)
		binding.viewBottomBarNavigation.ivScreenshot.setOnClickListener(listener)
		binding.leftButtonFrame.setOnClickListener(listener)
		binding.rightButtonFrame.setOnClickListener(listener)

		binding.leftButtonFrame.alpha = if (isNeed) ALPHA_HALF else ALPHA_FULL
		binding.rightButtonFrame.alpha = if (isNeed) ALPHA_HALF else ALPHA_FULL
		binding.viewBottomBarNavigation.constrBottomBarNav.alpha =
			if (isNeed) ALPHA_HALF else ALPHA_FULL
		binding.viewBottomBarNavigation.constrBottomBarNav.isClickable = !isNeed
		binding.viewBottomBarNavigation.constrBottomBarNav.isEnabled = !isNeed
	}

	override fun handleErrorArchive(errorText: String?) {
		if (isVisible) {
			if (camera.userStatus != USER_STATUS_BLOCKED) {
				showToast(errorText ?: getStringForLayoutByKey("archive_inactive"))
				errorText?.let { stopPlayerInTheEnd() }
			}
			if (camera.userStatus != USER_STATUS_INACTIVE && camera.isRestrictedLive != true) onShowLive()
		}
	}

	override fun delayMoveCamera() {
		Handler(Looper.getMainLooper()).postDelayed({ showOrHideProgressBar(false) }, 4000)
	}

	override fun loadLive(url: String?) {
		if (!isVisible || !isLive) return
		if (url == null) {
			showEmptyScreen(needShow = true)
		} else {
			showEmptyScreen(needShow = false)
			checkAllStreams()
			player.apply {
				actionStateChanged = { it, it2 -> onPlayerStateChanged(it, it2) }
				videoUrl = url
//				videoUrl = "rtsp://192.168.200.31:8554/live/camera-uid5065686-primary/"
//				videoUrl = "rtsp://admin:netland123@192.168.7.41:554/Streaming/Channels/101/"
				try {
					initPlayer(Surface(getCurrentFragment()!!.binding.vmsPlayerView.surfaceTexture))
				} catch (e: Exception) {
					// surfaceTexture can be null somehow
				}
			}
			logSdk(TAG, "passUrl videoUrl = ${player.videoUrl}")
//			player.startPlayer()
			binding.ivScreenshotLive.isGoneVMS(false)
		}
	}

	private fun handlePlayerWidthHeight(isPortrait: Boolean) {
		player.height.let { h ->
			player.width.let { w ->
				val width: Int
				val newHeight: Int
				// todo - later need handle size video
				if (isPortrait) {
					width = requireActivity().getWindowWidth()
					newHeight = width * h / w
				} else {
					newHeight = requireActivity().getWindowHeight()
					width = newHeight * w / h
				}
				getCurrentFragment()?.binding?.vmsPlayerView?.layoutParams =
					FrameLayout.LayoutParams(width, newHeight)
				if (!binding.viewEmptyScreenSdk.emptyScreen.isVisible)
					getCurrentFragment()?.binding?.vmsPlayerView?.isGoneVMS(false)
			}
		}
	}

	private fun handleLastElementWidthHeight(isPortrait: Boolean) {
		val metrics = DisplayMetrics()
		requireActivity().windowManager.defaultDisplay.getMetrics(metrics)
		lastFragment?.binding?.vmsPlayerView?.layoutParams?.height?.let { h ->
			lastFragment?.binding?.vmsPlayerView?.layoutParams?.width?.let { w ->
				val width: Int
				val newHeight: Int
				if (isPortrait) {
					width = metrics.widthPixels
					newHeight = width * h / w
				} else {
					newHeight = metrics.heightPixels
					width = newHeight * w / h
				}
				lastFragment?.binding?.vmsPlayerView?.layoutParams =
					FrameLayout.LayoutParams(width, newHeight)
			}
		}
	}

	private fun resetCurrentElementZoom() {
		if (activity != null && !requireActivity().isDestroyed &&  !requireActivity().isFinishing && isVisible && !isDetached) {
			getCurrentFragment()?.binding?.vmsPlayerView?.resetZoom()
		}
	}

	private fun handleLinearOrientation(isPortrait: Boolean) {
		val params =
			binding.viewTopBarNavigation.frBottomBar.layoutParams as LinearLayout.LayoutParams
		val clockParam =
			binding.viewTopBarNavigation.tvTimeTopBarArchive.layoutParams as LinearLayout.LayoutParams
		val clockLiveParam =
			binding.viewTopBarNavigation.tvTimeTopBarLive.layoutParams as LinearLayout.LayoutParams
		val txtParam =
			binding.viewTopBarNavigation.txtArchive.layoutParams as FrameLayout.LayoutParams
		val txtLiveParam =
			binding.viewTopBarNavigation.txtLive.layoutParams as FrameLayout.LayoutParams
		// Log.d("isPortrait", isPortrait.toString())
		if (activity != null && !requireActivity().isDestroyed &&  !requireActivity().isFinishing && isVisible && !isDetached) {
			binding.viewTopBarNavigation.lnrTopBar.orientation = if (isPortrait) {
				requireActivity().window?.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
				requireActivity().window?.decorView?.systemUiVisibility = 0

				params.height = 32.toPx()
				params.marginStart = 44.toPx()
				binding.viewTopBarNavigation.frBottomBar.layoutParams = params
				binding.viewTopBarNavigation.txtCameraNameTopBar.layoutParams.width =
					ViewGroup.LayoutParams.WRAP_CONTENT

				clockParam.gravity = Gravity.TOP
				binding.viewTopBarNavigation.tvTimeTopBarArchive.layoutParams = clockParam
				clockLiveParam.gravity = Gravity.TOP
				binding.viewTopBarNavigation.tvTimeTopBarLive.layoutParams = clockLiveParam
				txtParam.gravity = (Gravity.TOP or Gravity.CENTER_HORIZONTAL)
				binding.viewTopBarNavigation.txtArchive.layoutParams = txtParam
				txtLiveParam.gravity = (Gravity.TOP or Gravity.CENTER_HORIZONTAL)
				binding.viewTopBarNavigation.txtLive.layoutParams = txtLiveParam

				LinearLayout.VERTICAL
			} else {
				requireActivity().window?.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
				requireActivity().window?.decorView?.systemUiVisibility =
					View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
				//handler for delay after change configuration
				Handler(Looper.getMainLooper()).postDelayed({
					binding.viewBottomBarNavigation.moveLeftOneDay.measure(0, 0)
					binding.viewBottomBarNavigation.moveLeftOneHour.measure(0, 0)
					binding.viewBottomBarNavigation.moveLeftOneMinute.measure(0, 0)
					binding.viewBottomBarNavigation.moveLeftFiveSeconds.measure(0, 0)
					//find width of 4 time buttons in px
					val timeButtonsPx =
						binding.viewBottomBarNavigation.moveLeftOneDay.measuredWidth + binding.viewBottomBarNavigation.moveLeftOneHour.measuredWidth + binding.viewBottomBarNavigation.moveLeftOneMinute.measuredWidth + binding.viewBottomBarNavigation.moveLeftFiveSeconds.measuredWidth
					//width of click buttons
					val clickButtonsPx = 344.toPx()
					// find width of the screen required to display all buttons
					val needWidthAllPx = clickButtonsPx + 2 * timeButtonsPx
					//find width of the screen required to display only 3 buttons (without +/-1 minute)
					val needWidthCutPx =
						needWidthAllPx - 2 * binding.viewBottomBarNavigation.moveLeftOneMinute.measuredWidth
					//reserve width - buttons paddings in constraint layout can compressed
					val reservePx = 2 * 2 * 8.toPx()
					if (binding.playerFragment.measuredWidth < needWidthAllPx) {    //if can't display all buttons
						binding.viewBottomBarNavigation.moveLeftOneMinute.isGoneVMS(true)  //hide -1 minute
						binding.viewBottomBarNavigation.moveRightOneMinute.isGoneVMS(true)  //hide +1 minute
						if (binding.playerFragment.measuredWidth < (needWidthCutPx - reservePx)) {  //if can't display even 3 buttons
							binding.viewBottomBarNavigation.moveLeftFiveSeconds.isGoneVMS(true)  //hide -5 seconds
							binding.viewBottomBarNavigation.moveRightFiveSeconds.isGoneVMS(true)  //hide -5 seconds
						}
					}
				}, 100)

				params.height = 44.toPx()
				params.marginStart = 0
				binding.viewTopBarNavigation.frBottomBar.layoutParams = params
				val width =
					Paint().measureText(binding.viewTopBarNavigation.txtCameraNameTopBar.text.toString())
				if (width <= 220) binding.viewTopBarNavigation.txtCameraNameTopBar.layoutParams.width =
					ViewGroup.LayoutParams.WRAP_CONTENT
				else binding.viewTopBarNavigation.txtCameraNameTopBar.layoutParams.width =
					220.toPx()

				clockParam.gravity = Gravity.CENTER_VERTICAL
				binding.viewTopBarNavigation.tvTimeTopBarArchive.layoutParams = clockParam
				clockLiveParam.gravity = Gravity.CENTER_VERTICAL
				binding.viewTopBarNavigation.tvTimeTopBarLive.layoutParams = clockLiveParam
				txtParam.gravity = Gravity.CENTER
				binding.viewTopBarNavigation.txtArchive.layoutParams = txtParam
				txtLiveParam.gravity = Gravity.CENTER
				binding.viewTopBarNavigation.txtLive.layoutParams = txtLiveParam

				LinearLayout.HORIZONTAL
			}
			if (binding.viewSheetBottomList.viewBottomList.isVisible || binding.viewSheetBottomQuality.viewBottomQuality.isVisible
				|| binding.viewSheetBottomMore.viewBottomMore.isVisible || binding.viewSheetBottomEventsPlayer.viewBottomEventsPlayer.isVisible
				|| binding.viewSheetBottomDatePicker.viewBottomDatePicker.isVisible || binding.viewSheetBottomTimePicker.viewBottomTimePicker.isVisible
			) {
				changeStatusBarByKeyVMS(requireActivity(), KEY_SCREENSHOT_DETAIL_EXPANDED)
			} else {
				changeStatusBarByKeyVMS(requireActivity(), KEY_SCREENSHOT_DETAIL_HIDDEN)
			}

			if (isCreateMarkViewShown && currentDateCursor != null) {
				savedMarkName =
					binding.etMarkTitle.editableText.toString()    //save current mark name
				closeCreateMarkView(needPlayArchive = false)
				onLongClick(
					requireActivity().getWindowWidth().toFloat() / 2,
					currentDateCursor!!,
					isConfigChange = true
				)
			}
		}
	}

	private fun showHideBottomButtons() {
		val isPortrait = isPortrait(requireContext())
		binding.viewBottomBarNavigation.lnrBottomButtonsLeft.isGoneVMS(isPortrait)
		binding.viewBottomBarNavigation.lnrBottomButtonsRight.isGoneVMS(isPortrait)
	}

	private fun toLive() {
		if (isLive) {
			binding.viewTopBarNavigation.frLive.isEnabled = false
			binding.viewTopBarNavigation.frArchive.isEnabled = true

			setSpeed(speed_X_1)
			binding.viewBottomBarNavigation.moveRight.isEnabled = false
			binding.viewBottomBarNavigation.moveRight.alpha = ALPHA_HALF
			if (camera.userStatus == USER_STATUS_BLOCKED
				|| camera.status == INACTIVE || camera.status == EMPTY || camera.status == INITIAL
				|| (!statusStreamHighActive && !statusStreamLowActive) || camera.isRestrictedLive == true
			) {
				disableArchiveIfNeed()
				showEmptyScreen(
					needShow = true,
					isLocked = camera.userStatus == USER_STATUS_BLOCKED
				)
			} else {
				val id = if (cameraId != 0) cameraId.toString() else STREAM_ID // for test
				val typeCurrent =
					if (statusStreamHighActive && settings.videoQuality.equals(HIGH, true))
						HIGH
					else
						LOW
				presenter.getLiveStream(id, typeCurrent, requireContext())
			}
		}
	}

	private fun onClickMoveLeftOrRight(isLeft: Boolean, time: Long, mess: String) {
		try {
			val current = getEndOfArchive()
			val beginningOfArchive = setCalendarByDateServer(camera.getStartAtLocal())
			if (currentDateCursor != null) {
				currentDateCursor!!.timeInMillis = currentDateCursor!!.timeInMillis + time
				currentDateCursor?.timeInMillis =
					if (isLeft) {
						if (beginningOfArchive.timeInMillis > currentDateCursor!!.timeInMillis) {
							beginningOfArchive.timeInMillis + 1000 // for secure to avoid stuck
							return
						} else {
							currentDateCursor!!.timeInMillis
						}
					} else {
						if (current.timeInMillis < currentDateCursor!!.timeInMillis) {
							current.timeInMillis - 1000
							return
						} else {
							currentDateCursor!!.timeInMillis
						}
					}
				currentDateCursor?.let {
					enableOrDisableLeftButtons(isEnabled = false)
					enableOrDisableRightButtons(isEnabled = false)
					getArchive(currentDateCursor!!)
				}
			}
			val tag = when (time) {
				MOVE_LEFT_ONE_DAY -> TAP_PLAYBACK_MINUS_24H
				MOVE_LEFT_ONE_HOUR -> TAP_PLAYBACK_MINUS_1h
				MOVE_LEFT_ONE_MINUTE -> TAP_PLAYBACK_MINUS_1M
				MOVE_LEFT_FIVE_SECONDS -> TAP_PLAYBACK_MINUS_5S
				MOVE_RIGHT_ONE_DAY -> TAP_PLAYBACK_PLUS_24H
				MOVE_RIGHT_ONE_HOUR -> TAP_PLAYBACK_PLUS_1h
				MOVE_RIGHT_ONE_MINUTE -> TAP_PLAYBACK_PLUS_1M
				else -> TAP_PLAYBACK_PLUS_5S
			}
			callbackLogEvent?.onLogEvent(tag)
			showToast(mess)
		} catch (e: Exception) {
			e.message
		}
	}

	fun onShowLive() {
		callbackLogEvent?.onLogEvent(SHOW_STREAM_LIFE)
		isLive = true
		changedMark = null
		closeCreateMarkView(needPlayArchive = false)
		showEmptyScreen(needShow = false)
		countFailedLoads = 0
		stopTimeArchive(isShowTime = false)
		showLiveArchiveUI()
		hideTimeLine()
		toLive()
	}

	override fun playCameraArchive() {
		checkAllStreams() //check streams when swipe between archives (need if error occur)
		if (statusStreamHighActive || archiveRanges.isNotEmpty()) {
			isLive = false
			setDateAfterInactiveRange()
			currentDateCursor?.let {
				isPlayerStateEnd = false
				getArchive(currentDateCursor!!)
			}
		} else {
			showEmptyScreen(needShow = true)
		}
	}

	fun onShowArchive() {
		callbackLogEvent?.onLogEvent(SHOW_STREAM_ARCHIVE)
		isLive = false
		showEmptyScreen(needShow = false)
		countFailedLoads = 0
		stopTimeLive()
		showLiveArchiveUI()
		getArchiveTimeClient()
	}

	private fun showLiveArchiveUI() {
		binding.lnrBottomActions.isGoneVMS(!isLive)
		binding.viewPtz.mainPtz.isGoneVMS(true)
		binding.ivSound.isGoneVMS(true)
		binding.viewTopBarNavigation.viewLineLive.isGoneVMS(!isLive)
		binding.viewTopBarNavigation.viewLineArchive.isGoneVMS(isLive)
		binding.viewTopBarNavigation.tvTimeTopBarLive.isGoneVMS(!isLive)
		binding.viewTopBarNavigation.tvTimeTopBarArchive.isGoneVMS(isLive)
		binding.viewTopBarNavigation.txtLive.apply {
			setTypeface(null, if (!isLive) Typeface.NORMAL else Typeface.BOLD)
		}
		binding.viewTopBarNavigation.txtArchive.apply {
			setTypeface(null, if (isLive) Typeface.NORMAL else Typeface.BOLD)
		}
		binding.viewTopBarNavigation.txtArchive.setTextColorCompatVMS(
			requireContext(),
			if (isLive) R.color.white else R.color.yellow
		)

		binding.viewTopBarNavigation.frLive.isEnabled = !isLive
		binding.viewTopBarNavigation.frArchive.isEnabled = isLive
	}

	private fun getArchiveTimeClient() {
		checkAllStreams()
		if ((statusStreamHighActive && camera.isRestrictedArchive != true) || archiveRanges.isNotEmpty()) {
			currentDateCursor =
				getEndOfArchive(if (isMoveRight) 1 else 60 * 10) // when show end of archive -10min
			setDateAfterInactiveRange()
			getArchive(currentDateCursor!!)
			isMoveRight = false
		} else {
			showEmptyScreen(needShow = true)
		}
	}

	private fun getEndOfArchive(secondsBeforeEnd: Int = 1): Calendar {
		return Calendar.getInstance().apply {
			if (statusStreamHighActive && camera.isRestrictedArchive != true) {
				timeInMillis = playerOpenTime.timeInMillis - secondsBeforeEnd * 1000L
			} else if (archiveRanges.isNotEmpty()) {
				timeInMillis =
					(archiveRanges.last().from + archiveRanges.last().duration - secondsBeforeEnd) * 1000L
			} else {
				// never come here
			}
		}
	}

	private fun checkMarkStartEndArchive(from: String) {
		currentDateCursor = try {
			if (setCalendarByDateServer(from).after(Calendar.getInstance())) {
				handleParsingError()
				setCalendarByLastAccessRange(getEndOfArchive(), camera.getStartAtLocal())
			} else {
				getCalendarByDateServerMark(from, camera.getStartAtLocal())
			}
		} catch (e: Exception) {
			//if have mark, but archive deleted -> camera.getStartAtLocal() = "" -> handle in passCameraWithRanges()
			//this return just to prevent crash in setCalendarByDateServerMark in onCreate()
			getEndOfArchive()
		}
	}

	override fun loadArchive(url: String?, moveTo: String) {
		if (!isVisible || isLive) return
//                videoUrl = "rtsp://admin:456redko@192.168.204.33:554/Streaming/Channels/101"
		if (url == null) {
			showEmptyScreen(needShow = true)
		} else {
			showEmptyScreen(needShow = false)
			forcePlayVideo = false
			player.apply {
				actionStateChanged = { it, it2 -> onPlayerStateChanged(it, it2) }
				videoUrl = url //  +"&speed=10" // test speed
				try {
					initPlayer(Surface(getCurrentFragment()!!.binding.vmsPlayerView.surfaceTexture))
				} catch (e: Exception) {
					// surfaceTexture can be null somehow
				}
			}
			//reset current live calendar for loaded url, because playlist can contain no metadata
			currentLiveCalendar?.let { currentLiveCalendar = null }
			needLoadNewMarks = true //indicate that we must load marks for new load url
			logSdk(TAG, "videoUrl = ${player.videoUrl}")
//			player.startPlayer()
			if (!isSlideHandlerRun) {
//                logsdk(TAG,"temp loadArchive* = ${currentDateCursor?.time}" )
				currentDateCursor = setCalendarByDateServer(moveTo)
//                logsdk(TAG,"temp loadArchive* = ${currentDateCursor?.time}" )
				binding.timeLine.setCursorByClickMoveButton(currentDateCursor)
			}
			canHandleTimeline(canHandle = true) //allow handle timeline clicks
			if (timeDatePicker.isNotEmpty()) { // if set from datePicker
				timeDatePicker = ""
			}
		}
		if (!markFrom.isNullOrEmpty() && !isToastAlreadyShown && isMarkOlderStartedArchive(
				markFrom!!,
				camera.getStartAtLocal()
			)
		) {
			isToastAlreadyShown = true
			showToast(getStringForLayoutByKey("mark_was_deleted"))
		}
	}

	private fun stopPlayerInTheEnd() {
		binding.viewBottomBarNavigation.imgPlay.setImageResource(R.drawable.bt_play)
		player.pause()
		stopTimeArchive(isShowTime = false)
		setSpeed(speed_X_1)
		initSpeedAdapter()
	}

	private fun onClickScreenShot() {
		if (binding.viewEmptyScreenSdk.emptyScreen.isVisible) {
			showToast(getStringForLayoutByKey("err_common_short"))
		} else {
			val width = player.width
			val height = player.height
			val bitmap = getCurrentFragment()?.binding?.vmsPlayerView?.getBitmap(width, height)
			if (bitmap != null) {
				val date =
					if (currentDateCursor != null) currentDateCursor else Calendar.getInstance()
				callbackScreenshot?.onClickScreenshot(bitmap, camera, date!!, state)
			}
		}
	}

	private fun onClickCalendar(isShowPeriod: Boolean = false, statusPicker: String = "") {
		callbackLogEvent?.onLogEvent(SHOW_CALENDAR)
		statusTimePickerDialog = statusPicker
		if (activity != null && !requireActivity().isDestroyed && !requireActivity().isFinishing && isVisible && !isDetached
			&& !isLocaleCorrect(requireActivity())
		) changeLocale(requireActivity())
		if (statusTimePickerDialog == STATUS_TIME_START_PERIOD) {
			calendarStartPeriod =
				setCalendarByDatePeriod(binding.viewSheetBottomDownloadArchive.tvStartPeriodFull.text.toString())
		} else if (statusTimePickerDialog == STATUS_TIME_END_PERIOD) {
			calendarEndPeriod =
				setCalendarByDatePeriod(binding.viewSheetBottomDownloadArchive.tvEndPeriodFull.text.toString())
		}
		setStateBehaviorDatePicker(BottomSheetBehavior.STATE_EXPANDED, isShowPeriod = isShowPeriod)
	}

	private fun onClickMoveStart() {
		if (camera.getStartAtLocal().isEmpty()) return
		callbackLogEvent?.onLogEvent(TAP_PLAYBACK_START)
		val time = Calendar.getInstance().apply {
			timeInMillis = archiveRanges.first().from * 1000L
		} // test on office intercom + 60000
		getArchive(time)
		enableOrDisableLeftButtons(isEnabled = false)
		showToast(getStringForLayoutByKey("archive_start"))
	}

	private fun onClickMoveEnd() {
		callbackLogEvent?.onLogEvent(TAP_PLAYBACK_END)
		isMoveRight = true
		onShowArchive()
		setDateTimeMarkCreate()
		enableOrDisableRightButtons(isEnabled = false)
		showToast(getStringForLayoutByKey("archive_end"))
	}

	@SuppressLint("UnsafeOptInUsageError")
	private fun onTimelineChanged() {
		//try to get date and time from metadata
		currentDateCursor?.let { binding.timeLine.setCorrectLeftDate(it) } //set correct left date

		/*if (player.isRtsp()) {
//			logSdk(TAG, "onTimelineChanged, time = $time, currentDateCursor = ${currentDateCursor?.time}")
//			val calendar = Calendar.getInstance().apply { timeInMillis = (time * 1000) * speed_X_1.toInt() }    //calendar from metadata
//			currentDateCursor = calendar
//			logSdk(TAG, "onTimelineChanged, time = $time, currentDateCursor = ${currentDateCursor?.time}")
//			currentDateCursor?.let { binding.timeLine.setCorrectLeftDate(it) } //set correct left date
			currentDateCursor?.let { binding.timeLine.setCorrectLeftDate(it) } //set correct left date
		} else {
			val currentManifest = player.currentManifest  //playlist
			logSdk(TAG, "onTimelineChanged, currentManifest = $currentManifest")
			if (player.currentManifest !is HlsManifest) return // todo check for null it
			val hlsManifest = currentManifest as HlsManifest   //hls playlist
			val tags = hlsManifest.mediaPlaylist.tags   //get all tags in metadata
			for (tag in tags) {
				if (tag.contains("EXT-X-PROGRAM-DATE-TIME")) {  //needed tag
					//date and time can be: (YYYY-MM-DDThh:mm:ssZ or YYYY-MM-DDThh:mm:ss.SSSZ)
					//#EXT-X-PROGRAM-DATE-TIME:2020-09-29T09:22:07Z
					//#EXT-X-PROGRAM-DATE-TIME:2010-02-19T14:54:23.031+08:00
					val time = tag.split("EXT-X-PROGRAM-DATE-TIME:").last().substring(0, 19)
						.replace("T", " ")  //date from metadata
					val calendar = setCalendarByDateServerTimeLine(time)    //calendar from metadata
					if (isLive) {
						// todo test it later - now has bug for live, time from meta data wrong
//                            isNeedAddTimeToLive = true  //allow add additional time when load completed
//                            currentLiveCalendar = calendar  //set date for live
//                            tv_time_top_bar_live?.text = setCalendar(calendar)
					} else {
						try {
							logSdk(TAG, "onTimelineChanged1 = ${calendar.time}")
							logSdk(TAG, "onTimelineChanged2 = ${currentDateCursor?.time}")
							currentDateCursor = calendar    //set date for archive
							logSdk(TAG, "onTimelineChanged3 = ${currentDateCursor?.time}")
							binding.timeLine.setCorrectLeftDate(calendar)  //set correct left date (when timeline moved it doesn't)
							if (hasMarksToShow) getMarks()  //can load new marks, because we have all needed params
							needLoadNewMarks = false    //don't load marks on runnable
						} catch (e: Exception) {    //on first enter leftdate on timeline is null
							isNeedAddTimeToArchive = true
							correctTimeArchive = calendar
						}
					}
				}
			}
		}*/
	}

	override fun loadMarks(list: List<VMSEvent>) {
		currentCameraMarks.clear()
		for (item in list) {
			item.title?.let { name ->
				currentCameraMarks.add(name)
			}
		}
		binding.timeLine.setMarks(list)
		if (!isLive) binding.timeLine.invalidate()
	}

	private fun onPlayerStateChanged(playbackState: Int, any: Any?) {
		if (playbackState != 13)
			logSdk(TAG, "playbackState = $playbackState")
		try {
			when (playbackState) {
				STATE_BUFFERING.state -> {
					//You can use progress dialog to show user that video is preparing or buffering so please wait
					binding.viewBottomBarNavigation.constrBottomBarNav.isClickable = false
					binding.viewBottomBarNavigation.constrBottomBarNav.isEnabled = false
					showOrHideProgressBar(show = true) // show
					stopTimeArchive(isShowTime = false)
					val phoneBrand = MANUFACTURER.lowercase(Locale.ENGLISH)
					val phoneModel = MODEL.lowercase(Locale.ENGLISH)
					if (phoneBrand == "xiaomi" && phoneModel.contains("redmi") &&
						phoneModel.contains("4") && !player.playWhenReady
					) {
						// fixing bug on Xiaomi Redmi note 4 (Redmi 4x) - 7 (7.1) android, when loop states of stream
						++counterStatesOfBuffering
						Log.d("Player_STATE_play", "increase counter to $counterStatesOfBuffering")
					}
					if (counterStatesOfBuffering > MAX_COUNTER_BUFFERED) {
						player.stop()
						Log.d("Player_STATE_play", "stop player after bug happened")
						Handler(Looper.getMainLooper()).postDelayed({
							if (isVisible) showOrHideProgressBar(show = false)
						}, 1500)
					}
				}

				STATE_IDLE.state -> {
					// do nothing
				}

				STATE_READY.state -> {
					// video is ready to play now, only for hls
					if (!player.isRtsp()) {
						setStateReady()
					}

					updatePlayerView()
					if (checkAudioToShowImage() && getCurrentItem() == playerPosition) {
						player.sound = if (settings.enabledAudio) 1f else 0f
					}
				}

				STATE_WIDTH_HEIGHT_READY.state -> {
					// video is ready to play now, only for rtsp
					if (player.isRtsp()) {
						val size = any as? Pair<Int, Int>
						size?.first?.let {
							player.width = it
						}
						size?.second?.let {
							player.height = it
						}
						Log.d(TAG, "STATE_WIDTH_HEIGHT_READY size.w = ${player.width} size.h = ${player.height}")
						setStateReady()
						updatePlayerView()
						handleLastElementWidthHeight(isPortrait)
					}
				}

				STATE_ENDED.state -> {
					Log.d("Player_STATE", "STATE_ENDED")
					currentDateCursor?.let {
						val c = getEndOfArchive()
						// chunk with archive has 2 minutes, than 2 minutes before current
						// time always exist
						if ((c.timeInMillis - 1000) < it.timeInMillis) {
							if (!isPlayerStateEnd) showToast(getStringForLayoutByKey("archive_finished")) // *after onResume to avoid double show
							isPlayerStateEnd = true
							stopPlayerInTheEnd()
							enableOrDisableRightButtons(false)
						}
					}
				}

				STATE_TIMELINE_CHANGED.state -> {
					if (isVisible) onTimelineChanged()
				}

				STATE_SPEED_CHANGED.state -> {
					if (!player.isRtsp()) player.startPlayer()
					updatePlayerView()
				}

				STATE_RTSP_REPLAY_ARCHIVE.state -> {
					playCameraArchive()
				}

				STATE_ERROR_OCCURRED.state -> {
					when (any) {
						is PlaybackException -> onPlayerError(any)
						is RtspError204 -> if (!isLive) handleParsingError() else handleStreamOffError()
						is RtspErrorEmptyStream -> showEmptyScreen(needShow = any.needShowError)
						is RtspSocketTimeoutException -> handleParsingError()
					}
				}

				STATE_ERROR_DEVICE_PERFORMANCE.state -> {
					showToast(getStringForLayoutByKey("err_device_performance"))
					showEmptyScreen(needShow = true)
				}

				STATE_ERROR_QUEUE_FRAME.state, STATE_ERROR_VIDEO_DECODER.state -> {
					logSdk("PlayerFragment", "EMPTY_FRAMES")
					if (isLive) {
						onShowLive()
					} else {
						playCameraArchive()
					}
				}

				STATE_STARTED.state -> {
					if (player.isRtsp() && !isLive) {
						if (any == null) return
						val time = any.toString().toLong() // to timeInMillis
						currentDateCursor?.apply { timeInMillis = time }
						logSdk("PlayerFragment", "STATE_STARTED = currentDateCursor = ${currentDateCursor?.time}")
					}
				}

				STATE_PLAYING.state -> {
					if (player.isRtsp() && !isLive) {
						if (isCreateMarkViewShown && isVisible && !isDetached) {
							if (player.isRtsp()) {
								Handler(Looper.getMainLooper()).postDelayed({
									try {
										if (player.playbackState != STATE_RELEASED.state && !requireActivity().isDestroyed && !requireActivity().isFinishing && isVisible && !isDetached) {
											setDoneCreateMark(true)
											showOrHideProgressBar(show = false) // hide progress bar for mark creating
											updatePlayerView()
											stopTimeAndVideo()
										}
									} catch (e: Throwable) {
										e.printStackTrace()
									}
								}, 3000)
							} else {
								showOrHideProgressBar(show = false) // hide progress bar for mark creating
								stopTimeAndVideo()
							}
						}
						if (any == null) return
						val time = (any.toString().toDouble() * player.speedX).toLong()  // to timeInMillis
						currentDateCursor?.apply { timeInMillis += time }
						if (isVisible) handleArchiveTimeChanged()
					}
				}
			}
		} catch (e: Exception) {
			e.message
		}
	}

	@SuppressLint("SuspiciousIndentation")
	fun setStateReady() {
		setSpeed(player.speedX)
		countFailedLoads = 0
		handlePlayerWidthHeight(isPortrait(requireContext()))
		Log.d("Player_STATE", "STATE_READY")
		if (!player.isRtsp() || !isCreateMarkViewShown) showOrHideProgressBar(show = false) // hide
		if (isOnline(requireContext())) needDisableButtons(false)
		showEmptyScreen(false)
		binding.viewBottomBarNavigation.imgPlay.setImageResource(R.drawable.bt_pause)
		if (isLive) {
			startHandlerAndChangeDateLive()
		} else {
			currentDateCursor?.let { startHandlerAndChangeDateArchive(it) }
		}
		if (isCreateMarkViewShown) {
			//need to set correct time when swipe time-line when edit mark
			setDateTimeMarkCreate()
			if (!forcePlayVideo) {
				if (player.isRtsp()) {
					// need time to start new thread and invalidate surface
					// stop video only after start playing
//					stopTimeArchive(false) // not need if rtsp
				} else {
					stopTimeAndVideo()
				}
				listenChangeMovingButton()
			}
		} else {
			//for update mark
			changedMark?.let {
				markFrom?.let { from ->
					//long click for show mark update view only after player state ready
					//because we need exoPlayer width and height
					onLongClick(
						requireActivity().getWindowWidth() / 2f,
						setCalendarByDateServer(from)
					)
					//we stop timer -> set values to timeline
					setTimelineBitmapAndDate(from)
					binding.timeLine.isGoneVMS(false)
					binding.viewBottomBarNavigation.constrBottomBarNav.isGoneVMS(false)
				}
			}
		}
		if (isCreateMarkViewShown) {
			//todo check why it doesn't perform by onLongCLick()
			if (isPortrait(requireContext())) {
				getCurrentFragment()?.binding?.vmsPlayerView?.setMargins(0, 0, 0, 0)
			} else {
				getCurrentFragment()?.binding?.vmsPlayerView?.setMargins(
					-(markCreateViewWidth / 2),
					0,
					0,
					0
				)
			}
		}
	}

	private fun setTimelineBitmapAndDate(from: String? = null) {
		val timeLineW = requireActivity().getWindowWidth()
		val timeLineH = resources.getDimension(R.dimen.time_bar_h_new).toInt()
		binding.timeLine.onSizeChanged(timeLineW, timeLineH, timeLineW, timeLineH)
		binding.timeLine.setCursor(if (from != null) setCalendarByDateServer(from) else currentDateCursor)
	}

	private fun handleStreamOffError() {
		getCamera(cameraId) //reload camera for correct stream statuses
	}

	//handle connection lost during playing video
	private fun handleConnectionError() {
		//don't call getCamera(), it's called when start camera
		val error = ConnectException()  //connection exception to show toast
		if (isLive) {
			val id = if (cameraId != 0) cameraId.toString() else STREAM_ID
			val typeCurrent =
				if (statusStreamHighActive && settings.videoQuality.equals(HIGH, true)) HIGH
				else LOW
			if (isOnline(requireContext())) {   //have internet connection (mean problem with streams status -> backend bug)
				if (camera.userStatus == USER_STATUS_BLOCKED
					|| camera.status == INACTIVE || camera.status == EMPTY || camera.status == INITIAL
					|| camera.isRestrictedLive == true
				) {
					showEmptyScreen(
						needShow = true,
						isLocked = camera.userStatus == USER_STATUS_BLOCKED
					)
				} else {
					countFailedLoads++  //increase counter
					if (countFailedLoads >= 3) {    //if load failed 3 times
						showToast(getStringForLayoutByKey("err_no_streams_repeat"))
						stopTimeLive()  //stop timer
					} else {
						presenter.getLiveStream(
							id,
							typeCurrent,
							requireContext()
						) //try to get streams one more time
					}
				}
			} else {    //no internet connection
				needDisableButtons(true)
				stopTimeAndVideo() //stop timer and block controls
				didReceiveError(this, error, requireContext()) {
					presenter.getLiveStream(
						id,
						typeCurrent,
						requireContext()
					)
				}   //handle this error
			}
		} else {
			currentDateCursor?.let {
				if (isOnline(requireContext())) {   //have internet connection (mean problem with stream status -> backend bug)
					if (camera.userStatus == USER_STATUS_BLOCKED
						|| camera.status == INACTIVE || camera.status == EMPTY || camera.status == INITIAL || archiveRanges.isEmpty()
					) {
						disableArchiveIfNeed()
						showEmptyScreen(
							needShow = true,
							isLocked = camera.userStatus == USER_STATUS_BLOCKED
						)
					} else {
						countFailedLoads++  //increase counter
						if (countFailedLoads >= 3) {    //if load failed 3 times
							showToast(getStringForLayoutByKey("err_no_streams_repeat"))
							hideTimeLine()
						} else {
							currentDateCursor = setCalendarByLastAccessRange(
								getEndOfArchive(),
								camera.getStartAtLocal()
							)
							//try to get archive one more time
							getArchive(currentDateCursor!!)
						}
					}
				} else {    //no internet connection
					needDisableButtons(true)
					stopTimeAndVideo()    //stop timer and block controls
					canHandleTimeline(canHandle = false)    //bun handle timeline clicks
					didReceiveError(
						this,
						error,
						requireContext()
					) { getArchive(currentDateCursor!!) }   //handle this error
				}
				checkTimelineVisibility()
			}
		}
	}

	//handle error with video format or resolution
	private fun handleMediaError() {
		if (isLive) {   //if live
			if (statusStreamLowActive && settings.videoQuality.equals(
					HIGH,
					ignoreCase = true
				)
			) { //if have low quality and error with hd stream
				showToast(getStringForLayoutByKey("err_media_codec_change_quality"))   //show toast
				Handler(Looper.getMainLooper()).postDelayed({
					if (isVisible) {
						settings.videoQuality = LOW   //set low quality
						getLiveStream() //load low quality stream
						adapterQuality = null   //reset quality adapter
						initQualityAdapter()    //set new quality adapter for correct display bottom sheet
					}
				}, TOAST_DURATION)
			} else {    //haven't low quality stream or this error was already with sd stream
				showToast(getStringForLayoutByKey("err_media_codec"))  //show toast
				stopTimeLive()  //stop time live
				needDisableButtons(true)    //disable buttons
			}
		} else {    //if archive
			if (statusStreamLowActive) {    //if have low quality stream
				showToast(getStringForLayoutByKey("err_media_codec_change_stream"))    //show toast
				Handler(Looper.getMainLooper()).postDelayed({
					if (isVisible) {
						settings.videoQuality = LOW   //set low quality
						handleErrorArchive()    //handle error in archive (go to live)
						adapterQuality = null   //reset quality adapter
						initQualityAdapter()    //set new quality adapter for correct display bottom sheet
					}
				}, TOAST_DURATION)
			} else {    //haven't low quality stream
				showToast(getStringForLayoutByKey("err_media_codec"))  //show toast
				stopTimeArchive(false)  //stop time archive
				needDisableButtons(true)    //disable buttons
				binding.viewBottomBarNavigation.imgPlay.setImageResource(R.drawable.bt_play)  //set play image
			}
		}
	}

	private fun handleParsingError() {
		logSdk(TAG, "ERROR, pass 3 ")
		closeCreateMarkView(needPlayArchive = false)
		showPlayerNavigation()
		showEmptyScreen(needShow = true, isShowTimeline = true)
		binding.viewEmptyScreenSdk.emptyTitle.text =
			getStringForLayoutByKey("err_archive_unavailable")
		binding.viewEmptyScreenSdk.emptyTitle.setTextColorCompatVMS(requireContext(), R.color.white)
		binding.viewEmptyScreenSdk.emptyMes.text =
			getStringForLayoutByKey("it_takes_time_to_generate_archive")
		binding.viewEmptyScreenSdk.emptyMes.setTextColorCompatVMS(requireContext(), R.color._ACAFB8)
		binding.timeLine.setCursorByClickMoveButton(currentDateCursor)
		listenChangeMovingButton()
		Handler(Looper.getMainLooper()).postDelayed({ getMarks() }, 300) // wait timeline loading
		stopPlayerInTheEnd()
	}//handle unexpected errors in media player

	private fun showPlayerNavigation() {
		if (camera.getStartAtLocal()
				.isEmpty() || binding.viewBottomBarNavigation.constrBottomBarNav.isVisible
		) return
		isAnimationShow = true
		binding.viewCursor.isGoneSmooth(false)
		setTimelineBitmapAndDate() // to show timeline when open archive from unachievable event
		binding.timeLine.isGoneSmooth(false)
		binding.viewBottomBarNavigation.constrBottomBarNav.isGoneSmooth(false, ALPHA_FULL)
		if (hasMarksToShow && settings.hasEventsToShow()) {
			val needHalfAlpha = activity != null && !requireActivity().isDestroyed
					&& !requireActivity().isFinishing && errorType.isNotEmpty()
			showHideNearestEventButtonsSmooth(false, if (needHalfAlpha) ALPHA_HALF else ALPHA_FULL)
		}
		Handler(Looper.getMainLooper()).postDelayed({ isAnimationShow = false }, ANIM_DURATION)
	}

	private fun handleUnexpectedMediaError() {
		showToast(getStringForLayoutByKey("err_media_codec_unexpected"))
	}

	private fun onPlayerError(e: PlaybackException) {
		logSdk(TAG, "onPlayerError errorCode = ${e.errorCode} ${e.errorCodeName} ${e.message} ")
		logSdk(TAG, "onPlayerError message = ${e.message} ")
		logSdk(TAG, "onPlayerError cause = ${e.cause} ")
		showOrHideProgressBar(show = false)
		when (e.errorCode) {
			PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> {
				val cause = e.cause
				if (cause is InvalidResponseCodeException && cause.responseCode == 404) {
					showEmptyScreen(needShow = true)
					return
				}
				//stream with current quality is turn off
				handleStreamOffError()
			}

			PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
			PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> {
				//no network
				handleConnectionError()
			}

			PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
			PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED,
			PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
			PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED -> {
				//stream was restarted or renamed or error "Input does not start with the #EXTM3U header"
				logSdk(TAG, "onPlayerError pass 1 ")
				if (!isLive && Calendar.getInstance().timeInMillis - TEN_MIN < currentDateCursor!!.timeInMillis) {
					logSdk(TAG, "onPlayerError pass 2 ")
					handleParsingError()
				}
			}

			PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
			PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED,
			PlaybackException.ERROR_CODE_DECODING_FAILED,
			PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES,
			PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED -> {
				//not see, but must be media error
				handleMediaError()
			}

			PlaybackException.ERROR_CODE_IO_UNSPECIFIED -> {
				if (!player.isRtsp()) mainLoadViews()
			}

			else -> {
				//show toast for other errors
				handleUnexpectedMediaError()
			}
		}
	}

	override fun showOrHideProgressBar(show: Boolean, screenNumber: Int) {
		getCurrentFragment()?.binding?.pbPlayer?.isGoneVMS(!show)
		logSdk("PLAYER", "is show = $show ")
	}

	override fun customHandlingErrors(error: ApiError) {
		callbackErrors?.onHandleErrors(error)
	}

	override fun showToast(text: String) {
		showToastIfCurrentItem(text)
	}

	private fun onPressPlayOrPause() {
		if (isPlayerStateEnd) return
		if (player.playWhenReady) {
			pause()
		} else {
			// press play
			if (activity != null && !requireActivity().isDestroyed &&  !requireActivity().isFinishing && isVisible && !isDetached) {
				if (isOnline(requireContext())) {
					forcePlayVideo = true
					showToast(getStringForLayoutByKey("play_started"))
					currentDateCursor?.let { startHandlerAndChangeDateArchive(it) }
					play()
				} else {
					needDisableButtons(true)  //block controls
					canHandleTimeline(canHandle = false)    //bun handle timeline clicks
					didReceiveError(
						this,
						ConnectException(),
						requireContext()
					) {   //handle connect exception
						this.needDisableButtons(false)  //unblock controls
						this.canHandleTimeline(canHandle = true)    //allow handle timeline clicks
					}
				}
			}
		}
	}

	private fun pause() {
		callbackLogEvent?.onLogEvent(TAP_ARCHIVE_PAUSE)
		forcePlayVideo = false
		stopTimeArchive(false)
		showToast(getStringForLayoutByKey("play_stopped"))
		setDateTimeMarkCreate()
		player.pause()
		binding.viewBottomBarNavigation.imgPlay.setImageResource(R.drawable.bt_play)
		isLive = false
	}

	private fun play() {
		if (counterStatesOfBuffering > MAX_COUNTER_BUFFERED) { // fixing bug on Xiaomi Redmi 4x - 7 android, when loop states of stream
			counterStatesOfBuffering = 0
			player.stop()
			playCameraArchive()
		}

		callbackLogEvent?.onLogEvent(TAP_ARCHIVE_PLAY)
		player.play()

		if (isMoveRight) {
			stopPlayerInTheEnd()
		} else {
			if (isPlayerStateEnd) onShowArchive()
			if (!isCreateMarkViewShown || forcePlayVideo)
				binding.viewBottomBarNavigation.imgPlay.setImageResource(R.drawable.bt_pause)
		}
		isPlayerStateEnd = false
	}

	private fun enableOrDisableRightButtons(isEnabled: Boolean) {
		adapterMore?.updateSpeedEnabled(isEnabled)
		binding.viewBottomBarNavigation.moveRight.isEnabled = isEnabled
		binding.viewBottomBarNavigation.moveRightFiveSeconds.isEnabled = isEnabled
		binding.viewBottomBarNavigation.moveRightOneMinute.isEnabled = isEnabled
		binding.viewBottomBarNavigation.moveRightOneHour.isEnabled = isEnabled
		binding.viewBottomBarNavigation.moveRightOneDay.isEnabled = isEnabled

		binding.viewBottomBarNavigation.moveRight.alpha = if (isEnabled) ALPHA_FULL else ALPHA_HALF
		binding.viewBottomBarNavigation.moveRightFiveSeconds.alpha =
			if (isEnabled) ALPHA_FULL else ALPHA_HALF
		binding.viewBottomBarNavigation.moveRightOneMinute.alpha =
			if (isEnabled) ALPHA_FULL else ALPHA_HALF
		binding.viewBottomBarNavigation.moveRightOneHour.alpha =
			if (isEnabled) ALPHA_FULL else ALPHA_HALF
		binding.viewBottomBarNavigation.moveRightOneDay.alpha =
			if (isEnabled) ALPHA_FULL else ALPHA_HALF
	}

	private fun enableOrDisableLeftButtons(isEnabled: Boolean) {
		binding.viewBottomBarNavigation.moveLeft.isEnabled = isEnabled
		binding.viewBottomBarNavigation.moveLeftFiveSeconds.isEnabled = isEnabled
		binding.viewBottomBarNavigation.moveLeftOneMinute.isEnabled = isEnabled
		binding.viewBottomBarNavigation.moveLeftOneHour.isEnabled = isEnabled
		binding.viewBottomBarNavigation.moveLeftOneDay.isEnabled = isEnabled

		binding.viewBottomBarNavigation.moveLeft.alpha = if (isEnabled) ALPHA_FULL else ALPHA_HALF
		binding.viewBottomBarNavigation.moveLeftFiveSeconds.alpha =
			if (isEnabled) ALPHA_FULL else ALPHA_HALF
		binding.viewBottomBarNavigation.moveLeftOneMinute.alpha =
			if (isEnabled) ALPHA_FULL else ALPHA_HALF
		binding.viewBottomBarNavigation.moveLeftOneHour.alpha =
			if (isEnabled) ALPHA_FULL else ALPHA_HALF
		binding.viewBottomBarNavigation.moveLeftOneDay.alpha =
			if (isEnabled) ALPHA_FULL else ALPHA_HALF
	}

	private fun enableOrDisableButtonsInModeMark(isEnabled: Boolean) {
		binding.viewBottomBarNavigation.imgPlay.enableOrDisableMovingButtons(isEnabled)
	}

	private fun listenChangeMovingButton() {
		if (!binding.viewBottomBarNavigation.moveLeft.isEnabled) binding.viewBottomBarNavigation.moveLeft.enableOrDisableMovingButtons(
			isEnabled = true
		)

		if (camera.getStartAtLocal().isEmpty()) return
		val startTime = setCalendarByDateServer(camera.getStartAtLocal()).timeInMillis
		val endTime = getEndOfArchive().timeInMillis
		when {
			currentDateCursor?.timeInMillis!! < (startTime + 5 * 1000) -> {
				enableOrDisableLeftButtons(isEnabled = false)
			}

			currentDateCursor?.timeInMillis!! < (startTime + 60 * 1000) -> {
				binding.viewBottomBarNavigation.moveLeftFiveSeconds.enableOrDisableMovingButtons(
					isEnabled = true
				)
				binding.viewBottomBarNavigation.moveLeftOneMinute.enableOrDisableMovingButtons(
					isEnabled = false
				)
				binding.viewBottomBarNavigation.moveLeftOneHour.enableOrDisableMovingButtons(
					isEnabled = false
				)
				binding.viewBottomBarNavigation.moveLeftOneDay.enableOrDisableMovingButtons(
					isEnabled = false
				)
			}

			currentDateCursor?.timeInMillis!! < (startTime + 3600 * 1000) -> {
				binding.viewBottomBarNavigation.moveLeftFiveSeconds.enableOrDisableMovingButtons(
					isEnabled = true
				)
				binding.viewBottomBarNavigation.moveLeftOneMinute.enableOrDisableMovingButtons(
					isEnabled = true
				)
				binding.viewBottomBarNavigation.moveLeftOneHour.enableOrDisableMovingButtons(
					isEnabled = false
				)
				binding.viewBottomBarNavigation.moveLeftOneDay.enableOrDisableMovingButtons(
					isEnabled = false
				)
			}

			currentDateCursor?.timeInMillis!! < (startTime + 3600 * 24 * 1000) -> {
				binding.viewBottomBarNavigation.moveLeftFiveSeconds.enableOrDisableMovingButtons(
					isEnabled = true
				)
				binding.viewBottomBarNavigation.moveLeftOneMinute.enableOrDisableMovingButtons(
					isEnabled = true
				)
				binding.viewBottomBarNavigation.moveLeftOneHour.enableOrDisableMovingButtons(
					isEnabled = true
				)
				binding.viewBottomBarNavigation.moveLeftOneDay.enableOrDisableMovingButtons(
					isEnabled = false
				)
			}

			else -> {
				binding.viewBottomBarNavigation.moveLeftFiveSeconds.enableOrDisableMovingButtons(
					isEnabled = true
				)
				binding.viewBottomBarNavigation.moveLeftOneMinute.enableOrDisableMovingButtons(
					isEnabled = true
				)
				binding.viewBottomBarNavigation.moveLeftOneHour.enableOrDisableMovingButtons(
					isEnabled = true
				)
				binding.viewBottomBarNavigation.moveLeftOneDay.enableOrDisableMovingButtons(
					isEnabled = true
				)
			}
		}
		when {
			currentDateCursor?.timeInMillis!! > (endTime - 5 * 1000) -> {
				enableOrDisableRightButtons(isEnabled = false)
			}

			currentDateCursor?.timeInMillis!! > (endTime - 60 * 1000) -> {
				adapterMore?.updateSpeedEnabled(true)
				binding.viewBottomBarNavigation.moveRight.enableOrDisableMovingButtons(isEnabled = true)
				binding.viewBottomBarNavigation.moveRightFiveSeconds.enableOrDisableMovingButtons(
					isEnabled = true
				)
				binding.viewBottomBarNavigation.moveRightOneMinute.enableOrDisableMovingButtons(
					isEnabled = false
				)
				binding.viewBottomBarNavigation.moveRightOneHour.enableOrDisableMovingButtons(
					isEnabled = false
				)
				binding.viewBottomBarNavigation.moveRightOneDay.enableOrDisableMovingButtons(
					isEnabled = false
				)
			}

			currentDateCursor?.timeInMillis!! > (endTime - 3600 * 1000) -> {
				adapterMore?.updateSpeedEnabled(true)
				binding.viewBottomBarNavigation.moveRight.enableOrDisableMovingButtons(isEnabled = true)
				binding.viewBottomBarNavigation.moveRightFiveSeconds.enableOrDisableMovingButtons(
					isEnabled = true
				)
				binding.viewBottomBarNavigation.moveRightOneMinute.enableOrDisableMovingButtons(
					isEnabled = true
				)
				binding.viewBottomBarNavigation.moveRightOneHour.enableOrDisableMovingButtons(
					isEnabled = false
				)
				binding.viewBottomBarNavigation.moveRightOneDay.enableOrDisableMovingButtons(
					isEnabled = false
				)
			}

			currentDateCursor?.timeInMillis!! > (endTime - 3600 * 24 * 1000) -> {
				adapterMore?.updateSpeedEnabled(true)
				binding.viewBottomBarNavigation.moveRight.enableOrDisableMovingButtons(isEnabled = true)
				binding.viewBottomBarNavigation.moveRightFiveSeconds.enableOrDisableMovingButtons(
					isEnabled = true
				)
				binding.viewBottomBarNavigation.moveRightOneMinute.enableOrDisableMovingButtons(
					isEnabled = true
				)
				binding.viewBottomBarNavigation.moveRightOneHour.enableOrDisableMovingButtons(
					isEnabled = true
				)
				binding.viewBottomBarNavigation.moveRightOneDay.enableOrDisableMovingButtons(
					isEnabled = false
				)
			}

			else -> {
				adapterMore?.updateSpeedEnabled(true)
				binding.viewBottomBarNavigation.moveRight.enableOrDisableMovingButtons(isEnabled = true)
				binding.viewBottomBarNavigation.moveRightFiveSeconds.enableOrDisableMovingButtons(
					isEnabled = true
				)
				binding.viewBottomBarNavigation.moveRightOneMinute.enableOrDisableMovingButtons(
					isEnabled = true
				)
				binding.viewBottomBarNavigation.moveRightOneHour.enableOrDisableMovingButtons(
					isEnabled = true
				)
				binding.viewBottomBarNavigation.moveRightOneDay.enableOrDisableMovingButtons(
					isEnabled = true
				)
			}
		}
	}

	private fun setSpeed(speed: Float) {
		player.speedX = speed
		if (!isLive) {
			val timeForUpdate = (if (player.speedX == 1f) 1000 else JAVA_SECOND_FOR_SPEED).toFloat()
			timeForUpdateTimerSeconds = (timeForUpdate / player.speedX).toLong()
			logSdk(TAG, "setSpeed* timeForUpdateTimerSeconds = $timeForUpdateTimerSeconds")
		} else {
			timeForUpdateTimerSeconds = 1000
			adapterSpeeds?.innerSpeedX = player.speedX
			adapterSpeeds?.notifyDataSetChanged()
		}
		val tag = when (player.speedX) {
			0.5f -> TAP_PLAYBACK_SPEED_0_5x
			1f -> TAP_PLAYBACK_SPEED_1X
			2f -> TAP_PLAYBACK_SPEED_2X
			4f -> TAP_PLAYBACK_SPEED_4X
			8f -> TAP_PLAYBACK_SPEED_8X
			else -> TAP_PLAYBACK_PLUS_5S
		}
		callbackLogEvent?.onLogEvent(tag)
	}

	override fun onShowCameraController() {
		showHideNavigationViews()
	}
}