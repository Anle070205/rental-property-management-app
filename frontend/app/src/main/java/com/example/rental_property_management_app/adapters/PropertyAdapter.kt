package com.example.rental_property_management_app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.rental_property_management_app.databinding.ItemPropertySelectionBinding
import com.example.rental_property_management_app.models.PropertyResponse

/**
 * RecyclerView Adapter cho Property Selection
 */
class PropertyAdapter(
    private val onItemClick: (PropertyResponse) -> Unit
) : ListAdapter<PropertyResponse, PropertyAdapter.PropertyViewHolder>(PropertyDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PropertyViewHolder {
        val binding = ItemPropertySelectionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PropertyViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: PropertyViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class PropertyViewHolder(
        private val binding: ItemPropertySelectionBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(property: PropertyResponse) {
            binding.apply {
                tvPropertyName.text = property.name
                tvAddress.text = property.address
                tvRoomStats.text = "${property.totalRooms} phòng (${property.vacantRooms} trống)"
                
                root.setOnClickListener { onItemClick(property) }
            }
        }
    }
    
    class PropertyDiffCallback : DiffUtil.ItemCallback<PropertyResponse>() {
        override fun areItemsTheSame(oldItem: PropertyResponse, newItem: PropertyResponse): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: PropertyResponse, newItem: PropertyResponse): Boolean {
            return oldItem == newItem
        }
    }
}
