package com.example.rental_property_management_app.fragments

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
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
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class AdminAccountListFragment : Fragment() {
    private var _binding: FragmentAdminListCommonBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AdminViewModel by viewModels()

    private var selectedRole: String? = null
    private var selectedActive: Boolean? = null
    private var currentPage = 0
    private var totalPages = 1

    private lateinit var adapter: AdminSimpleCardAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminListCommonBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvScreenTitle.text = "Quản lý tài khoản"
        binding.tvScreenSubtitle.text = "Tra cứu toàn hệ thống"
        binding.tilSearch.hint = "Tìm tên, email hoặc số điện thoại"
        binding.btnPrimaryTop.isVisible = true
        binding.btnPrimaryTop.text = "+ Thêm tài khoản"
        binding.btnFilterPill.isVisible = true
        binding.llPagination.isVisible = true

        adapter = AdminSimpleCardAdapter { row ->
            findNavController().navigate(R.id.adminAccountDetailFragment, bundleOf("accountId" to row.id))
        }
        binding.rvItems.layoutManager = LinearLayoutManager(requireContext())
        binding.rvItems.adapter = adapter

        binding.btnPrimaryTop.setOnClickListener {
            findNavController().navigate(R.id.adminAccountFormFragment)
        }
        binding.btnFilterPill.setOnClickListener { showFilterDialog() }
        binding.swipeRefresh.setOnRefreshListener { reload() }
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                currentPage = 0
                reload()
                true
            } else false
        }
        binding.btnPrevPage.setOnClickListener {
            if (currentPage > 0) {
                currentPage--
                reload()
            }
        }
        binding.btnNextPage.setOnClickListener {
            if (currentPage + 1 < totalPages) {
                currentPage++
                reload()
            }
        }

        updateFilterLabel()
        observeAccounts()
        reload()
    }

    private fun reload() {
        val kw = binding.etSearch.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        viewModel.loadAccounts(selectedRole, selectedActive, kw, currentPage)
    }

    private fun updateFilterLabel() {
        val roleText = when (selectedRole) {
            "ADMIN" -> "ADMIN"
            "LANDLORD" -> "LANDLORD"
            "TENANT" -> "TENANT"
            else -> "Tất cả"
        }
        val statusText = when (selectedActive) {
            true -> "Đang hoạt động"
            false -> "Bị khóa"
            null -> "Tất cả"
        }
        binding.btnFilterPill.text = "Vai trò: $roleText · Trạng thái: $statusText (Nhấn để lọc)"
    }

    private fun showFilterDialog() {
        val dp = resources.displayMetrics.density
        val dialog = Dialog(requireContext())
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.rc_panel)
            val pad = (20 * dp).toInt()
            setPadding(pad, pad, pad, pad)
        }
        card.addView(TextView(requireContext()).apply {
            text = "Lọc tài khoản"
            textSize = 18f
            setTextColor(ContextCompat.getColor(context, R.color.rc_ink))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })

        val rgRole = RadioGroup(requireContext()).apply { orientation = RadioGroup.VERTICAL }
        val roles = listOf(null to "Tất cả vai trò", "ADMIN" to "ADMIN - Quản trị viên", "LANDLORD" to "LANDLORD - Chủ nhà", "TENANT" to "TENANT - Người thuê")
        roles.forEachIndexed { idx, (code, label) ->
            val rb = RadioButton(requireContext()).apply {
                id = 100 + idx
                text = label
                isChecked = selectedRole == code
                tag = code
            }
            rgRole.addView(rb)
        }
        card.addView(TextView(requireContext()).apply {
            text = "Vai trò"
            textSize = 13f
            setPadding(0, (10 * dp).toInt(), 0, (4 * dp).toInt())
        })
        card.addView(rgRole)

        val rgStatus = RadioGroup(requireContext()).apply { orientation = RadioGroup.VERTICAL }
        val statuses = listOf(null to "Tất cả trạng thái", true to "Đang hoạt động", false to "Bị khóa")
        statuses.forEachIndexed { idx, (code, label) ->
            val rb = RadioButton(requireContext()).apply {
                id = 200 + idx
                text = label
                isChecked = selectedActive == code
                tag = code
            }
            rgStatus.addView(rb)
        }
        card.addView(TextView(requireContext()).apply {
            text = "Trạng thái"
            textSize = 13f
            setPadding(0, (10 * dp).toInt(), 0, (4 * dp).toInt())
        })
        card.addView(rgStatus)

        val btnApply = MaterialButton(requireContext()).apply {
            text = "Áp dụng bộ lọc"
            isAllCaps = false
            setBackgroundColor(ContextCompat.getColor(context, R.color.rc_brand))
            setTextColor(Color.WHITE)
            setOnClickListener {
                val rChecked = rgRole.findViewById<RadioButton>(rgRole.checkedRadioButtonId)
                val sChecked = rgStatus.findViewById<RadioButton>(rgStatus.checkedRadioButtonId)
                selectedRole = rChecked?.tag as? String
                selectedActive = sChecked?.tag as? Boolean
                currentPage = 0
                updateFilterLabel()
                dialog.dismiss()
                reload()
            }
        }
        val btnClear = MaterialButton(requireContext()).apply {
            text = "Xóa bộ lọc"
            isAllCaps = false
            setBackgroundColor(ContextCompat.getColor(context, R.color.rc_tint_soft))
            setTextColor(ContextCompat.getColor(context, R.color.rc_brand_dark))
            setOnClickListener {
                selectedRole = null
                selectedActive = null
                binding.etSearch.setText("")
                currentPage = 0
                updateFilterLabel()
                dialog.dismiss()
                reload()
            }
        }
        card.addView(btnApply, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (46 * dp).toInt()).apply { topMargin = (12 * dp).toInt() })
        card.addView(btnClear, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (44 * dp).toInt()).apply { topMargin = (6 * dp).toInt() })

        dialog.setContentView(card)
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.90).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }

    private fun observeAccounts() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.accounts.collect { res ->
                when (res) {
                    is Resource.Loading -> binding.swipeRefresh.isRefreshing = true
                    is Resource.Success -> {
                        binding.swipeRefresh.isRefreshing = false
                        val data = res.data ?: return@collect
                        totalPages = data.totalPages.coerceAtLeast(1)
                        binding.tvScreenSubtitle.text = "${data.allCount} tài khoản trên toàn hệ thống"
                        binding.tvSummary.text = "Tất cả ${data.allCount} · ADMIN ${data.adminCount} · LANDLORD ${data.landlordCount} · TENANT ${data.tenantCount}"
                        binding.tvPageInfo.text = "Trang ${data.page + 1} / $totalPages (${data.totalElements} kết quả)"
                        binding.btnPrevPage.isEnabled = data.page > 0
                        binding.btnNextPage.isEnabled = data.page + 1 < totalPages
                        binding.tvEmpty.isVisible = data.items.isEmpty()

                        adapter.submitList(data.items.map { item ->
                            val contact = listOfNotNull(item.email?.takeIf { it.isNotBlank() }, item.phone?.takeIf { it.isNotBlank() }).joinToString(" · ")
                            AdminCardRow(
                                id = item.id,
                                title = item.fullName,
                                sub1 = contact.ifBlank { item.accountCode },
                                leftTag = "${item.role} · ${item.accountCode}",
                                rightTag = if (item.isActive) "Đang hoạt động" else "Bị khóa",
                                isDangerRight = !item.isActive
                            )
                        })
                    }
                    is Resource.Error -> {
                        binding.swipeRefresh.isRefreshing = false
                        view?.showSnackbar(res.message ?: "Không thể tải danh sách tài khoản")
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
