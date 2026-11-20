package com.example.rental_property_management_app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.databinding.ItemRoomBinding
import com.example.rental_property_management_app.models.RoomResponse
import com.example.rental_property_management_app.utils.CurrencyUtils

/**
 * RecyclerView Adapter cho Room List
 */
class RoomAdapter(
    private val onItemClick: (RoomResponse) -> Unit,
    private val onEditClick: (RoomResponse) -> Unit,
    private val onDeleteClick: (RoomResponse) -> Unit
) : ListAdapter<RoomResponse, RoomAdapter.RoomViewHolder>(RoomDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        val binding = ItemRoomBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RoomViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class RoomViewHolder(
        private val binding: ItemRoomBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(room: RoomResponse) {
            binding.apply {
                // Room code và floor
                tvRoomCode.text = room.roomCode
                tvFloor.text = "Tầng ${room.floor}"
                
                // Area
                tvArea.text = "${room.area} m²"
                
                // Price
                tvRentPrice.text = CurrencyUtils.formatVND(room.rentPrice.toLong())
                
                // Status
                tvStatus.text = room.status.getDisplayName()
                val statusColor = ContextCompat.getColor(
                    root.context,
                    room.status.getColorResId()
                )
                tvStatus.setTextColor(statusColor)
                
                // Property name
                tvPropertyName.text = room.propertyName
                
                // Tenant info (if available)
                if (!room.currentTenant.isNullOrEmpty()) {
                    tvTenantInfo.text = "Khách: ${room.currentTenant}"
                } else {
                    tvTenantInfo.text = "Chưa có khách"
                }

                // Health Score badge
                tvHealthScore.text = "Health Score: ${room.healthScore}/100"
                val (hsFg, hsBg) = when {
                    room.healthScore >= 80 -> R.color.health_good to R.drawable.rc_badge_success
                    room.healthScore >= 50 -> R.color.health_warning to R.drawable.rc_badge_warning
                    else -> R.color.health_risk to R.drawable.rc_icon_box_danger
                }
                tvHealthScore.setTextColor(ContextCompat.getColor(root.context, hsFg))
                tvHealthScore.setBackgroundResource(hsBg)
                
                // Click listeners
                root.setOnClickListener { onItemClick(room) }
                btnEdit.setOnClickListener { onEditClick(room) }
                btnDelete.setOnClickListener { onDeleteClick(room) }
            }
        }
    }
    
    class RoomDiffCallback : DiffUtil.ItemCallback<RoomResponse>() {
        override fun areItemsTheSame(oldItem: RoomResponse, newItem: RoomResponse): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: RoomResponse, newItem: RoomResponse): Boolean {
            return oldItem == newItem
        }
    }
}
