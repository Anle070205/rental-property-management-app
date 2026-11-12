package com.example.rental_property_management_app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.databinding.FragmentLoginBinding
import com.example.rental_property_management_app.utils.*
import com.example.rental_property_management_app.viewmodels.AuthViewModel
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {
    
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        observeLoginState()
    }

    private fun setupClickListeners() {
        // Navigate to Register
        binding.tvRegister.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        // Navigate to Forgot Password
        binding.tvForgotPassword.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_forgotPasswordFragment)
        }

        // Login button
        binding.btnLogin.setOnClickListener {
            handleLogin()
        }
    }

    private fun handleLogin() {
        // Get input values
        val emailOrPhone = binding.tilEmail.getText()
        val password = binding.tilPassword.getText()
        
        // Clear previous errors
        binding.tilEmail.clearError()
        binding.tilPassword.clearError()
        
        // Validate input
        var isValid = true
        
        val emailOrPhoneError = ValidationUtils.getEmailOrPhoneError(emailOrPhone)
        if (emailOrPhoneError != null) {
            binding.tilEmail.showError(emailOrPhoneError)
            isValid = false
        }
        
        val passwordError = ValidationUtils.getPasswordError(password)
        if (passwordError != null) {
            binding.tilPassword.showError(passwordError)
            isValid = false
        }
        
        // Check network
        if (!NetworkUtils.isNetworkAvailable(requireContext())) {
            view?.showSnackbar(Constants.ErrorMessages.NETWORK_ERROR)
            return
        }
        
        // Call login API if valid
        if (isValid) {
            binding.root.hideKeyboard()
            viewModel.login(emailOrPhone, password)
        }
    }

    private fun observeLoginState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.loginState.collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        showLoading(true)
                    }
                    is Resource.Success -> {
                        showLoading(false)
                        val authResponse = resource.data
                        if (authResponse != null) {
                            requireContext().showToast("Đăng nhập thành công!")
                            
                            // Navigate based on role
                            when (authResponse.role) {
                                Constants.Roles.LANDLORD -> {
                                    findNavController().navigate(
                                        R.id.action_loginFragment_to_landlordDashboardFragment
                                    )
                                }
                                Constants.Roles.TENANT -> {
                                    findNavController().navigate(
                                        R.id.action_loginFragment_to_tenantDashboardFragment
                                    )
                                }
                            }
                        }
                    }
                    is Resource.Error -> {
                        showLoading(false)
                        view?.showSnackbar(resource.message ?: Constants.ErrorMessages.UNKNOWN_ERROR)
                    }
                    null -> {
                        // Initial state
                    }
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            binding.progressBar.show()
            binding.btnLogin.disable()
            binding.btnLogin.text = ""
        } else {
            binding.progressBar.hide()
            binding.btnLogin.enable()
            binding.btnLogin.text = "Đăng nhập"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.resetLoginState()
        _binding = null
    }
}
