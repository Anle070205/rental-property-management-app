package com.example.rental_property_management_app.fragments

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
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
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class LandlordBookingListFragment : Fragment() {

    private var _binding: FragmentBookingListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MarketplaceBookingViewModel by viewModels()
    private lateinit var adapter: BookingRequestAdapter

    private var filterStatus: BookingRequestStatus? = null
    private var filterType: BookingRequestType? = null

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
        binding.toolbar.title = getString(R.string.rc_booking_requests)
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        adapter = BookingRequestAdapter(
            isLandlordMode = true,
            onCallClick = { dialPhone(it) },
            onApproveClick = { showRespondDialog(it, BookingRequestStatus.APPROVED) },
            onRejectClick = { showRespondDialog(it, BookingRequestStatus.REJECTED) }
        )
        binding.rvBookings.adapter = adapter

        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            when (checkedIds.firstOrNull()) {
                R.id.chipPending -> {
                    filterStatus = BookingRequestStatus.PENDING
                    filterType = null
                }
                R.id.chipBooking -> {
                    filterStatus = null
                    filterType = BookingRequestType.BOOKING
                }
                R.id.chipContact -> {
                    filterStatus = null
                    filterType = BookingRequestType.CONTACT
                }
                else -> {
                    filterStatus = null
                    filterType = null
                }
            }
            reload()
        }

        binding.swipeRefresh.setOnRefreshListener { reload() }
        binding.btnRetry.setOnClickListener { reload() }

        observeStates()
        reload()
    }

    private fun reload() {
        viewModel.loadLandlordBookings(filterStatus, filterType)
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
                        val list = resource.data.orEmpty()
                        adapter.submitList(list)
                        binding.rvBookings.isVisible = list.isNotEmpty()
                        binding.layoutEmpty.isVisible = list.isEmpty()
                        binding.tvEmpty.text = getString(R.string.rc_no_booking_requests)
                        binding.btnRetry.isVisible = false
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
                        Snackbar.make(binding.root, getString(R.string.rc_booking_updated), Snackbar.LENGTH_SHORT).show()
                        viewModel.consumeActionState()
                        reload()
                    }
                    is Resource.Error -> {
                        Snackbar.make(binding.root, resource.message ?: getString(R.string.error_server), Snackbar.LENGTH_LONG).show()
                        viewModel.consumeActionState()
                    }
                }
            }
        }
    }

    private fun showRespondDialog(item: BookingRequestItem, targetStatus: BookingRequestStatus) {
        val ctx = requireContext()
        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 20, 48, 8)
        }
        val tilNote = TextInputLayout(ctx).apply {
            hint = getString(R.string.rc_landlord_note_hint)
        }
        val etNote = TextInputEditText(ctx).apply {
            setText(
                if (targetStatus == BookingRequestStatus.APPROVED && item.requestType == BookingRequestType.BOOKING) {
                    "Đã duyệt đặt phòng ${item.roomCode}. Mời bạn liên hệ nhận phòng."
                } else if (targetStatus == BookingRequestStatus.APPROVED) {
                    "Chủ nhà đã tiếp nhận liên hệ. Bạn có thể gọi ${item.landlordPhone} để hẹn xem phòng."
                } else {
                    ""
                }
            )
        }
        tilNote.addView(etNote)
        container.addView(tilNote)

        val title = when {
            targetStatus == BookingRequestStatus.APPROVED && item.requestType == BookingRequestType.BOOKING ->
                "Duyệt đặt phòng ${item.roomCode}?"
            targetStatus == BookingRequestStatus.APPROVED ->
                "Phản hồi liên hệ phòng ${item.roomCode}"
            else ->
                "Từ chối yêu cầu phòng ${item.roomCode}?"
        }

        MaterialAlertDialogBuilder(ctx)
            .setTitle(title)
            .setMessage("Người thuê: ${item.tenantName} (${item.tenantPhone})")
            .setView(container)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.confirm) { _, _ ->
                viewModel.respondLandlordBooking(
                    item.id,
                    targetStatus,
                    etNote.text?.toString()?.trim()?.ifEmpty { null }
                )
            }
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
