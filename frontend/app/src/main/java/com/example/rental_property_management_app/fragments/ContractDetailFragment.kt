package com.example.rental_property_management_app.fragments

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.databinding.FragmentContractDetailBinding
import com.example.rental_property_management_app.models.*
import com.example.rental_property_management_app.utils.CccdQrParser
import com.example.rental_property_management_app.utils.CurrencyUtils
import com.example.rental_property_management_app.utils.Resource
import com.example.rental_property_management_app.viewmodels.ContractViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ContractDetailFragment : Fragment() {

    private var _binding: FragmentContractDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ContractViewModel by viewModels()
    private var contractId: Long = 0L
    private var currentContract: ContractResponse? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentContractDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        contractId = arguments?.getLong("contractId", 0L) ?: 0L
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.swipeRefresh.setOnRefreshListener {
            if (contractId > 0L) viewModel.loadContractDetail(contractId)
        }

        binding.btnSignContract.setOnClickListener {
            viewModel.signContract(contractId)
        }
        binding.btnActivateContract.setOnClickListener {
            viewModel.activateContract(contractId)
        }
        binding.btnTerminateContract.setOnClickListener {
            showTerminateDialog()
        }
        binding.btnProvisionTenantAccount.setOnClickListener {
            val c = currentContract ?: return@setOnClickListener
            findNavController().navigate(R.id.tenantDetailFragment, bundleOf("tenantId" to c.tenantId))
        }
        binding.btnCreateInvoiceFromContract.setOnClickListener {
            val c = currentContract ?: return@setOnClickListener
            findNavController().navigate(
                R.id.invoiceFragment,
                bundleOf("roomId" to c.roomId, "contractId" to c.id)
            )
        }
        binding.btnAddRoommate.setOnClickListener {
            val c = currentContract ?: return@setOnClickListener
            showAddRoommateDialog(c)
        }

        observeStates()
        if (contractId > 0L) {
            viewModel.loadContractDetail(contractId)
        }
    }

    private fun observeStates() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.contractDetailState.collectLatest { resource ->
                when (resource) {
                    is Resource.Loading -> binding.progressBar.visibility = View.VISIBLE
                    is Resource.Success -> {
                        binding.swipeRefresh.isRefreshing = false
                        binding.progressBar.visibility = View.GONE
                        resource.data?.let { bindContract(it) }
                    }
                    is Resource.Error -> {
                        binding.swipeRefresh.isRefreshing = false
                        binding.progressBar.visibility = View.GONE
                        Snackbar.make(binding.root, resource.message ?: "Không tải được hợp đồng", Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.actionState.collectLatest { resource ->
                resource ?: return@collectLatest
                when (resource) {
                    is Resource.Loading -> Unit
                    is Resource.Success -> {
                        Snackbar.make(binding.root, "Cập nhật trạng thái hợp đồng thành công", Snackbar.LENGTH_SHORT).show()
                        viewModel.resetActionState()
                    }
                    is Resource.Error -> {
                        AlertDialog.Builder(requireContext())
                            .setTitle("Thao tác hợp đồng thất bại")
                            .setMessage(resource.message ?: "Vui lòng thử lại")
                            .setPositiveButton("Đóng", null)
                            .show()
                        viewModel.resetActionState()
                    }
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.roommateActionState.collectLatest { resource ->
                resource ?: return@collectLatest
                when (resource) {
                    is Resource.Loading -> Unit
                    is Resource.Success -> {
                        Snackbar.make(binding.root, resource.data ?: "Đã cập nhật người ở cùng", Snackbar.LENGTH_SHORT).show()
                        viewModel.resetRoommateActionState()
                    }
                    is Resource.Error -> {
                        AlertDialog.Builder(requireContext())
                            .setTitle("Không thể lưu người ở cùng")
                            .setMessage(resource.message ?: "Vui lòng kiểm tra lại thông tin")
                            .setPositiveButton("Đóng", null)
                            .show()
                        viewModel.resetRoommateActionState()
                    }
                }
            }
        }
    }

    private fun bindContract(c: ContractResponse) {
        currentContract = c
        val ctx = requireContext()
        binding.apply {
            tvContractCode.text = c.contractCode
            tvStatusBadge.text = c.status.getDisplayName()
            tvStatusBadge.setTextColor(ContextCompat.getColor(ctx, c.status.getTextColorRes()))
            tvStatusBadge.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(ctx, c.status.getBgColorRes())
            )
            tvRoomProperty.text = "Phòng ${c.roomCode} • ${c.propertyName}"
            val contact = c.tenantPhone ?: c.tenantEmail ?: "Chưa có liên hệ"
            tvTenantInfo.text = "Khách đại diện: ${c.tenantName} ($contact)"
            tvTenantAccountStatus.text = when (c.tenantAccountStatus) {
                "ACTIVE" -> "Trạng thái tài khoản: Đã kích hoạt"
                "PENDING_ACTIVATION" -> "Trạng thái tài khoản: Chờ khách kích hoạt OTP"
                else -> "Trạng thái tài khoản: Chưa tạo tài khoản cổng khách thuê"
            }
            tvContractPeriod.text = "Thời hạn: ${formatDate(c.startDate)} → ${formatDate(c.endDate)}"
            val lifecycleNotes = listOfNotNull(
                c.signedAt?.let { "Đã ký: ${it.take(16).replace('T', ' ')}" },
                c.activatedAt?.let { "Kích hoạt: ${it.take(16).replace('T', ' ')}" },
                c.terminatedAt?.let { "Thanh lý: ${it.take(16).replace('T', ' ')}" },
                c.terminationReason?.takeIf { it.isNotBlank() }?.let { "Lý do kết thúc: $it" }
            )
            tvContractLifecycleInfo.text = lifecycleNotes.joinToString("\n")
            tvContractLifecycleInfo.visibility = if (lifecycleNotes.isEmpty()) View.GONE else View.VISIBLE

            tvPricingLines.text = buildString {
                append("• Tiền phòng: ${CurrencyUtils.formatVND(c.monthlyRent)}/tháng\n")
                append("• Tiền cọc: ${CurrencyUtils.formatVND(c.depositAmount)}\n")
                append("• Đơn giá điện: ${CurrencyUtils.formatVND(c.electricityUnitPrice)}/kWh\n")
                append("• Đơn giá nước: ${CurrencyUtils.formatVND(c.waterUnitPrice)} (${c.waterBillingMode.getDisplayName()})\n")
                append("• Internet: ${CurrencyUtils.formatVND(c.internetFee)}\n")
                append("• Phí rác: ${CurrencyUtils.formatVND(c.garbageFee)}\n")
                append("• Dịch vụ chung: ${CurrencyUtils.formatVND(c.serviceFee)}\n")
                append("• Ngày đóng hàng tháng: Ngày ${c.paymentDayOfMonth}")
            }
            tvTerms.text = c.terms?.takeIf { it.isNotBlank() }?.let { "Điều khoản: $it" } ?: "Chưa ghi chú điều khoản bổ sung"

            btnSignContract.visibility = if (c.status == ContractStatus.DRAFT) View.VISIBLE else View.GONE
            btnActivateContract.visibility = if (c.status == ContractStatus.SIGNED) View.VISIBLE else View.GONE
            btnCreateInvoiceFromContract.visibility = if (c.status == ContractStatus.ACTIVE) View.VISIBLE else View.GONE
            btnTerminateContract.visibility = if (c.status == ContractStatus.DRAFT || c.status == ContractStatus.SIGNED || c.status == ContractStatus.ACTIVE) View.VISIBLE else View.GONE

            llRoommates.removeAllViews()
            if (c.roommates.isEmpty()) {
                tvNoRoommates.visibility = View.VISIBLE
            } else {
                tvNoRoommates.visibility = View.GONE
                c.roommates.forEach { rm ->
                    val row = LinearLayout(ctx).apply {
                        orientation = LinearLayout.HORIZONTAL
                        setBackgroundResource(R.drawable.bg_badge_rounded)
                        setPadding(24, 16, 16, 16)
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply { bottomMargin = 10 }
                    }
                    val info = LinearLayout(ctx).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    }
                    val tvName = TextView(ctx).apply {
                        text = rm.fullName + (rm.relationship?.let { " ($it)" } ?: "")
                        setTextColor(ContextCompat.getColor(ctx, R.color.rc_ink))
                        textSize = 14f
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                    }
                    val tvSub = TextView(ctx).apply {
                        val details = listOfNotNull(
                            rm.phone?.takeIf { it.isNotBlank() }?.let { "SĐT: $it" },
                            rm.identityNumber?.takeIf { it.isNotBlank() }?.let { "CCCD: $it" }
                        )
                        text = if (details.isEmpty()) "Chưa thu CCCD/SĐT" else details.joinToString(" • ")
                        setTextColor(ContextCompat.getColor(ctx, R.color.rc_muted))
                        textSize = 12f
                    }
                    info.addView(tvName)
                    info.addView(tvSub)
                    val btnDel = MaterialButton(ctx, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                        text = "Xóa"
                        setOnClickListener {
                            viewModel.deleteRoommate(rm.id, c.id)
                        }
                    }
                    row.addView(info)
                    row.addView(btnDel)
                    llRoommates.addView(row)
                }
            }
        }
    }

    private fun showTerminateDialog() {
        val input = EditText(requireContext()).apply {
            hint = "Lý do chấm dứt / thanh lý hợp đồng (tùy chọn)"
            setPadding(36, 20, 36, 20)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Thanh lý / Chấm dứt hợp đồng")
            .setMessage("Phòng sẽ trở về trạng thái Trống nếu không còn hợp đồng hiệu lực khác.")
            .setView(input)
            .setPositiveButton("Xác nhận") { _, _ ->
                viewModel.terminateContract(contractId, input.text?.toString()?.trim()?.ifEmpty { null })
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showAddRoommateDialog(contract: ContractResponse) {
        val ctx = requireContext()
        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 12)
        }
        val etQr = EditText(ctx).apply { hint = "Dán chuỗi QR CCCD (tùy chọn để tự điền)" }
        val btnParseQr = MaterialButton(ctx).apply { text = "Giải mã QR CCCD" }
        val etName = EditText(ctx).apply { hint = "Họ và tên người ở cùng *" }
        val etPhone = EditText(ctx).apply { hint = "Số điện thoại (tùy chọn)" }
        val etRel = EditText(ctx).apply { hint = "Quan hệ (VD: Bạn cùng phòng / Vợ chồng)" }
        val etIdNum = EditText(ctx).apply { hint = "Số CCCD (tùy chọn)" }
        val cbConsent = CheckBox(ctx).apply { text = "Người ở cùng đồng ý lưu CCCD phục vụ cư trú" }

        btnParseQr.setOnClickListener {
            val parsed = CccdQrParser.parse(etQr.text?.toString())
            if (parsed == null) {
                Snackbar.make(binding.root, "Chuỗi QR CCCD không hợp lệ", Snackbar.LENGTH_SHORT).show()
            } else {
                etIdNum.setText(parsed.identityNumber)
                etName.setText(parsed.fullName)
                cbConsent.isChecked = true
            }
        }

        container.addView(etQr)
        container.addView(btnParseQr)
        container.addView(etName)
        container.addView(etPhone)
        container.addView(etRel)
        container.addView(etIdNum)
        container.addView(cbConsent)

        val dialog = AlertDialog.Builder(ctx)
            .setTitle("Thêm người ở cùng")
            .setView(container)
            .setPositiveButton("Thêm", null)
            .setNegativeButton(getString(R.string.cancel), null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = etName.text.toString().trim()
                val cccd = etIdNum.text.toString().trim().ifEmpty { null }
                if (name.isEmpty()) {
                    etName.error = "Nhập họ tên"
                    return@setOnClickListener
                }
                if (cccd != null && !cbConsent.isChecked) {
                    Snackbar.make(binding.root, "Vui lòng tick đồng ý lưu CCCD", Snackbar.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                viewModel.addRoommate(
                    CreateRoommateRequest(
                        tenantId = contract.tenantId,
                        roomId = contract.roomId,
                        contractId = contract.id,
                        fullName = name,
                        phone = etPhone.text.toString().trim().ifEmpty { null },
                        identityNumber = cccd,
                        relationship = etRel.text.toString().trim().ifEmpty { null },
                        consentConfirmed = cbConsent.isChecked
                    ),
                    contract.id
                )
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun formatDate(iso: String): String {
        val parts = iso.split("-")
        return if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else iso
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
