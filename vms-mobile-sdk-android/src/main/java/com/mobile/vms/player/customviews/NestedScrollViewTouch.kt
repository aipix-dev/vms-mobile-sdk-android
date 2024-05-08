package com.mobile.vms.player.customviews

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.core.widget.NestedScrollView
import com.mobile.vms.player.ui.VMSPlayerFragment

class NestedScrollViewTouch @JvmOverloads constructor(
	context: Context, attrs: AttributeSet? = null
): NestedScrollView(context, attrs) {

	override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
		//handle touch only if mark create view shown
		return if (!VMSPlayerFragment.isCreateMarkViewShown) false
		else super.onInterceptTouchEvent(ev)
	}
}