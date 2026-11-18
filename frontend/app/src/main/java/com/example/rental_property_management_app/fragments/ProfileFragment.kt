package com.example.rental_property_management_app.fragments

import android.os.Bundle
import android.view.View
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
import com.example.rental_property_management_app.viewmodels.AuthViewModel
import com.example.rental_property_management_app.viewmodels.ProfileViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class ProfileFragment : Fragment(R.layout.fragment_profile) {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val profile: ProfileViewModel by viewModels()
    private val auth: AuthViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentProfileBinding.bind(view)
        binding.retry.setOnClickListener { profile.load() }
        binding.save.setOnClickListener {
            profile.save(UpdateProfileRequest(binding.fullName.text.toString().trim(), binding.phone.text.toString().trim()))
        }
        binding.password.setOnClickListener { requestPassword() }
        binding.logout.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.rc_logout)
                .setMessage(R.string.rc_logout_confirm)
                .setNegativeButton(android.R.string.cancel, null)
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
                result.data?.let {
                    binding.fullName.setText(it.fullName)
                    binding.phone.setText(it.phone)
                    binding.email.text = it.email
                    binding.role.text = getString(if (it.role == "LANDLORD") R.string.rc_landlord else R.string.rc_tenant)
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            profile.mutation.collect { result ->
                binding.save.isEnabled = result !is Resource.Loading && profile.profile.value is Resource.Success
                binding.password.isEnabled = result !is Resource.Loading
                if (result is Resource.Success || result is Resource.Error) {
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
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.rc_save, null)
            .show()
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val current = fields.currentPassword.text.toString()
            val new = fields.newPassword.text.toString()
            when {
                current.isBlank() -> fields.currentPassword.error = getString(R.string.rc_required)
                new.isBlank() || new.length < 6 -> fields.newPassword.error = getString(R.string.rc_password_length)
                else -> {
                    profile.changePassword(ChangePasswordRequest(current, new))
                    dialog.dismiss()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
