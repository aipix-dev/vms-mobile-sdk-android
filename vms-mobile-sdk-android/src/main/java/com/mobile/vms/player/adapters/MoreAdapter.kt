package com.mobile.vms.player.adapters

import android.view.ViewGroup
import com.mobile.vms.*
import com.mobile.vms.databinding.SdkItemMorePlayerBinding
import com.mobile.vms.player.helpers.*
import com.mobile.vms.player.helpers.VMSSettings.getStringForLayoutByKey
import com.mobile.vms.player.ui.*

class MoreAdapter: VMSBaseAdapter<String>() {

	var onClick: ((name: String) -> Unit)? = null
	private var hasPermissionsIndexMark = false
	private var hasPermissionsDownloadArchive = false
	private var quality: String = ""
	private var qualityEnabled: Boolean = false
	private var speed: Float = speed_X_1
	private var speedEnabled: Boolean = true
	private var eventsEnabled: Boolean = false

	fun updateAdapter(
		hasPermissionsEvents: Boolean,
		hasPermissionsIndexMark: Boolean,
		hasPermissionsDownloadArchive: Boolean,
		isLive: Boolean,
		quality: String,
		qualityEnabled: Boolean,
		speed: Float,
		eventsEnabled: Boolean
	) {
		items = if (isLive) {
			val array = arrayListOf<String>()
			if (hasPermissionsEvents) {
				array.add(getStringForLayoutByKey("more_live_list"))
			}
			array.add(getStringForLayoutByKey("more_live_quality"))
//			array.add(getStringForLayoutByKey("video_playback_protocol"))
			array
		} else {
			val array = arrayListOf<String>()
			if (hasPermissionsEvents) {
				array.add(getStringForLayoutByKey("more_live_list"))
			}
			array.add(getStringForLayoutByKey("speeds_title"))
			if (hasPermissionsIndexMark){
				array.add(getStringForLayoutByKey("events"))
			}
			if (hasPermissionsDownloadArchive) {
				array.add(getStringForLayoutByKey("download_archive"))
			}
//			array.add(getStringForLayoutByKey("video_playback_protocol")) // hide until MS will do correct working for archive
			array
		}
		this.hasPermissionsIndexMark = hasPermissionsIndexMark
		this.hasPermissionsDownloadArchive = hasPermissionsDownloadArchive
		this.quality = quality
		this.qualityEnabled = qualityEnabled
		this.speed = speed
		this.eventsEnabled = eventsEnabled
		this.notifyDataSetChanged()
	}

	fun updatePermission(
		hasPermissionsIndexMark: Boolean,
		hasPermissionsDownloadArchive: Boolean,
		isLive: Boolean
	) {
		if (isLive) return
		val array = arrayListOf(
			getStringForLayoutByKey("more_live_list"),
			getStringForLayoutByKey("speeds_title")
		)
		if (this.hasPermissionsIndexMark != hasPermissionsIndexMark || this.hasPermissionsDownloadArchive != hasPermissionsDownloadArchive) {
			this.hasPermissionsIndexMark = hasPermissionsIndexMark
			this.hasPermissionsDownloadArchive = hasPermissionsDownloadArchive
			if (hasPermissionsIndexMark) array.add(getStringForLayoutByKey("events"))
			if (hasPermissionsDownloadArchive) array.add(getStringForLayoutByKey("download_archive"))
			this.items = array
			this.notifyDataSetChanged()
		}
	}

	fun updateSpeedEnabled(isEnabled: Boolean) {
		if (isEnabled != this.speedEnabled) {
			this.speedEnabled = isEnabled
			this.notifyItemChanged(1, isEnabled)
		}
	}

	override fun getItemLayoutId() = R.layout.sdk_item_more_player
	override fun getViewHolder(parent: ViewGroup, viewType: Int): VMSBaseViewHolder<String> {
		return ViewHolder(bindingAdapter as SdkItemMorePlayerBinding).apply {
			onClick = this@MoreAdapter.onClick
		}
	}

	override fun onBindViewHolder(
		holder: VMSBaseViewHolder<String>,
		position: Int,
		payloads: MutableList<Any>
	) {
		if (payloads.isEmpty()) {
			onBindViewHolder(holder, position)
		} else {
			for (payload in payloads) {
				if (payload is Boolean) (holder as? ViewHolder)?.changeSpeedEnabled(
					isEnabled = payload,
					name = items[position]
				)
				else onBindViewHolder(holder, position)
			}
		}
	}

	inner class ViewHolder(private val bindingItem: SdkItemMorePlayerBinding):
		VMSBaseViewHolder<String>(bindingItem) {
		var onClick: ((name: String) -> Unit)? = null

		override fun bind(item: String) {
			try {
				bindingItem.itemTvName.text = item   //set title item
				context?.let {
					bindingItem.itemIvMore.setColorFilterView(
						getColorCompat(
							it,
							R.color.txt_black
						)
					)
				}  //set color image
				bindingItem.itemIvMore.isGoneVMS(false)  //show image
				bindingItem.itemIvFlMore.isGoneVMS(true)  //hide frame layout with image and speed text
				bindingItem.itemIvMore.setImageResource( //set image
					when (item) {
						getStringForLayoutByKey("more_live_list") -> {
							bindingItem.itemTvNameAdd.isGoneVMS(true)
							R.drawable.ic_nav_events
						}

						getStringForLayoutByKey("more_live_quality") -> {
							bindingItem.itemTvNameAdd.isGoneVMS(false) //show additional text
							if (quality.equals(HIGH, ignoreCase = true)) {  //check stream quality
								bindingItem.itemTvNameAdd.text = getStringForLayoutByKey("camera_hd_short")  //set additional text
								R.drawable.ic_hd
							} else {
								bindingItem.itemTvNameAdd.text = getStringForLayoutByKey("camera_sd_short")  //set additional text
								R.drawable.ic_sd
							}
						}

						getStringForLayoutByKey("speeds_title") -> {
							bindingItem.itemIvMore.isGoneVMS(true)  //hide image
							bindingItem.itemIvFlMore.isGoneVMS(false)  //show frame layout with image and speed text
							bindingItem.itemTvNameAdd.isGoneVMS(false) //show additional text
							val speedText = getSpeedText(speed)
							bindingItem.itemTvNameAdd.text = if (speed == speed_X_1) getStringForLayoutByKey("speeds_normal")
							else speedText  //set additional text
							bindingItem.itemSpeedCurrent.text = speedText    //set text in frame layout (image)
							R.drawable.ic_action_stub   //this image is gone
						}

						getStringForLayoutByKey("events") -> {
							bindingItem.itemTvNameAdd.isGoneVMS(false) //show additional text
							var text = ""
							for (itemEvent in settings.chosenEventsTypes) {
								if (text.isNotEmpty()) text += ", "
								text += itemEvent.second
							}
							bindingItem.itemTvNameAdd.text = text  //set additional text
							R.drawable.ic_mark
						}

						getStringForLayoutByKey("download_archive") -> {
							bindingItem.itemTvNameAdd.isGoneVMS(true) //show additional text
							R.drawable.ic_download_archive
						}

//						getStringForLayoutByKey("video_playback_protocol") -> {
//							bindingItem.itemTvNameAdd.isGoneVMS(false)
//							bindingItem.itemTvNameAdd.text = settings.videoType.uppercase()  //set additional text
//							R.drawable.ic_video_type
//						}

						else -> R.drawable.ic_action_stub
					}
				)
				if ((!qualityEnabled && item == getStringForLayoutByKey("more_live_quality")) ||
					(!speedEnabled && item == getStringForLayoutByKey("speeds_title")) ||
					(!eventsEnabled && item == getStringForLayoutByKey("events"))
				) {
					//disable item
					bindingItem.itemMorePlayer.alpha = ALPHA_HALF
					bindingItem.itemMorePlayer.setOnClickListener(null)
				} else {
					//enable item
					bindingItem.itemMorePlayer.alpha = ALPHA_FULL
					bindingItem.itemMorePlayer.setSafeOnClickListenerVMS {
						onClick?.invoke(item)
					}
				}
			} catch (e: Exception) {
				e.message
			}
		}

		fun changeSpeedEnabled(isEnabled: Boolean, name: String) {
			if (isEnabled) {
				//enable speed
				bindingItem.itemMorePlayer.alpha = ALPHA_FULL
				bindingItem.itemMorePlayer.setSafeOnClickListenerVMS {
					onClick?.invoke(name)
				}
			} else {
				//disable speed
				bindingItem.itemMorePlayer.alpha = ALPHA_HALF
				bindingItem.itemMorePlayer.setOnClickListener(null)
			}
		}
	}
}