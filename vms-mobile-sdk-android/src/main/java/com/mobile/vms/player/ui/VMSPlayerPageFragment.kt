package com.mobile.vms.player.ui

import android.graphics.SurfaceTexture
import android.os.*
import android.view.*
import com.mobile.vms.R
import com.mobile.vms.databinding.SdkFragmentExoPlayerPageBinding
import com.mobile.vms.player.helpers.logSdk
import com.mobile.vms.player.zoom.ShowCameraControllerCallback

// brew install ffmpeg
// ffplay -rtsp_transport tcp "url"
// 403 status if token was expired

class VMSPlayerPageFragment: VMSBaseBindingFragment<SdkFragmentExoPlayerPageBinding>(),
	SurfaceHolder.Callback,
	TextureView.SurfaceTextureListener {
	val TAG = "PlayerPageFragment"

	override fun getLayoutId() = R.layout.sdk_fragment_exo_player_page

	private var playerPosition = 0
	var surface: Surface? = null
	private val DELAY_LOAD_SURFACE =
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.R) 1000L else 2000L

	fun newInstance(position: Int): VMSPlayerPageFragment {
		return VMSPlayerPageFragment().apply {
			playerPosition = position
		}
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		surface = null
		if (binding.vmsPlayerView.surfaceTextureListener != null) binding.vmsPlayerView.surfaceTextureListener =
			null
		binding.vmsPlayerView.surfaceTextureListener = this

		binding.vmsPlayerView.setShowCameraControllerCallback(object : ShowCameraControllerCallback {
			override fun onShowCameraController() {
				(parentFragment as? ShowCameraControllerCallback)?.onShowCameraController()
			}
		})
	}

	override fun surfaceCreated(holder: SurfaceHolder) {
		logSdk(TAG, "surfaceCreated !!!")
		surface = holder.surface
	}

	override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
		logSdk(TAG, "surfaceChanged: width = $width height = $height")
		surface = holder.surface
//        surfaceHeight = height
//        surfaceWidth = width
	}

	override fun surfaceDestroyed(holder: SurfaceHolder) {
		logSdk(TAG, "surfaceDestroyed")
//        surface = holder.surface
	}

	override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
		surface = Surface(texture)
	}

	override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {
	}

	override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
		logSdk(TAG, "onSurfaceTextureDestroyed")
//		surface = Surface(texture)
		return false
	}

	override fun onSurfaceTextureUpdated(texture: SurfaceTexture) {
		if (surface == null) {
			logSdk(TAG, "onSurfaceTextureUpdated")
			surface = Surface(texture)
		}
	}

}
