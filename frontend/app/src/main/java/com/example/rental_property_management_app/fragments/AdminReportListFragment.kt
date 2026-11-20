package com.example.rental_property_management_app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.adapters.AdminCardRow
import com.example.rental_property_management_app.adapters.AdminSimpleCardAdapter
import com.example.rental_property_management_app.databinding.FragmentAdminListCommonBinding
import com.example.rental_property_management_app.utils.AdminUiHelper
import com.example.rental_property_management_app.utils.Resource
import com.example.rental_property_management_app.utils.showSnackbar
import com.example.rental_property_management_app.viewmodels.AdminViewModel
import kotlinx.coroutines.launch

class AdminReportListFragment : Fragment() {
    private var _binding: FragmentAdminListCommonBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AdminViewModel by viewModels()
    private lateinit var adapter: AdminSimpleCardAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminListCommonBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvScreenTitle.text = "Quản lý báo cáo"
        binding.tvScreenSubtitle.text = "Khiếu nại và phản ánh hệ thống"
        binding.tilSearch.hint = "Mã báo cáo, người gửi hoặc nội dung"
        binding.tvFooterNote.isVisible = true
        binding.tvFooterNote.text = "Khiếu nại về thuê phòng được chuyển cho chủ nhà phụ trách; ADMIN hỗ trợ xử lý vấn đề cấp hệ thống."

        adapter = AdminSimpleCardAdapter { row ->
            findNavController().navigate(R.id.adminReportDetailFragment, bundleOf("reportId" to row.id))
        }
        binding.rvItems.layoutManager = LinearLayoutManager(requireContext())
        binding.rvItems.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { reload() }
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) { reload(); true } else false
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.reports.collect { res ->
                when (res) {
                    is Resource.Loading -> binding.swipeRefresh.isRefreshing = true
                    is Resource.Success -> {
                        binding.swipeRefresh.isRefreshing = false
                        val data = res.data ?: return@collect
                        binding.tvSummary.text = "${data.totalCount} báo cáo · ${data.pendingCount} chờ xử lý · ${data.inProgressCount} đang xử lý"
                        binding.tvEmpty.isVisible = data.items.isEmpty()
                        adapter.submitList(data.items.map { r ->
                            val statusLabel = when (r.status) {
                                "DONE" -> "Đã xử lý"
                                "IN_PROGRESS" -> "Đang xử lý"
                                else -> "Chờ xử lý"
                            }
                            AdminCardRow(
                                id = r.id,
                                badgeTop = "${r.reportCode} · $statusLabel",
                                title = r.title,
                                sub1 = "${r.senderName} · ${r.senderRole}",
                                leftTag = AdminUiHelper.formatDateTimeShort(r.createdAt),
                                rightTag = statusLabel,
                                isDangerRight = r.status == "RECEIVED",
                                actionLabel = if (r.status == "DONE") "Xem kết quả xử lý" else "Xem và xử lý báo cáo"
                            )
                        })
                    }
                    is Resource.Error -> {
                        binding.swipeRefresh.isRefreshing = false
                        view.showSnackbar(res.message ?: "Không thể tải danh sách báo cáo")
                    }
                    null -> Unit
                }
            }
        }
        reload()
    }

    private fun reload() {
        val kw = binding.etSearch.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        viewModel.loadReports(kw)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
