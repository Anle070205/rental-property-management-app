package com.example.rental_property_management_app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.rental_property_management_app.databinding.FragmentForgotPasswordBinding
import com.example.rental_property_management_app.utils.*
import com.example.rental_property_management_app.viewmodels.AuthViewModel
import kotlinx.coroutines.launch

class ForgotPasswordFragment : Fragment() {
    
    private var _binding: FragmentForgotPasswordBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForgotPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        observeForgotPasswordState()
    }

    private fun setupClickListeners() {
        // Back to Login
        binding.tvBackToLogin.setOnClickListener {
            findNavController().navigateUp()
        }

        // Submit button
        binding.btnSubmit.setOnClickListener {
            handleForgotPassword()
        }
    }

    private fun handleForgotPassword() {
        val email = binding.tilEmail.getText()
        
        // Clear previous errors
        binding.tilEmail.clearError()
        
        // Validate email
        val emailError = ValidationUtils.getEmailError(email)
        if (emailError != null) {
            binding.tilEmail.showError(emailError)
            return
        }
        
        // Check network
        if (!NetworkUtils.isNetworkAvailable(requireContext())) {
            view?.showSnackbar(Constants.ErrorMessages.NETWORK_ERROR)
            return
        }
        
        // Call forgot password API
        binding.root.hideKeyboard()
        viewModel.forgotPassword(email)
    }

    private fun observeForgotPasswordState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.forgotPasswordState.collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        showLoading(true)
                    }
                    is Resource.Success -> {
                        showLoading(false)
                        val message = resource.data ?: "Yêu cầu đã được gửi"
                        requireContext().showLongToast(message)
                        findNavController().navigateUp()
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
            binding.btnSubmit.disable()
            binding.btnSubmit.text = ""
        } else {
            binding.progressBar.hide()
            binding.btnSubmit.enable()
            binding.btnSubmit.text = "Gửi yêu cầu"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.resetForgotPasswordState()
        _binding = null
    }
}
