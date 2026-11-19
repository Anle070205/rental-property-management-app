package com.example.rental_property_management_app.fragments

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.adapters.BookingRequestAdapter
import com.example.rental_property_management_app.databinding.FragmentBookingListBinding
import com.example.rental_property_management_app.models.BookingRequestItem
import com.example.rental_property_management_app.models.BookingRequestStatus
import com.example.rental_property_management_app.models.BookingRequestType
import com.example.rental_property_management_app.utils.Resource
import com.example.rental_property_management_app.viewmodels.MarketplaceBookingViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class TenantBookingListFragment : Fragment() {

    private var _binding: FragmentBookingListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MarketplaceBookingViewModel by viewModels()
    private lateinit var adapter: BookingRequestAdapter

    private var allItems: List<BookingRequestItem> = emptyList()
    private var selectedFilterId: Int = R.id.chipAll

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookingListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.title = getString(R.string.rc_my_bookings)
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        adapter = BookingRequestAdapter(
            isLandlordMode = false,
            onCallClick = { dialPhone(it) },
            onCancelClick = { confirmCancel(it) }
        )
        binding.rvBookings.adapter = adapter

        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            selectedFilterId = checkedIds.firstOrNull() ?: R.id.chipAll
            applyFilter()
        }

        binding.swipeRefresh.setOnRefreshListener { viewModel.loadTenantBookings() }
        binding.btnRetry.setOnClickListener { viewModel.loadTenantBookings() }

        observeStates()
        viewModel.loadTenantBookings()
    }

    private fun applyFilter() {
        val filtered = when (selectedFilterId) {
            R.id.chipPending -> allItems.filter { it.status == BookingRequestStatus.PENDING }
            R.id.chipBooking -> allItems.filter { it.requestType == BookingRequestType.BOOKING }
            R.id.chipContact -> allItems.filter { it.requestType == BookingRequestType.CONTACT }
            else -> allItems
        }
        adapter.submitList(filtered)
        binding.rvBookings.isVisible = filtered.isNotEmpty()
        binding.layoutEmpty.isVisible = filtered.isEmpty()
        binding.tvEmpty.text = getString(R.string.rc_no_booking_requests)
        binding.btnRetry.isVisible = false
    }

    private fun observeStates() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.bookingsState.collect { resource ->
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
                        allItems = resource.data.orEmpty()
                        applyFilter()
                    }
                    is Resource.Error -> {
                        binding.swipeRefresh.isRefreshing = false
                        binding.progressBar.isVisible = false
                        binding.rvBookings.isVisible = false
                        binding.layoutEmpty.isVisible = true
                        binding.tvEmpty.text = resource.message ?: getString(R.string.error_loading)
                        binding.btnRetry.isVisible = true
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.actionState.collect { resource ->
                resource ?: return@collect
                when (resource) {
                    is Resource.Loading -> Unit
                    is Resource.Success -> {
                        Snackbar.make(binding.root, getString(R.string.rc_booking_cancelled), Snackbar.LENGTH_SHORT).show()
                        viewModel.consumeActionState()
                        viewModel.loadTenantBookings()
                    }
                    is Resource.Error -> {
                        Snackbar.make(binding.root, resource.message ?: getString(R.string.error_server), Snackbar.LENGTH_LONG).show()
                        viewModel.consumeActionState()
                    }
                }
            }
        }
    }

    private fun confirmCancel(item: BookingRequestItem) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Hủy yêu cầu?")
            .setMessage("Bạn muốn hủy yêu cầu cho phòng ${item.roomCode} (${item.propertyName})?")
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.confirm) { _, _ -> viewModel.cancelTenantBooking(item.id) }
            .show()
    }

    private fun dialPhone(phone: String) {
        try {
            startActivity(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phone, null)))
        } catch (_: ActivityNotFoundException) {
            Snackbar.make(binding.root, R.string.rc_dial_unavailable, Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
