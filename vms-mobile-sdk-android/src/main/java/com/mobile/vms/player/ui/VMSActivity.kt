package com.mobile.vms.player.ui

import android.os.*
import androidx.fragment.app.FragmentTransaction
import com.mobile.vms.R
import com.mobile.vms.R.id
import com.mobile.vms.databinding.ActivityPlayerBinding
import com.mobile.vms.models.VMSPlayerData
import com.mobile.vms.player.helpers.logSdk

/**
 * For the future if we will need to open screen and do all setup-things before it will be open
 */
class VMSActivity: VMSBaseBindingActivity<ActivityPlayerBinding>(), VMSPlayerCreationListener {
	override fun getLayoutId(): Int = R.layout.activity_player

	private fun getCurrentFragment() =
		supportFragmentManager.findFragmentById(id.containerVMS) as? VMSPlayerFragment

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(binding.root)
		onCreatePlayerScreen(savedInstanceState)
	}

	override fun onCreatePlayerScreen(savedInstanceState: Bundle?) {
		if (intent.hasExtra("VMSPlayerData")) {
			try {
				val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
					intent.getParcelableExtra("VMSPlayerData", VMSPlayerData::class.java)
				else
					intent.getParcelableExtra("VMSPlayerData")
				if (data != null) {
					val f = VMSPlayerFragment.newInstance(data)
					val transaction: FragmentTransaction = supportFragmentManager.beginTransaction()
					transaction.add(id.containerVMS, f)
					transaction.commit()
					logSdk("VMSActivity", "PlayerFragment created successfully.")
				}
			} catch (e: Exception) {
				e.printStackTrace()
			}
		} else {
			logSdk("VMSActivity", "Please, transfer object VMSPlayerData to VMSActivity.")
		}
	}

	override fun onRestart() {
		super.onRestart()
		val currentFragment = getCurrentFragment()
		if (VMSPlayerFragment.isLive) {
			currentFragment?.getLiveStream()
		} else {
			currentFragment?.currentDateCursor?.let {
				currentFragment.startHandlerAndChangeDateArchive(it)
			}
		}
		currentFragment?.player?.playWhenReady = true
	}

}

