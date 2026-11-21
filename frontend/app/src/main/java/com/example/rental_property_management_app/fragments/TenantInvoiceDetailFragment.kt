package com.example.rental_property_management_app.fragments

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.databinding.FragmentTenantInvoiceDetailBinding
import com.example.rental_property_management_app.databinding.ItemInvoiceChargeBinding
import com.example.rental_property_management_app.models.TenantInvoice
import com.example.rental_property_management_app.models.TenantInvoiceStatus
import com.example.rental_property_management_app.utils.Constants
import com.example.rental_property_management_app.utils.CurrencyUtils
import com.example.rental_property_management_app.utils.DateUtils
import com.example.rental_property_management_app.utils.Resource
import com.example.rental_property_management_app.viewmodels.TenantInvoiceViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.math.BigDecimal

class TenantInvoiceDetailFragment : Fragment(R.layout.fragment_tenant_invoice_detail) {
    private var _binding: FragmentTenantInvoiceDetailBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val viewModel: TenantInvoiceViewModel by viewModels()
    private val invoiceId get() = requireArguments().getLong("invoiceId")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentTenantInvoiceDetailBinding.bind(view)
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.retry.setOnClickListener { viewModel.loadDetail(invoiceId) }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.detail.collect { result ->
                binding.progress.isVisible = result is Resource.Loading
                binding.retry.isVisible = result is Resource.Error
                binding.error.isVisible = result is Resource.Error
                binding.error.text = result.message
                binding.content.isVisible = result is Resource.Success
                result.data?.let(::display)
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.proofState.collect { resource ->
                resource ?: return@collect
                when (resource) {
                    is Resource.Loading -> Unit
                    is Resource.Success -> {
                        Snackbar.make(
                            binding.root,
                            "Đã gửi minh chứng chuyển khoản. Vui lòng chờ chủ nhà xác nhận thực thu.",
                            Snackbar.LENGTH_LONG
                        ).show()
                        viewModel.resetProofState()
                    }
                    is Resource.Error -> {
                        AlertDialog.Builder(requireContext())
                            .setTitle("Không gửi được minh chứng")
                            .setMessage(resource.message ?: "Vui lòng thử lại")
                            .setPositiveButton("Đóng", null)
                            .show()
                        viewModel.resetProofState()
                    }
                }
            }
        }
        viewModel.loadDetail(invoiceId)
    }

    private fun display(invoice: TenantInvoice) {
        binding.sample.isVisible = Constants.USE_MOCK_TENANT_DATA
        val hasRemaining = invoice.remainingAmount.signum() > 0 && invoice.status != TenantInvoiceStatus.PAID
        binding.payment.isVisible = hasRemaining
        binding.btnSubmitPaymentProof.isVisible = hasRemaining
        binding.payment.setOnClickListener {
            findNavController().navigate(R.id.qrPaymentFragment, bundleOf("invoiceId" to invoice.id))
        }
        binding.btnSubmitPaymentProof.setOnClickListener {
            showSubmitProofDialog(invoice)
        }

        binding.code.text = invoice.invoiceCode
        binding.month.text = getString(R.string.rc_invoice_month, invoice.billingMonth)
        binding.status.text = resources.getStringArray(R.array.rc_invoice_status)[invoice.status.ordinal]

        val metaParts = listOfNotNull(
            invoice.roomCode?.let { rc ->
                val prop = invoice.propertyName?.let { " • $it" } ?: ""
                "Phòng $rc$prop"
            },
            invoice.contractCode?.let { "Hợp đồng: $it" }
        )
        binding.tvInvoiceMeta.isVisible = metaParts.isNotEmpty()
        binding.tvInvoiceMeta.text = metaParts.joinToString("\n")

        binding.dueDate.isVisible = invoice.dueDate != null
        binding.dueDate.text = invoice.dueDate?.let {
            getString(R.string.rc_tenant_due_date, DateUtils.parseApiDate(it)?.let(DateUtils::formatDate) ?: it)
        }

        binding.charges.removeAllViews()

        val elecLabel = buildString {
            append(getString(R.string.rc_invoice_electricity))
            if (invoice.prevElectricIndex != null && invoice.currElectricIndex != null && invoice.electricUsage != null) {
                append(" (${invoice.prevElectricIndex.stripTrailingZeros().toPlainString()}→${invoice.currElectricIndex.stripTrailingZeros().toPlainString()} = ${invoice.electricUsage.stripTrailingZeros().toPlainString()} kWh)")
            } else if (invoice.electricUsage != null) {
                append(" (${invoice.electricUsage.stripTrailingZeros().toPlainString()} kWh)")
            }
        }

        val waterLabel = buildString {
            append(getString(R.string.rc_invoice_water))
            when (invoice.waterBillingMode) {
                "FIXED" -> append(" (Khoán tháng)")
                "PER_PERSON" -> append(" (${invoice.occupantCount ?: 1} người)")
                else -> {
                    if (invoice.prevWaterIndex != null && invoice.currWaterIndex != null && invoice.waterUsage != null) {
                        append(" (${invoice.prevWaterIndex.stripTrailingZeros().toPlainString()}→${invoice.currWaterIndex.stripTrailingZeros().toPlainString()} = ${invoice.waterUsage.stripTrailingZeros().toPlainString()} m³)")
                    } else if (invoice.waterUsage != null) {
                        append(" (${invoice.waterUsage.stripTrailingZeros().toPlainString()} m³)")
                    }
                }
            }
        }

        val rows = mutableListOf<Triple<String, BigDecimal, Boolean>>(
            Triple(getString(R.string.rc_invoice_rent), invoice.roomRent, false),
            Triple(elecLabel, invoice.electricityAmount, false),
            Triple(waterLabel, invoice.waterAmount, false),
            Triple(getString(R.string.rc_invoice_internet), invoice.internetAmount, false)
        )
        if ((invoice.garbageFee ?: BigDecimal.ZERO).signum() > 0) {
            rows.add(Triple("Phí rác", invoice.garbageFee ?: BigDecimal.ZERO, false))
        }
        rows.add(Triple(getString(R.string.rc_invoice_service), invoice.serviceAmount, false))
        rows.add(Triple(getString(R.string.rc_invoice_other), invoice.otherFees, false))
        rows.add(Triple(getString(R.string.rc_invoice_previous_debt), invoice.previousDebt, false))
        if ((invoice.adjustmentAmount ?: BigDecimal.ZERO).signum() != 0) {
            val reason = invoice.adjustmentReason?.takeIf { it.isNotBlank() }?.let { " ($it)" } ?: ""
            rows.add(Triple("Điều chỉnh$reason", invoice.adjustmentAmount ?: BigDecimal.ZERO, false))
        }
        rows.add(Triple(getString(R.string.rc_invoice_discount), invoice.discountAmount, false))
        rows.add(Triple(getString(R.string.rc_invoice_total), invoice.totalAmount, true))
        rows.add(Triple(getString(R.string.rc_invoice_paid), invoice.paidAmount, false))
        rows.add(Triple(getString(R.string.rc_invoice_remaining), invoice.remainingAmount, true))

        rows.forEach { (labelText, amount, highlight) ->
            val row = ItemInvoiceChargeBinding.inflate(layoutInflater, binding.charges, false)
            row.label.text = labelText
            row.amount.text = CurrencyUtils.formatVND(amount)
            if (highlight) {
                row.amount.setTextColor(ContextCompat.getColor(requireContext(), R.color.rc_brand))
            }
            binding.charges.addView(row.root)
        }

        binding.tvInvoiceNotes.isVisible = !invoice.notes.isNullOrBlank()
        binding.tvInvoiceNotes.text = invoice.notes?.let { "Ghi chú: $it" }

        if (invoice.paymentProofSubmittedAt != null) {
            binding.tvPaymentProofBanner.isVisible = true
            binding.tvPaymentProofBanner.text = buildString {
                append("Đã gửi minh chứng lúc ${invoice.paymentProofSubmittedAt.take(16).replace('T', ' ')}\n")
                if (!invoice.paymentProofNote.isNullOrBlank()) append("Nội dung: ${invoice.paymentProofNote}\n")
                append("Trạng thái hóa đơn chỉ chuyển sang Đã thanh toán sau khi chủ trọ xác nhận thực thu.")
            }
        } else {
            binding.tvPaymentProofBanner.isVisible = false
        }

        if (invoice.payments.isNotEmpty()) {
            binding.tvConfirmedPayments.isVisible = true
            binding.tvConfirmedPayments.text = "Lịch sử thực thu đã xác nhận:\n" +
                invoice.payments.joinToString("\n") { p ->
                    "• ${p.paidAt?.take(16)?.replace('T', ' ') ?: ""} - ${CurrencyUtils.formatVND(p.amount)} (${p.method})"
                }
        } else {
            binding.tvConfirmedPayments.isVisible = false
        }
    }

    private fun showSubmitProofDialog(invoice: TenantInvoice) {
        val ctx = requireContext()
        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 12)
        }
        val etNote = EditText(ctx).apply {
            hint = "Mã giao dịch / Nội dung đã chuyển khoản *"
            setText("CK ROOMCARE ${invoice.invoiceCode}")
        }
        val etUrl = EditText(ctx).apply {
            hint = "Link ảnh biên lai (HTTP/HTTPS, tùy chọn)"
        }
        container.addView(etNote)
        container.addView(etUrl)

        val dialog = AlertDialog.Builder(ctx)
            .setTitle("Gửi minh chứng chuyển khoản")
            .setMessage("Hóa đơn vẫn giữ nguyên trạng thái cho đến khi chủ nhà kiểm tra tài khoản và xác nhận thực thu.")
            .setView(container)
            .setPositiveButton("Gửi minh chứng", null)
            .setNegativeButton(getString(R.string.cancel), null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val note = etNote.text?.toString()?.trim().orEmpty()
                val url = etUrl.text?.toString()?.trim()?.ifEmpty { null }
                if (note.isEmpty() && url == null) {
                    etNote.error = "Nhập mã giao dịch hoặc nội dung chuyển khoản"
                    return@setOnClickListener
                }
                viewModel.submitPaymentProof(invoice.id, note.ifEmpty { null }, url)
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
