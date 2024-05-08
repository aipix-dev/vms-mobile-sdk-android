package com.mobile.vms.player.helpers

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.os.Build.*
import android.util.DisplayMetrics
import android.view.*
import android.view.animation.*
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatImageView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.mobile.vms.player.helpers.Mode.SRC_ATOP

fun View.setCustomPaddingMarkTitle(cutPadding: Int) {
	this.setPadding(
		(12 - cutPadding).toPx(),
		(12 - cutPadding).toPx(),
		(12 - cutPadding).toPx(),
		(12 - cutPadding).toPx()
	)
}

fun View.setCustomPaddingMarkDate(cutPadding: Int) {
	this.setPadding(
		(12 - cutPadding).toPx(),
		(12 - cutPadding).toPx(),
		(12 - cutPadding).toPx(),
		(12 - cutPadding).toPx()
	)
}

fun View.setCustomPaddingMarkButtons(cutPadding: Int) {
	this.setPadding(
		(24 - cutPadding).toPx(),
		(14 - cutPadding).toPx(),
		(24 - cutPadding).toPx(),
		(14 - cutPadding).toPx()
	)
}

fun TextView.enableOrDisableMovingButtons(isEnabled: Boolean) {
	this.isEnabled = isEnabled
	this.alpha = if (isEnabled) ALPHA_FULL else ALPHA_HALF
}

fun AppCompatImageView.enableOrDisableMovingButtons(isEnabled: Boolean) {
	this.isEnabled = isEnabled
	this.alpha = if (isEnabled) ALPHA_FULL else ALPHA_HALF
}


const val ANIM_DURATION = 200L
const val COORDINATOR = "COORDINATOR"
const val FRAME = "FRAME"
const val LEFT = "LEFT"
const val TOP = "TOP"
const val RIGHT = "RIGHT"
const val BOTTOM = "BOTTOM"

fun View.isGoneVMS(value: Boolean) {
	visibility = if (value) View.GONE else View.VISIBLE
}

fun View.isGoneSmooth(
	isGone: Boolean,
	maxAlpha: Float = ALPHA_FULL,
	animDuration: Long = ANIM_DURATION
) {
	val anim = object: Animation() {
		override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
			if (isGone) {
				alpha = if (interpolatedTime == 0f) maxAlpha
				else maxAlpha * (1f - interpolatedTime)
				if (interpolatedTime == 1f) {
					visibility = View.INVISIBLE
				}
			} else {
				alpha = if (interpolatedTime == 0f) {
					visibility = View.VISIBLE
					ALPHA_ZERO
				} else interpolatedTime * maxAlpha
			}
		}
	}
		.apply {
			duration = animDuration
		}
	startAnimation(anim)
	requestLayout()
}

fun Int.toPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()
fun Float.toPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()

fun View.setMargins(l: Int, t: Int, r: Int, b: Int) {
	if (this.layoutParams is ViewGroup.MarginLayoutParams) {
		val p = this.layoutParams as ViewGroup.MarginLayoutParams
		p.setMargins(l, t, r, b)
		this.requestLayout()
	}
}

fun View.setSafeOnClickListenerVMS(onSafeClick: (View) -> Unit) {
	val safeClickListener = SafeClickListener {
		onSafeClick(it)
	}
	setOnClickListener(safeClickListener)
}

fun View.setLayoutParamsCustom(
	layout: String, layoutWidth: Int, layoutHeight: Int,
	layoutGravity: Int? = null, marginDirection: String? = null, margin: Int? = null
) {
	when (layout) {
		COORDINATOR -> {
			val layoutParamsNew = CoordinatorLayout.LayoutParams(layoutWidth, layoutHeight).apply {
				layoutGravity?.let { gravity = layoutGravity }
				marginDirection?.let {
					margin?.let {
						when (marginDirection) {
							LEFT -> leftMargin = margin
							TOP -> topMargin = margin
							RIGHT -> rightMargin = margin
							BOTTOM -> bottomMargin = margin
						}
					}
				}
			}
			layoutParams = layoutParamsNew
		}

		FRAME -> {
			val layoutParamsNew = FrameLayout.LayoutParams(layoutWidth, layoutHeight).apply {
				layoutGravity?.let { gravity = layoutGravity }
				marginDirection?.let {
					margin?.let {
						when (marginDirection) {
							LEFT -> leftMargin = margin
							TOP -> topMargin = margin
							RIGHT -> rightMargin = margin
							BOTTOM -> bottomMargin = margin
						}
					}
				}
			}
			layoutParams = layoutParamsNew
		}
	}
}

fun View.setBackgroundCompat(context: Context, id: Int) {
	background = getDrawableCompat(context, id)
}

fun AppCompatImageView.setImageDrawableCompatVMS(context: Context, id: Int) {
	setImageDrawable(getDrawableCompat(context, id))
}

fun TextView.setTextColorCompatVMS(context: Context, id: Int) {
	setTextColor(getColorCompat(context, id))
}

fun Activity.getWindowWidth(): Int {
	val displayMetrics = DisplayMetrics()
	windowManager.defaultDisplay.getMetrics(displayMetrics)
	return displayMetrics.widthPixels
}

fun Activity.getWindowHeight(): Int {
	val displayMetrics = DisplayMetrics()
	windowManager.defaultDisplay.getMetrics(displayMetrics)
	return displayMetrics.heightPixels
}


fun AppCompatImageView.setColorFilterView(color: Int, mode: Mode = SRC_ATOP) {
	if (VERSION.SDK_INT >= VERSION_CODES.Q) {
		colorFilter = BlendModeColorFilter(color, mode.getBlendMode())
	} else {
		@Suppress("DEPRECATION")
		setColorFilter(color, mode.getPorterDuffMode())
	}
}

// This class is needed to call the setColorFilter
// with different BlendMode on older API (before 29).
enum class Mode {

	CLEAR,
	SRC,
	DST,
	SRC_OVER,
	DST_OVER,
	SRC_IN,
	DST_IN,
	SRC_OUT,
	DST_OUT,
	SRC_ATOP,
	DST_ATOP,
	XOR,
	DARKEN,
	LIGHTEN,
	MULTIPLY,
	SCREEN,
	ADD,
	OVERLAY;

	@RequiresApi(VERSION_CODES.Q)
	fun getBlendMode(): BlendMode =
		when (this) {
			CLEAR -> BlendMode.CLEAR
			SRC -> BlendMode.SRC
			DST -> BlendMode.DST
			SRC_OVER -> BlendMode.SRC_OVER
			DST_OVER -> BlendMode.DST_OVER
			SRC_IN -> BlendMode.SRC_IN
			DST_IN -> BlendMode.DST_IN
			SRC_OUT -> BlendMode.SRC_OUT
			DST_OUT -> BlendMode.DST_OUT
			SRC_ATOP -> BlendMode.SRC_ATOP
			DST_ATOP -> BlendMode.DST_ATOP
			XOR -> BlendMode.XOR
			DARKEN -> BlendMode.DARKEN
			LIGHTEN -> BlendMode.LIGHTEN
			MULTIPLY -> BlendMode.MULTIPLY
			SCREEN -> BlendMode.SCREEN
			ADD -> BlendMode.PLUS
			OVERLAY -> BlendMode.OVERLAY
		}

	fun getPorterDuffMode(): PorterDuff.Mode =
		when (this) {
			CLEAR -> PorterDuff.Mode.CLEAR
			SRC -> PorterDuff.Mode.SRC
			DST -> PorterDuff.Mode.DST
			SRC_OVER -> PorterDuff.Mode.SRC_OVER
			DST_OVER -> PorterDuff.Mode.DST_OVER
			SRC_IN -> PorterDuff.Mode.SRC_IN
			DST_IN -> PorterDuff.Mode.DST_IN
			SRC_OUT -> PorterDuff.Mode.SRC_OUT
			DST_OUT -> PorterDuff.Mode.DST_OUT
			SRC_ATOP -> PorterDuff.Mode.SRC_ATOP
			DST_ATOP -> PorterDuff.Mode.DST_ATOP
			XOR -> PorterDuff.Mode.XOR
			DARKEN -> PorterDuff.Mode.DARKEN
			LIGHTEN -> PorterDuff.Mode.LIGHTEN
			MULTIPLY -> PorterDuff.Mode.MULTIPLY
			SCREEN -> PorterDuff.Mode.SCREEN
			ADD -> PorterDuff.Mode.ADD
			OVERLAY -> PorterDuff.Mode.OVERLAY
		}
}

