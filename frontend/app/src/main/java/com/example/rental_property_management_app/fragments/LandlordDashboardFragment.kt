package com.example.rental_property_management_app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.databinding.FragmentLandlordDashboardBinding

class LandlordDashboardFragment : Fragment() {
    
    private var _binding: FragmentLandlordDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLandlordDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Room Management
        binding.cardRoomManagement.setOnClickListener {
            findNavController().navigate(R.id.action_landlordDashboard_to_roomList)
        }
        
        // Tenant Management
        binding.cardTenantManagement.setOnClickListener {
            findNavController().navigate(R.id.action_landlordDashboard_to_tenantList)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
