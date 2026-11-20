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
import com.example.rental_property_management_app.databinding.FragmentRegisterBinding
import com.example.rental_property_management_app.utils.*
import com.example.rental_property_management_app.viewmodels.AuthViewModel
import kotlinx.coroutines.launch

/**
 * RegisterFragment - Đăng ký tài khoản Chủ nhà hoặc Người thuê
 */
class RegisterFragment : Fragment() {
    
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        observeRegisterState()
    }

    private fun setupClickListeners() {
        // Back to Login
        binding.tvLogin.setOnClickListener {
            findNavController().navigateUp()
        }

        // Register button
        binding.btnRegister.setOnClickListener {
            handleRegister()
        }
    }

    private fun handleRegister() {
        // Get input values
        val fullName = binding.tilFullName.getText()
        val email = binding.tilEmail.getText()
        val phone = binding.tilPhone.getText()
        val password = binding.tilPassword.getText()
        val confirmPassword = binding.tilConfirmPassword.getText()
        
        // Clear previous errors
        binding.tilFullName.clearError()
        binding.tilEmail.clearError()
        binding.tilPhone.clearError()
        binding.tilPassword.clearError()
        binding.tilConfirmPassword.clearError()
        
        // Validate input
        var isValid = true
        
        val fullNameError = ValidationUtils.getFullNameError(fullName)
        if (fullNameError != null) {
            binding.tilFullName.showError(fullNameError)
            isValid = false
        }
        
        val emailError = ValidationUtils.getEmailError(email)
        if (emailError != null) {
            binding.tilEmail.showError(emailError)
            isValid = false
        }
        
        val phoneError = ValidationUtils.getPhoneError(phone)
        if (phoneError != null) {
            binding.tilPhone.showError(phoneError)
            isValid = false
        }
        
        val passwordError = ValidationUtils.getPasswordError(password)
        if (passwordError != null) {
            binding.tilPassword.showError(passwordError)
            isValid = false
        }
        
        val confirmPasswordError = ValidationUtils.getConfirmPasswordError(password, confirmPassword)
        if (confirmPasswordError != null) {
            binding.tilConfirmPassword.showError(confirmPasswordError)
            isValid = false
        }
        
        // Check network
        if (!NetworkUtils.isNetworkAvailable(requireContext())) {
            view?.showSnackbar(Constants.ErrorMessages.NETWORK_ERROR)
            return
        }
        
        // Call register API if valid
        if (isValid) {
            val selectedRole = if (binding.rbTenant.isChecked) {
                Constants.Roles.TENANT
            } else {
                Constants.Roles.LANDLORD
            }
            binding.root.hideKeyboard()
            viewModel.register(fullName, email, phone, password, selectedRole)
        }
    }

    private fun observeRegisterState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.registerState.collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        showLoading(true)
                    }
                    is Resource.Success -> {
                        showLoading(false)
                        val authResponse = resource.data
                        if (authResponse != null) {
                            requireContext().showToast("Đăng ký thành công!")
                            when (authResponse.role) {
                                Constants.Roles.TENANT -> findNavController().navigate(
                                    R.id.action_registerFragment_to_tenantDashboardFragment
                                )
                                else -> findNavController().navigate(
                                    R.id.action_registerFragment_to_landlordDashboardFragment
                                )
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
            binding.btnRegister.disable()
            binding.btnRegister.text = ""
        } else {
            binding.progressBar.hide()
            binding.btnRegister.enable()
            binding.btnRegister.text = "Đăng ký"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.resetRegisterState()
        _binding = null
    }
}
