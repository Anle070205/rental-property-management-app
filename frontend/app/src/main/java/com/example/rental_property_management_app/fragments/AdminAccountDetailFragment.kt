package com.example.rental_property_management_app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.databinding.FragmentAdminAccountDetailBinding
import com.example.rental_property_management_app.models.AdminAccountItem
import com.example.rental_property_management_app.utils.AdminUiHelper
import com.example.rental_property_management_app.utils.Resource
import com.example.rental_property_management_app.utils.showSnackbar
import com.example.rental_property_management_app.viewmodels.AdminViewModel
import kotlinx.coroutines.launch

class AdminAccountDetailFragment : Fragment() {
    private var _binding: FragmentAdminAccountDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AdminViewModel by viewModels()
    private var accountId: Long = 0L
    private var currentItem: AdminAccountItem? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminAccountDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        accountId = arguments?.getLong("accountId", 0L) ?: 0L
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnEditAccount.setOnClickListener {
            findNavController().navigate(R.id.adminAccountFormFragment, bundleOf("accountId" to accountId))
        }
        binding.btnViewLandlord.setOnClickListener {
            val lId = currentItem?.landlordId ?: return@setOnClickListener
            findNavController().navigate(R.id.adminLandlordDetailFragment, bundleOf("landlordId" to lId))
        }
        binding.btnToggleLock.setOnClickListener {
            val item = currentItem ?: return@setOnClickListener
            if (item.isActive) {
                AdminUiHelper.showConfirmDialog(
                    context = requireContext(),
                    title = "Khóa tài khoản?",
                    message = "${item.fullName} · ${item.role}\nTài khoản sẽ không thể đăng nhập. Liên kết chủ nhà, nhà trọ, phòng và dữ liệu thuê vẫn được giữ nguyên.",
                    confirmText = "Xác nhận khóa",
                    isDanger = true
                ) { viewModel.lockAccount(item.id) }
            } else {
                AdminUiHelper.showConfirmDialog(
                    context = requireContext(),
                    title = "Mở khóa tài khoản?",
                    message = "Cho phép ${item.fullName} đăng nhập trở lại. Không thay đổi chủ nhà quản lý hoặc dữ liệu thuê.",
                    confirmText = "Xác nhận mở khóa"
                ) { viewModel.unlockAccount(item.id) }
            }
        }

        observeDetail()
        viewModel.loadAccountDetail(accountId)
    }

    private fun observeDetail() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.accountDetail.collect { res ->
                when (res) {
                    is Resource.Success -> res.data?.let { bindItem(it) }
                    is Resource.Error -> view?.showSnackbar(res.message ?: "Không thể tải chi tiết tài khoản")
                    else -> Unit
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.accountAction.collect { res ->
                when (res) {
                    is Resource.Success -> {
                        view?.showSnackbar(res.data?.noteMessage ?: "Thao tác thành công")
                        viewModel.resetAccountAction()
                    }
                    is Resource.Error -> {
                        view?.showSnackbar(res.message ?: "Không thể thực hiện thao tác")
                        viewModel.resetAccountAction()
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun bindItem(item: AdminAccountItem) {
        currentItem = item
        val initials = item.fullName.trim().split("\\s+".toRegex()).takeLast(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("").ifEmpty { "TK" }
        binding.tvInitials.text = initials
        binding.tvSubtitle.text = "Tra cứu tài khoản · ${item.accountCode}"
        binding.tvName.text = item.fullName
        binding.tvRoleCode.text = "${item.role} / ${item.accountCode}"
        binding.tvEmail.text = item.email ?: "--"
        binding.tvPhone.text = item.phone ?: "--"
        binding.tvCreatedAt.text = AdminUiHelper.formatDateShort(item.createdAt)

        if (item.isActive) {
            binding.tvStatusBadge.text = "Đang hoạt động"
            binding.tvStatusBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.rc_brand_dark))
            binding.btnToggleLock.text = "Khóa tài khoản"
            binding.btnToggleLock.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.rc_danger))
        } else {
            binding.tvStatusBadge.text = "Bị khóa"
            binding.tvStatusBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.rc_danger))
            binding.btnToggleLock.text = "Mở khóa tài khoản"
            binding.btnToggleLock.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.rc_brand))
        }

        binding.cardTenantUnit.isVisible = item.role == "TENANT" && item.landlordName != null
        if (binding.cardTenantUnit.isVisible) {
            binding.tvManagingLandlord.text = "Chủ nhà: ${item.landlordName} · ${item.landlordCode ?: ""}"
            val propRoom = listOfNotNull(item.propertyName, item.roomCode?.let { "P.$it" }).joinToString(" · ")
            binding.tvManagingPropertyRoom.text = if (propRoom.isNotBlank()) "Nhà trọ / Phòng: $propRoom" else "Nhà trọ: --"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
