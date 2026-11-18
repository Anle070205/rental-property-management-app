package com.example.rental_property_management_app.fragments

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.adapters.TenantInvoiceAdapter
import com.example.rental_property_management_app.databinding.FragmentTenantInvoiceBinding
import com.example.rental_property_management_app.models.TenantInvoiceStatus
import com.example.rental_property_management_app.utils.Constants
import com.example.rental_property_management_app.viewmodels.TenantInvoiceViewModel
import kotlinx.coroutines.launch

class TenantInvoiceFragment : Fragment(R.layout.fragment_tenant_invoice) {
    private var _binding: FragmentTenantInvoiceBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val viewModel: TenantInvoiceViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentTenantInvoiceBinding.bind(view)
        val nav = findNavController()
        val adapter = TenantInvoiceAdapter { nav.navigate(R.id.tenantInvoiceDetailFragment, bundleOf("invoiceId" to it)) }
        binding.items.layoutManager = LinearLayoutManager(requireContext())
        binding.items.adapter = adapter
        binding.toolbar.setNavigationOnClickListener { nav.navigateUp() }
        binding.sample.isVisible = Constants.USE_MOCK_TENANT_DATA
        binding.statusFilter.adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_dropdown_item,
            listOf(getString(R.string.rc_invoice_all_status)) + resources.getStringArray(R.array.rc_invoice_status)
        )
        binding.filter.setOnClickListener { applyFilter() }
        binding.more.setOnClickListener { viewModel.more() }
        binding.retry.setOnClickListener { viewModel.refresh() }
        binding.swipe.setOnRefreshListener { viewModel.refresh() }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.list.collect { state ->
                adapter.submitList(state.items)
                binding.swipe.isRefreshing = state.loading
                binding.more.isVisible = !state.loading && state.nextPage != null
                binding.retry.isVisible = state.error != null
                binding.status.isVisible = !state.loading && (state.error != null || state.items.isEmpty())
                binding.status.text = state.error ?: getString(R.string.rc_invoice_empty)
            }
        }
        viewModel.refresh()
    }

    private fun applyFilter() {
        val month = binding.month.text.toString().trim().ifBlank { null }
        if (month != null && !Regex("[0-9]{4}-(0[1-9]|1[0-2])").matches(month)) {
            binding.month.error = getString(R.string.rc_invoice_month_error)
            return
        }
        viewModel.filter(month, TenantInvoiceStatus.entries.getOrNull(binding.statusFilter.selectedItemPosition - 1))
    }

    override fun onDestroyView() {
        binding.items.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
