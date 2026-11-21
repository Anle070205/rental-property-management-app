package com.example.rental_property_management_app.fragments

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.databinding.FragmentMyRoomBinding
import com.example.rental_property_management_app.models.MyRoom
import com.example.rental_property_management_app.utils.Constants
import com.example.rental_property_management_app.utils.CurrencyUtils
import com.example.rental_property_management_app.utils.DateUtils
import com.example.rental_property_management_app.utils.Resource
import com.example.rental_property_management_app.viewmodels.MyRoomViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class MyRoomFragment : Fragment(R.layout.fragment_my_room) {
    private var _binding: FragmentMyRoomBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val viewModel: MyRoomViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentMyRoomBinding.bind(view)
        val nav = findNavController()
        binding.toolbar.setNavigationOnClickListener { nav.navigateUp() }
        binding.retry.setOnClickListener { viewModel.load() }
        binding.btnExploreRooms.setOnClickListener {
            nav.navigate(R.id.tenantPropertyListFragment)
        }
        binding.btnViewContract.setOnClickListener {
            nav.navigate(R.id.myContractFragment)
        }
        binding.btnViewInvoices.setOnClickListener {
            nav.navigate(R.id.tenantInvoiceFragment)
        }
        binding.btnReportIncident.setOnClickListener {
            nav.navigate(R.id.incidentListFragment)
        }
        binding.btnGuestStay.setOnClickListener {
            nav.navigate(R.id.guestStayListFragment)
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.room.collect { result ->
                val room = result.data
                binding.progress.isVisible = result is Resource.Loading
                binding.retry.isVisible = result is Resource.Error
                val hasNoRoom = result is Resource.Success && room?.roomCode == null
                binding.btnExploreRooms.isVisible = hasNoRoom
                binding.status.text = result.message ?: getString(R.string.rc_no_active_room)
                binding.status.isVisible = result is Resource.Error || hasNoRoom
                binding.content.isVisible = room?.roomCode != null
                if (room?.roomCode != null) display(room)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.load()
    }

    private fun display(room: MyRoom) {
        binding.sample.isVisible = Constants.USE_MOCK_TENANT_DATA

        val (statusText, statusBg, statusColor) = when (room.roomStatus?.uppercase()) {
            "VACANT" -> Triple("Phòng trống", R.drawable.rc_badge, R.color.rc_brand)
            "EXPIRING_SOON" -> Triple("Sắp hết hạn", R.drawable.rc_badge_warning, R.color.warning)
            "NEEDS_ATTENTION" -> Triple("Cần chú ý", R.drawable.rc_badge_warning, R.color.warning)
            else -> Triple("Đang thuê", R.drawable.rc_badge_success, R.color.success)
        }
        binding.tvRoomStatus.text = statusText
        binding.tvRoomStatus.setBackgroundResource(statusBg)
        binding.tvRoomStatus.setTextColor(ContextCompat.getColor(requireContext(), statusColor))

        val codeLabel = room.roomCode.orEmpty()
        binding.roomCode.text = listOfNotNull(
            codeLabel.takeIf { it.isNotBlank() },
            room.floor?.let { "Tầng $it" }
        ).joinToString(" · ")

        val occ = room.currentOccupants ?: 1
        val specs = listOfNotNull(
            room.area?.let { "${it.stripTrailingZeros().toPlainString()} m²" },
            room.capacity?.let { "Đang ở $occ/$it người" }
        )
        binding.tvRoomSpecs.isVisible = specs.isNotEmpty()
        binding.tvRoomSpecs.text = specs.joinToString(" · ")

        binding.rentPrice.text = getString(
            R.string.rc_room_rent,
            room.rentPrice?.let(CurrencyUtils::formatVND).orEmpty()
        )
        binding.tvDeposit.isVisible = room.deposit != null
        binding.tvDeposit.text = room.deposit?.let { "Cọc: ${CurrencyUtils.formatVND(it)}" }.orEmpty()

        val score = room.healthScore ?: 100
        binding.tvHealthScore.text = "Health Score: $score/100"

        // Contract summary
        if (!room.contractCode.isNullOrBlank() || !room.contractStartDate.isNullOrBlank()) {
            val contractStatusVi = when (room.contractStatus?.uppercase()) {
                "ACTIVE" -> "Đang hiệu lực"
                "SIGNED" -> "Đã ký"
                "DRAFT" -> "Nháp"
                "EXPIRED" -> "Hết hạn"
                "TERMINATED" -> "Đã kết thúc"
                else -> room.contractStatus.orEmpty()
            }
            binding.tvContractCodeStatus.text = listOfNotNull(
                room.contractCode?.let { "Hợp đồng: $it" },
                contractStatusVi.takeIf { it.isNotBlank() }
            ).joinToString(" · ")

            val startFmt = room.contractStartDate?.let { DateUtils.parseApiDate(it)?.let(DateUtils::formatDate) ?: it }
            val endFmt = room.contractEndDate?.let { DateUtils.parseApiDate(it)?.let(DateUtils::formatDate) ?: it }
            binding.tvContractPeriod.isVisible = startFmt != null && endFmt != null
            binding.tvContractPeriod.text = "Thời hạn: $startFmt – $endFmt"

            binding.tvContractPaymentDay.isVisible = room.paymentDayOfMonth != null
            binding.tvContractPaymentDay.text = room.paymentDayOfMonth?.let { "Ngày thanh toán: Ngày $it hàng tháng" }.orEmpty()
        } else {
            binding.tvContractCodeStatus.text = "Chưa có hợp đồng chính thức gắn với phòng"
            binding.tvContractPeriod.isVisible = false
            binding.tvContractPaymentDay.isVisible = false
        }

        val occupantsText = buildList {
            room.primaryTenantName?.takeIf { it.isNotBlank() }?.let { add("Người thuê chính: $it") }
            if (room.roommates.isNotEmpty()) add("Người ở cùng: ${room.roommates.joinToString(", ")}")
        }
        binding.tvPrimaryAndRoommates.isVisible = occupantsText.isNotEmpty()
        binding.tvPrimaryAndRoommates.text = occupantsText.joinToString("\n")

        // Address, utility rates, amenities
        binding.address.text = getString(R.string.rc_room_address, room.address.orEmpty())
        val utilityParts = listOfNotNull(
            room.electricityPrice?.let { "Điện: ${CurrencyUtils.formatVND(it)}/kWh" },
            room.waterPrice?.let { "Nước: ${CurrencyUtils.formatVND(it)}/m³" },
            room.internetFee?.let { "Wifi: ${CurrencyUtils.formatVND(it)}" },
            room.serviceFee?.let { "Dịch vụ: ${CurrencyUtils.formatVND(it)}" }
        )
        binding.tvUtilityFees.text = if (utilityParts.isNotEmpty()) {
            utilityParts.joinToString(" · ")
        } else {
            "Chưa cập nhật đơn giá điện, nước"
        }
        binding.amenities.text = getString(
            R.string.rc_room_amenities,
            if (room.amenities.isNotEmpty()) room.amenities.joinToString(", ") else "Chưa cập nhật"
        )
        binding.tvRoomDescription.isVisible = !room.description.isNullOrBlank()
        binding.tvRoomDescription.text = room.description.orEmpty()

        // Latest invoice
        if (!room.latestInvoiceMonth.isNullOrBlank() && room.latestInvoiceAmount != null) {
            binding.tvLatestInvoiceMonth.text = "Kỳ ${room.latestInvoiceMonth}"
            binding.tvLatestInvoiceAmount.text = CurrencyUtils.formatVND(room.latestInvoiceAmount)
            val (invLabel, invBg, invColor) = when (room.latestInvoiceStatus?.uppercase()) {
                "PAID" -> Triple("Đã thanh toán", R.drawable.rc_badge_success, R.color.success)
                "PARTIALLY_PAID" -> Triple("Thanh toán một phần", R.drawable.rc_badge_warning, R.color.warning)
                "OVERDUE" -> Triple("Quá hạn", R.drawable.rc_badge_danger, R.color.rc_danger)
                else -> Triple("Chưa thanh toán", R.drawable.rc_badge, R.color.rc_brand)
            }
            binding.tvLatestInvoiceStatus.isVisible = true
            binding.tvLatestInvoiceStatus.text = invLabel
            binding.tvLatestInvoiceStatus.setBackgroundResource(invBg)
            binding.tvLatestInvoiceStatus.setTextColor(ContextCompat.getColor(requireContext(), invColor))
        } else {
            binding.tvLatestInvoiceMonth.text = "Chưa có hóa đơn nào"
            binding.tvLatestInvoiceAmount.text = "Hóa đơn mới sẽ hiển thị tại đây"
            binding.tvLatestInvoiceStatus.isVisible = false
        }

        // Landlord contact
        binding.landlord.text = getString(
            R.string.rc_room_landlord,
            listOfNotNull(room.landlordName, room.landlordPhone).joinToString(" · ")
        )
        val phone = room.landlordPhone
        binding.dial.isVisible = !phone.isNullOrBlank()
        binding.dial.setOnClickListener {
            if (!phone.isNullOrBlank()) {
                try {
                    startActivity(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phone, null)))
                } catch (_: ActivityNotFoundException) {
                    Snackbar.make(binding.root, R.string.rc_dial_unavailable, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
