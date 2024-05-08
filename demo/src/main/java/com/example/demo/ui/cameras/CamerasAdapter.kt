package com.example.demo.ui.cameras

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.demo.R
import com.example.demo.databinding.ItemCameraBinding
import com.mobile.vms.models.VMSCamera

class CamerasAdapter(
    private val items: ArrayList<VMSCamera>,
    private val onClickAction: (camera: VMSCamera) -> Unit
) :
    RecyclerView.Adapter<CamerasAdapter.ViewHolder>() {

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val binding = DataBindingUtil.inflate(
            LayoutInflater.from(viewGroup.context), R.layout.item_camera, viewGroup, false
        ) as ItemCameraBinding
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        viewHolder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(private val binding: ItemCameraBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: VMSCamera) {
            binding.tvCameraName.text = item.name
            binding.tvCameraAddress.text = item.prettyText
            binding.itemCamera.setOnClickListener {
                onClickAction.invoke(item)
            }
        }
    }

}