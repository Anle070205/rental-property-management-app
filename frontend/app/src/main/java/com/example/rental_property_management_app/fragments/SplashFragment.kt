package com.example.rental_property_management_app.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.RoomCareApplication
import com.example.rental_property_management_app.databinding.FragmentSplashBinding
import com.example.rental_property_management_app.utils.Constants

class SplashFragment : Fragment() {
    
    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!
    
    private val SPLASH_DELAY = 2000L // 2 seconds

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Check authentication status after delay
        Handler(Looper.getMainLooper()).postDelayed({
            checkAuthenticationStatus()
        }, SPLASH_DELAY)
    }

    private fun checkAuthenticationStatus() {
        val isLoggedIn = RoomCareApplication.prefsManager.isLoggedIn()
        val token = RoomCareApplication.prefsManager.getAccessToken()
        val role = RoomCareApplication.prefsManager.getUserRole()
        
        if (isLoggedIn && token != null && role != null) {
            // User đã đăng nhập, điều hướng theo role
            when (role) {
                Constants.Roles.LANDLORD -> {
                    findNavController().navigate(R.id.action_splashFragment_to_landlordDashboardFragment)
                }
                Constants.Roles.TENANT -> {
                    findNavController().navigate(R.id.action_splashFragment_to_tenantDashboardFragment)
                }
                Constants.Roles.ADMIN -> {
                    findNavController().navigate(R.id.action_splashFragment_to_adminDashboardFragment)
                }
                else -> {
                    // Invalid role, go to login
                    findNavController().navigate(R.id.action_splashFragment_to_loginFragment)
                }
            }
        } else {
            // User chưa đăng nhập
            findNavController().navigate(R.id.action_splashFragment_to_loginFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
