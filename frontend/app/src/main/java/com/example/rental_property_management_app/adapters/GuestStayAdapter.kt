package com.example.rental_property_management_app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.databinding.ItemGuestStayBinding
import com.example.rental_property_management_app.models.GuestStayResponse
import com.example.rental_property_management_app.models.guestStayRelationshipLabel
import com.example.rental_property_management_app.models.guestStayStatusLabel

class GuestStayAdapter(
    private val onClick: (GuestStayResponse) -> Unit
) : ListAdapter<GuestStayResponse, GuestStayAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGuestStayBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemGuestStayBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: GuestStayResponse) {
            binding.tvGuestName.text = item.guestFullName
            binding.tvIdentityMasked.text =
                "CCCD: ${item.maskedIdentityNumber} · ${guestStayRelationshipLabel(item.relationship)}"
            binding.tvQrBadge.isVisible = item.qrVerified
            val roomLabel = item.roomCode?.let { "Phòng $it" } ?: "Phòng đang thuê"
            val start = item.stayStartAt.replace("T", " ").take(16)
            val end = item.stayEndAt.replace("T", " ").take(16)
            binding.tvRoomAndTime.text = "$roomLabel · $start → $end"

            binding.tvStatusBadge.text = guestStayStatusLabel(item.status)
            val (bgRes, textColorRes) = when (item.status.uppercase()) {
                "RECEIVED", "COMPLETED" -> R.drawable.rc_badge_success to R.color.success
                "NEEDS_INFO" -> R.drawable.rc_badge_warning to R.color.warning
                "CANCELLED" -> R.drawable.rc_badge_danger to R.color.rc_danger
                else -> R.drawable.rc_badge to R.color.rc_brand
            }
            binding.tvStatusBadge.setBackgroundResource(bgRes)
            binding.tvStatusBadge.setTextColor(
                ContextCompat.getColor(binding.root.context, textColorRes)
            )
            binding.root.setOnClickListener { onClick(item) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<GuestStayResponse>() {
            override fun areItemsTheSame(oldItem: GuestStayResponse, newItem: GuestStayResponse): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: GuestStayResponse, newItem: GuestStayResponse): Boolean =
                oldItem == newItem
        }
    }
}
