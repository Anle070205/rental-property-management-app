package com.example.rental_property_management_app.fragments

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.databinding.FragmentTenantMarketplaceRoomDetailBinding
import com.example.rental_property_management_app.models.BookingRequestType
import com.example.rental_property_management_app.models.CreateBookingRequest
import com.example.rental_property_management_app.models.MarketplaceRoom
import com.example.rental_property_management_app.models.RoomStatus
import com.example.rental_property_management_app.utils.CurrencyUtils
import com.example.rental_property_management_app.utils.DateUtils
import com.example.rental_property_management_app.utils.Resource
import com.example.rental_property_management_app.viewmodels.MarketplaceBookingViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import java.util.Calendar

class TenantMarketplaceRoomDetailFragment : Fragment() {

    private var _binding: FragmentTenantMarketplaceRoomDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MarketplaceBookingViewModel by viewModels()
    private var roomId: Long = 0L
    private var currentRoom: MarketplaceRoom? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTenantMarketplaceRoomDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        roomId = arguments?.getLong("roomId", 0L) ?: 0L
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.btnRetry.setOnClickListener { viewModel.loadRoomDetail(roomId) }

        observeStates()
        viewModel.loadRoomDetail(roomId)
    }

    private fun observeStates() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.roomDetailState.collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        binding.progressBar.isVisible = true
                        binding.contentLayout.isVisible = false
                        binding.layoutError.isVisible = false
                    }
                    is Resource.Success -> {
                        binding.progressBar.isVisible = false
                        binding.layoutError.isVisible = false
                        binding.contentLayout.isVisible = true
                        resource.data?.let { bindRoom(it) }
                    }
                    is Resource.Error -> {
                        binding.progressBar.isVisible = false
                        binding.contentLayout.isVisible = false
                        binding.layoutError.isVisible = true
                        binding.tvError.text = resource.message ?: getString(R.string.error_loading)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.actionState.collect { resource ->
                resource ?: return@collect
                when (resource) {
                    is Resource.Loading -> Unit
                    is Resource.Success -> {
                        val msg = if (resource.data?.requestType == BookingRequestType.BOOKING) {
                            getString(R.string.rc_booking_sent_success)
                        } else {
                            getString(R.string.rc_contact_sent_success)
                        }
                        Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG)
                            .setAction(R.string.rc_view_requests) {
                                findNavController().navigate(R.id.tenantBookingListFragment)
                            }
                            .show()
                        viewModel.consumeActionState()
                        viewModel.loadRoomDetail(roomId)
                    }
                    is Resource.Error -> {
                        Snackbar.make(binding.root, resource.message ?: getString(R.string.error_server), Snackbar.LENGTH_LONG).show()
                        viewModel.consumeActionState()
                    }
                }
            }
        }
    }

    private fun bindRoom(room: MarketplaceRoom) {
        currentRoom = room
        val ctx = requireContext()
        binding.apply {
            tvStatus.text = room.status.getDisplayName()
            val (statusFg, statusBg) = when (room.status) {
                RoomStatus.VACANT -> R.color.success to R.drawable.rc_badge_success
                RoomStatus.OCCUPIED -> R.color.rc_brand to R.drawable.rc_badge
                RoomStatus.EXPIRING_SOON -> R.color.warning to R.drawable.rc_badge_warning
                RoomStatus.NEEDS_ATTENTION -> R.color.rc_danger to R.drawable.rc_icon_box_danger
            }
            tvStatus.setTextColor(ContextCompat.getColor(ctx, statusFg))
            tvStatus.setBackgroundResource(statusBg)

            tvRoomCode.text = "Phòng ${room.roomCode}"
            tvPropertyInfo.text = "${room.propertyName} • ${room.propertyAddress}"
            tvRentPrice.text = "${CurrencyUtils.formatVND(room.rentPrice)}/tháng"
            tvSpecs.text = "Tầng ${room.floor} • ${room.area.stripTrailingZeros().toPlainString()} m² • Tối đa ${room.capacity} người • Cọc: ${CurrencyUtils.formatVND(room.deposit)}"

            tvHealthScoreValue.text = "${room.healthScore}/100 • ${room.healthRatingLabel}"
            progressHealth.progress = room.healthScore
            val (hsFg, hsBg) = when {
                room.healthScore >= 80 -> R.color.health_good to R.drawable.rc_badge_success
                room.healthScore >= 50 -> R.color.health_warning to R.drawable.rc_badge_warning
                else -> R.color.health_risk to R.drawable.rc_icon_box_danger
            }
            val hsColor = ContextCompat.getColor(ctx, hsFg)
            tvHealthScoreValue.setTextColor(hsColor)
            tvHealthScoreValue.setBackgroundResource(hsBg)
            progressHealth.setIndicatorColor(hsColor)

            val fees = listOfNotNull(
                room.electricityPrice?.let { "Điện: ${CurrencyUtils.formatVND(it)}/kWh" },
                room.waterPrice?.let { "Nước: ${CurrencyUtils.formatVND(it)}/m³" },
                room.internetFee?.let { "Wifi: ${CurrencyUtils.formatVND(it)}" },
                room.serviceFee?.let { "Dịch vụ: ${CurrencyUtils.formatVND(it)}" }
            )
            tvUtilities.text = if (fees.isNotEmpty()) fees.joinToString(" • ") else "Thỏa thuận với chủ nhà"
            tvAmenities.text = room.amenities?.takeIf { it.isNotBlank() } ?: "Trang bị cơ bản"
            tvDescription.text = room.description?.takeIf { it.isNotBlank() } ?: "Chưa có mô tả chi tiết"

            val emailPart = room.landlordEmail?.takeIf { it.isNotBlank() }?.let { "\nEmail: $it" }.orEmpty()
            tvLandlordContact.text = "${room.landlordName} • ${room.landlordPhone}$emailPart"

            btnCallLandlord.isVisible = room.landlordPhone.isNotBlank()
            btnCallLandlord.setOnClickListener { dialPhone(room.landlordPhone) }
            btnContactLandlord.setOnClickListener { showContactDialog(room) }

            val canBook = room.status == RoomStatus.VACANT && !room.hasPendingBookingByMe
            btnBookRoom.isEnabled = canBook
            btnBookRoom.text = when {
                room.hasPendingBookingByMe -> getString(R.string.rc_booking_already_pending)
                room.status != RoomStatus.VACANT -> "Phòng không còn trống"
                else -> getString(R.string.rc_book_room_now)
            }
            btnBookRoom.setOnClickListener { showBookDialog(room) }
        }
    }

    private fun showContactDialog(room: MarketplaceRoom) {
        val ctx = requireContext()
        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 20, 48, 8)
        }
        val tilMsg = TextInputLayout(ctx).apply {
            hint = getString(R.string.rc_contact_message_hint)
        }
        val etMsg = TextInputEditText(ctx).apply {
            setText("Chào anh/chị ${room.landlordName}, tôi muốn tìm hiểu và hẹn xem phòng ${room.roomCode} tại ${room.propertyName}.")
        }
        tilMsg.addView(etMsg)
        container.addView(tilMsg)

        val dialog = MaterialAlertDialogBuilder(ctx)
            .setTitle("Liên hệ chủ nhà - Phòng ${room.roomCode}")
            .setView(container)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.rc_send_request, null)
            .create()

        dialog.show()
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val msg = etMsg.text?.toString()?.trim().orEmpty()
            if (msg.isEmpty()) {
                tilMsg.error = getString(R.string.rc_required)
                return@setOnClickListener
            }
            viewModel.submitBookingOrContact(
                CreateBookingRequest(
                    roomId = room.id,
                    requestType = BookingRequestType.CONTACT,
                    message = msg
                )
            )
            dialog.dismiss()
        }
    }

    private fun showBookDialog(room: MarketplaceRoom) {
        val ctx = requireContext()
        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 20, 48, 8)
        }

        val defaultCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 7) }
        val defaultDateStr = DateUtils.formatToApiDate(defaultCal.time)

        val tilDate = TextInputLayout(ctx).apply {
            hint = getString(R.string.rc_move_in_date_hint)
        }
        val etDate = TextInputEditText(ctx).apply {
            setText(defaultDateStr)
        }
        tilDate.addView(etDate)
        container.addView(tilDate)

        val tilCccd = TextInputLayout(ctx).apply {
            hint = "Số CCCD/CMND (tùy chọn)"
            setPadding(0, 14, 0, 0)
        }
        val etCccd = TextInputEditText(ctx)
        tilCccd.addView(etCccd)
        container.addView(tilCccd)

        val tilNote = TextInputLayout(ctx).apply {
            hint = "Lời nhắn cho chủ nhà"
            setPadding(0, 14, 0, 0)
        }
        val etNote = TextInputEditText(ctx).apply {
            setText("Tôi muốn đặt thuê phòng ${room.roomCode} và giữ tình trạng chuẩn Health Score ${room.healthScore}/100.")
        }
        tilNote.addView(etNote)
        container.addView(tilNote)

        val dialog = MaterialAlertDialogBuilder(ctx)
            .setTitle("Đặt phòng ${room.roomCode}")
            .setMessage("Giá thuê: ${CurrencyUtils.formatVND(room.rentPrice)}/tháng • Cọc: ${CurrencyUtils.formatVND(room.deposit)}")
            .setView(container)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.rc_confirm_booking, null)
            .create()

        dialog.show()
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val dateStr = etDate.text?.toString()?.trim().orEmpty()
            if (dateStr.isNotEmpty() && DateUtils.parseApiDate(dateStr) == null) {
                tilDate.error = "Nhập ngày theo định dạng yyyy-MM-dd"
                return@setOnClickListener
            }
            tilDate.error = null
            viewModel.submitBookingOrContact(
                CreateBookingRequest(
                    roomId = room.id,
                    requestType = BookingRequestType.BOOKING,
                    moveInDate = dateStr.ifEmpty { null },
                    identityNumber = etCccd.text?.toString()?.trim()?.ifEmpty { null },
                    message = etNote.text?.toString()?.trim()?.ifEmpty { null }
                )
            )
            dialog.dismiss()
        }
    }

    private fun dialPhone(phone: String) {
        try {
            startActivity(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phone, null)))
        } catch (_: ActivityNotFoundException) {
            Snackbar.make(binding.root, R.string.rc_dial_unavailable, Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
