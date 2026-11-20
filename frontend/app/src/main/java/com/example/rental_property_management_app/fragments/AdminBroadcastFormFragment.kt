package com.example.rental_property_management_app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.rental_property_management_app.databinding.FragmentAdminBroadcastFormBinding
import com.example.rental_property_management_app.models.AdminBroadcastAudienceCounts
import com.example.rental_property_management_app.models.AdminBroadcastRequest
import com.example.rental_property_management_app.utils.AdminUiHelper
import com.example.rental_property_management_app.utils.Resource
import com.example.rental_property_management_app.utils.showSnackbar
import com.example.rental_property_management_app.viewmodels.AdminViewModel
import kotlinx.coroutines.launch

class AdminBroadcastFormFragment : Fragment() {
    private var _binding: FragmentAdminBroadcastFormBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AdminViewModel by viewModels()

    private var editBroadcastId: Long = 0L
    private var selectedAudience: String = "ALL"
    private var counts: AdminBroadcastAudienceCounts? = null
    private var submittedAction: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminBroadcastFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        editBroadcastId = arguments?.getLong("broadcastId", 0L) ?: 0L
        val isEdit = editBroadcastId > 0L

        binding.tvFormTitle.text = if (isEdit) "Chỉnh sửa thông báo" else "Tạo thông báo"
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnCancel.setOnClickListener { findNavController().navigateUp() }

        binding.btnPickAudience.setOnClickListener {
            AdminUiHelper.showAudienceDialog(requireContext(), counts) { chosen ->
                selectedAudience = chosen
                updateAudienceText()
            }
        }

        binding.btnSaveDraft.setOnClickListener { submit(sendNow = false, isEdit = isEdit) }
        binding.btnSendNow.setOnClickListener {
            val title = binding.etTitle.text?.toString()?.trim().orEmpty()
            val content = binding.etContent.text?.toString()?.trim().orEmpty()
            if (title.isBlank() || content.isBlank()) {
                view.showSnackbar("Vui lòng nhập tiêu đề và nội dung")
                return@setOnClickListener
            }
            val recCount = currentRecipientCount()
            AdminUiHelper.showConfirmDialog(
                context = requireContext(),
                title = "Gửi thông báo cho ${audienceLabel()}?",
                message = "$title\nGửi ngay đến $recCount tài khoản đang hoạt động. Không thể chỉnh sửa sau khi gửi.",
                confirmText = "Gửi đến $recCount tài khoản"
            ) {
                submit(sendNow = true, isEdit = isEdit)
            }
        }

        observeStates(isEdit)
        updateAudienceText()
        viewModel.loadAudienceCounts()
        if (isEdit) {
            viewModel.loadBroadcastDetail(editBroadcastId)
        }
    }

    private fun submit(sendNow: Boolean, isEdit: Boolean) {
        val title = binding.etTitle.text?.toString()?.trim().orEmpty()
        val content = binding.etContent.text?.toString()?.trim().orEmpty()
        if (title.isBlank() || content.isBlank()) {
            view?.showSnackbar("Vui lòng nhập tiêu đề và nội dung")
            return
        }
        val req = AdminBroadcastRequest(title, content, selectedAudience)
        submittedAction = if (sendNow) "SENT" else "DRAFT"
        if (isEdit) {
            viewModel.updateBroadcast(editBroadcastId, req)
            if (sendNow) {
                viewModel.sendBroadcast(editBroadcastId)
            }
        } else {
            viewModel.createBroadcast(req, sendNow)
        }
    }

    private fun currentRecipientCount(): Long = when (selectedAudience) {
        "ADMIN" -> counts?.adminActive ?: 0
        "LANDLORD" -> counts?.landlordActive ?: 0
        "TENANT" -> counts?.tenantActive ?: 0
        else -> counts?.allActive ?: 0
    }

    private fun audienceLabel(): String = when (selectedAudience) {
        "ADMIN" -> "ADMIN"
        "LANDLORD" -> "LANDLORD"
        "TENANT" -> "TENANT"
        else -> "Toàn hệ thống"
    }

    private fun updateAudienceText() {
        binding.tvSelectedAudience.text = when (selectedAudience) {
            "ADMIN" -> "ADMIN (Quản trị viên)"
            "LANDLORD" -> "LANDLORD (Chủ nhà)"
            "TENANT" -> "TENANT (Người thuê)"
            else -> "Toàn hệ thống (ADMIN, LANDLORD, TENANT)"
        }
        val active = currentRecipientCount()
        val locked = counts?.lockedCount ?: 0
        binding.tvRecipientPreview.text = "$active tài khoản sẽ nhận thông báo. Chỉ bao gồm tài khoản đang hoạt động; $locked tài khoản bị khóa được loại trừ."
    }

    private fun observeStates(isEdit: Boolean) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.audienceCounts.collect { res ->
                if (res is Resource.Success) {
                    counts = res.data
                    updateAudienceText()
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.broadcastDetail.collect { res ->
                when (res) {
                    is Resource.Success -> {
                        val b = res.data ?: return@collect
                        if (submittedAction == null && isEdit) {
                            binding.etTitle.setText(b.title)
                            binding.etContent.setText(b.content)
                            selectedAudience = b.targetAudience
                            updateAudienceText()
                        } else if (submittedAction != null) {
                            val act = submittedAction
                            submittedAction = null
                            AdminUiHelper.showConfirmDialog(
                                context = requireContext(),
                                title = if (act == "SENT") "Đã gửi thông báo" else "Đã lưu bản nháp",
                                message = if (act == "SENT") "Đã phát hành đến ${b.recipientCount} tài khoản." else "Bản nháp đã được lưu, chưa gửi đến người dùng.",
                                confirmText = "Quay lại danh sách",
                                cancelText = "Đóng"
                            ) {
                                findNavController().navigateUp()
                            }
                        }
                    }
                    is Resource.Error -> {
                        submittedAction = null
                        view?.showSnackbar(res.message ?: "Không thể lưu thông báo")
                    }
                    else -> Unit
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
