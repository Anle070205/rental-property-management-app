package com.example.rental_property_management_app.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.databinding.FragmentRegisterBinding
import com.example.rental_property_management_app.utils.*
import com.example.rental_property_management_app.viewmodels.AuthViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

/**
 * RegisterFragment - Nhập đủ 5 thông tin Chủ nhà trọ -> Xác nhận đăng ký ->
 * Kiểm tra hợp lệ & trùng lặp -> Chọn Gmail hoặc Số điện thoại trong BottomSheet -> Gửi OTP.
 */
class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    private var pendingFullName: String = ""
    private var pendingEmail: String = ""
    private var pendingPhone: String = ""
    private var pendingPassword: String = ""
    private var chosenChannel: String = "EMAIL"

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
        observeStates()
    }

    private fun setupClickListeners() {
        binding.tvLogin.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnRegister.setOnClickListener {
            handleValidateRegister()
        }
    }

    private fun handleValidateRegister() {
        val fullName = binding.tilFullName.getText().trim()
        val email = binding.tilEmail.getText().trim()
        val phone = binding.tilPhone.getText().trim()
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

        val emailError = ValidationUtils.getGmailError(email)
        if (emailError != null) {
            binding.tilEmail.showError(emailError)
            isValid = false
        }

        val phoneError = ValidationUtils.getVietnamPhoneError(phone)
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

        if (!NetworkUtils.isNetworkAvailable(requireContext())) {
            view?.showSnackbar(Constants.ErrorMessages.NETWORK_ERROR)
            return
        }

        if (!isValid) return

        pendingFullName = fullName
        pendingEmail = email.lowercase()
        pendingPhone = ValidationUtils.normalizeVietnamPhoneToE164(phone) ?: phone
        pendingPassword = password

        binding.root.hideKeyboard()
        viewModel.validateBeforeRegister(pendingFullName, pendingEmail, pendingPhone, pendingPassword)
    }

    private fun showVerificationMethodChooser() {
        val dp = resources.displayMetrics.density
        val dialog = BottomSheetDialog(requireContext())

        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ContextCompat.getColor(context, R.color.surface))
            val pad = (20 * dp).toInt()
            setPadding(pad, pad, pad, (24 * dp).toInt())
        }

        val tvTitle = TextView(requireContext()).apply {
            text = "Chọn phương thức xác thực"
            textSize = 20f
            setTextColor(ContextCompat.getColor(context, R.color.rc_ink))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        val tvDesc = TextView(requireContext()).apply {
            text = "Vui lòng chọn phương thức nhận mã OTP để hoàn tất đăng ký tài khoản RoomCare."
            textSize = 13.5f
            setTextColor(ContextCompat.getColor(context, R.color.rc_muted))
            setPadding(0, (6 * dp).toInt(), 0, (16 * dp).toInt())
        }

        var selected = "EMAIL"
        val rbEmail = RadioButton(requireContext()).apply { isChecked = true; isClickable = false }
        val rbPhone = RadioButton(requireContext()).apply { isChecked = false; isClickable = false }

        fun buildOptionCard(
            iconRes: Int,
            title: String,
            maskedTarget: String,
            radio: RadioButton
        ): LinearLayout {
            return LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setBackgroundResource(R.drawable.rc_panel)
                val p = (14 * dp).toInt()
                setPadding(p, p, p, p)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = (10 * dp).toInt() }

                val icon = ImageView(requireContext()).apply {
                    setImageResource(iconRes)
                    setColorFilter(ContextCompat.getColor(context, R.color.rc_brand))
                }
                addView(icon, LinearLayout.LayoutParams((24 * dp).toInt(), (24 * dp).toInt()))

                val textCol = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                        marginStart = (12 * dp).toInt()
                        marginEnd = (8 * dp).toInt()
                    }
                    addView(TextView(requireContext()).apply {
                        text = title
                        textSize = 14.5f
                        setTextColor(ContextCompat.getColor(context, R.color.rc_ink))
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                    })
                    addView(TextView(requireContext()).apply {
                        text = maskedTarget
                        textSize = 13f
                        setTextColor(ContextCompat.getColor(context, R.color.rc_muted))
                    })
                }
                addView(textCol)
                addView(radio)
            }
        }

        val emailCard = buildOptionCard(
            R.drawable.ic_email,
            "Xác thực qua Gmail",
            maskEmail(pendingEmail),
            rbEmail
        )
        val phoneCard = buildOptionCard(
            R.drawable.ic_phone,
            "Xác thực qua số điện thoại",
            maskPhone(pendingPhone),
            rbPhone
        )

        emailCard.setOnClickListener {
            selected = "EMAIL"
            rbEmail.isChecked = true
            rbPhone.isChecked = false
        }
        phoneCard.setOnClickListener {
            selected = "PHONE"
            rbEmail.isChecked = false
            rbPhone.isChecked = true
        }

        val btnContinue = MaterialButton(requireContext()).apply {
            text = "Tiếp tục"
            isAllCaps = false
            textSize = 15f
            cornerRadius = (12 * dp).toInt()
            setTextColor(Color.WHITE)
            setBackgroundColor(ContextCompat.getColor(context, R.color.rc_brand))
            setOnClickListener {
                chosenChannel = selected
                dialog.dismiss()
                viewModel.register(
                    pendingFullName,
                    chosenChannel,
                    pendingEmail,
                    pendingPhone,
                    pendingPassword,
                    Constants.Roles.LANDLORD
                )
            }
        }

        root.addView(tvTitle)
        root.addView(tvDesc)
        root.addView(emailCard)
        root.addView(phoneCard)
        root.addView(
            btnContinue,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (50 * dp).toInt()).apply {
                topMargin = (6 * dp).toInt()
            }
        )

        dialog.setContentView(root)
        dialog.show()
    }

    private fun maskEmail(email: String): String {
        val parts = email.split("@")
        if (parts.size != 2 || parts[0].length <= 2) return email
        val prefix = parts[0].take(2)
        return "$prefix***@${parts[1]}"
    }

    private fun maskPhone(phone: String): String {
        if (phone.length <= 6) return phone
        return "${phone.take(4)}***${phone.takeLast(3)}"
    }

    private fun observeStates() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.validateRegisterState.collect { resource ->
                when (resource) {
                    is Resource.Loading -> showLoading(true)
                    is Resource.Success -> {
                        showLoading(false)
                        viewModel.resetValidateRegisterState()
                        showVerificationMethodChooser()
                    }
                    is Resource.Error -> {
                        showLoading(false)
                        val msg = resource.message ?: Constants.ErrorMessages.UNKNOWN_ERROR
                        viewModel.resetValidateRegisterState()
                        if (msg.contains("Email", ignoreCase = true)) {
                            binding.tilEmail.showError(msg)
                        } else if (msg.contains("Số điện thoại", ignoreCase = true)) {
                            binding.tilPhone.showError(msg)
                        } else {
                            view?.showSnackbar(msg)
                        }
                    }
                    null -> Unit
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.registerState.collect { resource ->
                when (resource) {
                    is Resource.Loading -> showLoading(true)
                    is Resource.Success -> {
                        showLoading(false)
                        val authResponse = resource.data
                        if (authResponse != null) {
                            if (authResponse.requiresVerification || authResponse.accessToken.isNullOrBlank()) {
                                val ch = authResponse.verificationChannel ?: chosenChannel
                                val target = authResponse.verificationTarget
                                    ?: if (ch == "PHONE") pendingPhone else pendingEmail
                                val altCh = if (ch == "PHONE") "EMAIL" else "PHONE"
                                val altTarget = if (ch == "PHONE") pendingEmail else pendingPhone
                                requireContext().showLongToast(
                                    if (ch == "PHONE") getString(R.string.rc_register_check_sms)
                                    else getString(R.string.rc_register_check_gmail)
                                )
                                findNavController().navigate(
                                    R.id.action_registerFragment_to_verifyOtpFragment,
                                    bundleOf(
                                        "channel" to ch,
                                        "target" to target,
                                        "cooldownSeconds" to (authResponse.resendCooldownSeconds ?: 60),
                                        "altChannel" to altCh,
                                        "altTarget" to altTarget
                                    )
                                )
                            } else {
                                requireContext().showToast("Đăng ký thành công!")
                                findNavController().navigate(R.id.action_registerFragment_to_landlordDashboardFragment)
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
            binding.btnRegister.text = "Xác nhận đăng ký"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.resetRegisterState()
        viewModel.resetValidateRegisterState()
        _binding = null
    }
}
