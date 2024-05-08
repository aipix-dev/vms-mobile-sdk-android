package com.mobile.vms.player.adapters

import android.view.*
import com.mobile.vms.*
import com.mobile.vms.databinding.SdkItemMarksPlayerBinding
import com.mobile.vms.player.helpers.*
import com.mobile.vms.player.helpers.VMSSettings.getStringForLayoutByKey
import com.mobile.vms.player.ui.*

class EventsAdapter: VMSBaseAdapter<Pair<String, String>>() {

	var chosenList: ArrayList<Pair<String, String>> = arrayListOf()

	init {
		items.apply {
			add(Pair(EVENTS_SHOW_ALL, getStringForLayoutByKey("display_all")))
			for (item in settings.markTypes) {
				item.name?.let { key ->
					item.description?.let { type -> add(Pair(key, type)) }
				}
			}
			add(Pair(EVENTS_NOT_SHOW, getStringForLayoutByKey("marks_no_show")))
		}
	}

	override fun getItemLayoutId() = R.layout.sdk_item_marks_player
	override fun getViewHolder(
		parent: ViewGroup,
		viewType: Int
	): VMSBaseViewHolder<Pair<String, String>> {
		return TagViewHolder(bindingAdapter as SdkItemMarksPlayerBinding)
	}

	override fun onBindViewHolder(
		holder: VMSBaseViewHolder<Pair<String, String>>, position: Int, payloads: MutableList<Any>
	) {
		if (payloads.isEmpty()) {
			onBindViewHolder(holder, position)
		} else {
			for (payload in payloads) {
				when (payload) {
					is Boolean -> {
						(holder as? TagViewHolder)?.setChosen(payload)
					}

					else -> {
						onBindViewHolder(holder, position)
					}
				}
			}
		}
	}

	inner class TagViewHolder(private val bindingItem: SdkItemMarksPlayerBinding):
		VMSBaseViewHolder<Pair<String, String>>(bindingItem) {

		override fun bind(item: Pair<String, String>) {
			bindingItem.tvNameMark.text = item.second
			bindingItem.ivCheckMark.isGoneVMS(!chosenList.map { it.first }.contains(item.first))
			bindingItem.itemMarksPlayer.setSafeOnClickListenerVMS {
				val wasChecked = bindingItem.ivCheckMark.visibility == View.VISIBLE
				when (item.first) {
					EVENTS_SHOW_ALL -> {
						if (!wasChecked) {
							notifyItemRangeChanged(1, items.size - 1, false)
							setChosen(true)
							chosenList.clear()
							chosenList.add(
								Pair(
									EVENTS_SHOW_ALL,
									getStringForLayoutByKey("display_all")
								)
							)
						}
					}

					EVENTS_NOT_SHOW -> {
						if (!wasChecked) {
							notifyItemRangeChanged(0, items.size - 1, false)
							setChosen(true)
							chosenList.clear()
							chosenList.add(
								Pair(
									EVENTS_NOT_SHOW,
									getStringForLayoutByKey("marks_no_show")
								)
							)
						}
					}

					else -> {
						if (wasChecked) {
							chosenList.remove(item)
							if (chosenList.isEmpty()) { // remove all types
								setChosen(false)
								notifyItemChanged(items.lastIndex, true)
								chosenList.clear()
								chosenList.add(
									Pair(
										EVENTS_NOT_SHOW,
										getStringForLayoutByKey("marks_no_show")
									)
								)
							} else {
								setChosen(false)
							}
						} else {
							if (chosenList.size == 1) {
								if (chosenList.first().first == EVENTS_NOT_SHOW) {
									chosenList.clear()
									notifyItemChanged(items.lastIndex, false)
								} else if (chosenList.first().first == EVENTS_SHOW_ALL) {
									chosenList.clear()
									notifyItemChanged(0, false)
								}
							}
							chosenList.add(item)
							if (chosenList.size == items.size - 2) { // add all possible types
								notifyItemRangeChanged(1, items.size - 1, false)
								notifyItemChanged(0, true)
								chosenList.clear()
								chosenList.add(
									Pair(
										EVENTS_SHOW_ALL,
										getStringForLayoutByKey("display_all")
									)
								)
							} else {
								setChosen(true)
							}
						}
					}
				}
			}
		}

		fun setChosen(needSet: Boolean) {
			bindingItem.ivCheckMark.isGoneVMS(!needSet)
		}
	}

}