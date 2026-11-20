package com.example.rental_property_management_app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.databinding.FragmentAdminReportDetailBinding
import com.example.rental_property_management_app.models.AdminReportItem
import com.example.rental_property_management_app.utils.AdminUiHelper
import com.example.rental_property_management_app.utils.Resource
import com.example.rental_property_management_app.utils.showSnackbar
import com.example.rental_property_management_app.viewmodels.AdminViewModel
import kotlinx.coroutines.launch

class AdminReportDetailFragment : Fragment() {
    private var _binding: FragmentAdminReportDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AdminViewModel by viewModels()

    private var reportId: Long = 0L
    private var reportCode: String = ""
    private var senderUserId: Long? = null
    private var senderName: String = ""

    private val statusLabels = listOf("Đã xử lý", "Đang xử lý")
    private val statusCodes = listOf("DONE", "IN_PROGRESS")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminReportDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        reportId = arguments?.getLong("reportId", 0L) ?: 0L
        binding.spStatus.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, statusLabels)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnViewSender.setOnClickListener {
            senderUserId?.let {
                findNavController().navigate(R.id.adminAccountDetailFragment, bundleOf("accountId" to it))
            } ?: view.showSnackbar("Người gửi chưa có tài khoản đăng nhập")
        }
        binding.btnResolve.setOnClickListener {
            val note = binding.etResponseNote.text?.toString()?.trim().orEmpty()
            if (note.isBlank()) {
                view.showSnackbar("Vui lòng nhập nội dung phản hồi")
                return@setOnClickListener
            }
            val code = statusCodes[binding.spStatus.selectedItemPosition]
            val label = statusLabels[binding.spStatus.selectedItemPosition]
            AdminUiHelper.showConfirmDialog(
                context = requireContext(),
                title = "Hoàn tất báo cáo?",
                message = "Báo cáo $reportCode sẽ được đánh dấu $label và phản hồi sẽ được gửi đến $senderName.",
                confirmText = "Xác nhận hoàn tất"
            ) {
                viewModel.resolveReport(reportId, note, code)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.reportDetail.collect { res ->
                when (res) {
                    is Resource.Loading -> binding.btnResolve.isEnabled = false
                    is Resource.Success -> {
                        binding.btnResolve.isEnabled = true
                        res.data?.let { bindItem(it) }
                    }
                    is Resource.Error -> {
                        binding.btnResolve.isEnabled = true
                        view.showSnackbar(res.message ?: "Không thể tải hoặc xử lý báo cáo")
                    }
                    null -> Unit
                }
            }
        }
        viewModel.loadReportDetail(reportId)
    }

    private fun bindItem(r: AdminReportItem) {
        reportCode = r.reportCode
        senderUserId = r.senderUserId
        senderName = r.senderName
        binding.tvCodeSub.text = "Mã tiếp nhận · ${r.reportCode}"
        binding.tvStatus.text = when (r.status) {
            "DONE" -> "Đã xử lý"
            "IN_PROGRESS" -> "Đang xử lý"
            else -> "Chờ xử lý"
        }
        binding.tvTitle.text = r.title
        binding.tvSender.text = "Người gửi: ${r.senderName} · ${r.senderRole}"
        binding.tvTime.text = "Thời gian: ${AdminUiHelper.formatDateTimeShort(r.createdAt)}"
        val ctxText = listOfNotNull(
            r.landlordName.takeIf { it.isNotBlank() },
            r.propertyName.takeIf { it.isNotBlank() },
            r.roomCode.takeIf { it.isNotBlank() }?.let { "P.$it" }
        ).joinToString(" · ")
        binding.tvContext.text = if (ctxText.isNotBlank()) "Đơn vị: $ctxText" else "Đơn vị: --"
        binding.tvDescription.text = r.description

        val isDone = r.status == "DONE"
        binding.cardResult.isVisible = isDone || !r.resolutionNote.isNullOrBlank()
        binding.cardResolveForm.isVisible = !isDone
        if (binding.cardResult.isVisible) {
            binding.tvResolutionNote.text = r.resolutionNote.orEmpty()
            val resolver = r.resolvedByName?.takeIf { it.isNotBlank() } ?: "Admin"
            binding.tvResolvedBy.text = "$resolver · ${AdminUiHelper.formatDateTimeShort(r.resolvedAt)}"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
