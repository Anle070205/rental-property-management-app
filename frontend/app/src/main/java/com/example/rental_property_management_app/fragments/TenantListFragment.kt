package com.example.rental_property_management_app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.rental_property_management_app.adapters.TenantAdapter
import com.example.rental_property_management_app.databinding.FragmentTenantListBinding
import com.example.rental_property_management_app.utils.Resource
import com.example.rental_property_management_app.viewmodels.TenantViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Tenant List Fragment
 * Module 3 - Hiển thị danh sách khách thuê
 */
class TenantListFragment : Fragment() {
    
    private var _binding: FragmentTenantListBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: TenantViewModel by viewModels()
    private lateinit var adapter: TenantAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTenantListBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupSearchView()
        setupClickListeners()
        observeTenantsState()
        
        // Load initial data
        viewModel.loadTenants()
    }
    
    private fun setupRecyclerView() {
        adapter = TenantAdapter { tenant ->
            val action = TenantListFragmentDirections.actionTenantListToTenantDetail(tenant.id)
            findNavController().navigate(action)
        }
        binding.rvTenants.adapter = adapter
    }
    
    private fun setupSearchView() {
        binding.etSearch.addTextChangedListener { text ->
            val keyword = text?.toString()
            if (!keyword.isNullOrEmpty() && keyword.length >= 2) {
                viewModel.searchTenants(keyword)
            } else if (keyword.isNullOrEmpty()) {
                viewModel.loadTenants()
            }
        }
    }
    
    private fun setupClickListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadTenants()
        }
        
        binding.btnRetry.setOnClickListener {
            viewModel.loadTenants()
        }
    }
    
    private fun observeTenantsState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.tenantsState.collectLatest { resource ->
                when (resource) {
                    is Resource.Loading -> showLoading()
                    is Resource.Success -> {
                        binding.swipeRefresh.isRefreshing = false
                        hideAllStates()
                        
                        val tenants = resource.data?.content.orEmpty()
                        if (tenants.isEmpty()) {
                            showEmptyState()
                        } else {
                            adapter.submitList(tenants)
                            binding.rvTenants.visibility = View.VISIBLE
                        }
                    }
                    is Resource.Error -> {
                        binding.swipeRefresh.isRefreshing = false
                        hideAllStates()
                        showErrorState(resource.message ?: "Lỗi không xác định")
                    }
                }
            }
        }
    }
    
    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.rvTenants.visibility = View.GONE
        binding.layoutEmpty.visibility = View.GONE
        binding.layoutError.visibility = View.GONE
    }
    
    private fun showEmptyState() {
        binding.layoutEmpty.visibility = View.VISIBLE
    }
    
    private fun showErrorState(message: String) {
        binding.layoutError.visibility = View.VISIBLE
        binding.tvError.text = message
    }
    
    private fun hideAllStates() {
        binding.progressBar.visibility = View.GONE
        binding.layoutEmpty.visibility = View.GONE
        binding.layoutError.visibility = View.GONE
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
