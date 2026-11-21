package com.example.rental_property_management_app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.databinding.FragmentForgotPasswordBinding
import com.example.rental_property_management_app.utils.*
import com.example.rental_property_management_app.viewmodels.AuthViewModel
import kotlinx.coroutines.launch

class ForgotPasswordFragment : Fragment() {

    private var _binding: FragmentForgotPasswordBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()
    private var requestedChannel: String = "EMAIL"
    private var requestedTarget: String = ""

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
        applySelectedMethodUi(isPhoneMethod())
        setupClickListeners()
        observeForgotPasswordState()
    }

    private fun isPhoneMethod(): Boolean = binding.toggleMethod.checkedButtonId == R.id.btn_method_phone

    private fun applySelectedMethodUi(phoneSelected: Boolean) {
        if (phoneSelected) {
            binding.tilEmail.hide()
            binding.tilPhone.show()
            binding.tilEmail.clearError()
            binding.tvInputLabel.text = "Số điện thoại đã đăng ký *"
        } else {
            binding.tilPhone.hide()
            binding.tilEmail.show()
            binding.tilPhone.clearError()
            binding.tvInputLabel.text = "Địa chỉ Gmail đã đăng ký *"
        }
    }

    private fun setupClickListeners() {
        binding.toggleMethod.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            viewModel.clearOtpAndResetProgress()
            binding.tilEmail.clearError()
            binding.tilPhone.clearError()
            applySelectedMethodUi(checkedId == R.id.btn_method_phone)
        }

        binding.tvBackToLogin.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnSubmit.setOnClickListener {
            handleForgotPassword()
        }
    }

    private fun handleForgotPassword() {
        val phoneMethod = isPhoneMethod()
        binding.tilEmail.clearError()
        binding.tilPhone.clearError()

        if (phoneMethod) {
            val rawPhone = binding.tilPhone.getText()
            val phoneError = ValidationUtils.getVietnamPhoneError(rawPhone)
            if (phoneError != null) {
                binding.tilPhone.showError(phoneError)
                return
            }
            requestedChannel = "PHONE"
            requestedTarget = ValidationUtils.normalizeVietnamPhoneToE164(rawPhone) ?: rawPhone
        } else {
            val email = binding.tilEmail.getText()
            val emailError = ValidationUtils.getEmailError(email)
            if (emailError != null) {
                binding.tilEmail.showError(emailError)
                return
            }
            requestedChannel = "EMAIL"
            requestedTarget = email.trim()
        }

        if (!NetworkUtils.isNetworkAvailable(requireContext())) {
            view?.showSnackbar(Constants.ErrorMessages.NETWORK_ERROR)
            return
        }

        binding.root.hideKeyboard()
        viewModel.forgotPassword(requestedChannel, requestedTarget)
    }

    private fun observeForgotPasswordState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.forgotPasswordState.collect { resource ->
                when (resource) {
                    is Resource.Loading -> showLoading(true)
                    is Resource.Success -> {
                        showLoading(false)
                        val message = resource.data ?: getString(R.string.rc_forgot_otp_sent)
                        requireContext().showLongToast(message)
                        val ch = requestedChannel.ifBlank { if (isPhoneMethod()) "PHONE" else "EMAIL" }
                        val targetToPass = requestedTarget.ifBlank {
                            if (ch == "PHONE") binding.tilPhone.getText() else binding.tilEmail.getText()
                        }
                        findNavController().navigate(
                            R.id.action_forgotPasswordFragment_to_resetPasswordFragment,
                            bundleOf(
                                "channel" to ch,
                                "target" to targetToPass,
                                "cooldownSeconds" to 60
                            )
                        )
                    }
                    is Resource.Error -> {
                        showLoading(false)
                        view?.showSnackbar(resource.message ?: Constants.ErrorMessages.UNKNOWN_ERROR)
                    }
                    null -> Unit
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
