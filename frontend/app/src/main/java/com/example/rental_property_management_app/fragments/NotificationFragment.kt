package com.example.rental_property_management_app.fragments

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AdapterView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.adapters.NotificationAdapter
import com.example.rental_property_management_app.databinding.FragmentNotificationBinding
import com.example.rental_property_management_app.models.NotificationType
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
        binding.swipe.setOnRefreshListener { viewModel.refresh() }
        binding.readAll.setOnClickListener { viewModel.markAllRead() }
        binding.more.setOnClickListener { viewModel.loadMore() }
        binding.retry.setOnClickListener { viewModel.refresh() }

        val labels = resources.getStringArray(R.array.rc_notification_types)
        binding.type.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, labels)
        binding.type.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, row: View?, position: Int, id: Long) = applyFilter()
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        binding.unreadOnly.setOnCheckedChangeListener { _, _ -> applyFilter() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collect { state ->
                adapter.submitList(state.items)
                binding.swipe.isRefreshing = state.loading
                binding.readAll.isEnabled = !state.loading
                binding.more.isVisible = state.hasMore && !state.loading
                binding.retry.isVisible = state.error != null
                binding.status.isVisible = !state.loading && (state.error != null || state.items.isEmpty())
                binding.status.text = state.error ?: getString(R.string.rc_no_notifications)
            }
        }
    }

    private fun applyFilter() {
        viewModel.filter(
            NotificationType.entries.getOrNull(binding.type.selectedItemPosition - 1),
            binding.unreadOnly.isChecked
        )
    }

    override fun onDestroyView() {
        binding.items.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
