package com.mobile.vms.player.ui

import android.view.*
import androidx.databinding.*
import androidx.fragment.app.DialogFragment

abstract class VMSBaseBindingDialog<B: ViewDataBinding>: DialogFragment() {

	lateinit var _binding: B
	val binding get() = _binding

	protected abstract fun getLayoutId(): Int

	protected fun setViewBinding(container: ViewGroup?) {
		_binding = DataBindingUtil.inflate(
			LayoutInflater.from(context), getLayoutId(), container, false
		)
		binding.apply { lifecycleOwner = this@VMSBaseBindingDialog }
	}

}