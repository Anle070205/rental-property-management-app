package com.example.rental_property_management_app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.adapters.TenantMarketplaceRoomAdapter
import com.example.rental_property_management_app.databinding.FragmentTenantPropertyRoomsBinding
import com.example.rental_property_management_app.models.MarketplaceRoom
import com.example.rental_property_management_app.models.RoomStatus
import com.example.rental_property_management_app.utils.Resource
import com.example.rental_property_management_app.viewmodels.MarketplaceBookingViewModel
import kotlinx.coroutines.launch

class TenantPropertyRoomsFragment : Fragment() {

    private var _binding: FragmentTenantPropertyRoomsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MarketplaceBookingViewModel by viewModels()
    private lateinit var adapter: TenantMarketplaceRoomAdapter

    private var propertyId: Long = 0L
    private var propertyName: String = ""
    private var allRooms: List<MarketplaceRoom> = emptyList()
    private var onlyVacant: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTenantPropertyRoomsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        propertyId = arguments?.getLong("propertyId", 0L) ?: 0L
        propertyName = arguments?.getString("propertyName").orEmpty()

        binding.toolbar.title = if (propertyName.isNotBlank()) propertyName else "Danh sách phòng"
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        adapter = TenantMarketplaceRoomAdapter { room ->
            findNavController().navigate(
                R.id.tenantMarketplaceRoomDetailFragment,
                bundleOf("roomId" to room.id)
            )
        }
        binding.rvRooms.adapter = adapter

        binding.chipGroupStatus.setOnCheckedStateChangeListener { _, checkedIds ->
            onlyVacant = checkedIds.firstOrNull() == R.id.chipVacant
            applyFilter()
        }

        binding.swipeRefresh.setOnRefreshListener { reload() }
        binding.btnRetry.setOnClickListener { reload() }

        observeStates()
        reload()
    }

    private fun reload() {
        viewModel.loadPropertyRooms(propertyId)
    }

    private fun applyFilter() {
        val filtered = if (onlyVacant) {
            allRooms.filter { it.status == RoomStatus.VACANT }
        } else {
            allRooms
        }
        adapter.submitList(filtered)
        binding.rvRooms.isVisible = filtered.isNotEmpty()
        binding.layoutEmpty.isVisible = filtered.isEmpty()
        binding.tvEmpty.text = "Nhà trọ này chưa có phòng phù hợp"
        binding.btnRetry.isVisible = false
    }

    private fun observeStates() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.roomsState.collect { resource ->
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
                        allRooms = resource.data.orEmpty()
                        applyFilter()
                    }
                    is Resource.Error -> {
                        binding.swipeRefresh.isRefreshing = false
                        binding.progressBar.isVisible = false
                        binding.rvRooms.isVisible = false
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
