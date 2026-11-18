package com.example.rental_property_management_app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.databinding.FragmentTenantDashboardBinding

class TenantDashboardFragment : Fragment() {
    
    private var _binding: FragmentTenantDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTenantDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        binding.openProfile.setOnClickListener { findNavController().navigate(R.id.profileFragment) }
        binding.openNotifications.setOnClickListener { findNavController().navigate(R.id.notificationFragment) }
    }

    private fun setupClickListeners() {
        // TODO: Implement dashboard navigation and logic
        // Ví dụ: view room info, contracts, invoices, etc.
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
