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

class AdminPropertyListFragment : Fragment() {
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
        binding.tvScreenTitle.text = "Quản lý nhà trọ"
        binding.tvScreenSubtitle.text = "Danh sách nhà trọ toàn hệ thống"
        binding.tilSearch.hint = "Tên nhà trọ, địa chỉ hoặc chủ sở hữu"
        binding.tvFooterNote.isVisible = true
        binding.tvFooterNote.text = "Phòng là dữ liệu tra cứu, không có thao tác thêm/sửa phòng, lập hợp đồng hoặc thu tiền trong khu vực ADMIN."

        adapter = AdminSimpleCardAdapter { row ->
            findNavController().navigate(R.id.adminPropertyDetailFragment, bundleOf("propertyId" to row.id))
        }
        binding.rvItems.layoutManager = LinearLayoutManager(requireContext())
        binding.rvItems.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { reload() }
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) { reload(); true } else false
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.properties.collect { res ->
                when (res) {
                    is Resource.Loading -> binding.swipeRefresh.isRefreshing = true
                    is Resource.Success -> {
                        binding.swipeRefresh.isRefreshing = false
                        val data = res.data ?: return@collect
                        binding.tvScreenSubtitle.text = "${data.totalProperties} nhà trọ · ${data.totalRooms} phòng"
                        binding.tvSummary.text = "${data.totalProperties} nhà trọ · ${data.activeProperties} hoạt động · ${data.pausedProperties} tạm dừng"
                        binding.tvEmpty.isVisible = data.items.isEmpty()
                        adapter.submitList(data.items.map { p ->
                            AdminCardRow(
                                id = p.id,
                                title = p.name,
                                sub1 = "${p.address.orEmpty()} · Chủ sở hữu: ${p.landlordName}",
                                sub2 = "${p.totalRooms} phòng · ${p.occupiedRooms} đang thuê · ${p.vacantRooms} trống · ${p.maintenanceRooms} bảo trì",
                                leftTag = p.propertyCode,
                                rightTag = p.statusLabel,
                                isDangerRight = p.statusLabel != "Đang hoạt động",
                                actionLabel = "Xem chi tiết nhà trọ"
                            )
                        })
                    }
                    is Resource.Error -> {
                        binding.swipeRefresh.isRefreshing = false
                        view.showSnackbar(res.message ?: "Không thể tải danh sách nhà trọ")
                    }
                    null -> Unit
                }
            }
        }
        reload()
    }

    private fun reload() {
        val kw = binding.etSearch.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        viewModel.loadProperties(kw)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
