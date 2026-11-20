package com.example.rental_property_management_app.fragments

import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.View
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.databinding.DialogChangePasswordBinding
import com.example.rental_property_management_app.databinding.FragmentProfileBinding
import com.example.rental_property_management_app.models.ChangePasswordRequest
import com.example.rental_property_management_app.models.UpdateProfileRequest
import com.example.rental_property_management_app.utils.Resource
import com.example.rental_property_management_app.utils.ValidationUtils
import com.example.rental_property_management_app.viewmodels.AuthViewModel
import com.example.rental_property_management_app.viewmodels.ProfileViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class ProfileFragment : Fragment(R.layout.fragment_profile) {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val profile: ProfileViewModel by viewModels()
    private val auth: AuthViewModel by viewModels()
    private var pendingReloginAfterPasswordChange = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentProfileBinding.bind(view)
        if (com.example.rental_property_management_app.RoomCareApplication.prefsManager.getUserRole() != "TENANT") {
            binding.toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
            binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        }
        binding.phone.isEnabled = false
        binding.retry.setOnClickListener { profile.load() }
        binding.save.setOnClickListener {
            val name = binding.fullName.text.toString().trim()
            if (name.isBlank()) {
                binding.fullName.error = getString(R.string.error_full_name_required)
            } else {
                profile.save(UpdateProfileRequest(name, null))
            }
        }
        binding.password.setOnClickListener { requestPassword() }
        binding.btnLinkContact.setOnClickListener { showLinkContactDialog() }
        binding.logout.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.rc_logout)
                .setMessage(R.string.rc_logout_confirm)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.rc_logout) { _, _ -> auth.logout() }
                .show()
        }
        viewLifecycleOwner.lifecycleScope.launch {
            profile.profile.collect { result ->
                binding.progress.isVisible = result is Resource.Loading
                binding.retry.isVisible = result is Resource.Error
                binding.error.isVisible = result is Resource.Error
                binding.error.text = result.message
                binding.save.isEnabled = result is Resource.Success && profile.mutation.value !is Resource.Loading
                binding.btnLinkContact.isEnabled = result is Resource.Success && profile.mutation.value !is Resource.Loading
                result.data?.let {
                    binding.displayName.text = it.fullName
                    binding.avatar.text = profileInitials(it.fullName)
                    binding.fullName.setText(it.fullName)
                    binding.phone.isEnabled = false
                    binding.phone.setText(
                        it.phone?.takeIf { p -> p.isNotBlank() }
                            ?.let { p -> if (it.phoneVerified) "$p (Đã xác thực)" else p }
                            ?: getString(R.string.rc_not_linked_phone)
                    )
                    binding.email.text = it.email?.takeIf { e -> e.isNotBlank() }
                        ?.let { e -> if (it.emailVerified) "$e (Đã xác thực)" else e }
                        ?: getString(R.string.rc_not_linked_email)
                    binding.role.text = getString(if (it.role == "LANDLORD") R.string.rc_landlord else R.string.rc_tenant)
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            profile.mutation.collect { result ->
                binding.save.isEnabled = result !is Resource.Loading && profile.profile.value is Resource.Success
                binding.btnLinkContact.isEnabled = result !is Resource.Loading && profile.profile.value is Resource.Success
                binding.password.isEnabled = result !is Resource.Loading
                if (result is Resource.Success) {
                    val shouldLogout = pendingReloginAfterPasswordChange
                    pendingReloginAfterPasswordChange = false
                    val message = if (shouldLogout) {
                        "Đổi mật khẩu thành công. Vui lòng đăng nhập lại."
                    } else {
                        result.message ?: getString(R.string.rc_saved)
                    }
                    Snackbar.make(view, message, Snackbar.LENGTH_LONG).show()
                    profile.consumeMutation()
                    if (shouldLogout) {
                        auth.logout()
                    }
                } else if (result is Resource.Error) {
                    pendingReloginAfterPasswordChange = false
                    Snackbar.make(view, result.message ?: getString(R.string.rc_saved), Snackbar.LENGTH_LONG).show()
                    profile.consumeMutation()
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            auth.logoutState.collect { result ->
                binding.logout.isEnabled = result !is Resource.Loading
                if (result is Resource.Success || result is Resource.Error) {
                    findNavController().navigate(R.id.loginFragment, null, navOptions {
                        popUpTo(R.id.nav_graph) { inclusive = true }
                    })
                }
            }
        }
    }

    private fun requestPassword() {
        val fields = DialogChangePasswordBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.rc_change_password)
            .setView(fields.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.rc_save, null)
            .show()
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val current = fields.currentPassword.text.toString()
            val new = fields.newPassword.text.toString()
            when {
                current.isBlank() -> fields.currentPassword.error = getString(R.string.rc_required)
                new.isBlank() || new.length < 6 -> fields.newPassword.error = getString(R.string.rc_password_length)
                else -> {
                    pendingReloginAfterPasswordChange = true
                    profile.changePassword(ChangePasswordRequest(current, new))
                    dialog.dismiss()
                }
            }
        }
    }

    private fun showLinkContactDialog() {
        val ctx = requireContext()
        val pad = (20 * resources.displayMetrics.density).toInt()
        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad / 2, pad, 0)
        }

        val rbEmail = RadioButton(ctx).apply {
            id = View.generateViewId()
            text = "Gmail (@gmail.com)"
        }
        val rbPhone = RadioButton(ctx).apply {
            id = View.generateViewId()
            text = "Số điện thoại (+84 / 0...)"
        }
        val rgChannel = RadioGroup(ctx).apply {
            orientation = LinearLayout.VERTICAL
            addView(rbEmail)
            addView(rbPhone)
        }
        container.addView(rgChannel)

        val tilTarget = TextInputLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = pad / 2
            }
        }
        val etTarget = TextInputEditText(ctx)
        tilTarget.addView(etTarget)
        container.addView(tilTarget)

        val currentProfile = profile.profile.value.data
        val defaultChannel = if (currentProfile?.email.isNullOrBlank()) "EMAIL" else "PHONE"
        if (defaultChannel == "EMAIL") {
            rbEmail.isChecked = true
            tilTarget.hint = "Địa chỉ Gmail (@gmail.com)"
            etTarget.inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            etTarget.setText(currentProfile?.email.orEmpty())
        } else {
            rbPhone.isChecked = true
            tilTarget.hint = "Số điện thoại Việt Nam (0... hoặc +84...)"
            etTarget.inputType = InputType.TYPE_CLASS_PHONE
            etTarget.setText(currentProfile?.phone.orEmpty())
        }

        rgChannel.setOnCheckedChangeListener { _, checkedId ->
            tilTarget.error = null
            if (checkedId == rbEmail.id) {
                tilTarget.hint = "Địa chỉ Gmail (@gmail.com)"
                etTarget.inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                etTarget.setText(currentProfile?.email.orEmpty())
            } else {
                tilTarget.hint = "Số điện thoại Việt Nam (0... hoặc +84...)"
                etTarget.inputType = InputType.TYPE_CLASS_PHONE
                etTarget.setText(currentProfile?.phone.orEmpty())
            }
        }

        val dialog = MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.rc_link_contact_button)
            .setMessage("Chọn 1 phương thức cần bổ sung hoặc cập nhật và xác thực bằng mã OTP 6 số.")
            .setView(container)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton("Gửi mã OTP", null)
            .show()

        val sendBtn = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
        sendBtn.setOnClickListener {
            val channel = if (rbPhone.isChecked) "PHONE" else "EMAIL"
            val rawTarget = etTarget.text?.toString()?.trim().orEmpty()
            val error = if (channel == "EMAIL") {
                ValidationUtils.getGmailError(rawTarget)
            } else {
                ValidationUtils.getVietnamPhoneError(rawTarget)
            }
            if (error != null) {
                tilTarget.error = error
                return@setOnClickListener
            }
            val normalizedTarget = if (channel == "PHONE") {
                ValidationUtils.normalizeVietnamPhoneToE164(rawTarget) ?: rawTarget
            } else {
                rawTarget.lowercase()
            }
            tilTarget.error = null
            sendBtn.isEnabled = false
            profile.requestContactOtp(channel, normalizedTarget) { res ->
                if (!isAdded || _binding == null) return@requestContactOtp
                sendBtn.isEnabled = true
                when (res) {
                    is Resource.Success -> {
                        dialog.dismiss()
                        showVerifyContactOtpDialog(channel, normalizedTarget)
                    }
                    is Resource.Error -> {
                        tilTarget.error = res.message ?: "Không thể gửi mã OTP"
                    }
                    is Resource.Loading -> Unit
                }
            }
        }
    }

    private fun showVerifyContactOtpDialog(channel: String, normalizedTarget: String) {
        val ctx = requireContext()
        val pad = (20 * resources.displayMetrics.density).toInt()
        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad / 2, pad, 0)
        }
        val tilOtp = TextInputLayout(ctx).apply {
            hint = getString(R.string.rc_otp_label)
        }
        val etOtp = TextInputEditText(ctx).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            filters = arrayOf(InputFilter.LengthFilter(6))
        }
        tilOtp.addView(etOtp)
        container.addView(tilOtp)

        val channelLabel = if (channel == "PHONE") "SMS" else "Gmail"
        val dialog = MaterialAlertDialogBuilder(ctx)
            .setTitle("Nhập mã OTP ($channelLabel)")
            .setMessage("Nhập mã OTP 6 chữ số đã gửi tới $normalizedTarget:")
            .setView(container)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton("Xác thực", null)
            .show()

        val verifyBtn = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
        verifyBtn.setOnClickListener {
            val otp = etOtp.text?.toString()?.trim().orEmpty()
            val error = ValidationUtils.getOtpError(otp)
            if (error != null) {
                tilOtp.error = error
                return@setOnClickListener
            }
            tilOtp.error = null
            verifyBtn.isEnabled = false
            profile.verifyContactOtp(channel, normalizedTarget, otp) { res ->
                if (!isAdded || _binding == null) return@verifyContactOtp
                verifyBtn.isEnabled = true
                when (res) {
                    is Resource.Success -> {
                        dialog.dismiss()
                        Snackbar.make(
                            binding.root,
                            "Đã xác thực và liên kết $normalizedTarget thành công",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                    is Resource.Error -> {
                        tilOtp.error = res.message ?: "Mã OTP không hợp lệ"
                    }
                    is Resource.Loading -> Unit
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
