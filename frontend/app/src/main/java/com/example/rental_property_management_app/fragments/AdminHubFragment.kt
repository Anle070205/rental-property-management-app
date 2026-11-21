package com.example.rental_property_management_app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.RoomCareApplication
import com.example.rental_property_management_app.databinding.FragmentAdminHubBinding
import com.example.rental_property_management_app.utils.AdminUiHelper

class AdminHubFragment : Fragment() {
    private var _binding: FragmentAdminHubBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminHubBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.cardNavDashboard.setOnClickListener { findNavController().navigate(R.id.adminDashboardFragment) }
        binding.cardNavAccounts.setOnClickListener { findNavController().navigate(R.id.adminAccountListFragment) }
        binding.cardNavLandlords.setOnClickListener { findNavController().navigate(R.id.adminLandlordListFragment) }
        binding.cardNavProperties.setOnClickListener { findNavController().navigate(R.id.adminPropertyListFragment) }
        binding.cardNavReports.setOnClickListener { findNavController().navigate(R.id.adminReportListFragment) }
        binding.cardNavBroadcasts.setOnClickListener { findNavController().navigate(R.id.adminBroadcastListFragment) }
        binding.cardNavProfile.setOnClickListener { findNavController().navigate(R.id.adminProfileFragment) }

        binding.btnLogout.setOnClickListener {
            AdminUiHelper.showConfirmDialog(
                context = requireContext(),
                title = "Đăng xuất khỏi ADMIN?",
                message = "Bạn sẽ kết thúc phiên quản trị hiện tại. Hãy lưu các thay đổi trước khi đăng xuất.",
                confirmText = "Đăng xuất",
                isDanger = true
            ) {
                RoomCareApplication.prefsManager.clearAllData()
                findNavController().navigate(R.id.loginFragment, null, navOptions {
                    popUpTo(R.id.nav_graph) { inclusive = true }
                })
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
