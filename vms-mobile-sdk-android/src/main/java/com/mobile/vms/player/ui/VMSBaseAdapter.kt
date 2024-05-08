package com.mobile.vms.player.ui

import android.content.Context
import android.view.*
import androidx.databinding.*
import androidx.recyclerview.widget.RecyclerView

abstract class VMSBaseAdapter<T>: RecyclerView.Adapter<VMSBaseViewHolder<T>>() {

	private var _bindingAdapter: ViewDataBinding? = null
	val bindingAdapter get() = _bindingAdapter!!
	var items: ArrayList<T> = arrayListOf()
	var context: Context? = null

	abstract fun getItemLayoutId(): Int

	abstract fun getViewHolder(parent: ViewGroup, viewType: Int): VMSBaseViewHolder<T>

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VMSBaseViewHolder<T> {
		setViewBindingAdapter(parent)
		context = parent.context
		return getViewHolder(parent, viewType)
	}

	private fun setViewBindingAdapter(parent: ViewGroup) {
		_bindingAdapter = DataBindingUtil.inflate(
			LayoutInflater.from(parent.context), getItemLayoutId(), parent, false
		)
	}

	override fun onBindViewHolder(holder: VMSBaseViewHolder<T>, position: Int) {
		holder.bind(items[position])
	}

	override fun getItemCount() = items.size

	override fun getItemId(position: Int) = position.toLong() // avoid mixed data

	override fun getItemViewType(position: Int) = position // avoid mixed data

	override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
		super.onDetachedFromRecyclerView(recyclerView)
		_bindingAdapter = null
		context = null
	}
}