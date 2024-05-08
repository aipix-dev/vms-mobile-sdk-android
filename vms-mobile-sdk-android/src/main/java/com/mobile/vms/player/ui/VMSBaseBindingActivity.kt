package com.mobile.vms.player.ui

import android.os.Bundle
import androidx.databinding.*

open abstract class VMSBaseBindingActivity<B: ViewDataBinding>: VMSBaseActivity() {

	lateinit var _binding: B
	val binding get() = _binding

	protected abstract fun getLayoutId(): Int

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		_binding = DataBindingUtil.setContentView(this, getLayoutId()) as B
		binding.apply { lifecycleOwner = this@VMSBaseBindingActivity }
	}

}