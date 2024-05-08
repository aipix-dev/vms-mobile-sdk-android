package com.mobile.vms.player.ui

import android.os.Bundle
import android.view.*
import androidx.databinding.*
import androidx.fragment.app.Fragment
import com.mobile.vms.VMSMobileSDK.settings

abstract class VMSBaseBindingFragment<B: ViewDataBinding>: Fragment() {

	lateinit var _binding: B
	val binding get() = _binding

	fun getStringForLayoutByKey(key: String): String = settings.getStringForLayoutByKey(key)

	protected abstract fun getLayoutId(): Int

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		setViewBinding(container)
		return binding.root
	}

	private fun setViewBinding(container: ViewGroup?) {
		_binding = DataBindingUtil.inflate(
			LayoutInflater.from(context),
			getLayoutId(),
			container,
			false
		)
		binding.apply { lifecycleOwner = this@VMSBaseBindingFragment }
	}

}