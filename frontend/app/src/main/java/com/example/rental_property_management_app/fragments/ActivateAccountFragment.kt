package com.example.rental_property_management_app.fragments

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.databinding.FragmentActivateAccountBinding
import com.example.rental_property_management_app.utils.Resource
import com.example.rental_property_management_app.utils.ValidationUtils
import com.example.rental_property_management_app.utils.hideKeyboard
import com.example.rental_property_management_app.utils.showLongToast
import com.example.rental_property_management_app.utils.showSnackbar
import com.example.rental_property_management_app.viewmodels.AuthViewModel
import kotlinx.coroutines.launch

class ActivateAccountFragment : Fragment() {

    private var _binding: FragmentActivateAccountBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentActivateAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvBackToLogin.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.rgChannel.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rbEmail) {
                binding.tilTarget.hint = "Email / Gmail được cấp"
                binding.etTarget.inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            } else {
                binding.tilTarget.hint = "Số điện thoại được cấp"
                binding.etTarget.inputType = InputType.TYPE_CLASS_PHONE
            }
        }

        binding.btnActivate.setOnClickListener {
            submitActivation()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.activateAccountState.collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.btnActivate.isEnabled = false
                        binding.btnActivate.text = ""
                    }
                    is Resource.Success -> {
                        binding.progressBar.visibility = View.GONE
                        binding.btnActivate.isEnabled = true
                        binding.btnActivate.text = "Kích hoạt & Đặt mật khẩu"
                        requireContext().showLongToast(resource.data ?: "Kích hoạt tài khoản thành công!")
                        val target = binding.etTarget.text?.toString()?.trim().orEmpty()
                        viewModel.resetActivateAccountState()
                        val navOptions = NavOptions.Builder()
                            .setPopUpTo(R.id.loginFragment, true)
                            .build()
                        findNavController().navigate(
                            R.id.loginFragment,
                            bundleOf("prefillIdentifier" to target),
                            navOptions
                        )
                    }
                    is Resource.Error -> {
                        binding.progressBar.visibility = View.GONE
                        binding.btnActivate.isEnabled = true
                        binding.btnActivate.text = "Kích hoạt & Đặt mật khẩu"
                        view.showSnackbar(resource.message ?: "Không thể kích hoạt tài khoản")
                        viewModel.resetActivateAccountState()
                    }
                    null -> Unit
                }
            }
        }
    }

    private fun submitActivation() {
        val channel = if (binding.rbPhone.isChecked) "PHONE" else "EMAIL"
        val target = binding.etTarget.text?.toString()?.trim().orEmpty()
        val otp = binding.etOtp.text?.toString()?.trim().orEmpty()
        val pass = binding.etNewPassword.text?.toString().orEmpty()
        val confirm = binding.etConfirmPassword.text?.toString().orEmpty()

        var valid = true
        if (channel == "EMAIL" && !ValidationUtils.isValidEmail(target)) {
            binding.tilTarget.error = getString(R.string.error_email_invalid)
            valid = false
        } else if (channel == "PHONE" && !ValidationUtils.isValidPhone(target)) {
            binding.tilTarget.error = getString(R.string.error_phone_invalid)
            valid = false
        } else {
            binding.tilTarget.error = null
        }

        if (!ValidationUtils.isValidOtp(otp)) {
            binding.tilOtp.error = "Mã OTP gồm đúng 6 chữ số"
            valid = false
        } else {
            binding.tilOtp.error = null
        }

        if (!ValidationUtils.isValidPassword(pass)) {
            binding.tilNewPassword.error = getString(R.string.rc_password_length)
            valid = false
        } else {
            binding.tilNewPassword.error = null
        }

        if (pass != confirm) {
            binding.tilConfirmPassword.error = "Mật khẩu xác nhận không khớp"
            valid = false
        } else {
            binding.tilConfirmPassword.error = null
        }

        if (valid) {
            binding.root.hideKeyboard()
            viewModel.activateAccount(channel, target, otp, pass, confirm)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
