package com.mobile.vms.player.timeline

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.graphics.Bitmap.Config
import android.graphics.PorterDuff.Mode
import android.os.*
import android.util.AttributeSet
import android.view.*
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import androidx.core.content.ContextCompat
import com.mobile.vms.*
import com.mobile.vms.R.color
import com.mobile.vms.models.*
import com.mobile.vms.player.helpers.*
import com.mobile.vms.player.helpers.VMSSettings.getStringForLayoutByKey
import com.mobile.vms.player.ui.VMSPlayerFragment
import io.reactivex.disposables.CompositeDisposable
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

class TimelineView(context: Context, attrs: AttributeSet): View(context, attrs) {

	var bitmap: Bitmap? = null // object map where store all views, to hold the pixels
	var leftDate: Calendar? = null
	var calendarCursor: Calendar? = null
	private var lastTouchX = -1.0f
	var maxPixelsPerStep = 0.0f
	var millisPerStep: Long = 0
	var minPixelsPerStep = 0.0f
	var pixelsPerStep = 0.0f
	private var callbackDateTime: ArchiveTimeCallback? =
		null // for pass date and mark clicks to fragment
	var secondScales: LongArray // array with items of seconds
	var zoomStarted = false
	private var canvas: Canvas? =
		null // object where draw all views, to host the draw calls (writing into the bitmap)
	private var drawCalendar = Calendar.getInstance(Locale(settings.getChosenLanguage()))
	private val drawDays = ArrayList<ArrayList<Any>>() // calendar days like - 21 August
	private var streamBottomBlockHeight: Int = 0
	private val activeStream: Paint // test view
	private val inactiveStream: Paint // test view
	private val marksPaint: Paint
	private val path = Path()
	private var hhmmFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
	private var lastTouchTime: Long = 0
	private var moveStarted = false
	private val paint = Paint(0) // common object to describe the colors and styles for the drawing
	private val scaleGestureDetector =
		ScaleGestureDetector(getContext(), ScaleListener()) // for listen zoom
	private var serverTimeZoneOffset: Int = 0
	private var timelineMoved = false
	private var camera: VMSCamera? = null
	private var archiveRanges: ArrayList<VMSArchiveRange>? = ArrayList()
	private var archiveMarks: ArrayList<VMSEvent>? = ArrayList()
	lateinit var compositeDisposable: CompositeDisposable
	private var calRangeCurr = Calendar.getInstance(Locale(settings.getChosenLanguage()))
	private var downClickX = 0f //position x of action down
	private var downClickY = 0f //position y of action down
	private var isLongClick = false //if long click can happened
	private var isLongClickShown = false    //if long click already shown and handled
	private val aggregateArray = mutableListOf<Float>() //for store aggregate marks
	private val minSpacingMarks = 27.toPx() //minimum spacing between two marks to draw it separate
	private var currentMinSpacingMarks =
		minSpacingMarks.toFloat()  //current minimum spacing between overlap marks
	private val clickPixelsBorder = 10   //to have an area for better long click
	private var firstScroll = true  //detect first during scroll state
	private val delayTouch = 100
	private var lastTouchDuringScroll =
		-1.0f   //for avoid vibrate when scroll holding finger in one place
	var canSwipeTimeline = true
	private var chosenEventsFilter = arrayListOf<String>()
	var markFrom: String? = ""
	var lastAvailableTime = Calendar.getInstance()
	private var streamStatus = "";

	init {
		paint.color = -1
		paint.flags = 1
		activeStream = Paint()
		activeStream.color = ContextCompat.getColor(context, color.bg_stream)
		inactiveStream = Paint(0)
		inactiveStream.color = ContextCompat.getColor(context, color.txt_gray_light)
		marksPaint = Paint()
		secondScales = longArrayOf(5, 60, 300, 900, 1800, 3600)
		millisPerStep = if (secondScales.isNotEmpty()) secondScales[secondScales.size - 1] * 1000L else 3600 * 1000L
		setEventsFilters()
	}

	// here init in fragment
	fun init(
		serverDateTimeCallback: ArchiveTimeCallback,
		c: VMSCamera,
		archiveRanges: ArrayList<VMSArchiveRange>,
		cD: CompositeDisposable,
		lastAvailableTime: Calendar
	) {
		drawCalendar = Calendar.getInstance(Locale(settings.getChosenLanguage()))
		calRangeCurr = Calendar.getInstance(Locale(settings.getChosenLanguage()))

		hhmmFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
		compositeDisposable = cD
		serverTimeZoneOffset = TimeZone.getDefault().getOffset(Date().time).toInt()
		callbackDateTime = serverDateTimeCallback
		camera = c
		this.archiveRanges = archiveRanges
		if (!markFrom.isNullOrEmpty()) {
			millisPerStep = secondScales[1] * 1000L
		}
		val stream = camera?.streams?.filter { it.type == HIGH } as ArrayList<VMSStream>
		streamStatus = stream.first().status ?: ""
		this.lastAvailableTime = lastAvailableTime
		invalidate()
	}

	private fun setEventsFilters() {
		chosenEventsFilter = ArrayList(settings.chosenEventsTypes.map { it.first })
	}

	fun setMarks(list: List<VMSEvent>) {
		logSdk("TAG", "**** setMarks list.size = ${list.size}")
		archiveMarks?.clear()
		archiveMarks?.addAll(ArrayList(list))
		setEventsFilters()
	}

	fun getLeftTimelineCalendar(): Calendar? = leftDate

	fun getLeftTimelineDate(): String? =
		if (getLeftTimelineCalendar() != null) setCalendarServer(getLeftTimelineCalendar()!!) else null

	fun getRightTimelineCalendar(): Calendar? {
		return if (leftDate != null) {
			Calendar.getInstance(Locale(settings.getChosenLanguage())).apply {
				timeInMillis =
					Date(leftDate!!.timeInMillis + (bitmap!!.width.toFloat() / pixelsPerStep * millisPerStep.toFloat()).toLong()).time +
						1000 * 60 * 60  //add 1h to get marks after current end timeline to have opportunity to show them while timeline goes itself
			}
		} else {
			null
		}
	}

	fun getRightTimelineDate(): String? =
		if (getRightTimelineCalendar() != null) setCalendarServer(getRightTimelineCalendar()!!) else null

	// handle touches of user
	override fun onTouchEvent(event: MotionEvent): Boolean {
		try {
			super.onTouchEvent(event)
			val x = event.x
			val y = event.y
			if (leftDate != null) {
				val timeCursorInCenter = leftDate!!.timeInMillis + (bitmap!!.width.toFloat()
					.toDouble() / 2.0 / pixelsPerStep.toDouble() * millisPerStep.toDouble()).toLong() //  del 2.0d - it set to center position of cursor
				val calendarInCenter = Calendar.getInstance(Locale(settings.getChosenLanguage()))
				calendarInCenter.timeInMillis = timeCursorInCenter
				event.action
				if (event.pointerCount != 2) {
					if (canSwipeTimeline) {
						when (event.action and 255) {
							// when touch start
							0 -> {
								isLongClick = true
								isLongClickShown = false
								firstScroll = true
								// Log.d("case0", "leftDate=" + leftDate!!.time)
								this.lastTouchTime =
									Calendar.getInstance(Locale(settings.getChosenLanguage())).timeInMillis
								downClickX = x
								downClickY = y
								lastTouchDuringScroll = bitmap!!.width / 2.0f   //center of view
								Handler(Looper.getMainLooper()).postDelayed({
									//check that there was no lost touch or scroll or not zoom started (isLongClick = true)
									try {
										if (leftDate != null && isLongClick && y <= (bitmap!!.height - streamBottomBlockHeight).toFloat() && !zoomStarted) {
											val dateEnd =
												Date(leftDate!!.timeInMillis + (bitmap!!.width.toFloat() / pixelsPerStep * millisPerStep.toFloat()).toLong())
											val length = dateEnd.time - leftDate!!.timeInMillis
											archiveMarks?.let { arrayMarks ->
												if (arrayMarks.isNotEmpty() && settings.hasEventsToShow()) {   //if have marks && need display it
													val arrayFiltered =
														(if (chosenEventsFilter.contains(EVENTS_SHOW_ALL)) arrayMarks
														else arrayMarks.filter {
															chosenEventsFilter.contains(
																it.type
															)
														}) as ArrayList<VMSEvent>
													logSdk(
														"TAG",
														"arrayFiltered - touch start = ${arrayFiltered.size}"
													)

													loop@ for ((index, mark) in arrayFiltered.reversed()
														.withIndex()) { //reversed order for handle newer marks first
														try {   //use try for mark.getFromLocal() exist
															val timeStart =
																setCalendarByDateServer(mark.getFromLocal()).timeInMillis
															if (timeStart >= leftDate!!.timeInMillis && timeStart <= dateEnd.time) {    //if mark gets into current timeline
																val timeStartX =
																	(timeStart - leftDate!!.timeInMillis) * bitmap!!.width.toFloat() / length  //get x coordinate of mark for current timeline
																if (x >= timeStartX - 15.toPx() && x <= timeStartX + 15.toPx()) {   //15dp in both sides for set paddings
																	isLongClickShown =
																		true //prevent handle single click
																	//check if it separate mark or aggregate mark (check previous and next marks in archiveMarks)
																	var firstMarkAggregateX =
																		timeStartX    //start x of aggregate array
																	var lastMarkAggregateX =
																		timeStartX    //end x of aggregate array
																	val indexInArray =
																		arrayFiltered.lastIndex - index //get index for arrayList (because we on reverse loop)
																	if (indexInArray + 1 <= arrayFiltered.lastIndex) { //check next marks in array marks
																		for (item in indexInArray + 1..arrayFiltered.lastIndex) {
																			val nextMarkX =
																				checkMarksOverlap(
																					arrayFiltered,
																					item,
																					dateEnd,
																					length,
																					lastMarkAggregateX
																				)    //check mark x coordinate
																			if (nextMarkX != lastMarkAggregateX) {  //if marks overlap
																				val dif = max(
																					nextMarkX,
																					lastMarkAggregateX
																				) - min(
																					nextMarkX,
																					lastMarkAggregateX
																				)   //difference between 2 nearest marks
																				if (dif < currentMinSpacingMarks) currentMinSpacingMarks =
																					dif  //find minimal difference from all overlap marks
																				lastMarkAggregateX =
																					nextMarkX  //set new end x of aggregate array
																			} else {
																				break   //stop loop
																			}
																		}
																	}
																	if (indexInArray - 1 >= 0) {    //check previous marks in array marks
																		for (item in indexInArray - 1 downTo 0) {
																			val previousMarkX =
																				checkMarksOverlap(
																					arrayFiltered,
																					item,
																					dateEnd,
																					length,
																					firstMarkAggregateX
																				)    //check mark x coordinate
																			if (previousMarkX != firstMarkAggregateX) {  //if marks overlap
																				val dif = max(
																					previousMarkX,
																					firstMarkAggregateX
																				) - min(
																					previousMarkX,
																					firstMarkAggregateX
																				)   //difference between 2 nearest marks
																				if (dif < currentMinSpacingMarks) currentMinSpacingMarks =
																					dif  //find minimal difference from all overlap marks
																				firstMarkAggregateX =
																					previousMarkX  //set new start x of aggregate array
																			} else {
																				break   //stop loop
																			}
																		}
																	}
																	val activeSecondScaleIndex =
																		getActiveScaleIndex()
																	if ((firstMarkAggregateX == timeStartX && lastMarkAggregateX == timeStartX) || activeSecondScaleIndex == 0 || getActiveScaleIndex() == 1) {
																		//if separate mark or already in high zoom
																		callbackDateTime!!.onLongClickMark(
																			mark,
																			timeStartX,
																			bitmap!!.width.toFloat()
																		)  //long click on separate mark
																	} else {
																		zoomIn(
																			activeSecondScaleIndex,
																			firstMarkAggregateX,
																			lastMarkAggregateX,
																			length
																		)  //zoom in
																	}
																	break@loop
																}
															}
														} catch (e: Exception) {
															e.message
														}
													}
												}
											}
										}
										if (leftDate != null && isLongClick && y <= bitmap!!.height && !isLongClickShown && !zoomStarted && !VMSPlayerFragment.isCreateMarkViewShown) {
											val dateClick =
												Date(leftDate!!.timeInMillis + (x / pixelsPerStep * millisPerStep.toFloat()).toLong())
											val calendarClick =
												Calendar.getInstance(Locale(settings.getChosenLanguage()))
													.apply { timeInMillis = dateClick.time }
											isLongClickShown = true
											callbackDateTime!!.onLongClick(x, calendarClick)
										}
									} catch (e: Exception) {
										e.printStackTrace()
									}
								}, LONG_CLICK_TIMEOUT)
							}
							// when touch lost
							1 -> {
								if (!isLongClickShown) {    //handle lost touch only if long click wasn't happen
									isLongClick = false
									callbackDateTime!!.previousNavigatedMarkFrom = null
									callbackDateTime!!.previousNavigatedMarkTime = null
									logSdk("TAG", "previousNavigatedMarkFrom = null}")
									if (Calendar.getInstance(Locale(settings.getChosenLanguage())).timeInMillis - this.lastTouchTime >= delayTouch && moveStarted) {
										callbackDateTime!!.startHandlerAndChangeDateArchive(
											calendarInCenter
										)
										callbackDateTime!!.playCameraArchive()
										updateTimelineMoved()
										invalidate()
									}
									moveStarted = false
									zoomStarted = false
									if (x >= downClickX - clickPixelsBorder && x <= downClickX + clickPixelsBorder
										&& y >= downClickY - clickPixelsBorder && y <= downClickY + clickPixelsBorder
										&& y <= (bitmap!!.height - streamBottomBlockHeight).toFloat()
									) {    //check if it's a single click
										val dateEnd =
											Date(leftDate!!.timeInMillis + (bitmap!!.width.toFloat() / pixelsPerStep * millisPerStep.toFloat()).toLong())
										val length = dateEnd.time - leftDate!!.timeInMillis
										archiveMarks?.let { arrayMarks ->
											if (arrayMarks.isNotEmpty() && settings.hasEventsToShow()) {   //if have marks && need display it
												val arrayFiltered =
													(if (chosenEventsFilter.contains(EVENTS_SHOW_ALL)) arrayMarks
													else arrayMarks.filter {
														chosenEventsFilter.contains(
															it.type
														)
													}) as ArrayList<VMSEvent>
												loop@ for ((index, mark) in arrayFiltered.reversed()
													.withIndex()) { //reversed order for handle newer marks first
													try {   //use try for mark.getFromLocal() exist
														val timeStart =
															setCalendarByDateServer(mark.getFromLocal()).timeInMillis
														if (timeStart >= leftDate!!.timeInMillis && timeStart <= dateEnd.time) {    //if mark gets into current timeline
															val timeStartX =
																(timeStart - leftDate!!.timeInMillis) * bitmap!!.width.toFloat() / length  //get x coordinate of mark for current timeline
															if (x >= timeStartX - 15.toPx() && x <= timeStartX + 15.toPx()) {   //15dp in both sides for set paddings
																//check if it separate mark or aggregate mark (check previous and next marks in archiveMarks)
																var firstMarkAggregateX =
																	timeStartX    //start x of aggregate array
																var lastMarkAggregateX =
																	timeStartX    //end x of aggregate array
																val indexInArray =
																	arrayFiltered.lastIndex - index //get index for arrayFiltered (because we on reverse loop)
																if (indexInArray + 1 <= arrayFiltered.lastIndex) { //check next marks in array marks
																	for (item in indexInArray + 1..arrayFiltered.lastIndex) {
																		val nextMarkX =
																			checkMarksOverlap(
																				arrayFiltered,
																				item,
																				dateEnd,
																				length,
																				lastMarkAggregateX
																			)    //check mark x coordinate
																		if (nextMarkX != lastMarkAggregateX) {  //if marks overlap
																			val dif = max(
																				nextMarkX,
																				lastMarkAggregateX
																			) - min(
																				nextMarkX,
																				lastMarkAggregateX
																			)   //difference between 2 nearest marks
																			if (dif < currentMinSpacingMarks) currentMinSpacingMarks =
																				dif  //find minimal difference from all overlap marks
																			lastMarkAggregateX =
																				nextMarkX  //set new end x of aggregate array
																		} else {
																			break   //stop loop
																		}
																	}
																}
																if (indexInArray - 1 >= 0) {    //check previous marks in array marks
																	for (item in indexInArray - 1 downTo 0) {
																		val previousMarkX =
																			checkMarksOverlap(
																				arrayFiltered,
																				item,
																				dateEnd,
																				length,
																				firstMarkAggregateX
																			)    //check mark x coordinate
																		if (previousMarkX != firstMarkAggregateX) {  //if marks overlap
																			val dif = max(
																				previousMarkX,
																				firstMarkAggregateX
																			) - min(
																				previousMarkX,
																				firstMarkAggregateX
																			)   //difference between 2 nearest marks
																			if (dif < currentMinSpacingMarks) currentMinSpacingMarks =
																				dif  //find minimal difference from all overlap marks
																			firstMarkAggregateX =
																				previousMarkX  //set new start x of aggregate array
																		} else {
																			break   //stop loop
																		}
																	}
																}
																val activeSecondScaleIndex =
																	getActiveScaleIndex()
																if ((firstMarkAggregateX == timeStartX && lastMarkAggregateX == timeStartX) || activeSecondScaleIndex == 0 || getActiveScaleIndex() == 1) {
																	//if separate mark or already in high zoom
																	callbackDateTime!!.clickMark(
																		mark
																	)  //click on separate mark
																} else {
																	logSdk(
																		"TAG",
																		"activeSecondScaleIndex $activeSecondScaleIndex firstMarkAggregateX = $firstMarkAggregateX lastMarkAggregateX = $lastMarkAggregateX length = $length"
																	)
																	zoomIn(
																		activeSecondScaleIndex,
																		firstMarkAggregateX,
																		lastMarkAggregateX,
																		length
																	)  //zoom in
																}
																break@loop
															}
														}
													} catch (e: Exception) {
														e.message
													}
												}
											}
										}
									}
								}
								isLongClickShown = false
							}
							// during scroll
							2 -> {
								val startTime =
									setCalendarByDateServer(camera!!.getStartAtLocal()).timeInMillis
								if (startTime > calendarInCenter.timeInMillis) {
									//                                Log.d("case2", "break cause start")
									setCursor(setCalendarByDateServer(camera!!.getStartAtLocal()))
									return false
								} else if (lastAvailableTime.timeInMillis < calendarInCenter.timeInMillis) {
									// Log.d("case2", "break cause end")
									setCursor(calendarInCenter)
									return false
								} else if (!zoomStarted) {
									val x1 = event.x - lastTouchX
									lastTouchX = event.x
									if ((Calendar.getInstance().timeInMillis - this.lastTouchTime > delayTouch) //delay before handle scroll (not less then in lost touch state)
										&& !firstScroll    //don't set left date on first scroll to write correct value into lastTouchX
										&& (event.x > downClickX + clickPixelsBorder || event.x < downClickX - clickPixelsBorder
												|| event.y > downClickY + clickPixelsBorder || event.y < downClickY - clickPixelsBorder)
									) {
										// Log.d("case2", "inner=" + leftDate!!.time)
										isLongClick = false
										isLongClickShown = false
										moveStarted = true
										setLeftDate(leftDate!!.timeInMillis - (x1 / pixelsPerStep * millisPerStep.toFloat()).toLong())
										updateTimelineMoved()
										callbackDateTime!!.stopTimeArchive(isShowTime = true)
										callbackDateTime!!.slideTimeLineAndPassDateArchive(
											calendarInCenter
										)
										calendarCursor = calendarInCenter
										invalidate()

										if (event.x - lastTouchDuringScroll > pixelsPerStep || lastTouchDuringScroll - event.x > pixelsPerStep) {
											lastTouchDuringScroll =
												event.x //save last x during scroll
											callbackDateTime?.vibrate(pixelsPerStep.toLong()) // vibrate like spinner instead AK-47
										}
									} else {
										//do nothing (it's just click on timeline)
										// Log.d("case2", "outer=" + leftDate!!.time)
									}
									firstScroll = false
								}
							}
						}
					}
				} else {
					// call if start zoom in-out
					this.scaleGestureDetector.onTouchEvent(event)
				}
			}
		} catch (e: Exception) {
			e.printStackTrace()
		}
		return true
	}

	// draw all time
	public override fun onDraw(c: Canvas) {
		try {
			if (minPixelsPerStep == 0.0f) {
				val h = height
				if (h > 0) {
					minPixelsPerStep = h.toFloat() * 0.15f
					maxPixelsPerStep = h.toFloat() * 0.25f
					val sizeOfText = h.toFloat() * 0.25f
					paint.textSize = sizeOfText // size text for time
					streamBottomBlockHeight = 8.toPx()
					pixelsPerStep = maxPixelsPerStep
				}
			}
			canvas!!.drawColor(0, Mode.CLEAR)
			if (leftDate == null) return
			// draw stream by date
			val dateEnd =
				Date(leftDate!!.timeInMillis + (bitmap!!.width.toFloat() / pixelsPerStep * millisPerStep.toFloat()).toLong()) // date of end timeline
			val length = dateEnd.time - leftDate!!.timeInMillis
			val timeStartFull: Long = archiveRanges!![0].from.toLong() * 1000L
			val durationFull = lastAvailableTime.timeInMillis - timeStartFull
			val floatLeftFull =
				(timeStartFull - leftDate!!.timeInMillis).toFloat() / length.toFloat() * bitmap!!.width.toFloat() // all ok
			val floatRightFull =
				((timeStartFull - leftDate!!.timeInMillis).toFloat() + durationFull.toFloat()) / length.toFloat() * bitmap!!.width.toFloat() // all ok
			if (streamStatus == "active" && camera?.isRestrictedArchive != true) {
				// Log.d("drawRect", "$status")
				// draw full stream
				canvas!!.drawRect(
					floatRightFull,
					bitmap!!.height.toFloat(),
					floatLeftFull,
					(bitmap!!.height - streamBottomBlockHeight).toFloat(),
					activeStream
				) // draw all background of all stream
			} else {
				// draw only active stream
				// Log.d("drawRect", "$status")
				for (i in archiveRanges!!.indices) {
					calRangeCurr.timeInMillis =
						(archiveRanges!![i].from + archiveRanges!![i].duration) * 1000L
					val timeStart =
						calRangeCurr.timeInMillis - (calRangeCurr.timeInMillis - setCalendarByDateServer(
							camera!!.getStartAtLocal()
						).timeInMillis)
					val duration = calRangeCurr.timeInMillis - timeStart
					val floatLeft =
						(timeStart - leftDate!!.timeInMillis).toFloat() / length.toFloat() * bitmap!!.width.toFloat()
					val floatRight =
						((timeStart - leftDate!!.timeInMillis).toFloat() + duration.toFloat()) / length.toFloat() * bitmap!!.width.toFloat()
//                    listActionsResult.add { canvas!!.drawRect(floatRight, bitmap!!.height.toFloat(), floatLeft, (bitmap!!.height - streamBottomBlockHeight).toFloat(), activeStream) }
					canvas!!.drawRect(
						floatRight,
						bitmap!!.height.toFloat(),
						floatLeft,
						(bitmap!!.height - streamBottomBlockHeight).toFloat(),
						activeStream
					)
					// draw last minutes, before live stream
					if (i == archiveRanges!!.size - 1) {
						val timeStartLast = calRangeCurr.timeInMillis
						val durationLast = lastAvailableTime.timeInMillis - timeStartLast
						val floatLeftLast =
							(timeStartLast - leftDate!!.timeInMillis).toFloat() / length.toFloat() * bitmap!!.width.toFloat()
						val floatRightLast =
							((timeStartLast - leftDate!!.timeInMillis).toFloat() + durationLast.toFloat()) / length.toFloat() * bitmap!!.width.toFloat()

						if (camera != null && camera!!.status != null && camera!!.status == "active") {
							canvas!!.drawRect(
								floatRightLast,
								bitmap!!.height.toFloat(),
								floatLeftLast,
								(bitmap!!.height - streamBottomBlockHeight).toFloat(),
								activeStream
							)
						} else {
							canvas!!.drawRect(
								floatRightLast,
								bitmap!!.height.toFloat(),
								floatLeftLast,
								(bitmap!!.height - streamBottomBlockHeight).toFloat(),
								inactiveStream
							)
						}
					}
				}

			}
			// draw only inactive stream
			if (archiveRanges!!.isNotEmpty() && archiveRanges!!.size > 1) {
				for (i in archiveRanges!!.indices) {
					if (i != 0) {
						val rangePr =
							(archiveRanges!![i - 1].from + archiveRanges!![i - 1].duration) * 1000L
						val rangeCu = archiveRanges!![i].from * 1000L
						val floatL =
							(rangePr - leftDate!!.timeInMillis).toFloat() / length.toFloat() * bitmap!!.width.toFloat()
						val floatR =
							(rangeCu - leftDate!!.timeInMillis).toFloat() / length.toFloat() * bitmap!!.width.toFloat()

						canvas!!.drawRect(
							floatR,
							bitmap!!.height.toFloat(),
							floatL,
							(bitmap!!.height - streamBottomBlockHeight).toFloat(),
							inactiveStream
						)
					}
				}
			}
			//draw marks
			archiveMarks?.let { arrayMarks ->
				if (arrayMarks.isNotEmpty() && settings.hasEventsToShow()) {   //if have marks && need display it
					aggregateArray.clear()
					val arrayFiltered =
						(if (chosenEventsFilter.contains(EVENTS_SHOW_ALL)) arrayMarks
						else arrayMarks.filter { chosenEventsFilter.contains(it.type) }) as ArrayList<VMSEvent>

					for ((index, mark) in arrayFiltered.withIndex()) {
						mark.getFromLocal().let { timeMark ->
							//if have time from in mark
							val timeStart = setCalendarByDateServer(timeMark).timeInMillis
							val cameraStartAt =
								setCalendarByDateServer(camera!!.getStartAtLocal()).timeInMillis
							if (timeStart >= cameraStartAt && timeStart >= leftDate!!.timeInMillis && timeStart <= dateEnd.time) {    //if mark gets into current timeline
								val floatX =
									(timeStart - leftDate!!.timeInMillis).toFloat() / length.toFloat() * bitmap!!.width.toFloat()  //x coordinate of start mark
								if (getActiveScaleIndex() == 0 || getActiveScaleIndex() == 1) {
									//draw all marks as separate at high zoom
									drawMark(floatX, mark.type ?: "mark")   //draw custom mark
								} else {
									//not max zoom -> do the algorithm
									if (aggregateArray.isEmpty() || floatX - aggregateArray.last() < minSpacingMarks) {   //if it's the first mark or marks overlap
										aggregateArray.add(floatX)  //add x coordinate to array
									} else {    //if have saved x coordinates in array and marks not overlap
										if (aggregateArray.size == 1) { //if array has 1 mark
											drawMark(
												aggregateArray.last(),
												mark.type ?: "mark"
											)   //draw custom mark
										} else {    //if array has several marks
											val centerAggregate =
												(aggregateArray.first() + aggregateArray.last()) / 2  //center of aggregate marks
											drawAggregateMark(
												centerAggregate,
												aggregateArray.size.toString()
											) //draw aggregate mark
										}
										aggregateArray.clear()  //clear array
										aggregateArray.add(floatX)  //add current x coordinate to array
									}
								}
							}
							//we draw or add to array previous mark, so we have to handle last mark separately
							//if it max g -> aggregateArray is empty -> next block doesn't draw anything
							if (index == arrayFiltered.lastIndex) {
								if (aggregateArray.size == 1) { //if array has 1 mark
									drawMark(
										aggregateArray.last(),
										mark.type ?: "mark"
									)   //draw custom mark
								} else if (aggregateArray.isNotEmpty()) {    //if array has several marks
									val centerAggregate =
										(aggregateArray.first() + aggregateArray.last()) / 2  //center of aggregate marks
									drawAggregateMark(
										centerAggregate,
										aggregateArray.size.toString()
									) //draw aggregate mark
								}
							}
						}
					}
				}
			}

			if (floatLeftFull > 0f) {
				canvas!!.drawRect(
					0f,
					(bitmap!!.height - streamBottomBlockHeight).toFloat(),
					floatLeftFull,
					bitmap!!.height.toFloat(),
					inactiveStream
				)  //white rect between timeline and buttons before start
			}
			if (floatRightFull < bitmap!!.width.toFloat()) {
				canvas!!.drawRect(
					floatRightFull,
					(bitmap!!.height - streamBottomBlockHeight).toFloat(),
					bitmap!!.width.toFloat(),
					bitmap!!.height.toFloat(),
					inactiveStream
				)  //white rect between timeline and buttons after end
			}
			val ms = millisPerStep - leftDate!!.timeInMillis % millisPerStep
			drawCalendar.timeInMillis = leftDate!!.timeInMillis + ms
			//                // Log.d("leftDate", "" + leftDate.getTime());
			//                // Log.d("drawCale", "" + drawCalendar.getTime());
			drawDays.clear()
			val item = ArrayList<Any>()
			item.add(0.0f)
			item.add(getVerboseDay(drawCalendar.time, serverTimeZoneOffset))
			item.add(drawCalendar.get(Calendar.DAY_OF_YEAR).toFloat())
			drawDays.add(item)
			// job to draw specific sections on timeline
			var x =
				floor((ms.toFloat() / millisPerStep.toFloat() * pixelsPerStep).toDouble()).toFloat()
			while (x < bitmap!!.width.toFloat()) {
				var lineHeight = this.minPixelsPerStep
				var lineText: String? = null
				val seconds = drawCalendar.get(Calendar.SECOND)
				val minutes = drawCalendar.get(Calendar.MINUTE)
				val hours = drawCalendar.get(Calendar.HOUR_OF_DAY)
				if (millisPerStep == 5000L) {
					if (seconds % 60 == 0) {
						lineHeight *= 2.0f
						lineText = this.hhmmFormat.format(drawCalendar.time)
					} else if (seconds % 15 == 0) {
						lineHeight = (lineHeight.toDouble() * 1.5).toInt().toFloat()
					}
				} else if (millisPerStep == 60000L) {
					if (seconds == 0) {
						if (minutes == 0) {
							lineHeight *= 2.0f
							lineText = this.hhmmFormat.format(drawCalendar.time)
						} else if (minutes % 15 == 0) {
							lineHeight = (lineHeight.toDouble() * 1.5).toInt().toFloat()
							lineText = this.hhmmFormat.format(drawCalendar.time)
						}
					}
				} else if (millisPerStep == 300000L) {
					if (seconds == 0) {
						if (minutes == 0) {
							lineHeight *= 2.0f
							lineText = this.hhmmFormat.format(drawCalendar.time)
						} else if (minutes % 15 == 0) {
							lineHeight = (lineHeight.toDouble() * 1.5).toInt().toFloat()
						}
					}
				} else if (millisPerStep == 900000L) {
					if (seconds == 0) {
						if (minutes == 0 && hours % 3 == 0) {
							lineHeight *= 2.0f
							lineText = this.hhmmFormat.format(drawCalendar.time)
						} else if (minutes % 30 == 0) {
							lineHeight = (lineHeight.toDouble() * 1.5).toInt().toFloat()
						}
					}
				} else if (millisPerStep == 1800000L && seconds == 0) {
					if (hours == 0 && minutes == 0) {
						lineHeight *= 2.0f
						lineText = this.hhmmFormat.format(drawCalendar.time)
					} else if (minutes == 0) {
						lineHeight = (lineHeight.toDouble() * 1.5).toInt().toFloat()
					}
				} else if (millisPerStep == 3600000L && seconds == 0) {
					if (hours == 0) {
						lineHeight *= 2.4f
						lineText = hhmmFormat.format(drawCalendar.time)
					}
				}
				// draw main table of timeline
				if (lineHeight > 0.0f) {
					paint.color = getColorCompat(
						context,
						color.white_line
					)   //draw timeline table with alpha 50%
					canvas!!.drawLine(
						x,
						(bitmap!!.height - streamBottomBlockHeight).toFloat() - lineHeight,
						x,
						(bitmap!!.height - streamBottomBlockHeight).toFloat(),
						paint
					) // draw vertical lines
					paint.color = -1
					if (lineText != null && lineText.isNotEmpty()) {
						canvas!!.drawText(
							lineText,
							x - this.paint.measureText(lineText) / 2.0f,
							(bitmap!!.height - streamBottomBlockHeight).toFloat() - lineHeight - 10.0f,
							paint
						)
					}
				}
				if (seconds == 0 && minutes == 0 && hours == 0) {
					val item2 = ArrayList<Any>()
					item2.add(x)
					item2.add(getVerboseDay(drawCalendar.time, serverTimeZoneOffset))
					item2.add(drawCalendar.get(Calendar.DAY_OF_YEAR).toFloat())
					drawDays.add(item2)
				}
				x += pixelsPerStep
				drawCalendar.timeInMillis = drawCalendar.timeInMillis + millisPerStep
			}
			// set days of months
			if (drawDays.size > 0 && (millisPerStep == 3600000L || millisPerStep == 1800000L)) {
				paint.color = getColorCompat(context, color.white_70)
				var i2 = 0
				while (i2 < drawDays.size) {
					val item3 = drawDays[i2]
					val x2 = item3[0] as Float
					val nextX =
						if (i2 < drawDays.size + -1) drawDays[i2 + 1][0] as Float else bitmap!!.width.toFloat()
					val text = item3[1] as String
					val textWidth = paint.measureText(text)
					if (nextX - x2 > 20.0f + textWidth) {
						val heightMargin =
							if (millisPerStep == 3600000L || millisPerStep == 1800000L) 27.0f else 30.0f
						canvas!!.drawText(
							text,
							(nextX - x2) / 2.0f + x2 - textWidth / 2.0f,
							bitmap!!.height.toFloat() - streamBottomBlockHeight.toFloat() - heightMargin - 5f,
							paint
						)
					}
					i2++
				}
			}
			c.drawBitmap(
				bitmap!!,
				Rect(0, 0, bitmap!!.width, bitmap!!.height),
				Rect(0, 0, bitmap!!.width, bitmap!!.height),
				null
			) // finally draw in canvas
		} catch (e: Exception) {
			e.message
		}

	}

	public override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
		try {
			if (bitmap != null) bitmap!!.recycle()
			this.canvas = Canvas()
			bitmap = Bitmap.createBitmap(w, h, Config.ARGB_8888)
			this.canvas!!.setBitmap(bitmap)
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}

	fun destroy() {
		if (bitmap != null) {
			bitmap!!.recycle()
		}
	}

	// set date in center of timeline
	fun setCursor(calendarCursor: Calendar?) {
		try {
			if (calendarCursor == null) return
//            logsdk ("TAG","*** calendarCursor = ${calendarCursor?.time}")
			var needCut = false
			this.calendarCursor =
				if (lastAvailableTime.timeInMillis < calendarCursor.timeInMillis) {
					needCut = true
					lastAvailableTime
				} else {
					calendarCursor
				}
			//        // Log.d("getTimeCursor", "before=" + this.cursorDate.getTime());
			if (!timelineMoved || needCut) {
				if (leftDate == null && bitmap != null || needCut) {
					leftDate = Calendar.getInstance(Locale(settings.getChosenLanguage()))
					if (pixelsPerStep == 0f) {
						//for set from update mark, avoid division by 0
						pixelsPerStep = bitmap!!.height.toFloat() * 0.25f
					}
					val leftTimeCursor =
						this.calendarCursor!!.timeInMillis - (bitmap!!.width.toFloat()
							.toDouble() / 2.0 / pixelsPerStep.toDouble() * millisPerStep.toDouble()).toLong() //  del 2.0d - it set to center position of cursor
					setLeftDate(leftTimeCursor)
					//                // Log.d("getTimeNull", "after=" + leftDate.getTime());
				}
			} else {
				leftDate!!.timeInMillis = leftDate!!.timeInMillis + 1000
			}
			updateTimelineMoved()
			invalidate() // for update all timeline lifecycle
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}

	fun setCursorByClickMoveButton(calendar: Calendar?) {
		try {
			if (calendar == null || bitmap == null) return
			logSdk("TAG", "temp setCursorByClickMoveButton1 = ${calendarCursor?.time}")
			this.calendarCursor = calendar
			logSdk("TAG", "temp setCursorByClickMoveButton2 = ${calendarCursor?.time}")
			val leftTimeCursor = calendar.timeInMillis - (bitmap!!.width.toFloat()
				.toDouble() / 2.0 / pixelsPerStep.toDouble() * millisPerStep.toDouble()).toLong() //  del 2.0d - it set to center position of cursor
			setLeftDate(leftTimeCursor)
			updateTimelineMoved()
			invalidate() // for update all timeline lifecycle
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}

	fun updateTimelineMoved() {
		try {
			if (leftDate != null) {
				val centerTimeCursor = leftDate!!.timeInMillis - (bitmap!!.width.toFloat()
					.toDouble() / pixelsPerStep.toDouble() * millisPerStep.toDouble()).toLong() //  del 2.0d - it set to center position of cursor
				timelineMoved = leftDate!!.timeInMillis > centerTimeCursor
			}
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}

	fun clear() {
		leftDate = null
		timelineMoved = false
		zoomStarted = false
		moveStarted = false
		invalidate()
	}

	fun setCorrectLeftDate(correctDate: Calendar) {
		try {
			if (bitmap == null) return
			val leftTimeCursor = correctDate.timeInMillis - (bitmap!!.width.toFloat() // todo - check java.lang.NullPointerException
				.toDouble() / 2.0 / pixelsPerStep.toDouble() * millisPerStep.toDouble()).toLong() //  del 2.0d - it set to center position of cursor
			setLeftDate(leftTimeCursor)
		} catch (e: Throwable) {
			e.printStackTrace()
		}
	}

	fun setLeftDate(time: Long) {
		if (leftDate != null && leftDate!!.timeInMillis != time) {
			leftDate!!.timeInMillis = time
		}
	}

	// here handle zoom timeline
	private inner class ScaleListener: SimpleOnScaleGestureListener() {

		override fun onScale(detector: ScaleGestureDetector): Boolean {
			if (!detector.isInProgress) {
				return true
			}
			zoomStarted = true
			var pixels = pixelsPerStep * detector.scaleFactor
			val activeSecondScaleIndex = getActiveScaleIndex()
			if (pixels < minPixelsPerStep) {
				if (activeSecondScaleIndex < secondScales.size - 1) {
					pixels = maxPixelsPerStep
					millisPerStep = secondScales[activeSecondScaleIndex + 1] * 1000L
				} else {
					pixels = minPixelsPerStep
				}
			} else if (pixels > maxPixelsPerStep) {
				if (activeSecondScaleIndex > 0) {
					pixels = minPixelsPerStep
					millisPerStep = secondScales[activeSecondScaleIndex - 1] * 1000L
				} else {
					pixels = maxPixelsPerStep
				}
			}
			pixelsPerStep = pixels
			calendarCursor?.let {
				val leftTimeCursor = it.timeInMillis - (bitmap!!.width.toFloat()
					.toDouble() / 2.0 / pixelsPerStep.toDouble() * millisPerStep.toDouble()).toLong() //  del 2.0d - it set to center position of cursor
				setLeftDate(leftTimeCursor)
				updateTimelineMoved()
				invalidate()
			}
			if (getActiveScaleIndex() > activeSecondScaleIndex) {
				// zoom out for at least one step
				callbackDateTime?.zoomOut()
			}
			return true
		}
	}

	companion object {

		fun getVerboseDay(day: Date, timeZoneOffset: Int): String {
			var res = ""
			try {
				val timeZoneNames = TimeZone.getAvailableIDs(timeZoneOffset * 1000)
				val format = SimpleDateFormat("d", Locale.getDefault())
				if (timeZoneNames != null) {
					try {
						if (timeZoneNames.isNotEmpty()) {
							format.timeZone = TimeZone.getTimeZone(timeZoneNames[0])
						}
					} catch (e: Exception) {
//                        val simpleDateFormat = format // test
						return res
					}

				}
				res = format.format(day)
				val format2 = SimpleDateFormat("M", Locale.getDefault())
				if (timeZoneNames != null) {
					if (timeZoneNames.isNotEmpty()) {
						format2.timeZone = TimeZone.getTimeZone(timeZoneNames[0])
					}
				}
				val m = Integer.valueOf(Integer.parseInt(format2.format(day)))
				val res2 = StringBuilder(res).append(" ").toString()
				return when (m) {
					1 -> StringBuilder(res2).append(getStringForLayoutByKey("january")).toString()
					2 -> StringBuilder(res2).append(getStringForLayoutByKey("february")).toString()
					3 -> StringBuilder(res2).append(getStringForLayoutByKey("march")).toString()
					4 -> StringBuilder(res2).append(getStringForLayoutByKey("april")).toString()
					5 -> StringBuilder(res2).append(getStringForLayoutByKey("may")).toString()
					6 -> StringBuilder(res2).append(getStringForLayoutByKey("june")).toString()
					7 -> StringBuilder(res2).append(getStringForLayoutByKey("july")).toString()
					8 -> StringBuilder(res2).append(getStringForLayoutByKey("august")).toString()
					9 -> StringBuilder(res2).append(getStringForLayoutByKey("september")).toString()
					10 -> StringBuilder(res2).append(getStringForLayoutByKey("october")).toString()
					11 -> StringBuilder(res2).append(getStringForLayoutByKey("november")).toString()
					12 -> StringBuilder(res2).append(getStringForLayoutByKey("december")).toString()
					else -> res2
				}
			} catch (e2: Exception) {
				return res
			}
		}

	}

	private fun drawMark(floatX: Float, type: String) {
		val centerY =
			(bitmap!!.height - streamBottomBlockHeight).toFloat() / 2 //y coordinate of center view

		val color = when (type) {
			"mark" -> color.colorPrimary
			else -> color._2CA329
		}
		marksPaint.color = getColorCompat(context, color)
		marksPaint.strokeWidth = centerY / 2
		marksPaint.style = Paint.Style.STROKE
		marksPaint.isAntiAlias = true
		canvas!!.drawCircle(
			floatX,
			centerY - 2f,
			centerY - marksPaint.strokeWidth,
			marksPaint
		) //draw main circle
		marksPaint.color = getColorCompat(context, R.color.txt_black)
		marksPaint.strokeWidth = 1f
		canvas!!.drawCircle(
			floatX,
			centerY - 2f,
			centerY - centerY / 4,
			marksPaint
		)    //draw shadow outside circle
		canvas!!.drawCircle(
			floatX,
			centerY - 2f,
			centerY - centerY * 3 / 4,
			marksPaint
		)    //draw shadow inside circle
		marksPaint.color = getColorCompat(context, color)
		marksPaint.style = Paint.Style.FILL

		path.reset()
		path.moveTo(floatX, 2 * centerY)
		path.lineTo(floatX - centerY / 2 - 1f, 2 * centerY - centerY / 2 - 1f)
		path.lineTo(floatX + centerY / 2 + 1f, 2 * centerY - centerY / 2 - 1f)
		path.lineTo(floatX, 2 * centerY)
		path.close()
		canvas!!.drawPath(path, marksPaint) //draw triangle in bottom
		marksPaint.color = getColorCompat(context, R.color.txt_black)
		marksPaint.strokeWidth = 1f
		canvas!!.drawLine(
			floatX,
			2 * centerY,
			floatX - centerY / 2 - 2f,
			2 * centerY - centerY / 2 - 1f,
			marksPaint
		) //draw triangle shadow left in bottom
		canvas!!.drawLine(
			floatX,
			2 * centerY,
			floatX + centerY / 2 + 2f,
			2 * centerY - centerY / 2 - 1f,
			marksPaint
		) //draw triangle shadow right in bottom
	}

	private fun drawAggregateMark(floatX: Float, count: String) {
		val centerY =
			(bitmap!!.height - streamBottomBlockHeight).toFloat() / 2 //y coordinate of center view
		val textHeight = 12.toPx().toFloat()

		marksPaint.color = getColorCompat(context, color._C1C1C1)
		marksPaint.strokeWidth = centerY / 2
		marksPaint.style = Paint.Style.FILL_AND_STROKE
		marksPaint.isAntiAlias = true
		canvas!!.drawCircle(
			floatX,
			centerY - 2f,
			centerY - marksPaint.strokeWidth,
			marksPaint
		) //draw main circle
		marksPaint.color = getColorCompat(context, color.txt_black)
		marksPaint.strokeWidth = 1f
		marksPaint.style = Paint.Style.STROKE
		canvas!!.drawCircle(
			floatX,
			centerY - 2f,
			centerY - centerY / 4,
			marksPaint
		)    //draw shadow outside circle
		marksPaint.color = getColorCompat(context, color._C1C1C1)
		marksPaint.style = Paint.Style.FILL

		path.reset()
		path.moveTo(floatX, 2 * centerY)
		path.lineTo(floatX - centerY / 2 - 1f, 2 * centerY - centerY / 2 - 1f)
		path.lineTo(floatX + centerY / 2 + 1f, 2 * centerY - centerY / 2 - 1f)
		path.lineTo(floatX, 2 * centerY)
		path.close()
		canvas!!.drawPath(path, marksPaint) //draw triangle in bottom
		marksPaint.color = getColorCompat(context, color.txt_black)
		marksPaint.strokeWidth = 1f
		canvas!!.drawLine(
			floatX,
			2 * centerY,
			floatX - centerY / 2 - 2f,
			2 * centerY - centerY / 2 - 1f,
			marksPaint
		) //draw triangle shadow left in bottom
		canvas!!.drawLine(
			floatX,
			2 * centerY,
			floatX + centerY / 2 + 2f,
			2 * centerY - centerY / 2 - 1f,
			marksPaint
		) //draw triangle shadow right in bottom
		marksPaint.color = getColorCompat(context, color.white)
		marksPaint.textSize = textHeight
		marksPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
		canvas!!.drawText(
			count,
			floatX - marksPaint.measureText(count) / 2,
			centerY - 2f + textHeight / 3,
			marksPaint
		)
	}

	private fun checkMarksOverlap(
		arrayMarks: ArrayList<VMSEvent>,
		checkedIndex: Int,
		dateEnd: Date,
		length: Long,
		currentStartX: Float
	): Float {
		//if marks overlap -> return next mark x coordinate, else -> return current x coordinate
		val timeStartCheck =
			setCalendarByDateServer(arrayMarks[checkedIndex].getFromLocal()).timeInMillis
		if (leftDate != null && timeStartCheck >= leftDate!!.timeInMillis && timeStartCheck <= dateEnd.time) {    //if checked mark gets into current timeline
			val timeStartCheckX =
				(timeStartCheck - leftDate!!.timeInMillis) * bitmap!!.width.toFloat() / length    //x coordinate of checked mark
			if (max(currentStartX, timeStartCheckX) - min(
					currentStartX,
					timeStartCheckX
				) < minSpacingMarks
			) return timeStartCheckX
		}
		return currentStartX
	}

	private fun getActiveScaleIndex(): Int {
		var activeSecondScaleIndex = 0
		while (activeSecondScaleIndex < secondScales.size && secondScales[activeSecondScaleIndex] * 1000L != millisPerStep) {
			activeSecondScaleIndex++
		}
		return activeSecondScaleIndex
	}

	private fun zoomIn(
		activeSecondScaleIndex: Int,
		firstMarkAggregateX: Float,
		lastMarkAggregateX: Float,
		length: Long
	) {
		val timeCenterAggregateX =
			(firstMarkAggregateX + lastMarkAggregateX) / 2f  //center x of aggregate array
		val timeCenterAggregate =
			(timeCenterAggregateX * length / bitmap!!.width.toFloat()).toLong() + leftDate!!.timeInMillis //center time of aggregate array
		val calendar = Calendar.getInstance(Locale(settings.getChosenLanguage()))
			.apply { timeInMillis = timeCenterAggregate }  //calendar for center of aggregate array
		callbackDateTime?.startHandlerAndChangeDateArchive(calendar)   //set calendar to display correct time in centre
		callbackDateTime!!.playCameraArchive() //load archive for choose calendar
		updateTimelineMoved()   //update timeline
		val minNeededZoom =
			minSpacingMarks / currentMinSpacingMarks + 0.01f    //min zoom to draw all marks in aggregate array as separates
		val halfWidth =
			bitmap!!.width.toFloat() / 2.0f - 15.toPx() //half of screen width (15dp - to have possibility to draw all mark without cut)
		val maxAvailableZoom =
			halfWidth / (lastMarkAggregateX - timeCenterAggregateX) - 0.01f  //max zoom to draw all marks in aggregate array into screen width
		val minZoomByPixels =
			minPixelsPerStep / pixelsPerStep  //min zoom only by change pixelsPerStep
		val maxZoomByPixels =
			maxPixelsPerStep / pixelsPerStep  //max zoom only by change pixelsPerStep
		val minZoomByMillis =
			secondScales[activeSecondScaleIndex] / secondScales[activeSecondScaleIndex - 1]     //min zoom only by change millisPerStep
		val maxZoomByMillis =
			secondScales[activeSecondScaleIndex] / secondScales[1]     //max zoom only by change millisPerStep
		val minZoomToNextIndex =
			minZoomByMillis * minZoomByPixels  //minimum zoom step to next index
		val maxZoom =
			maxZoomByPixels * maxZoomByMillis //max available zoom for current pixelsPerStep and millisPerStep
		var needZoom = minZoomToNextIndex    //set minimum zoom step as current
		if (minNeededZoom <= maxAvailableZoom) {
			//if min needed zoom <= max available zoom -> set min needed zoom as current
			needZoom = minNeededZoom
		} else {
			//if min needed zoom > max available zoom and max available zoom > minimum available zoom -> set max available zoom as current
			//otherwise -> minimum zoom step remain as current
			if (maxAvailableZoom > needZoom) needZoom = maxAvailableZoom
		}

		if (needZoom >= maxZoom) {
			//if need zoom > max zoom -> set max zoom (secondScalesIndex = 1 and pixelsPerStep = maxPixelsPerStep)
			//we don't aggregate marks on secondScalesIndex = 0 and 1, so we zoom only to index 1
			pixelsPerStep =
				maxPixelsPerStep    //set without animation (avoid double animation or use handler for it)
			val startMillis = millisPerStep
			val endMillis = secondScales[1] * 1000L
			ValueAnimator.ofFloat(0f, 1f).apply {
				duration = ANIM_DURATION
				addUpdateListener { update ->
					millisPerStep =
						(startMillis + (endMillis - startMillis) * (update.animatedValue as Float)).toLong()
					setCursorByClickMoveButton(calendar)  //for save current cursor position
					invalidate()
				}
				start()
			}
		} else {
			if (needZoom <= maxZoomByPixels) {
				//if zooming by pixelsPerStep will be enough -> zoom by pixelsPerStep
				ValueAnimator.ofFloat(pixelsPerStep, needZoom * pixelsPerStep).apply {
					duration = ANIM_DURATION
					addUpdateListener { update ->
						pixelsPerStep = update.animatedValue as Float
						setCursorByClickMoveButton(calendar)  //for save current cursor position
						invalidate()
					}
					start()
				}
			} else {
				//if zooming by pixelsPerStep won't be enough
				loop@ for (index in activeSecondScaleIndex - 1 downTo 1) {
					val zoomByMillis =
						secondScales[activeSecondScaleIndex] / secondScales[index]   //get zoom by millis for this index
					val minZoomCurrentIndex =
						minZoomByPixels * zoomByMillis    //min zoom at this index
					val maxZoomCurrentIndex =
						maxZoomByPixels * zoomByMillis    //max zoom at this index
					if (needZoom <= maxZoomCurrentIndex) {
						//if we can achieve need zoom at this index
						val startMillis = millisPerStep
						var endMillis = secondScales[index] * 1000L
						if (minZoomCurrentIndex <= maxAvailableZoom) {
							//if we can achieve need zoom exactly
							//if minZoomCurrentIndex > needZoom -> we increase zoom to minZoomCurrentIndex
							//example: needZoom = 4.3f, minZoomCurrentIndex = 3.0f, maxZoomCurrentIndex = 5.0f (zoomByMillis = 5.0f)
							pixelsPerStep *= needZoom / zoomByMillis    //set without animation (avoid double animation or use handler for it)
							if (pixelsPerStep < minPixelsPerStep) pixelsPerStep =
								minPixelsPerStep  //set min pixelsPerStep
						} else {
							//if min zoom at this index > max available zoom
							val maxZoomPreviousIndex =
								maxZoomByPixels * secondScales[activeSecondScaleIndex] / secondScales[index + 1]
							if (maxZoomPreviousIndex >= minZoomToNextIndex) {
								//if max zoom at previous index > min zoom step -> set max zoom at previous index
								pixelsPerStep =
									maxPixelsPerStep    //set without animation (avoid double animation or use handler for it)
								endMillis =
									secondScales[index + 1] * 1000L  //set previous index to millis (index + 1 is previous because reverse loop)
							} else {
								//if max zoom at previous index < min zoom step -> set min zoom at this index (zoom = min zoom step)
								pixelsPerStep =
									minPixelsPerStep    //set without animation (avoid double animation or use handler for it)
								endMillis =
									secondScales[activeSecondScaleIndex - 1] * 1000L //set next index for current active
							}
						}
						ValueAnimator.ofFloat(0f, 1f).apply {
							duration = ANIM_DURATION
							addUpdateListener { update ->
								millisPerStep =
									(startMillis + (endMillis - startMillis) * (update.animatedValue as Float)).toLong()
								setCursorByClickMoveButton(calendar)  //for save current cursor position
								invalidate()
							}
							start()
						}
						break@loop
					}
				}
			}
		}
		currentMinSpacingMarks =
			minSpacingMarks.toFloat()  //reset current minimum spacing between marks after zoom
	}

}
