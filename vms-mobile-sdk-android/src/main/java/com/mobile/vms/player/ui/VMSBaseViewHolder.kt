package com.mobile.vms.player.ui

import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView

abstract class VMSBaseViewHolder<T>(bindingViewHolder: ViewDataBinding):
	RecyclerView.ViewHolder(bindingViewHolder.root) {

	abstract fun bind(item: T)
}