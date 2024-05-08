package com.mobile.vms.player.ui

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.os.Bundle
import android.view.*
import com.mobile.vms.R
import com.mobile.vms.databinding.SdkDialogSettingsBinding
import com.mobile.vms.player.helpers.*

class VMSSettingsDialogFragment: VMSBaseBindingDialog<SdkDialogSettingsBinding>() {

	override fun getLayoutId() = R.layout.sdk_dialog_settings

	var onClickFirstButton: (() -> Unit)? = null
	var onClickSecondButton: (() -> Unit)? = null
	private var imageDrawable: Int? = null
	private var titleSettings: String? = null
	private var msgSettings: String? = null
	private var buttonFirstText: String? = null
	private var buttonSecondText: String? = null

	@SuppressLint("InflateParams")
	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		setViewBinding(container)
		try {
			dialog?.window?.setBackgroundDrawableResource(R.drawable.shape_dialog)
			dialog?.window?.requestFeature(STYLE_NO_TITLE)
			dialog?.setCanceledOnTouchOutside(false)
		} catch (e: NullPointerException) {
			e.message
		}
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		imageDrawable?.let {
			binding.imageSettingDialog.setImageDrawableCompatVMS(
				requireContext(),
				it
			)
		}
		titleSettings?.let { binding.titleSettingDialog.text = it }
		msgSettings?.let { binding.msgSettingDialog.text = it }
		buttonFirstText?.let { binding.btFirstSettingDialog.text = it }
		buttonSecondText?.let { binding.btSecondSettingDialog.text = it }
		binding.btFirstSettingDialog.setSafeOnClickListenerVMS {
			onClickFirstButton?.invoke()
			dismiss()
		}
		binding.btSecondSettingDialog.setSafeOnClickListenerVMS {
			onClickSecondButton?.invoke()
			dismiss()
		}
	}

	override fun onDismiss(dialog: DialogInterface) {
		super.onDismiss(dialog)
		onClickFirstButton = null
	}

}
