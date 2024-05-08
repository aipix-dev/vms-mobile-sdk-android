package com.example.demo.ui.cameras

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.demo.App
import com.example.demo.R
import com.example.demo.databinding.FragmentCamerasBinding
import com.google.gson.JsonObject
import com.mobile.vms.models.VMSCamera
import com.mobile.vms.models.VMSPlayerData
import com.mobile.vms.player.ui.VMSPlayerCallbackScreenshot
import com.mobile.vms.player.ui.VMSPlayerCallbackVideoQuality
import com.mobile.vms.player.ui.VMSPlayerFragment
import com.mobile.vms.player.ui.VMSScreenState
import java.util.Calendar

class CamerasFragment : Fragment() {

    private var _binding: FragmentCamerasBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCamerasBinding.inflate(inflater, container, false)

        val camerasViewModel = ViewModelProvider(this)[CamerasViewModel::class.java]
        camerasViewModel.camerasState.observe(viewLifecycleOwner) {
            binding.progressBar.visibility = if (it.isLoading) View.VISIBLE else View.GONE
            binding.tvError.visibility =
                if (it.isError && !it.isLoading) View.VISIBLE else View.GONE
            if (!it.cameras.isNullOrEmpty()) setupRecyclerView(it.cameras)
        }
        return binding.root
    }

    private fun setupRecyclerView(cameras: ArrayList<VMSCamera>) {
        binding.recyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(requireContext())
            adapter = CamerasAdapter(cameras) { camera -> cameraClick(camera) }
        }
        binding.recyclerView.visibility = View.VISIBLE
    }

    private fun cameraClick(camera: VMSCamera) {
        val data = VMSPlayerData(
            camera,
            null,
            null,
            App.translations?.json as? JsonObject,
            App.statics?.videoRates ?: arrayListOf(),
            App.statics?.markTypes ?: arrayListOf(),
            App.permissions ?: listOf(),
            true
        )
        val fragment = VMSPlayerFragment.newInstance(data)

        val transaction = requireActivity().supportFragmentManager.beginTransaction()
        transaction.replace(R.id.container, fragment)
        transaction.commit()

        val screenshotCallback = object : VMSPlayerCallbackScreenshot {
            override fun onClickScreenshot(
                bitmap: Bitmap,
                camera: VMSCamera,
                time: Calendar,
                state: VMSScreenState
            ) {
                // here you can save screenshot from player
            }
        }
        VMSPlayerFragment.callbackScreenshotMLD.postValue(screenshotCallback)

        val videoQualityCallback = object : VMSPlayerCallbackVideoQuality {
            override var videoQuality: String
                get() {
                    // here you can return any quality to set it inside player
                    return ""
                }
                set(value) {}

            override fun onSaveVideoQuality(videoQuality: String) {
                // here you can save chosen video quality
            }
        }
        VMSPlayerFragment.callbackVideoQualityMLD.postValue(videoQualityCallback)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}