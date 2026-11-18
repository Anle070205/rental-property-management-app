package com.example.rental_property_management_app.fragments

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.adapters.TenantPaymentAdapter
import com.example.rental_property_management_app.databinding.FragmentPaymentHistoryBinding
import com.example.rental_property_management_app.utils.Constants
import com.example.rental_property_management_app.viewmodels.TenantPaymentViewModel
import kotlinx.coroutines.launch

class PaymentHistoryFragment : Fragment(R.layout.fragment_payment_history) {
    private var _binding: FragmentPaymentHistoryBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val viewModel: TenantPaymentViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentPaymentHistoryBinding.bind(view)
        val adapter = TenantPaymentAdapter()
        binding.items.layoutManager = LinearLayoutManager(requireContext())
        binding.items.adapter = adapter
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.sample.isVisible = Constants.USE_MOCK_TENANT_DATA
        binding.retry.setOnClickListener { viewModel.refreshHistory() }
        binding.swipe.setOnRefreshListener { viewModel.refreshHistory() }
        binding.more.setOnClickListener { viewModel.moreHistory() }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.history.collect { state ->
                adapter.submitList(state.items)
                binding.swipe.isRefreshing = state.loading
                binding.more.isVisible = !state.loading && state.nextPage != null
                binding.retry.isVisible = state.error != null
                binding.status.isVisible = !state.loading && (state.error != null || state.items.isEmpty())
                binding.status.text = state.error ?: getString(R.string.rc_payment_history_empty)
            }
        }
        viewModel.refreshHistory()
    }

    override fun onDestroyView() {
        binding.items.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
