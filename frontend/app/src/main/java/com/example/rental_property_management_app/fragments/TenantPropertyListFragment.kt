package com.example.rental_property_management_app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.adapters.TenantPropertyAdapter
import com.example.rental_property_management_app.databinding.FragmentTenantPropertyListBinding
import com.example.rental_property_management_app.models.MarketplaceProperty
import com.example.rental_property_management_app.utils.Resource
import com.example.rental_property_management_app.viewmodels.MarketplaceBookingViewModel
import kotlinx.coroutines.launch

class TenantPropertyListFragment : Fragment() {

    private var _binding: FragmentTenantPropertyListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MarketplaceBookingViewModel by viewModels()
    private lateinit var adapter: TenantPropertyAdapter

    private var allProperties: List<MarketplaceProperty> = emptyList()
    private var onlyVacant: Boolean = false
    private var keyword: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTenantPropertyListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        adapter = TenantPropertyAdapter { property ->
            findNavController().navigate(
                R.id.tenantPropertyRoomsFragment,
                bundleOf(
                    "propertyId" to property.id,
                    "propertyName" to property.name
                )
            )
        }
        binding.rvProperties.adapter = adapter

        binding.etSearch.addTextChangedListener { text ->
            keyword = text?.toString()?.trim().orEmpty()
            applyFilter()
        }

        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            onlyVacant = checkedIds.firstOrNull() == R.id.chipVacantOnly
            applyFilter()
        }

        binding.swipeRefresh.setOnRefreshListener { viewModel.loadProperties() }
        binding.btnRetry.setOnClickListener { viewModel.loadProperties() }

        observeStates()
        viewModel.loadProperties()
    }

    private fun applyFilter() {
        val kw = keyword.lowercase()
        val filtered = allProperties.filter { item ->
            val matchVacant = !onlyVacant || item.vacantRooms > 0
            val matchKw = kw.isEmpty() ||
                item.name.lowercase().contains(kw) ||
                item.address.lowercase().contains(kw) ||
                item.landlordName.lowercase().contains(kw)
            matchVacant && matchKw
        }
        adapter.submitList(filtered)
        binding.rvProperties.isVisible = filtered.isNotEmpty()
        binding.layoutEmpty.isVisible = filtered.isEmpty()
        binding.tvEmpty.text = "Chưa có nhà trọ phù hợp"
        binding.btnRetry.isVisible = false
    }

    private fun observeStates() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.propertiesState.collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        if (!binding.swipeRefresh.isRefreshing) {
                            binding.progressBar.isVisible = true
                        }
                        binding.layoutEmpty.isVisible = false
                    }
                    is Resource.Success -> {
                        binding.swipeRefresh.isRefreshing = false
                        binding.progressBar.isVisible = false
                        allProperties = resource.data.orEmpty()
                        applyFilter()
                    }
                    is Resource.Error -> {
                        binding.swipeRefresh.isRefreshing = false
                        binding.progressBar.isVisible = false
                        binding.rvProperties.isVisible = false
                        binding.layoutEmpty.isVisible = true
                        binding.tvEmpty.text = resource.message ?: getString(R.string.error_loading)
                        binding.btnRetry.isVisible = true
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
