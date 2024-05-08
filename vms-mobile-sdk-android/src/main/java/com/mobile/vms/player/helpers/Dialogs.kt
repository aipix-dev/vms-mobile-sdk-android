package com.mobile.vms.player.helpers

import android.app.AlertDialog
import android.content.Context

object Dialogs {
	fun showSimpleAlertDialog(
		context: Context,
		title: String,
		message: String? = null,
		okText: String? = null,
		okAction: (() -> Unit)? = null,
		cancelText: String? = null,
		cancelAction: (() -> Unit)? = null
	) {
		val dialog = AlertDialog.Builder(context)
			.setCancelable(false)
			.setTitle(title)
		message?.let { dialog.setMessage(message) }
		okText?.let { text ->
			dialog.setPositiveButton(text) { dialog, _ ->
				okAction?.invoke()
				dialog.dismiss()
			}
		}
		cancelText?.let { text ->
			dialog.setNegativeButton(text) { dialog, _ ->
				cancelAction?.invoke()
				dialog.dismiss()
			}
		}
		dialog.create().show()
	}
}