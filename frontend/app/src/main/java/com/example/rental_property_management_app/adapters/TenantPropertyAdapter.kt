package com.example.rental_property_management_app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.databinding.ItemTenantPropertyBinding
import com.example.rental_property_management_app.models.MarketplaceProperty
import com.example.rental_property_management_app.utils.CurrencyUtils

class TenantPropertyAdapter(
    private val onPropertyClick: (MarketplaceProperty) -> Unit
) : ListAdapter<MarketplaceProperty, TenantPropertyAdapter.PropertyViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PropertyViewHolder {
        val binding = ItemTenantPropertyBinding.inflate(
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
        private val binding: ItemTenantPropertyBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MarketplaceProperty) {
            val ctx = binding.root.context
            binding.apply {
                tvPropertyName.text = item.name
                tvAddress.text = item.address
                tvDescription.isVisible = !item.description.isNullOrBlank()
                tvDescription.text = item.description.orEmpty()

                val hasVacant = item.vacantRooms > 0
                tvVacantBadge.text = if (item.totalRooms == 0) {
                    "Chưa có phòng"
                } else {
                    "Còn ${item.vacantRooms}/${item.totalRooms} phòng trống"
                }
                tvVacantBadge.setBackgroundResource(
                    if (hasVacant) R.drawable.rc_badge_success else R.drawable.rc_badge_warning
                )
                tvVacantBadge.setTextColor(
                    ContextCompat.getColor(ctx, if (hasVacant) R.color.success else R.color.warning)
                )

                tvHealthBadge.text = "Health TB: ${item.averageHealthScore}/100"

                tvPriceRange.text = when {
                    item.minRentPrice == null -> "Chưa niêm yết giá"
                    item.maxRentPrice != null && item.maxRentPrice > item.minRentPrice ->
                        "Từ ${CurrencyUtils.formatVND(item.minRentPrice)}/tháng"
                    else -> "${CurrencyUtils.formatVND(item.minRentPrice)}/tháng"
                }

                tvLandlordContact.text = "${item.landlordName} • ${item.landlordPhone}"
                root.setOnClickListener { onPropertyClick(item) }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<MarketplaceProperty>() {
        override fun areItemsTheSame(oldItem: MarketplaceProperty, newItem: MarketplaceProperty): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MarketplaceProperty, newItem: MarketplaceProperty): Boolean {
            return oldItem == newItem
        }
    }
}
