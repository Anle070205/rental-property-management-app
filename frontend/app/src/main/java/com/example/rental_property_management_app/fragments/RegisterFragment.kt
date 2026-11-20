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
import com.example.rental_property_management_app.databinding.FragmentRegisterBinding
import com.example.rental_property_management_app.utils.*
import com.example.rental_property_management_app.viewmodels.AuthViewModel
import kotlinx.coroutines.launch

/**
 * RegisterFragment - Cho phép người dùng chọn 1 trong 2 phương thức xác thực:
 * Phương án A: Gmail (@gmail.com) -> Gửi OTP qua Email
 * Phương án B: Số điện thoại VN -> Gửi OTP qua SMS
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
        applySelectedMethodUi(isPhoneMethod())
        setupClickListeners()
        observeRegisterState()
    }

    private fun isPhoneMethod(): Boolean = binding.toggleMethod.checkedButtonId == R.id.btn_method_phone

    private fun applySelectedMethodUi(phoneSelected: Boolean) {
        if (phoneSelected) {
            binding.tilEmail.hide()
            binding.tilPhone.show()
            binding.tilEmail.clearError()
            if (viewModel.registerState.value !is Resource.Loading) {
                binding.btnRegister.text = getString(R.string.rc_send_verify_code_phone)
            }
        } else {
            binding.tilPhone.hide()
            binding.tilEmail.show()
            binding.tilPhone.clearError()
            if (viewModel.registerState.value !is Resource.Loading) {
                binding.btnRegister.text = getString(R.string.rc_send_verify_code_email)
            }
        }
    }

    private fun setupClickListeners() {
        binding.toggleMethod.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            // Xóa sạch trạng thái OTP cũ khi đổi phương thức để tránh xác minh nhầm
            viewModel.clearOtpAndResetProgress()
            binding.tilEmail.clearError()
            binding.tilPhone.clearError()
            applySelectedMethodUi(checkedId == R.id.btn_method_phone)
        }

        binding.tvLogin.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnRegister.setOnClickListener {
            handleRegister()
        }
    }

    private fun handleRegister() {
        val phoneMethod = isPhoneMethod()
        val fullName = binding.tilFullName.getText()
        val email = binding.tilEmail.getText()
        val phone = binding.tilPhone.getText()
        val password = binding.tilPassword.getText()
        val confirmPassword = binding.tilConfirmPassword.getText()

        binding.tilFullName.clearError()
        binding.tilEmail.clearError()
        binding.tilPhone.clearError()
        binding.tilPassword.clearError()
        binding.tilConfirmPassword.clearError()

        var isValid = true

        val fullNameError = ValidationUtils.getFullNameError(fullName)
        if (fullNameError != null) {
            binding.tilFullName.showError(fullNameError)
            isValid = false
        }

        if (phoneMethod) {
            val phoneError = ValidationUtils.getVietnamPhoneError(phone)
            if (phoneError != null) {
                binding.tilPhone.showError(phoneError)
                isValid = false
            }
        } else {
            val emailError = ValidationUtils.getGmailError(email)
            if (emailError != null) {
                binding.tilEmail.showError(emailError)
                isValid = false
            }
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

        if (!NetworkUtils.isNetworkAvailable(requireContext())) {
            view?.showSnackbar(Constants.ErrorMessages.NETWORK_ERROR)
            return
        }

        if (isValid) {
            val selectedRole = if (binding.rbTenant.isChecked) {
                Constants.Roles.TENANT
            } else {
                Constants.Roles.LANDLORD
            }
            binding.root.hideKeyboard()
            if (phoneMethod) {
                val normalizedPhone = ValidationUtils.normalizeVietnamPhoneToE164(phone) ?: phone
                viewModel.register(fullName, "PHONE", null, normalizedPhone, password, selectedRole)
            } else {
                viewModel.register(fullName, "EMAIL", email, null, password, selectedRole)
            }
        }
    }

    private fun observeRegisterState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.registerState.collect { resource ->
                when (resource) {
                    is Resource.Loading -> showLoading(true)
                    is Resource.Success -> {
                        showLoading(false)
                        val authResponse = resource.data
                        if (authResponse != null) {
                            if (authResponse.requiresVerification || authResponse.accessToken.isNullOrBlank()) {
                                val ch = authResponse.verificationChannel
                                    ?: if (isPhoneMethod()) "PHONE" else "EMAIL"
                                val target = authResponse.verificationTarget
                                    ?: if (ch == "PHONE") authResponse.phone.orEmpty() else authResponse.email.orEmpty()
                                requireContext().showLongToast(
                                    if (ch == "PHONE") getString(R.string.rc_register_check_sms)
                                    else getString(R.string.rc_register_check_gmail)
                                )
                                findNavController().navigate(
                                    R.id.action_registerFragment_to_verifyOtpFragment,
                                    bundleOf(
                                        "channel" to ch,
                                        "target" to target,
                                        "cooldownSeconds" to (authResponse.resendCooldownSeconds ?: 60)
                                    )
                                )
                            } else {
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
                    }
                    is Resource.Error -> {
                        showLoading(false)
                        val message = resource.message ?: Constants.ErrorMessages.UNKNOWN_ERROR
                        val challenge = viewModel.unverifiedChallenge.value
                        if (challenge != null) {
                            view?.showSnackbarWithAction(message, getString(R.string.rc_verify_now)) {
                                findNavController().navigate(
                                    R.id.action_registerFragment_to_verifyOtpFragment,
                                    bundleOf(
                                        "channel" to challenge.channel,
                                        "target" to challenge.target,
                                        "cooldownSeconds" to 0
                                    )
                                )
                            }
                        } else {
                            view?.showSnackbar(message)
                        }
                    }
                    null -> Unit
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
            binding.btnRegister.text = if (isPhoneMethod()) {
                getString(R.string.rc_send_verify_code_phone)
            } else {
                getString(R.string.rc_send_verify_code_email)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.resetRegisterState()
        _binding = null
    }
}
