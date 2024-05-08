package com.mobile.vms.player.ui

import android.content.Context
import android.os.*
import android.util.AttributeSet
import android.view.MotionEvent
import com.mobile.vms.player.helpers.logSdk
import com.mobile.vms.player.zoom.*
import kotlin.math.abs

/**
 * This is a custom viewpager
 */
class VMSPlayerViewPager @JvmOverloads constructor(
	context: Context, attrs: AttributeSet? = null
): androidx.viewpager.widget.ViewPager(context, attrs) {

	private var downX = 0f //stores the starting X position of the ACTION_DOWN event
	private var downY = 0f //stores the starting Y position of the ACTION_DOWN event
	private var topViewHeight = 0 //76dp (44dp) not intercept in top screen
	private var bottomViewHeight = 0 //92dp not intercept in bottom screen archive
	private var lastDownMills = 0L //last click time
	private var isDoubleClick = false //check if double click
	private val TIME_DOUBLE_CLICK = 250L   //time for double click
	private var callback: ShowCameraControllerCallback? = null//callback for show/hide buttons

	fun setShowCameraControllerCallback(callback: ShowCameraControllerCallback) {
		this.callback = callback
	}

	//clicks on zoomable texture view not pass here at all
	override fun onTouchEvent(ev: MotionEvent?): Boolean {
		try {
			if (ev == null) {
				return false
			}
			//if click in available screen area
			if (height - ev.y > bottomViewHeight && ev.y > topViewHeight) {
				when (ev.action) {
					MotionEvent.ACTION_DOWN -> {
						ZoomableTextureView.isActionMove = false
						if (SystemClock.uptimeMillis() < (lastDownMills + TIME_DOUBLE_CLICK)) {
							isDoubleClick = true
						} else {
							isDoubleClick = false
							lastDownMills = SystemClock.uptimeMillis()
						}
						downX = ev.x
						downY = ev.y
						logSdk("TAG", "downX = $downX, downY = $downY")
						return true //true - for opportunity to get ACTION_UP when it happened
					}

					MotionEvent.ACTION_MOVE -> {
						ZoomableTextureView.isActionMove = true
					}

					MotionEvent.ACTION_UP -> {
						ZoomableTextureView.isActionMove = false
						try {
							val xInLimit = abs(ev.x - downX) < 30
							val yxInLimit = abs(ev.y - downY) < 60
							if (xInLimit && yxInLimit) {
								Handler(Looper.getMainLooper()).postDelayed({
									if (!isDoubleClick) callback?.onShowCameraController()   //callback if not double click
								}, TIME_DOUBLE_CLICK)
							}
						} catch (e: Throwable) {
							e.message
						}
						//not set return - here can handle swipe for example
					}
				}
			} else if (ZoomableTextureView.isActionMove) return super.onTouchEvent(ev)  //for swipe started on available area
			else return false //false - ban swipe if clicks under top/bottom views
			if (ZoomableTextureView.isZoomEnabled || VMSPlayerFragment.Companion.isCreateMarkViewShown) {
				return false //false - ban swipe if texture view zoomed or mark create view shown
			}
			return super.onTouchEvent(ev)
		} catch (e: Exception) {
			e.message
			return false
		}
	}

	override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
		try {
			if (ev == null) {
				return false
			}
			setupBorders()  //set top/bottom views height (done here so don't need call it in onTouchEvent)
			return if (height - ev.y <= bottomViewHeight || ev.y <= topViewHeight || VMSPlayerFragment.Companion.isCreateMarkViewShown) {
				//clicks not pass to zoomable texture view even if they get into zoomable texture view
				//that bun swipe and zoom
				true
			} else if (ZoomableTextureView.isZoomEnabled) {
				false   //can scroll inside zoomable texture view if it zoomed
			} else {
				super.onInterceptTouchEvent(ev)
			}
		} catch (e: Exception) {
			e.message
			return false
		}
	}

	private fun setupBorders() {
		if (!VMSPlayerFragment.isHideButtons) {
			//controls shown
//            topViewHeight = if (isPortrait(context)) {
//                //portrait orientation
//                76.toPx()
//            } else {
//                //land orientation
//                44.toPx()
//            }
//            bottomViewHeight = if (PlayerFragment.Companion.isLive) {
//                //live
//                0
//            } else {
//                //archive
//                92.toPx()
//            }
		} else {
			//controls not shown
			bottomViewHeight = 0
			topViewHeight = 0
		}
	}

}