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
import com.example.rental_property_management_app.databinding.FragmentVerifyOtpBinding
import com.example.rental_property_management_app.utils.Constants
import com.example.rental_property_management_app.utils.NetworkUtils
import com.example.rental_property_management_app.utils.OtpBoxGroup
import com.example.rental_property_management_app.utils.Resource
import com.example.rental_property_management_app.utils.ValidationUtils
import com.example.rental_property_management_app.utils.disable
import com.example.rental_property_management_app.utils.enable
import com.example.rental_property_management_app.utils.hide
import com.example.rental_property_management_app.utils.hideKeyboard
import com.example.rental_property_management_app.utils.show
import com.example.rental_property_management_app.utils.showLongToast
import com.example.rental_property_management_app.utils.showSnackbar
import com.example.rental_property_management_app.viewmodels.AuthViewModel
import kotlinx.coroutines.launch

/**
 * Màn hình nhập OTP 6 số cho cả 2 phương thức đăng ký: EMAIL (Gmail) và PHONE (SMS +84).
 * Giữ bộ đếm ngược & trạng thái an toàn khi xoay màn hình.
 */
class VerifyOtpFragment : Fragment() {

    private var _binding: FragmentVerifyOtpBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()
    private lateinit var otpBoxGroup: OtpBoxGroup

    private var channel: String = "EMAIL"
    private var target: String = ""
    private var altChannel: String? = null
    private var altTarget: String? = null
    private var resendTimer: CountDownTimer? = null
    private var timerRunning = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVerifyOtpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        channel = arguments?.getString("channel")?.takeIf { it.isNotBlank() } ?: "EMAIL"
        target = arguments?.getString("target").orEmpty().trim()
        altChannel = arguments?.getString("altChannel")?.takeIf { it.isNotBlank() }
        altTarget = arguments?.getString("altTarget")?.takeIf { it.isNotBlank() }
        val initialCooldown = arguments?.getInt("cooldownSeconds", 60) ?: 60

        configureChannelCopy()
        setupOtpBoxes()
        restoreOrStartCooldown(initialCooldown)
        setupListeners()
        observeStates()
    }

    private fun configureChannelCopy() {
        val isPhone = channel == "PHONE"
        binding.tvMethodBadge.text = if (isPhone) {
            getString(R.string.rc_verify_subtitle_phone)
        } else {
            getString(R.string.rc_verify_subtitle)
        }
        binding.tvTitle.text = if (isPhone) {
            getString(R.string.rc_verify_otp_title_phone)
        } else {
            getString(R.string.rc_verify_otp_title)
        }
        binding.tvDescription.text = if (isPhone) {
            getString(R.string.rc_verify_otp_desc_phone)
        } else {
            getString(R.string.rc_verify_otp_desc)
        }
        binding.tvVerifyTip.text = if (isPhone) {
            getString(R.string.rc_verify_tip_phone)
        } else {
            getString(R.string.rc_verify_tip_email)
        }
        binding.ivChannelIcon.setImageResource(if (isPhone) R.drawable.ic_phone else R.drawable.ic_email)
        binding.tvTarget.text = target
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

    private fun restoreOrStartCooldown(argCooldownSeconds: Int) {
        val remaining = viewModel.remainingVerificationCooldownSeconds()
        if (remaining > 0) {
            startCooldown(remaining)
        } else if (viewModel.verificationCooldownEndMs == 0L && argCooldownSeconds > 0) {
            viewModel.markVerificationCooldown(argCooldownSeconds)
            startCooldown(argCooldownSeconds)
        }
    }

    private fun setupListeners() {
        binding.btnVerify.setOnClickListener {
            submitOtp()
        }

        binding.btnResend.setOnClickListener {
            resendOtp()
        }

        binding.tvSwitchMethod.setOnClickListener {
            val nextCh = altChannel
            val nextTarget = altTarget
            if (!nextCh.isNullOrBlank() && !nextTarget.isNullOrBlank()) {
                otpBoxGroup.clear()
                viewModel.switchRegistrationChannel(channel, target, nextCh)
            } else {
                viewModel.clearOtpAndResetProgress()
                findNavController().popBackStack()
            }
        }

        binding.tvBackToLogin.setOnClickListener {
            viewModel.clearOtpAndResetProgress()
            findNavController().navigate(
                R.id.action_verifyOtpFragment_to_loginFragment,
                bundleOf("prefillIdentifier" to target)
            )
        }
    }

    private fun submitOtp() {
        val otp = otpBoxGroup.getOtp()
        binding.tvOtpError.hide()

        if (target.isBlank()) {
            view?.showSnackbar("Thiếu thông tin tài khoản cần xác thực. Vui lòng quay lại màn hình trước.")
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
        viewModel.verifyOtp(channel, target, otp)
    }

    private fun resendOtp() {
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
        viewModel.resendOtp(channel, target)
    }

    private fun observeStates() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.verifyOtpState.collect { resource ->
                when (resource) {
                    is Resource.Loading -> showVerifyLoading(true)
                    is Resource.Success -> {
                        showVerifyLoading(false)
                        viewModel.clearOtpAndResetProgress()
                        requireContext().showLongToast(
                            resource.data ?: getString(R.string.rc_verify_success)
                        )
                        findNavController().navigate(
                            R.id.action_verifyOtpFragment_to_loginFragment,
                            bundleOf("prefillIdentifier" to target)
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
            viewModel.resendOtpState.collect { resource ->
                when (resource) {
                    is Resource.Loading -> showResendLoading(true)
                    is Resource.Success -> {
                        showResendLoading(false)
                        view?.showSnackbar(resource.data ?: "Đã gửi lại mã OTP tới $target")
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
            viewModel.switchChannelState.collect { resource ->
                when (resource) {
                    is Resource.Loading -> showResendLoading(true)
                    is Resource.Success -> {
                        showResendLoading(false)
                        val data = resource.data
                        if (data != null) {
                            val oldCh = channel
                            val oldTarget = target
                            channel = data.verificationChannel ?: (altChannel ?: channel)
                            target = data.verificationTarget ?: (altTarget ?: target)
                            altChannel = oldCh
                            altTarget = oldTarget
                            configureChannelCopy()
                            startCooldown(data.resendCooldownSeconds ?: 60)
                            view?.showSnackbar("Đã chuyển phương thức xác thực sang $target")
                        }
                        viewModel.resetSwitchChannelState()
                    }
                    is Resource.Error -> {
                        showResendLoading(false)
                        view?.showSnackbar(resource.message ?: Constants.ErrorMessages.UNKNOWN_ERROR)
                        viewModel.resetSwitchChannelState()
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
        viewModel.resetVerifyOtpState()
        viewModel.resetResendOtpState()
        _binding = null
    }
}
