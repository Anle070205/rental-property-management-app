package com.example.rental_property_management_app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.databinding.ItemMarketplaceRoomBinding
import com.example.rental_property_management_app.models.MarketplaceRoom
import com.example.rental_property_management_app.models.RoomStatus
import com.example.rental_property_management_app.utils.CurrencyUtils

class TenantMarketplaceRoomAdapter(
    private val onRoomClick: (MarketplaceRoom) -> Unit
) : ListAdapter<MarketplaceRoom, TenantMarketplaceRoomAdapter.RoomViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        val binding = ItemMarketplaceRoomBinding.inflate(
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
        private val binding: ItemMarketplaceRoomBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(room: MarketplaceRoom) {
            val ctx = binding.root.context
            binding.apply {
                tvRoomCode.text = "Phòng ${room.roomCode}"
                tvRoomMeta.text = "Tầng ${room.floor} • ${room.area.stripTrailingZeros().toPlainString()} m² • Tối đa ${room.capacity} người"
                tvRentPrice.text = "${CurrencyUtils.formatVND(room.rentPrice)}/tháng"
                tvDeposit.text = "Cọc: ${CurrencyUtils.formatVND(room.deposit)}"

                tvStatus.text = room.status.getDisplayName()
                val (statusFg, statusBg) = when (room.status) {
                    RoomStatus.VACANT -> R.color.success to R.drawable.rc_badge_success
                    RoomStatus.OCCUPIED -> R.color.rc_brand to R.drawable.rc_badge
                    RoomStatus.EXPIRING_SOON -> R.color.warning to R.drawable.rc_badge_warning
                    RoomStatus.NEEDS_ATTENTION -> R.color.rc_danger to R.drawable.rc_icon_box_danger
                }
                tvStatus.setTextColor(ContextCompat.getColor(ctx, statusFg))
                tvStatus.setBackgroundResource(statusBg)

                tvAmenities.text = if (!room.amenities.isNullOrBlank()) {
                    "Tiện nghi: ${room.amenities}"
                } else {
                    "Tiện nghi: Cơ bản"
                }

                val fees = listOfNotNull(
                    room.electricityPrice?.let { "Điện ${CurrencyUtils.formatVND(it)}/kWh" },
                    room.waterPrice?.let { "Nước ${CurrencyUtils.formatVND(it)}/m³" },
                    room.internetFee?.let { "Wifi ${CurrencyUtils.formatVND(it)}" },
                    room.serviceFee?.let { "DV ${CurrencyUtils.formatVND(it)}" }
                )
                tvUtilitiesSummary.isVisible = fees.isNotEmpty()
                tvUtilitiesSummary.text = fees.joinToString(" • ")

                tvHealthScore.text = "Health Score: ${room.healthScore}/100"
                val (hsFg, hsBg) = when {
                    room.healthScore >= 80 -> R.color.health_good to R.drawable.rc_badge_success
                    room.healthScore >= 50 -> R.color.health_warning to R.drawable.rc_badge_warning
                    else -> R.color.health_risk to R.drawable.rc_icon_box_danger
                }
                tvHealthScore.setTextColor(ContextCompat.getColor(ctx, hsFg))
                tvHealthScore.setBackgroundResource(hsBg)

                tvPendingBadge.isVisible = room.hasPendingBookingByMe

                root.setOnClickListener { onRoomClick(room) }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<MarketplaceRoom>() {
        override fun areItemsTheSame(oldItem: MarketplaceRoom, newItem: MarketplaceRoom): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MarketplaceRoom, newItem: MarketplaceRoom): Boolean {
            return oldItem == newItem
        }
    }
}
