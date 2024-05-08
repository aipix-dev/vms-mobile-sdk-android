package com.mobile.vms.player.adapters

import android.content.Context
import android.graphics.Typeface
import android.view.*
import com.mobile.vms.R
import com.mobile.vms.databinding.SdkItemAdapterCheckedBinding
import com.mobile.vms.player.helpers.*
import com.mobile.vms.player.helpers.VMSSettings.getStringForLayoutByKey
import com.mobile.vms.player.ui.*

class QualityAdapter(context: Context): VMSBaseAdapter<String>() {

	var onClick: ((quality: String) -> Unit)? = null
	var previousItemView: SdkItemAdapterCheckedBinding? = null
	var firstItemView: SdkItemAdapterCheckedBinding? = null
	var innerQuality: String = HIGH

	init {
		items = arrayListOf(
			getStringForLayoutByKey("more_live_quality"), HIGH, LOW
		)
	}

	override fun getItemLayoutId() = R.layout.sdk_item_adapter_checked
	override fun getViewHolder(parent: ViewGroup, viewType: Int): VMSBaseViewHolder<String> {
		return ViewHolder(bindingAdapter as SdkItemAdapterCheckedBinding).apply {
			onClick = this@QualityAdapter.onClick
		}
	}

	inner class ViewHolder(private val bindingItem: SdkItemAdapterCheckedBinding):
		VMSBaseViewHolder<String>(bindingItem) {

		var onClick: ((quality: String) -> Unit)? = null

		override fun bind(item: String) {
			try {
				bindingItem.itemAdapterChecked.setSafeOnClickListenerVMS {
					if (adapterPosition != 0 && !item.equals(
							this@QualityAdapter.innerQuality,
							true
						)
					) {
						innerQuality = item
						onClick?.invoke(item)

						bindingItem.ivChecked.visibility = View.VISIBLE
						previousItemView?.ivChecked?.visibility = View.INVISIBLE

						previousItemView = bindingItem
						firstItemView?.ivHeaderIcon?.setImageResource(if (adapterPosition == 1) R.drawable.ic_hd else R.drawable.ic_sd)
					}
				}

				if (adapterPosition == 0) {
					bindingItem.ivHeaderIcon.setImageResource(
						if (innerQuality.equals(HIGH, ignoreCase = true)) R.drawable.ic_hd
						else R.drawable.ic_sd
					)
					bindingItem.tvChecked.text = item
					firstItemView = bindingItem
				} else {
					bindingItem.tvChecked.setTypeface(null, Typeface.NORMAL)
					bindingItem.ivHeaderIcon.isGoneVMS(true)
					bindingItem.ivChecked.visibility = View.INVISIBLE
				}

				if (item.equals(this@QualityAdapter.innerQuality, true)) {
					bindingItem.ivChecked.isGoneVMS(false)
					previousItemView = bindingItem
				}

				if (adapterPosition == 1) {
					bindingItem.tvChecked.text = getStringForLayoutByKey("camera_hd")
				} else if (adapterPosition == 2) {
					bindingItem.tvChecked.text = getStringForLayoutByKey("camera_sd")
				}
			} catch (e: Exception) {
				e.message
			}
		}
	}

}