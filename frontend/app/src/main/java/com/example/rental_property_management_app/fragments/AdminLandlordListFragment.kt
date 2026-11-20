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
import com.example.rental_property_management_app.utils.Resource
import com.example.rental_property_management_app.utils.showSnackbar
import com.example.rental_property_management_app.viewmodels.AdminViewModel
import kotlinx.coroutines.launch

class AdminLandlordListFragment : Fragment() {
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
        binding.tvScreenTitle.text = "Quản lý chủ nhà"
        binding.tvScreenSubtitle.text = "Chủ nhà trên toàn hệ thống"
        binding.tilSearch.hint = "Tìm theo tên, email, số điện thoại"
        binding.btnPrimaryTop.isVisible = true
        binding.btnPrimaryTop.text = "Xem danh sách nhà trọ ›"
        binding.btnPrimaryTop.setOnClickListener {
            findNavController().navigate(R.id.adminPropertyListFragment)
        }
        binding.tvFooterNote.isVisible = true
        binding.tvFooterNote.text = "Chủ nhà chỉ quản lý TENANT trong nhà trọ của mình. Không được truy cập người thuê của chủ nhà khác."

        adapter = AdminSimpleCardAdapter { row ->
            findNavController().navigate(R.id.adminLandlordDetailFragment, bundleOf("landlordId" to row.id))
        }
        binding.rvItems.layoutManager = LinearLayoutManager(requireContext())
        binding.rvItems.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { reload() }
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) { reload(); true } else false
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.landlords.collect { res ->
                when (res) {
                    is Resource.Loading -> binding.swipeRefresh.isRefreshing = true
                    is Resource.Success -> {
                        binding.swipeRefresh.isRefreshing = false
                        val data = res.data ?: return@collect
                        binding.tvScreenSubtitle.text = "${data.totalCount} chủ nhà trên toàn hệ thống"
                        binding.tvSummary.text = "${data.totalCount} chủ nhà · ${data.activeCount} hoạt động · ${data.lockedCount} bị khóa"
                        binding.tvEmpty.isVisible = data.items.isEmpty()
                        adapter.submitList(data.items.map { item ->
                            val contact = listOfNotNull(item.phone?.takeIf { it.isNotBlank() }, item.email?.takeIf { it.isNotBlank() }).joinToString(" · ")
                            AdminCardRow(
                                id = item.id,
                                title = item.fullName,
                                sub1 = contact.ifBlank { item.landlordCode },
                                sub2 = "${item.propertyCount} nhà trọ · ${item.roomCount} phòng · ${item.tenantCount} người thuê",
                                leftTag = item.landlordCode,
                                rightTag = if (item.isActive) "Đang hoạt động" else "Bị khóa",
                                isDangerRight = !item.isActive,
                                actionLabel = "Xem chi tiết chủ nhà"
                            )
                        })
                    }
                    is Resource.Error -> {
                        binding.swipeRefresh.isRefreshing = false
                        view.showSnackbar(res.message ?: "Không thể tải danh sách chủ nhà")
                    }
                    null -> Unit
                }
            }
        }
        reload()
    }

    private fun reload() {
        val kw = binding.etSearch.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        viewModel.loadLandlords(kw)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
