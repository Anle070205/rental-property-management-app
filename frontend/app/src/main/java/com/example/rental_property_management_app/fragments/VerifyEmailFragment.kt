package com.example.rental_property_management_app.fragments

import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.databinding.FragmentVerifyEmailBinding
import com.example.rental_property_management_app.utils.Constants
import com.example.rental_property_management_app.utils.NetworkUtils
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
 * Màn hình nhập mã OTP 6 số gửi đến Gmail thật để kích hoạt tài khoản vừa đăng ký.
 */
class VerifyEmailFragment : Fragment() {

    private var _binding: FragmentVerifyEmailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()
    private var targetEmail: String = ""
    private var resendTimer: CountDownTimer? = null
    private var timerRunning = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVerifyEmailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        targetEmail = arguments?.getString("email").orEmpty().trim()
        val initialCooldown = arguments?.getInt("cooldownSeconds", 60) ?: 60
        binding.tvTargetEmail.text = targetEmail

        if (initialCooldown > 0) {
            startCooldown(initialCooldown)
        }

        setupListeners()
        observeStates()
    }

    private fun setupListeners() {
        binding.btnVerify.setOnClickListener {
            submitOtp()
        }

        binding.btnResend.setOnClickListener {
            resendOtp()
        }

        binding.tvBackToLogin.setOnClickListener {
            findNavController().navigate(
                R.id.action_verifyEmailFragment_to_loginFragment,
                bundleOf("prefillEmail" to targetEmail)
            )
        }
    }

    private fun submitOtp() {
        val otp = binding.tilOtp.getText()
        binding.tilOtp.clearError()

        if (targetEmail.isBlank()) {
            view?.showSnackbar("Thiếu thông tin Gmail cần xác thực. Vui lòng quay lại đăng ký hoặc đăng nhập.")
            return
        }

        val otpError = ValidationUtils.getOtpError(otp)
        if (otpError != null) {
            binding.tilOtp.showError(otpError)
            return
        }

        if (!NetworkUtils.isNetworkAvailable(requireContext())) {
            view?.showSnackbar(Constants.ErrorMessages.NETWORK_ERROR)
            return
        }

        binding.root.hideKeyboard()
        viewModel.verifyEmail(targetEmail, otp)
    }

    private fun resendOtp() {
        if (targetEmail.isBlank()) {
            view?.showSnackbar("Thiếu thông tin Gmail để gửi lại mã.")
            return
        }

        if (!NetworkUtils.isNetworkAvailable(requireContext())) {
            view?.showSnackbar(Constants.ErrorMessages.NETWORK_ERROR)
            return
        }

        binding.root.hideKeyboard()
        viewModel.resendVerification(targetEmail)
    }

    private fun observeStates() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.verifyEmailState.collect { resource ->
                when (resource) {
                    is Resource.Loading -> showVerifyLoading(true)
                    is Resource.Success -> {
                        showVerifyLoading(false)
                        requireContext().showLongToast(
                            resource.data ?: getString(R.string.rc_verify_success)
                        )
                        findNavController().navigate(
                            R.id.action_verifyEmailFragment_to_loginFragment,
                            bundleOf("prefillEmail" to targetEmail)
                        )
                    }
                    is Resource.Error -> {
                        showVerifyLoading(false)
                        view?.showSnackbar(resource.message ?: Constants.ErrorMessages.UNKNOWN_ERROR)
                    }
                    null -> Unit
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.resendVerificationState.collect { resource ->
                when (resource) {
                    is Resource.Loading -> showResendLoading(true)
                    is Resource.Success -> {
                        showResendLoading(false)
                        view?.showSnackbar(resource.data ?: "Đã gửi lại mã xác thực tới Gmail của bạn")
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
    }

    private fun startCooldown(seconds: Int) {
        resendTimer?.cancel()
        val bindingRef = _binding ?: return
        timerRunning = true
        bindingRef.btnResend.disable()
        resendTimer = object : CountDownTimer(seconds * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val secLeft = ((millisUntilFinished + 999L) / 1000L).toInt()
                _binding?.btnResend?.text = getString(R.string.rc_resend_countdown, secLeft)
            }

            override fun onFinish() {
                timerRunning = false
                _binding?.let {
                    it.btnResend.enable()
                    it.btnResend.text = getString(R.string.rc_resend_button)
                }
            }
        }.start()
    }

    private fun showVerifyLoading(loading: Boolean) {
        if (loading) {
            binding.progressVerify.show()
            binding.btnVerify.disable()
            binding.btnVerify.text = ""
        } else {
            binding.progressVerify.hide()
            binding.btnVerify.enable()
            binding.btnVerify.text = getString(R.string.rc_verify_button)
        }
    }

    private fun showResendLoading(loading: Boolean) {
        if (loading) {
            binding.progressResend.show()
            binding.btnResend.disable()
            binding.btnResend.text = ""
        } else {
            binding.progressResend.hide()
            if (!timerRunning) {
                binding.btnResend.enable()
                binding.btnResend.text = getString(R.string.rc_resend_button)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        resendTimer?.cancel()
        resendTimer = null
        timerRunning = false
        viewModel.resetVerifyEmailState()
        viewModel.resetResendVerificationState()
        _binding = null
    }
}
