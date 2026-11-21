package com.example.rental_property_management_app.fragments

import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.RoomCareApplication
import com.example.rental_property_management_app.databinding.FragmentResetPasswordBinding
import com.example.rental_property_management_app.utils.Constants
import com.example.rental_property_management_app.utils.NetworkUtils
import com.example.rental_property_management_app.utils.OtpBoxGroup
import com.example.rental_property_management_app.utils.Resource
import com.example.rental_property_management_app.utils.ValidationUtils
import com.example.rental_property_management_app.utils.clearError
import com.example.rental_property_management_app.utils.disable
import com.example.rental_property_management_app.utils.enable
import com.example.rental_property_management_app.utils.getText
import com.example.rental_property_management_app.utils.hide
import com.example.rental_property_management_app.utils.hideKeyboard
import com.example.rental_property_management_app.utils.show
import com.example.rental_property_management_app.utils.showError
import com.example.rental_property_management_app.utils.showLongToast
import com.example.rental_property_management_app.utils.showSnackbar
import com.example.rental_property_management_app.viewmodels.AuthViewModel
import kotlinx.coroutines.launch

/**
 * Màn hình đặt lại mật khẩu 2 bước:
 * Bước 1: Nhập OTP 6 chữ số từ Gmail hoặc SMS (+84) + gửi lại mã
 * Bước 2: Nhập & xác nhận mật khẩu mới (chỉ mở sau khi OTP được backend xác minh)
 */
class ResetPasswordFragment : Fragment() {

    private var _binding: FragmentResetPasswordBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()
    private lateinit var otpBoxGroup: OtpBoxGroup

    private var channel: String = "EMAIL"
    private var target: String = ""
    private var resendTimer: CountDownTimer? = null
    private var timerRunning = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResetPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        channel = arguments?.getString("channel")?.takeIf { it.isNotBlank() } ?: "EMAIL"
        target = arguments?.getString("target").orEmpty().trim()
        val initialCooldown = arguments?.getInt("cooldownSeconds", 60) ?: 60

        binding.tvTarget.text = target
        setupOtpBoxes()
        restoreStepAndCooldown(initialCooldown)
        setupListeners()
        observeStates()
    }

    private fun setupOtpBoxes() {
        otpBoxGroup = OtpBoxGroup(
            boxes = listOf(
                binding.otp1, binding.otp2, binding.otp3,
                binding.otp4, binding.otp5, binding.otp6
            ),
            onOtpChanged = {
                if (binding.tvOtpError.isVisible) {
                    binding.tvOtpError.hide()
                }
            },
            onCompleted = {
                binding.root.hideKeyboard()
            }
        )
    }

    private fun restoreStepAndCooldown(argCooldownSeconds: Int) {
        val alreadyVerifiedThisTarget =
            !viewModel.verifiedResetToken.isNullOrBlank() &&
                viewModel.resetTokenChannel == channel &&
                viewModel.resetTokenTarget == target

        if (alreadyVerifiedThisTarget) {
            showPasswordStepUi()
            return
        }

        binding.layoutOtpStep.show()
        binding.layoutPasswordStep.hide()
        binding.tvDescription.text = if (channel == "PHONE") {
            getString(R.string.rc_reset_desc_phone)
        } else {
            getString(R.string.rc_reset_desc_email)
        }

        val remaining = viewModel.remainingResetCooldownSeconds()
        if (remaining > 0) {
            startCooldown(remaining)
        } else if (viewModel.resetCooldownEndMs == 0L && argCooldownSeconds > 0) {
            viewModel.markResetCooldown(argCooldownSeconds)
            startCooldown(argCooldownSeconds)
        }
    }

    private fun showPasswordStepUi() {
        resendTimer?.cancel()
        timerRunning = false
        binding.layoutOtpStep.hide()
        binding.layoutPasswordStep.show()
        binding.tvDescription.text = getString(R.string.rc_reset_desc_step2)
    }

    private fun setupListeners() {
        binding.btnVerifyOtp.setOnClickListener {
            submitOtp()
        }

        binding.btnResendOtp.setOnClickListener {
            resendResetOtp()
        }

        binding.btnResetPassword.setOnClickListener {
            submitNewPassword()
        }

        binding.tvSwitchMethod.setOnClickListener {
            viewModel.clearOtpAndResetProgress()
            findNavController().popBackStack()
        }

        binding.tvBackToLogin.setOnClickListener {
            viewModel.clearOtpAndResetProgress()
            findNavController().navigate(
                R.id.action_resetPasswordFragment_to_loginFragment,
                bundleOf("prefillIdentifier" to target)
            )
        }
    }

    private fun submitOtp() {
        val otp = otpBoxGroup.getOtp()
        binding.tvOtpError.hide()

        if (target.isBlank()) {
            view?.showSnackbar("Thiếu thông tin liên hệ. Vui lòng quay lại màn hình Quên mật khẩu.")
            return
        }

        val otpError = ValidationUtils.getOtpError(otp)
        if (otpError != null) {
            binding.tvOtpError.text = otpError
            binding.tvOtpError.show()
            return
        }

        if (!NetworkUtils.isNetworkAvailable(requireContext())) {
            view?.showSnackbar(Constants.ErrorMessages.NETWORK_ERROR)
            return
        }

        binding.root.hideKeyboard()
        viewModel.verifyResetOtp(channel, target, otp)
    }

    private fun resendResetOtp() {
        if (target.isBlank()) {
            view?.showSnackbar("Thiếu thông tin liên hệ để gửi lại mã.")
            return
        }

        if (!NetworkUtils.isNetworkAvailable(requireContext())) {
            view?.showSnackbar(Constants.ErrorMessages.NETWORK_ERROR)
            return
        }

        binding.root.hideKeyboard()
        otpBoxGroup.clear()
        viewModel.forgotPassword(channel, target)
    }

    private fun submitNewPassword() {
        val token = viewModel.verifiedResetToken
        if (token.isNullOrBlank()) {
            view?.showSnackbar("Vui lòng xác minh mã OTP trước khi đặt mật khẩu mới.")
            return
        }

        val newPassword = binding.tilNewPassword.getText()
        val confirmPassword = binding.tilConfirmPassword.getText()
        binding.tilNewPassword.clearError()
        binding.tilConfirmPassword.clearError()

        var isValid = true
        val passwordError = ValidationUtils.getPasswordError(newPassword)
        if (passwordError != null) {
            binding.tilNewPassword.showError(passwordError)
            isValid = false
        }

        val confirmError = ValidationUtils.getConfirmPasswordError(newPassword, confirmPassword)
        if (confirmError != null) {
            binding.tilConfirmPassword.showError(confirmError)
            isValid = false
        }

        if (!isValid) return

        if (!NetworkUtils.isNetworkAvailable(requireContext())) {
            view?.showSnackbar(Constants.ErrorMessages.NETWORK_ERROR)
            return
        }

        binding.root.hideKeyboard()
        viewModel.resetPassword(channel, target, token, newPassword, confirmPassword)
    }

    private fun observeStates() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.verifyResetOtpState.collect { resource ->
                when (resource) {
                    is Resource.Loading -> showVerifyOtpLoading(true)
                    is Resource.Success -> {
                        showVerifyOtpLoading(false)
                        if (resource.data != null) {
                            showPasswordStepUi()
                            view?.showSnackbar(getString(R.string.rc_verify_otp_success))
                        }
                    }
                    is Resource.Error -> {
                        showVerifyOtpLoading(false)
                        view?.showSnackbar(resource.message ?: Constants.ErrorMessages.UNKNOWN_ERROR)
                    }
                    null -> Unit
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.forgotPasswordState.collect { resource ->
                when (resource) {
                    is Resource.Loading -> showResendLoading(true)
                    is Resource.Success -> {
                        showResendLoading(false)
                        view?.showSnackbar(resource.data ?: "Đã gửi lại mã OTP khôi phục")
                        startCooldown(60)
                    }
                    is Resource.Error -> {
                        showResendLoading(false)
                        view?.showSnackbar(resource.message ?: Constants.ErrorMessages.UNKNOWN_ERROR)
                    }
                    null -> Unit
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.resetPasswordState.collect { resource ->
                when (resource) {
                    is Resource.Loading -> showResetPasswordLoading(true)
                    is Resource.Success -> {
                        showResetPasswordLoading(false)
                        viewModel.clearOtpAndResetProgress()
                        RoomCareApplication.prefsManager.clearAllData()
                        requireContext().showLongToast(
                            resource.data ?: getString(R.string.rc_reset_password_success)
                        )
                        findNavController().navigate(
                            R.id.action_resetPasswordFragment_to_loginFragment,
                            bundleOf("prefillIdentifier" to target)
                        )
                    }
                    is Resource.Error -> {
                        showResetPasswordLoading(false)
                        view?.showSnackbar(resource.message ?: Constants.ErrorMessages.UNKNOWN_ERROR)
                    }
                    null -> Unit
                }
            }
        }
    }

    private fun startCooldown(seconds: Int) {
        resendTimer?.cancel()
        val bindingRef = _binding ?: return
        timerRunning = true
        bindingRef.btnResendOtp.disable()
        resendTimer = object : CountDownTimer(seconds * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val secLeft = ((millisUntilFinished + 999L) / 1000L).toInt()
                _binding?.btnResendOtp?.text = getString(R.string.rc_resend_countdown, secLeft)
            }

            override fun onFinish() {
                timerRunning = false
                _binding?.let {
                    it.btnResendOtp.enable()
                    it.btnResendOtp.text = getString(R.string.rc_resend_button)
                }
            }
        }.start()
    }

    private fun showVerifyOtpLoading(loading: Boolean) {
        if (loading) {
            binding.progressVerifyOtp.show()
            binding.btnVerifyOtp.disable()
            binding.btnVerifyOtp.text = ""
        } else {
            binding.progressVerifyOtp.hide()
            binding.btnVerifyOtp.enable()
            binding.btnVerifyOtp.text = getString(R.string.rc_verify_otp_button)
        }
    }

    private fun showResendLoading(loading: Boolean) {
        if (loading) {
            binding.progressResend.show()
            binding.btnResendOtp.disable()
            binding.btnResendOtp.text = ""
        } else {
            binding.progressResend.hide()
            if (!timerRunning) {
                binding.btnResendOtp.enable()
                binding.btnResendOtp.text = getString(R.string.rc_resend_button)
            }
        }
    }

    private fun showResetPasswordLoading(loading: Boolean) {
        if (loading) {
            binding.progressResetPassword.show()
            binding.btnResetPassword.disable()
            binding.btnResetPassword.text = ""
        } else {
            binding.progressResetPassword.hide()
            binding.btnResetPassword.enable()
            binding.btnResetPassword.text = getString(R.string.rc_reset_password_button)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        resendTimer?.cancel()
        resendTimer = null
        timerRunning = false
        viewModel.resetVerifyResetOtpState()
        viewModel.resetForgotPasswordState()
        viewModel.resetPasswordResetState()
        _binding = null
    }
}
