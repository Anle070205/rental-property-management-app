package com.example.rental_property_management_app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.rental_property_management_app.databinding.ItemTenantBinding
import com.example.rental_property_management_app.models.TenantResponse

/**
 * RecyclerView Adapter cho Tenant List
 */
class TenantAdapter(
    private val onItemClick: (TenantResponse) -> Unit
) : ListAdapter<TenantResponse, TenantAdapter.TenantViewHolder>(TenantDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TenantViewHolder {
        val binding = ItemTenantBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TenantViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: TenantViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class TenantViewHolder(
        private val binding: ItemTenantBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(tenant: TenantResponse) {
            binding.apply {
                // Tenant name
                tvTenantName.text = tenant.fullName
                
                // Phone
                tvPhone.text = tenant.phone
                
                // Property name
                tvPropertyName.text = tenant.propertyName
                
                // Current room (if available)
                if (!tenant.currentRoom.isNullOrEmpty()) {
                    tvRoomInfo.text = "Phòng: ${tenant.currentRoom}"
                } else {
                    tvRoomInfo.text = "Chưa thuê phòng"
                }
                
                // Identity number (masked)
                tvIdentity.text = "CMND: ${tenant.identityNumber}"
                
                // Email (if available)
                if (!tenant.email.isNullOrEmpty()) {
                    tvEmail.text = tenant.email
                } else {
                    tvEmail.text = "Chưa có email"
                }
                
                // Click listener
                root.setOnClickListener { onItemClick(tenant) }
            }
        }
    }
    
    class TenantDiffCallback : DiffUtil.ItemCallback<TenantResponse>() {
        override fun areItemsTheSame(oldItem: TenantResponse, newItem: TenantResponse): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: TenantResponse, newItem: TenantResponse): Boolean {
            return oldItem == newItem
        }
    }
}
