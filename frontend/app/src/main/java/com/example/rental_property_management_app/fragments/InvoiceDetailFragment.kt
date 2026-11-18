package com.example.rental_property_management_app.fragments

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.adapters.PaymentHistoryAdapter
import com.example.rental_property_management_app.databinding.DialogRecordPaymentBinding
import com.example.rental_property_management_app.databinding.FragmentInvoiceDetailBinding
import com.example.rental_property_management_app.models.*
import com.example.rental_property_management_app.utils.CurrencyUtils
import com.example.rental_property_management_app.utils.Resource
import com.example.rental_property_management_app.viewmodels.InvoiceViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

class InvoiceDetailFragment : Fragment() {

    private var _binding: FragmentInvoiceDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InvoiceViewModel by viewModels()
    private lateinit var paymentAdapter: PaymentHistoryAdapter

    private var invoiceId: Long = 0L
    private var currentInvoice: InvoiceResponse? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInvoiceDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        invoiceId = arguments?.getLong("invoiceId", 0L) ?: 0L

        setupToolbar()
        setupRecyclerView()
        setupListeners()
        observeStates()

        if (invoiceId > 0L) {
            viewModel.loadInvoiceDetail(invoiceId)
        } else {
            showError("Không tìm thấy mã hóa đơn hợp lệ")
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        paymentAdapter = PaymentHistoryAdapter()
        binding.rvPayments.adapter = paymentAdapter
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            if (invoiceId > 0L) {
                viewModel.loadInvoiceDetail(invoiceId)
            }
        }

        binding.btnRetry.setOnClickListener {
            if (invoiceId > 0L) {
                viewModel.loadInvoiceDetail(invoiceId)
            }
        }

        binding.btnRecordPayment.setOnClickListener {
            val invoice = currentInvoice ?: return@setOnClickListener
            showRecordPaymentDialog(invoice)
        }
    }

    private fun observeStates() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.invoiceDetailState.collectLatest { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.layoutError.visibility = View.GONE
                    }
                    is Resource.Success -> {
                        binding.swipeRefresh.isRefreshing = false
                        binding.progressBar.visibility = View.GONE
                        binding.layoutError.visibility = View.GONE
                        binding.contentScroll.visibility = View.VISIBLE
                        resource.data?.let { bindInvoice(it) }
                    }
                    is Resource.Error -> {
                        binding.swipeRefresh.isRefreshing = false
                        showError(resource.message ?: getString(R.string.error_loading))
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.paymentState.collectLatest { resource ->
                resource ?: return@collectLatest
                when (resource) {
                    is Resource.Loading -> {
                        binding.btnRecordPayment.isEnabled = false
                    }
                    is Resource.Success -> {
                        binding.btnRecordPayment.isEnabled = true
                        Snackbar.make(binding.root, "Ghi nhận thanh toán thành công", Snackbar.LENGTH_SHORT).show()
                        viewModel.resetPaymentState()
                    }
                    is Resource.Error -> {
                        binding.btnRecordPayment.isEnabled = true
                        AlertDialog.Builder(requireContext())
                            .setTitle("Không thể ghi nhận thanh toán")
                            .setMessage(resource.message ?: "Dữ liệu thanh toán không hợp lệ")
                            .setPositiveButton("Đóng", null)
                            .show()
                        viewModel.resetPaymentState()
                    }
                }
            }
        }
    }

    private fun bindInvoice(invoice: InvoiceResponse) {
        currentInvoice = invoice
        val ctx = requireContext()

        binding.apply {
            tvInvoiceCode.text = invoice.invoiceCode
            tvRoomTitle.text = "Phòng ${invoice.roomCode} • Kỳ ${formatMonth(invoice.billingMonth)}"
            tvPropertyName.text = invoice.propertyName

            tvStatusBadge.text = invoice.status.getDisplayName()
            tvStatusBadge.setTextColor(ContextCompat.getColor(ctx, invoice.status.getTextColorRes()))
            tvStatusBadge.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(ctx, invoice.status.getBgColorRes())
            )

            tvTenantInfo.text = if (!invoice.tenantName.isNullOrBlank()) {
                val phonePart = if (!invoice.tenantPhone.isNullOrBlank()) " (${invoice.tenantPhone})" else ""
                "Người thuê: ${invoice.tenantName}$phonePart"
            } else {
                "Người thuê: Chưa gán người thuê"
            }

            val overdueInfo = when {
                invoice.daysOverdue > 0 && invoice.remainingAmount.signum() > 0 && invoice.paidAmount.signum() > 0 ->
                    " • Trễ ${invoice.daysOverdue} ngày (Đã thu một phần)"
                invoice.daysOverdue > 0 && invoice.remainingAmount.signum() > 0 ->
                    " • Quá hạn ${invoice.daysOverdue} ngày"
                else -> ""
            }
            tvDueDate.text = "Hạn thanh toán: ${formatDate(invoice.dueDate)}$overdueInfo"

            val elecLine = buildString {
                append("• Tiền điện: ${CurrencyUtils.formatVND(invoice.electricityAmount)}")
                if (invoice.electricUsage != null) {
                    append(" (${invoice.electricUsage.stripTrailingZeros().toPlainString()} kWh)")
                }
            }
            val waterLine = buildString {
                append("• Tiền nước: ${CurrencyUtils.formatVND(invoice.waterAmount)}")
                if (invoice.waterUsage != null) {
                    append(" (${invoice.waterUsage.stripTrailingZeros().toPlainString()} m³)")
                }
            }

            val feeLines = buildString {
                append("• Tiền phòng: ${CurrencyUtils.formatVND(invoice.roomRent)}\n")
                append("$elecLine\n")
                append("$waterLine\n")
                append("• Internet: ${CurrencyUtils.formatVND(invoice.internetAmount)}\n")
                append("• Phí dịch vụ: ${CurrencyUtils.formatVND(invoice.serviceAmount)}")
                if (invoice.otherFees.signum() > 0) {
                    append("\n• Khoản phát sinh: ${CurrencyUtils.formatVND(invoice.otherFees)}")
                }
                if (invoice.previousDebt.signum() > 0) {
                    append("\n• Nợ kỳ trước chuyển sang: ${CurrencyUtils.formatVND(invoice.previousDebt)}")
                }
                if (invoice.discountAmount.signum() > 0) {
                    append("\n• Giảm trừ: -${CurrencyUtils.formatVND(invoice.discountAmount)}")
                }
            }
            tvFeeLines.text = feeLines

            if (!invoice.notes.isNullOrBlank()) {
                tvInvoiceNotes.visibility = View.VISIBLE
                tvInvoiceNotes.text = "Ghi chú: ${invoice.notes}"
            } else {
                tvInvoiceNotes.visibility = View.GONE
            }

            tvTotalAmount.text = CurrencyUtils.formatVND(invoice.totalAmount)
            tvPaidAmount.text = CurrencyUtils.formatVND(invoice.paidAmount)
            tvRemainingAmount.text = CurrencyUtils.formatVND(invoice.remainingAmount)

            val percent = if (invoice.totalAmount.signum() > 0) {
                invoice.paidAmount
                    .multiply(BigDecimal(100))
                    .divide(invoice.totalAmount, 0, RoundingMode.DOWN)
                    .toInt()
                    .coerceIn(0, 100)
            } else {
                100
            }
            progressPayment.progress = percent

            btnRecordPayment.visibility =
                if (invoice.status == InvoiceStatus.PAID || invoice.remainingAmount.signum() <= 0) {
                    View.GONE
                } else {
                    View.VISIBLE
                }

            val payments = invoice.payments
            if (payments.isEmpty()) {
                tvEmptyPayments.visibility = View.VISIBLE
                rvPayments.visibility = View.GONE
            } else {
                tvEmptyPayments.visibility = View.GONE
                rvPayments.visibility = View.VISIBLE
                paymentAdapter.submitList(payments)
            }
        }
    }

    private fun showRecordPaymentDialog(invoice: InvoiceResponse) {
        val dialogBinding = DialogRecordPaymentBinding.inflate(layoutInflater)
        dialogBinding.tvRemainingHint.text = "Số tiền còn thiếu: ${CurrencyUtils.formatVND(invoice.remainingAmount)}"
        dialogBinding.etPaymentAmount.setText(invoice.remainingAmount.stripTrailingZeros().toPlainString())

        dialogBinding.btnFillRemaining.setOnClickListener {
            dialogBinding.etPaymentAmount.setText(invoice.remainingAmount.stripTrailingZeros().toPlainString())
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Ghi nhận thanh toán")
            .setView(dialogBinding.root)
            .setPositiveButton("Xác nhận thu tiền", null)
            .setNegativeButton(getString(R.string.cancel), null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (!dialogBinding.cbLandlordConfirmed.isChecked) {
                    Snackbar.make(
                        dialogBinding.root,
                        "Cần chủ nhà xác nhận đã thực nhận tiền trước khi ghi nhận",
                        Snackbar.LENGTH_LONG
                    ).show()
                    return@setOnClickListener
                }

                val amount = dialogBinding.etPaymentAmount.text?.toString()?.trim()?.toBigDecimalOrNull()
                if (amount == null || amount <= BigDecimal.ZERO) {
                    dialogBinding.tilPaymentAmount.error = "Số tiền thanh toán phải lớn hơn 0"
                    return@setOnClickListener
                }
                if (amount > invoice.remainingAmount) {
                    dialogBinding.tilPaymentAmount.error = "Không được vượt quá số tiền còn thiếu"
                    return@setOnClickListener
                }
                dialogBinding.tilPaymentAmount.error = null

                val method = if (dialogBinding.rbBankTransfer.isChecked) {
                    PaymentMethod.BANK_TRANSFER
                } else {
                    PaymentMethod.CASH
                }

                val txCode = dialogBinding.etTransactionCode.text?.toString()?.trim()?.ifEmpty { null }
                val note = dialogBinding.etReceiptNote.text?.toString()?.trim()?.ifEmpty { null }

                viewModel.recordPayment(
                    invoice.id,
                    CreatePaymentRequest(
                        amount = amount,
                        paymentMethod = method,
                        transactionCode = txCode,
                        receiptNote = note,
                        confirmedByLandlord = true
                    )
                )
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun showError(msg: String) {
        binding.progressBar.visibility = View.GONE
        binding.contentScroll.visibility = View.GONE
        binding.layoutError.visibility = View.VISIBLE
        binding.tvError.text = msg
    }

    private fun formatMonth(month: String): String {
        val parts = month.split("-")
        return if (parts.size == 2) "${parts[1]}/${parts[0]}" else month
    }

    private fun formatDate(isoDate: String): String {
        val parts = isoDate.split("-")
        return if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else isoDate
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
