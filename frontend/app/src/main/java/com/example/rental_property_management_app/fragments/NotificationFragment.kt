package com.example.rental_property_management_app.fragments

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.RoomCareApplication
import com.example.rental_property_management_app.adapters.NotificationAdapter
import com.example.rental_property_management_app.databinding.FragmentNotificationBinding
import com.example.rental_property_management_app.models.NotificationType
import com.example.rental_property_management_app.utils.populateRoomCareChoices
import com.example.rental_property_management_app.utils.selectedRoomCareChoice
import com.example.rental_property_management_app.viewmodels.NotificationViewModel
import kotlinx.coroutines.launch

class NotificationFragment : Fragment(R.layout.fragment_notification) {
    private var _binding: FragmentNotificationBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val viewModel: NotificationViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentNotificationBinding.bind(view)
        val adapter = NotificationAdapter(viewModel::markRead)
        binding.items.layoutManager = LinearLayoutManager(requireContext())
        binding.items.adapter = adapter
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.swipe.setColorSchemeResources(R.color.rc_brand)
        binding.swipe.setOnChildScrollUpCallback { _, _ -> binding.items.canScrollVertically(-1) }
        binding.swipe.setOnRefreshListener { viewModel.refresh() }
        binding.readAll.setOnClickListener { viewModel.markAllRead() }
        binding.more.setOnClickListener { viewModel.loadMore() }
        binding.retry.setOnClickListener { viewModel.refresh() }

        binding.types.populateRoomCareChoices(
            resources.getStringArray(R.array.rc_notification_types),
            savedInstanceState?.getInt(SELECTED_TYPE) ?: 0
        )
        binding.types.setOnCheckedStateChangeListener { _, _ -> applyFilter() }
        applyFilter()
        binding.unreadOnly.setOnCheckedChangeListener { _, _ -> applyFilter() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.opened.collect { item ->
                if (item == null) return@collect
                viewModel.consumeOpened()
                val refId = item.refId
                val nav = findNavController()
                if (RoomCareApplication.prefsManager.getUserRole() == "TENANT") {
                    when (item.type) {
                        NotificationType.INCIDENT -> if (refId != null) {
                            nav.navigate(R.id.incidentDetailFragment, bundleOf("incidentId" to refId))
                        }
                        NotificationType.INVOICE -> if (refId != null) {
                            nav.navigate(R.id.tenantInvoiceDetailFragment, bundleOf("invoiceId" to refId))
                        }
                        NotificationType.CONTRACT -> nav.navigate(R.id.myContractFragment)
                        NotificationType.PAYMENT -> nav.navigate(R.id.paymentHistoryFragment)
                        else -> Unit
                    }
                } else {
                    when (item.type) {
                        NotificationType.INVOICE, NotificationType.PAYMENT -> if (refId != null) {
                            nav.navigate(R.id.invoiceDetailFragment, bundleOf("invoiceId" to refId))
                        } else {
                            nav.navigate(R.id.invoiceFragment)
                        }
                        NotificationType.CONTRACT -> if (refId != null) {
                            nav.navigate(R.id.roomDetailFragment, bundleOf("roomId" to refId))
                        } else {
                            nav.navigate(R.id.roomListFragment)
                        }
                        NotificationType.INCIDENT, NotificationType.MAINTENANCE ->
                            nav.navigate(R.id.healthScoreFragment)
                        else -> Unit
                    }
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collect { state ->
                adapter.submitList(state.items)
                binding.swipe.isRefreshing = state.loading
                binding.readAll.isEnabled = !state.loading
                binding.more.isVisible = state.hasMore && !state.loading
                binding.retry.isVisible = state.error != null
                binding.status.isVisible = !state.loading && state.error != null
                binding.status.text = state.error
                binding.empty.isVisible = !state.loading && state.error == null && state.items.isEmpty()
            }
        }
    }

    private fun applyFilter() {
        viewModel.filter(
            NotificationType.entries.getOrNull(binding.types.selectedRoomCareChoice() - 1),
            binding.unreadOnly.isChecked
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        _binding?.let { outState.putInt(SELECTED_TYPE, it.types.selectedRoomCareChoice()) }
        super.onSaveInstanceState(outState)
    }

    private companion object {
        const val SELECTED_TYPE = "notification_type"
    }

    override fun onDestroyView() {
        binding.items.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
