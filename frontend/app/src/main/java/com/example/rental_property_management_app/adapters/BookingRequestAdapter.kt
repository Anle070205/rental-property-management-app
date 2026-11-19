package com.example.rental_property_management_app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.databinding.ItemBookingRequestBinding
import com.example.rental_property_management_app.models.BookingRequestItem
import com.example.rental_property_management_app.models.BookingRequestStatus
import com.example.rental_property_management_app.models.BookingRequestType
import com.example.rental_property_management_app.utils.CurrencyUtils
import com.example.rental_property_management_app.utils.DateUtils

class BookingRequestAdapter(
    private val isLandlordMode: Boolean,
    private val onCallClick: (String) -> Unit,
    private val onApproveClick: (BookingRequestItem) -> Unit = {},
    private val onRejectClick: (BookingRequestItem) -> Unit = {},
    private val onCancelClick: (BookingRequestItem) -> Unit = {}
) : ListAdapter<BookingRequestItem, BookingRequestAdapter.BookingViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val binding = ItemBookingRequestBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BookingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BookingViewHolder(
        private val binding: ItemBookingRequestBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: BookingRequestItem) {
            val ctx = binding.root.context
            binding.apply {
                tvTypeBadge.text = item.requestType.getDisplayName()
                tvStatusBadge.text = item.status.getDisplayName()

                val (fgColor, bgRes) = when (item.status) {
                    BookingRequestStatus.APPROVED -> R.color.success to R.drawable.rc_badge_success
                    BookingRequestStatus.PENDING -> R.color.warning to R.drawable.rc_badge_warning
                    BookingRequestStatus.REJECTED, BookingRequestStatus.CANCELLED ->
                        R.color.rc_danger to R.drawable.rc_icon_box_danger
                }
                tvStatusBadge.setTextColor(ContextCompat.getColor(ctx, fgColor))
                tvStatusBadge.setBackgroundResource(bgRes)

                tvRoomTitle.text = "Phòng ${item.roomCode} • ${item.propertyName}"
                val rentStr = item.rentPrice?.let { "${CurrencyUtils.formatVND(it)}/tháng" }.orEmpty()
                tvPriceAndScore.text = if (rentStr.isNotEmpty()) {
                    "$rentStr • Health Score: ${item.roomHealthScore}/100"
                } else {
                    "Health Score: ${item.roomHealthScore}/100"
                }

                val phoneToCall: String
                if (isLandlordMode) {
                    val cccd = item.identityNumber?.takeIf { it.isNotBlank() }?.let { " • CCCD: $it" }.orEmpty()
                    tvCounterpart.text = "Người thuê: ${item.tenantName} • ${item.tenantPhone}$cccd"
                    phoneToCall = item.tenantPhone
                } else {
                    tvCounterpart.text = "Chủ nhà: ${item.landlordName} • ${item.landlordPhone}"
                    phoneToCall = item.landlordPhone
                }

                val formattedMoveIn = item.moveInDate?.let {
                    DateUtils.parseApiDate(it)?.let(DateUtils::formatDate) ?: it
                }
                tvMoveInDate.isVisible = !formattedMoveIn.isNullOrBlank()
                tvMoveInDate.text = "Dự kiến nhận phòng: $formattedMoveIn"

                tvMessage.isVisible = !item.message.isNullOrBlank()
                tvMessage.text = "Lời nhắn: ${item.message.orEmpty()}"

                tvLandlordNote.isVisible = !item.landlordNote.isNullOrBlank()
                tvLandlordNote.text = "Phản hồi chủ nhà: ${item.landlordNote.orEmpty()}"

                btnCall.isVisible = phoneToCall.isNotBlank()
                btnCall.setOnClickListener { onCallClick(phoneToCall) }

                val isPending = item.status == BookingRequestStatus.PENDING
                btnApprove.isVisible = isLandlordMode && isPending
                btnReject.isVisible = isLandlordMode && isPending
                btnCancel.isVisible = !isLandlordMode && isPending

                btnApprove.text = if (item.requestType == BookingRequestType.BOOKING) {
                    "Duyệt đặt phòng"
                } else {
                    "Phản hồi liên hệ"
                }
                btnApprove.setOnClickListener { onApproveClick(item) }
                btnReject.setOnClickListener { onRejectClick(item) }
                btnCancel.setOnClickListener { onCancelClick(item) }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<BookingRequestItem>() {
        override fun areItemsTheSame(oldItem: BookingRequestItem, newItem: BookingRequestItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: BookingRequestItem, newItem: BookingRequestItem): Boolean {
            return oldItem == newItem
        }
    }
}
