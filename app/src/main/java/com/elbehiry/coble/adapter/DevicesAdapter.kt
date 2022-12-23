package com.elbehiry.coble.adapter

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.elbehiry.coble.databinding.DeviceItemViewBinding

class DevicesAdapter(
    private val devices: MutableList<BluetoothDevice> = mutableListOf(),
    val onConnect: (BluetoothDevice) -> Unit,
) : RecyclerView.Adapter<DevicesAdapter.DevicesViewHolder>() {

    fun addItem(device: BluetoothDevice) {
        devices.add(device)
        notifyItemRangeInserted(devices.size - 1, devices.size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DevicesViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return DevicesViewHolder(
            DeviceItemViewBinding.inflate(inflater, parent, false)
        )
    }

    @SuppressLint("MissingPermission")
    override fun onBindViewHolder(holder: DevicesViewHolder, position: Int) {
        val device = devices[position]
        holder.binding.deviceName.text = device.name
        holder.binding.connect.setOnClickListener {
            onConnect(device)
        }
    }

    override fun getItemCount(): Int = devices.size

    class DevicesViewHolder(val binding: DeviceItemViewBinding) :
        RecyclerView.ViewHolder(binding.root)
}
