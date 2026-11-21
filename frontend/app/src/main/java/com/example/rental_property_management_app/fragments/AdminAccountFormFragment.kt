package com.example.rental_property_management_app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.rental_property_management_app.databinding.FragmentAdminAccountFormBinding
import com.example.rental_property_management_app.models.AdminCreateAccountRequest
import com.example.rental_property_management_app.models.AdminLandlordItem
import com.example.rental_property_management_app.models.AdminPropertyItem
import com.example.rental_property_management_app.models.AdminUpdateAccountRequest
import com.example.rental_property_management_app.utils.AdminUiHelper
import com.example.rental_property_management_app.utils.Resource
import com.example.rental_property_management_app.utils.showSnackbar
import com.example.rental_property_management_app.viewmodels.AdminViewModel
import kotlinx.coroutines.launch

class AdminAccountFormFragment : Fragment() {
    private var _binding: FragmentAdminAccountFormBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AdminViewModel by viewModels()

    private var editAccountId: Long = 0L
    private val roles = listOf("TENANT", "LANDLORD", "ADMIN")
    private val statuses = listOf("Đang hoạt động", "Bị khóa")
    private var landlords: List<AdminLandlordItem> = emptyList()
    private var allProperties: List<AdminPropertyItem> = emptyList()
    private var filteredProperties: List<AdminPropertyItem> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminAccountFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        editAccountId = arguments?.getLong("accountId", 0L) ?: 0L
        val isEdit = editAccountId > 0L

        binding.tvFormTitle.text = if (isEdit) "Chỉnh sửa tài khoản" else "Thêm tài khoản"
        binding.btnSubmit.text = if (isEdit) "Lưu thay đổi" else "Tạo tài khoản"
        binding.tilPassword.isVisible = !isEdit

        binding.spRole.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, roles)
        binding.spStatus.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, statuses)

        binding.spRole.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                val r = roles[pos]
                binding.cardTenantLink.isVisible = !isEdit && r == "TENANT"
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        binding.spLandlord.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                val chosen = landlords.getOrNull(pos) ?: return
                filteredProperties = allProperties.filter { it.landlordId == chosen.id }
                val propNames = if (filteredProperties.isEmpty()) listOf("(Chưa có nhà trọ)") else filteredProperties.map { "${it.name} (${it.propertyCode})" }
                binding.spProperty.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, propNames)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnCancel.setOnClickListener { findNavController().navigateUp() }
        binding.btnSubmit.setOnClickListener { handleSubmit(isEdit) }

        observeStates(isEdit)
        if (isEdit) {
            viewModel.loadAccountDetail(editAccountId)
        } else {
            viewModel.loadLandlords()
            viewModel.loadProperties()
        }
    }

    private fun handleSubmit(isEdit: Boolean) {
        val fullName = binding.etFullName.text?.toString()?.trim().orEmpty()
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        val phone = binding.etPhone.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        val password = binding.etPassword.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        val role = roles[binding.spRole.selectedItemPosition]
        val isActive = binding.spStatus.selectedItemPosition == 0

        if (fullName.isBlank() || email.isBlank()) {
            view?.showSnackbar("Vui lòng nhập đầy đủ họ tên và email")
            return
        }

        if (isEdit) {
            AdminUiHelper.showConfirmDialog(
                context = requireContext(),
                title = "Lưu thông tin tài khoản?",
                message = "Cập nhật thông tin truy cập của $fullName. Chủ nhà quản lý và liên kết thuê không thay đổi.",
                confirmText = "Lưu thay đổi"
            ) {
                viewModel.updateAccount(editAccountId, AdminUpdateAccountRequest(fullName, email, phone, role, isActive))
            }
        } else {
            if (password != null && password.length < 6) {
                view?.showSnackbar("Mật khẩu tối thiểu 6 ký tự")
                return
            }
            var lId: Long? = null
            var pId: Long? = null
            if (role == "TENANT") {
                lId = landlords.getOrNull(binding.spLandlord.selectedItemPosition)?.id
                pId = filteredProperties.getOrNull(binding.spProperty.selectedItemPosition)?.id
                if (lId == null || pId == null) {
                    view?.showSnackbar("Tài khoản Người thuê cần chọn Chủ nhà và Nhà trọ")
                    return
                }
            }
            viewModel.createAccount(AdminCreateAccountRequest(fullName, email, phone, role, isActive, password, lId, pId, null))
        }
    }

    private fun observeStates(isEdit: Boolean) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.accountDetail.collect { res ->
                if (res is Resource.Success && isEdit) {
                    val item = res.data ?: return@collect
                    binding.etFullName.setText(item.fullName)
                    binding.etEmail.setText(item.email.orEmpty())
                    binding.etPhone.setText(item.phone.orEmpty())
                    val roleIdx = roles.indexOf(item.role).coerceAtLeast(0)
                    binding.spRole.setSelection(roleIdx)
                    binding.spRole.isEnabled = !item.hasTenantLink
                    binding.spStatus.setSelection(if (item.isActive) 0 else 1)
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.landlords.collect { res ->
                if (res is Resource.Success) {
                    landlords = res.data?.items ?: emptyList()
                    binding.spLandlord.adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_spinner_dropdown_item,
                        landlords.map { "${it.fullName} · ${it.landlordCode}" }
                    )
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.properties.collect { res ->
                if (res is Resource.Success) {
                    allProperties = res.data?.items ?: emptyList()
                    val selectedLandlord = landlords.getOrNull(binding.spLandlord.selectedItemPosition)
                    if (selectedLandlord != null) {
                        filteredProperties = allProperties.filter { it.landlordId == selectedLandlord.id }
                        binding.spProperty.adapter = ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_spinner_dropdown_item,
                            filteredProperties.map { "${it.name} (${it.propertyCode})" }
                        )
                    }
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.accountAction.collect { res ->
                when (res) {
                    is Resource.Loading -> binding.btnSubmit.isEnabled = false
                    is Resource.Success -> {
                        binding.btnSubmit.isEnabled = true
                        val created = res.data
                        viewModel.resetAccountAction()
                        AdminUiHelper.showConfirmDialog(
                            context = requireContext(),
                            title = if (isEdit) "Cập nhật thành công" else "Tạo tài khoản thành công",
                            message = "${created?.fullName ?: ""}\n${created?.accountCode ?: ""} / ${created?.role ?: ""}\n${created?.noteMessage ?: ""}",
                            confirmText = "Về danh sách tài khoản",
                            cancelText = "Đóng"
                        ) {
                            findNavController().navigateUp()
                        }
                    }
                    is Resource.Error -> {
                        binding.btnSubmit.isEnabled = true
                        view?.showSnackbar(res.message ?: "Không thể lưu tài khoản")
                        viewModel.resetAccountAction()
                    }
                    null -> Unit
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
