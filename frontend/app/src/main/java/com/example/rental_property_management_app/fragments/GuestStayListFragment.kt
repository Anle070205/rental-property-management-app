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
import com.example.rental_property_management_app.adapters.GuestStayAdapter
import com.example.rental_property_management_app.databinding.FragmentGuestStayListBinding
import com.example.rental_property_management_app.utils.Resource
import com.example.rental_property_management_app.viewmodels.GuestStayViewModel
import kotlinx.coroutines.launch

class GuestStayListFragment : Fragment(R.layout.fragment_guest_stay_list) {
    private var _binding: FragmentGuestStayListBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val viewModel: GuestStayViewModel by viewModels()

    private var isLandlord = false
    private var selectedStatus: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentGuestStayListBinding.bind(view)
        isLandlord = RoomCareApplication.prefsManager.getUserRole().equals("LANDLORD", ignoreCase = true)

        binding.btnNewGuestStay.isVisible = !isLandlord
        binding.tvSubtitle.text = if (isLandlord) {
            "Theo dõi và tiếp nhận khai báo khách lưu trú từ người thuê"
        } else {
            "Khai báo khách đến ở qua đêm cho chủ nhà trọ"
        }

        val adapter = GuestStayAdapter { item ->
            findNavController().navigate(
                R.id.guestStayDetailFragment,
                bundleOf("guestStayId" to item.id, "isLandlord" to isLandlord)
            )
        }
        binding.recyclerGuestStays.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerGuestStays.adapter = adapter

        binding.btnNewGuestStay.setOnClickListener {
            findNavController().navigate(R.id.guestStayFormFragment)
        }

        binding.btnRetry.setOnClickListener {
            viewModel.loadList(isLandlord, selectedStatus)
        }

        binding.chipGroupStatus.setOnCheckedStateChangeListener { _, checkedIds ->
            selectedStatus = when (checkedIds.firstOrNull()) {
                R.id.chipPending -> "PENDING"
                R.id.chipReceived -> "RECEIVED"
                R.id.chipNeedsInfo -> "NEEDS_INFO"
                R.id.chipCompleted -> "COMPLETED"
                else -> null
            }
            viewModel.loadList(isLandlord, selectedStatus)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.listState.collect { res ->
                binding.progress.isVisible = res is Resource.Loading
                binding.tvError.isVisible = res is Resource.Error
                binding.tvError.text = res?.message
                binding.btnRetry.isVisible = res is Resource.Error
                if (res is Resource.Success) {
                    val items = res.data.orEmpty()
                    adapter.submitList(items)
                    binding.tvEmpty.isVisible = items.isEmpty()
                } else {
                    binding.tvEmpty.isVisible = false
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadList(isLandlord, selectedStatus)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
