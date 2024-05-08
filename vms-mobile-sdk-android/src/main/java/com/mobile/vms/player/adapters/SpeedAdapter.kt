package com.mobile.vms.player.adapters

import android.graphics.Typeface
import android.view.*
import com.mobile.vms.R
import com.mobile.vms.databinding.SdkItemSpeedsBinding
import com.mobile.vms.player.helpers.*
import com.mobile.vms.player.helpers.VMSSettings.getStringForLayoutByKey
import com.mobile.vms.player.ui.*

class SpeedAdapter(private val speedX: Float, private val videoCodec: String):
	VMSBaseAdapter<Float>() {

	var onClick: ((speed: Float) -> Unit)? = null
	var previousItemView: SdkItemSpeedsBinding? = null
	var firstItemView: SdkItemSpeedsBinding? = null
	var innerSpeedX = speedX

	fun setList(videoRates: ArrayList<Double>) {
		items.clear()
		items.add(speed_X_1) // 1x speed as default for title
		logSdk("SpeedAdapter", "videoCodec: $videoCodec")
		if (videoCodec.equals("h265", true)) {
			items.add(speed_X_1 / 2)
			items.add(speed_X_1)
			items.add(speed_X_1 * 2)
		} else {
			items.addAll(videoRates.map { it.toFloat() })
		}
	}

	override fun getItemLayoutId() = R.layout.sdk_item_speeds
	override fun getViewHolder(parent: ViewGroup, viewType: Int): VMSBaseViewHolder<Float> {
		return SpeedViewHolder(bindingAdapter as SdkItemSpeedsBinding).apply {
			onClick = this@SpeedAdapter.onClick
		}
	}

	inner class SpeedViewHolder(private val bindingItem: SdkItemSpeedsBinding):
		VMSBaseViewHolder<Float>(bindingItem) {

		var onClick: ((speed: Float) -> Unit)? = null

		override fun bind(item: Float) {
			try {
				val speedText = getSpeedText(item)
				val itemText =
					if (item == speed_X_1) getStringForLayoutByKey("speeds_normal") else speedText
				if (adapterPosition == 0) {
					firstItemView = bindingItem
					bindingItem.tvNameItemSpeed.text = getStringForLayoutByKey("speeds_title")
					bindingItem.tvSpeedCurrent.text = speedText
				} else {
					bindingItem.tvNameItemSpeed.text = itemText
					bindingItem.tvNameItemSpeed.setTypeface(null, Typeface.NORMAL)
					bindingItem.lnrSpeedItem.isGoneVMS(true)
					bindingItem.ivCheckSpeed.visibility = View.INVISIBLE
					if (item == innerSpeedX) {
						previousItemView = bindingItem
						bindingItem.ivCheckSpeed.isGoneVMS(false)
					}
				}
				bindingItem.itemSpeeds.setSafeOnClickListenerVMS {
					if (adapterPosition != 0 && innerSpeedX != item) {
						bindingItem.ivCheckSpeed.visibility = View.VISIBLE
						previousItemView?.ivCheckSpeed?.visibility = View.INVISIBLE
						firstItemView?.tvSpeedCurrent?.text = speedText
						previousItemView = bindingItem
						innerSpeedX = item
						onClick?.invoke(item)
					}
				}
			} catch (e: Exception) {
				e.message
			}
		}
	}

}