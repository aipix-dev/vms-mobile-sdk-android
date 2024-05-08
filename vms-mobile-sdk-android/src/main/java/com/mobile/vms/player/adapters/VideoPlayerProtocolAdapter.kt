package com.mobile.vms.player.adapters

import android.graphics.Typeface
import android.view.*
import com.mobile.vms.R
import com.mobile.vms.databinding.SdkItemAdapterCheckedBinding
import com.mobile.vms.player.helpers.*
import com.mobile.vms.player.helpers.VMSSettings.getStringForLayoutByKey
import com.mobile.vms.player.ui.*

class VideoPlayerProtocolAdapter(protocol: String): VMSBaseAdapter<String>() {

	var onClick: ((quality: String) -> Unit)? = null
	var previousItemView: SdkItemAdapterCheckedBinding? = null
	var firstItemView: SdkItemAdapterCheckedBinding? = null
	var innerProtocol: String = protocol

	init {
		items = arrayListOf(
			getStringForLayoutByKey("video_playback_protocol"),
			VIDEO_TYPE_RTSP,
			VIDEO_TYPE_HLS
		)
	}

	override fun getItemLayoutId() = R.layout.sdk_item_adapter_checked
	override fun getViewHolder(parent: ViewGroup, viewType: Int): VMSBaseViewHolder<String> {
		return ViewHolder(bindingAdapter as SdkItemAdapterCheckedBinding).apply {
			onClick = this@VideoPlayerProtocolAdapter.onClick
		}
	}

	inner class ViewHolder(private val bindingItem: SdkItemAdapterCheckedBinding):
		VMSBaseViewHolder<String>(bindingItem) {

		var onClick: ((quality: String) -> Unit)? = null

		override fun bind(item: String) {
			try {
				bindingItem.itemAdapterChecked.setSafeOnClickListenerVMS {
					if (adapterPosition != 0 && !item.equals(
							this@VideoPlayerProtocolAdapter.innerProtocol,
							true
						)
					) {
						innerProtocol = item
						onClick?.invoke(item)

						bindingItem.ivChecked.visibility = View.VISIBLE
						previousItemView?.ivChecked?.visibility = View.INVISIBLE
						previousItemView = bindingItem
					}
				}

				if (adapterPosition == 0) {
					bindingItem.ivHeaderIcon.setImageResource(R.drawable.ic_video_type)
					bindingItem.tvChecked.text = item
					firstItemView = bindingItem
				} else {
					bindingItem.tvChecked.setTypeface(null, Typeface.NORMAL)
					bindingItem.ivHeaderIcon.isGoneVMS(true)
					bindingItem.ivChecked.visibility = View.INVISIBLE
				}

				if (item.equals(this@VideoPlayerProtocolAdapter.innerProtocol, true)) {
					bindingItem.ivChecked.isGoneVMS(false)
					previousItemView = bindingItem
				}

				if (adapterPosition == 1) {
					bindingItem.tvChecked.text =
						getStringForLayoutByKey("player_with_minimal_delay")
				} else if (adapterPosition == 2) {
					bindingItem.tvChecked.text = getStringForLayoutByKey("player_with_buffering")
				}
			} catch (e: Exception) {
				e.message
			}
		}
	}

}