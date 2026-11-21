package com.example.rental_property_management_app.fragments

import android.os.Bundle
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
import com.example.rental_property_management_app.databinding.FragmentAdminBroadcastDetailBinding
import com.example.rental_property_management_app.models.AdminBroadcastItem
import com.example.rental_property_management_app.utils.AdminUiHelper
import com.example.rental_property_management_app.utils.Resource
import com.example.rental_property_management_app.utils.showSnackbar
import com.example.rental_property_management_app.viewmodels.AdminViewModel
import kotlinx.coroutines.launch

class AdminBroadcastDetailFragment : Fragment() {
    private var _binding: FragmentAdminBroadcastDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AdminViewModel by viewModels()
    private var broadcastId: Long = 0L
    private var currentItem: AdminBroadcastItem? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminBroadcastDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        broadcastId = arguments?.getLong("broadcastId", 0L) ?: 0L
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnEditDraft.setOnClickListener {
            findNavController().navigate(R.id.adminBroadcastFormFragment, bundleOf("broadcastId" to broadcastId))
        }
        binding.btnSendNow.setOnClickListener {
            val item = currentItem ?: return@setOnClickListener
            val targetName = when (item.targetAudience) {
                "ALL" -> "toàn hệ thống"
                else -> item.targetAudience
            }
            AdminUiHelper.showConfirmDialog(
                context = requireContext(),
                title = "Gửi thông báo cho $targetName?",
                message = "${item.title}\nKhông thể chỉnh sửa nội dung sau khi gửi.",
                confirmText = "Xác nhận gửi"
            ) {
                viewModel.sendBroadcast(broadcastId)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.broadcastDetail.collect { res ->
                when (res) {
                    is Resource.Loading -> binding.btnSendNow.isEnabled = false
                    is Resource.Success -> {
                        binding.btnSendNow.isEnabled = true
                        res.data?.let { bindItem(it) }
                    }
                    is Resource.Error -> {
                        binding.btnSendNow.isEnabled = true
                        view.showSnackbar(res.message ?: "Không thể tải hoặc gửi thông báo")
                    }
                    null -> Unit
                }
            }
        }
        viewModel.loadBroadcastDetail(broadcastId)
    }

    private fun bindItem(b: AdminBroadcastItem) {
        currentItem = b
        val isDraft = b.status == "DRAFT"
        binding.tvCodeSub.text = (if (isDraft) "Bản nháp" else "Đã gửi") + " · ${b.broadcastCode}"
        binding.tvStatus.text = if (isDraft) "BẢN NHÁP" else "ĐÃ GỬI THÀNH CÔNG"
        binding.tvTitle.text = b.title
        binding.tvContent.text = b.content
        binding.tvAudience.text = "Đối tượng nhận: " + when (b.targetAudience) {
            "ALL" -> "Toàn hệ thống (ADMIN, LANDLORD, TENANT)"
            else -> b.targetAudience
        }
        binding.tvRecipients.text = "Số tài khoản nhận: ${b.recipientCount}"
        binding.tvCreator.text = "Người tạo: ${b.createdByName ?: "ADMIN"}"
        binding.tvTime.text = (if (isDraft) "Cập nhật: " else "Thời gian gửi: ") +
            AdminUiHelper.formatDateTimeShort(b.sentAt ?: b.updatedAt)

        binding.btnEditDraft.isVisible = isDraft
        binding.btnSendNow.isVisible = isDraft
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
