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
import com.example.rental_property_management_app.databinding.FragmentAdminProfileBinding
import com.example.rental_property_management_app.utils.AdminUiHelper

class AdminProfileFragment : Fragment() {
    private var _binding: FragmentAdminProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val prefs = RoomCareApplication.prefsManager
        val name = prefs.getUserName() ?: "Admin RoomCare"
        val initials = name.trim().split("\\s+".toRegex()).takeLast(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("").ifEmpty { "AD" }
        binding.tvInitials.text = initials
        binding.tvFullName.text = name
        binding.tvEmail.text = prefs.getUserEmail() ?: "--"
        binding.tvPhone.text = "--"

        binding.btnChangePassword.setOnClickListener {
            findNavController().navigate(R.id.adminChangePasswordFragment)
        }
        binding.btnLogout.setOnClickListener {
            AdminUiHelper.showConfirmDialog(
                context = requireContext(),
                title = "Đăng xuất khỏi ADMIN?",
                message = "Bạn sẽ kết thúc phiên quản trị hiện tại.",
                confirmText = "Đăng xuất",
                isDanger = true
            ) {
                prefs.clearAllData()
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
