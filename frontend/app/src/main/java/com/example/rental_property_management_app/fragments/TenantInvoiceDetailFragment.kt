package com.example.rental_property_management_app.fragments

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.databinding.FragmentTenantInvoiceDetailBinding
import com.example.rental_property_management_app.models.TenantInvoice
import com.example.rental_property_management_app.utils.Constants
import com.example.rental_property_management_app.utils.CurrencyUtils
import com.example.rental_property_management_app.utils.DateUtils
import com.example.rental_property_management_app.utils.Resource
import com.example.rental_property_management_app.viewmodels.TenantInvoiceViewModel
import kotlinx.coroutines.launch

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
        viewModel.loadDetail(invoiceId)
    }

    private fun display(invoice: TenantInvoice) {
        binding.sample.isVisible = Constants.USE_MOCK_TENANT_DATA
        binding.code.text = invoice.invoiceCode
        binding.month.text = getString(R.string.rc_invoice_month, invoice.billingMonth)
        binding.status.text = resources.getStringArray(R.array.rc_invoice_status)[invoice.status.ordinal]
        binding.dueDate.isVisible = invoice.dueDate != null
        binding.dueDate.text = invoice.dueDate?.let {
            getString(R.string.rc_tenant_due_date, DateUtils.parseApiDate(it)?.let(DateUtils::formatDate) ?: it)
        }
        binding.charges.text = listOf(
            R.string.rc_invoice_rent to invoice.roomRent,
            R.string.rc_invoice_electricity to invoice.electricityAmount,
            R.string.rc_invoice_water to invoice.waterAmount,
            R.string.rc_invoice_internet to invoice.internetAmount,
            R.string.rc_invoice_service to invoice.serviceAmount,
            R.string.rc_invoice_other to invoice.otherFees,
            R.string.rc_invoice_previous_debt to invoice.previousDebt,
            R.string.rc_invoice_discount to invoice.discountAmount,
            R.string.rc_invoice_total to invoice.totalAmount,
            R.string.rc_invoice_paid to invoice.paidAmount,
            R.string.rc_invoice_remaining to invoice.remainingAmount
        ).joinToString("\n") { (label, amount) ->
            getString(R.string.rc_invoice_charge, getString(label), CurrencyUtils.formatVND(amount))
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
