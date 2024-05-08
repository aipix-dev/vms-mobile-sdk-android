package com.mobile.vms.player.customviews

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.mobile.vms.*

class VMSLocalizedTextView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0
): AppCompatTextView(context, attrs, defStyleAttr) {

	init {
		checkTag(context, attrs)
	}

	private fun checkTag(context: Context, attrs: AttributeSet?) {
		val ta = context.obtainStyledAttributes(attrs, R.styleable.LocalizedTextView)
		if (!isInEditMode) {
			ta.getString(R.styleable.LocalizedTextView_locTagText)?.let { tag ->
				val data = settings.getStringForLayoutByKey(tag)
				if (data.isNotEmpty() && data != tag) {
					text = data
				}
			}
			ta.getString(R.styleable.LocalizedTextView_locTagHint)?.let { tag ->
				val data = settings.getStringForLayoutByKey(tag)
				if (data.isNotEmpty() && data != tag) {
					hint = data
				}
			}
		}
		ta.recycle()
	}

	fun setLocTagText(tag: String) {
		settings.getStringForLayoutByKey(tag).let { data ->
			if (data.isNotEmpty() && data != tag) {
				text = data
			}
		}
	}
}