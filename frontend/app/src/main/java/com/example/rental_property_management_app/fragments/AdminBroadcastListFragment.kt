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

class AdminBroadcastListFragment : Fragment() {
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
        binding.tvScreenTitle.text = "Quản lý thông báo"
        binding.tvScreenSubtitle.text = "Thông tin từ ban quản trị"
        binding.tilSearch.hint = "Tiêu đề hoặc nội dung"
        binding.btnPrimaryTop.isVisible = true
        binding.btnPrimaryTop.text = "+ Tạo thông báo"
        binding.btnPrimaryTop.setOnClickListener {
            findNavController().navigate(R.id.adminBroadcastFormFragment)
        }
        binding.tvFooterNote.isVisible = true
        binding.tvFooterNote.text = "Chỉ ADMIN được phát hành thông báo hệ thống. Thông báo đã gửi được giữ nguyên để lưu lịch sử."

        adapter = AdminSimpleCardAdapter { row ->
            findNavController().navigate(R.id.adminBroadcastDetailFragment, bundleOf("broadcastId" to row.id))
        }
        binding.rvItems.layoutManager = LinearLayoutManager(requireContext())
        binding.rvItems.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { reload() }
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) { reload(); true } else false
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.broadcasts.collect { res ->
                when (res) {
                    is Resource.Loading -> binding.swipeRefresh.isRefreshing = true
                    is Resource.Success -> {
                        binding.swipeRefresh.isRefreshing = false
                        val data = res.data ?: return@collect
                        binding.tvSummary.text = "${data.totalCount} thông báo · ${data.sentCount} đã gửi · ${data.draftCount} bản nháp"
                        binding.tvEmpty.isVisible = data.items.isEmpty()
                        adapter.submitList(data.items.map { b ->
                            val isDraft = b.status == "DRAFT"
                            val audLabel = when (b.targetAudience) {
                                "ALL" -> "Toàn hệ thống"
                                else -> b.targetAudience
                            }
                            AdminCardRow(
                                id = b.id,
                                badgeTop = (if (isDraft) "BẢN NHÁP" else "ĐÃ GỬI") + " · ${b.broadcastCode}",
                                title = b.title,
                                sub1 = if (isDraft) audLabel else "$audLabel · ${b.recipientCount} tài khoản nhận",
                                leftTag = AdminUiHelper.formatDateTimeShort(b.sentAt ?: b.updatedAt),
                                rightTag = if (isDraft) "Bản nháp" else "Đã gửi",
                                actionLabel = if (isDraft) "Xem bản nháp" else "Xem chi tiết"
                            )
                        })
                    }
                    is Resource.Error -> {
                        binding.swipeRefresh.isRefreshing = false
                        view.showSnackbar(res.message ?: "Không thể tải danh sách thông báo")
                    }
                    null -> Unit
                }
            }
        }
        reload()
    }

    private fun reload() {
        val kw = binding.etSearch.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        viewModel.loadBroadcasts(kw)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
