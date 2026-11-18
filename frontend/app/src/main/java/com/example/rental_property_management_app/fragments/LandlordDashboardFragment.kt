package com.example.rental_property_management_app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.rental_property_management_app.databinding.FragmentLandlordDashboardBinding

/**
 * Host fragment for Landlord home dashboard, delegating directly to [LandlordHomeFragment]
 * so Splash/Login flows and direct home navigation share one synchronized UI and data source.
 */
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
        binding.openProfile.setOnClickListener { findNavController().navigate(R.id.profileFragment) }
        binding.openNotifications.setOnClickListener { findNavController().navigate(R.id.notificationFragment) }
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
