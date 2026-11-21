package com.example.rental_property_management_app.fragments

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.RoomCareApplication
import com.example.rental_property_management_app.databinding.FragmentGuestStayDetailBinding
import com.example.rental_property_management_app.models.GuestStayResponse
import com.example.rental_property_management_app.models.guestStayRelationshipLabel
import com.example.rental_property_management_app.models.guestStayStatusLabel
import com.example.rental_property_management_app.utils.Resource
import com.example.rental_property_management_app.viewmodels.GuestStayViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class GuestStayDetailFragment : Fragment(R.layout.fragment_guest_stay_detail) {
    private var _binding: FragmentGuestStayDetailBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val viewModel: GuestStayViewModel by viewModels()

    private var guestStayId: Long = -1L
    private var isLandlord: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentGuestStayDetailBinding.bind(view)
        guestStayId = arguments?.getLong("guestStayId", -1L) ?: -1L
        isLandlord = arguments?.getBoolean("isLandlord", false)
            ?: RoomCareApplication.prefsManager.getUserRole().equals("LANDLORD", ignoreCase = true)

        if (guestStayId <= 0L) {
            findNavController().popBackStack()
            return
        }

        binding.btnCancelStay.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Hủy khai báo khách lưu trú")
                .setMessage("Bạn có chắc muốn hủy khai báo này?")
                .setNegativeButton("Không", null)
                .setPositiveButton("Hủy khai báo") { _, _ ->
                    viewModel.cancelGuestStay(guestStayId)
                }
                .show()
        }

        binding.btnDeleteStay.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Xóa dữ liệu khai báo")
                .setMessage("Dữ liệu khai báo lưu trú sẽ được xóa khỏi hệ thống.")
                .setNegativeButton("Đóng", null)
                .setPositiveButton("Xóa") { _, _ ->
                    viewModel.deleteGuestStay(guestStayId)
                }
                .show()
        }

        binding.btnMarkReceived.setOnClickListener { submitLandlordStatus("RECEIVED") }
        binding.btnMarkNeedsInfo.setOnClickListener { submitLandlordStatus("NEEDS_INFO") }
        binding.btnMarkCompleted.setOnClickListener { submitLandlordStatus("COMPLETED") }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.detailState.collect { res ->
                binding.progress.isVisible = res is Resource.Loading
                res?.data?.let(::bindDetail)
                if (res is Resource.Error) {
                    Toast.makeText(requireContext(), res.message, Toast.LENGTH_LONG).show()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.actionState.collect { res ->
                if (res is Resource.Success && res.data != null) {
                    Toast.makeText(requireContext(), res.message ?: "Đã cập nhật", Toast.LENGTH_SHORT).show()
                    bindDetail(res.data)
                    viewModel.resetActionState()
                } else if (res is Resource.Error) {
                    Toast.makeText(requireContext(), res.message, Toast.LENGTH_LONG).show()
                    viewModel.resetActionState()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.deleteState.collect { res ->
                if (res is Resource.Success) {
                    Toast.makeText(requireContext(), res.message ?: "Đã xóa", Toast.LENGTH_SHORT).show()
                    viewModel.resetDeleteState()
                    findNavController().popBackStack()
                } else if (res is Resource.Error) {
                    Toast.makeText(requireContext(), res.message, Toast.LENGTH_LONG).show()
                    viewModel.resetDeleteState()
                }
            }
        }

        viewModel.loadDetail(isLandlord, guestStayId)
    }

    private fun submitLandlordStatus(status: String) {
        val note = binding.etLandlordNote.text?.toString()?.trim()?.ifEmpty { null }
        viewModel.updateStatus(guestStayId, status, note)
    }

    private fun bindDetail(item: GuestStayResponse) {
        binding.tvGuestName.text = item.guestFullName
        binding.tvStatusBadge.text = guestStayStatusLabel(item.status)
        val (bgRes, colorRes) = when (item.status.uppercase()) {
            "RECEIVED", "COMPLETED" -> R.drawable.rc_badge_success to R.color.success
            "NEEDS_INFO" -> R.drawable.rc_badge_warning to R.color.warning
            "CANCELLED" -> R.drawable.rc_badge_danger to R.color.rc_danger
            else -> R.drawable.rc_badge to R.color.rc_brand
        }
        binding.tvStatusBadge.setBackgroundResource(bgRes)
        binding.tvStatusBadge.setTextColor(ContextCompat.getColor(requireContext(), colorRes))

        val lines = buildList {
            add("• Phòng lưu trú: ${item.roomCode ?: "N/A"}${item.propertyName?.let { " ($it)" } ?: ""}")
            item.tenantName?.let { add("• Người thuê khai báo: $it") }
            add("• Số CCCD (che bớt): ${item.maskedIdentityNumber}${if (item.qrVerified) " (Đã quét QR)" else ""}")
            item.guestDateOfBirth?.let { add("• Ngày sinh: $it") }
            item.guestGender?.let { add("• Giới tính: $it") }
            item.guestPhone?.let { add("• Liên hệ: $it") }
            add("• Quan hệ: ${guestStayRelationshipLabel(item.relationship)}")
            add("• Bắt đầu: ${item.stayStartAt.replace("T", " ").take(16)}")
            add("• Dự kiến kết thúc: ${item.stayEndAt.replace("T", " ").take(16)}")
            item.note?.let { add("• Ghi chú người thuê: $it") }
            item.landlordNote?.let { add("• Phản hồi chủ trọ: $it") }
            item.retentionUntil?.let { add("• Thời hạn lưu dữ liệu đến: ${it.take(10)}") }
        }
        binding.tvDetails.text = lines.joinToString("\n")
        binding.tvDisclaimer.text = item.legalDisclaimer
            ?: "Thông báo nội bộ RoomCare gửi chủ nhà trọ; không thay thế thủ tục đăng ký tạm trú với cơ quan chức năng."

        val status = item.status.uppercase()
        binding.btnCancelStay.isVisible = !isLandlord && (status == "PENDING" || status == "NEEDS_INFO")
        binding.btnDeleteStay.isVisible = !isLandlord && (status == "CANCELLED" || status == "COMPLETED")
        binding.layoutLandlordActions.isVisible = isLandlord && status != "CANCELLED"
        if (binding.etLandlordNote.text.isNullOrBlank() && !item.landlordNote.isNullOrBlank()) {
            binding.etLandlordNote.setText(item.landlordNote)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
