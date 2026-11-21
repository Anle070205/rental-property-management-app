package com.example.rental_property_management_app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.rental_property_management_app.RoomCareApplication
import com.example.rental_property_management_app.databinding.FragmentAdminChangePasswordBinding
import com.example.rental_property_management_app.utils.AdminUiHelper
import com.example.rental_property_management_app.utils.Resource
import com.example.rental_property_management_app.utils.clearError
import com.example.rental_property_management_app.utils.getText
import com.example.rental_property_management_app.utils.showError
import com.example.rental_property_management_app.utils.showSnackbar
import com.example.rental_property_management_app.viewmodels.AdminViewModel
import kotlinx.coroutines.launch

class AdminChangePasswordFragment : Fragment() {
    private var _binding: FragmentAdminChangePasswordBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AdminViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminChangePasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBackProfile.setOnClickListener { findNavController().navigateUp() }
        binding.btnCancel.setOnClickListener { findNavController().navigateUp() }
        binding.btnUpdatePw.setOnClickListener { submit() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.passwordChange.collect { res ->
                when (res) {
                    is Resource.Loading -> binding.btnUpdatePw.isEnabled = false
                    is Resource.Success -> {
                        binding.btnUpdatePw.isEnabled = true
                        val token = res.data?.accessToken.orEmpty()
                        if (token.isNotBlank()) {
                            RoomCareApplication.prefsManager.saveAccessToken(token)
                        }
                        viewModel.resetPasswordChange()
                        AdminUiHelper.showConfirmDialog(
                            context = requireContext(),
                            title = "Đã đổi mật khẩu",
                            message = res.data?.message ?: "Mật khẩu mới đã được cập nhật. Các phiên đăng nhập khác đã kết thúc.",
                            confirmText = "Về hồ sơ Admin",
                            cancelText = "Đóng"
                        ) {
                            findNavController().navigateUp()
                        }
                    }
                    is Resource.Error -> {
                        binding.btnUpdatePw.isEnabled = true
                        view.showSnackbar(res.message ?: "Không thể đổi mật khẩu")
                    }
                    null -> Unit
                }
            }
        }
    }

    private fun submit() {
        val current = binding.tilCurrentPw.getText()
        val next = binding.tilNewPw.getText()
        val confirm = binding.tilConfirmPw.getText()
        binding.tilCurrentPw.clearError()
        binding.tilNewPw.clearError()
        binding.tilConfirmPw.clearError()

        if (current.isBlank()) {
            binding.tilCurrentPw.showError("Nhập mật khẩu hiện tại")
            return
        }
        if (next.length < 6) {
            binding.tilNewPw.showError("Mật khẩu mới tối thiểu 6 ký tự")
            return
        }
        if (next == current) {
            binding.tilNewPw.showError("Mật khẩu mới không được trùng mật khẩu hiện tại")
            return
        }
        if (next != confirm) {
            binding.tilConfirmPw.showError("Xác nhận mật khẩu mới không khớp")
            return
        }
        AdminUiHelper.showConfirmDialog(
            context = requireContext(),
            title = "Xác nhận đổi mật khẩu?",
            message = "Sau khi đổi mật khẩu, các phiên đăng nhập khác sẽ bị đăng xuất.",
            confirmText = "Cập nhật mật khẩu"
        ) {
            viewModel.changeAdminPassword(current, next)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
