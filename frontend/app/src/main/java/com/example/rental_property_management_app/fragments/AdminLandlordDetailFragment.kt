package com.example.rental_property_management_app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.databinding.FragmentAdminLandlordDetailBinding
import com.example.rental_property_management_app.models.AdminLandlordItem
import com.example.rental_property_management_app.utils.AdminUiHelper
import com.example.rental_property_management_app.utils.Resource
import com.example.rental_property_management_app.utils.showSnackbar
import com.example.rental_property_management_app.viewmodels.AdminViewModel
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class AdminLandlordDetailFragment : Fragment() {
    private var _binding: FragmentAdminLandlordDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AdminViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminLandlordDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val landlordId = arguments?.getLong("landlordId", 0L) ?: 0L
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.landlordDetail.collect { res ->
                when (res) {
                    is Resource.Success -> res.data?.let { bindItem(it) }
                    is Resource.Error -> view.showSnackbar(res.message ?: "Không thể tải chi tiết chủ nhà")
                    else -> Unit
                }
            }
        }
        viewModel.loadLandlordDetail(landlordId)
    }

    private fun bindItem(item: AdminLandlordItem) {
        binding.tvName.text = item.fullName
        val st = if (item.isActive) "Đang hoạt động" else "Bị khóa"
        binding.tvRoleStatus.text = "LANDLORD · ${item.landlordCode} · $st"
        binding.tvEmail.text = "Email: ${item.email ?: "--"}"
        binding.tvPhone.text = "Số điện thoại: ${item.phone ?: "--"}"
        binding.tvCreated.text = "Ngày tạo: ${AdminUiHelper.formatDateShort(item.createdAt)}"
        binding.tvAddress.text = "Địa chỉ: ${item.address?.takeIf { it.isNotBlank() } ?: "--"}"

        val dp = resources.displayMetrics.density
        binding.llProperties.removeAllViews()
        if (item.properties.isEmpty()) {
            binding.llProperties.addView(TextView(requireContext()).apply { text = "Chưa có nhà trọ" })
        } else {
            item.properties.forEach { p ->
                val card = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL
                    setBackgroundResource(R.drawable.rc_panel)
                    val pad = (12 * dp).toInt()
                    setPadding(pad, pad, pad, pad)
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                        bottomMargin = (8 * dp).toInt()
                    }
                }
                card.addView(TextView(requireContext()).apply {
                    text = p.name
                    textSize = 14f
                    setTextColor(ContextCompat.getColor(context, R.color.rc_ink))
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                })
                card.addView(TextView(requireContext()).apply {
                    text = "${p.roomCount} phòng · ${p.occupiedCount} đang thuê · ${p.tenantCount} người thuê"
                    textSize = 12f
                    setTextColor(ContextCompat.getColor(context, R.color.rc_muted))
                })
                card.addView(MaterialButton(requireContext()).apply {
                    text = "Xem ${p.name}"
                    isAllCaps = false
                    setBackgroundColor(ContextCompat.getColor(context, R.color.rc_tint_soft))
                    setTextColor(ContextCompat.getColor(context, R.color.rc_brand_dark))
                    setOnClickListener {
                        findNavController().navigate(R.id.adminPropertyDetailFragment, bundleOf("propertyId" to p.id))
                    }
                }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (40 * dp).toInt()).apply { topMargin = (6 * dp).toInt() })
                binding.llProperties.addView(card)
            }
        }

        binding.tvTenantsHeader.text = "Người thuê trực thuộc · ${item.tenants.size}"
        binding.llTenants.removeAllViews()
        if (item.tenants.isEmpty()) {
            binding.llTenants.addView(TextView(requireContext()).apply { text = "Chưa có người thuê trực thuộc" })
        } else {
            item.tenants.forEach { t ->
                val card = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL
                    setBackgroundResource(R.drawable.rc_panel)
                    val pad = (12 * dp).toInt()
                    setPadding(pad, pad, pad, pad)
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                        bottomMargin = (8 * dp).toInt()
                    }
                }
                card.addView(TextView(requireContext()).apply {
                    text = "${t.fullName} · ${t.propertyName}"
                    textSize = 13.5f
                    setTextColor(ContextCompat.getColor(context, R.color.rc_ink))
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                })
                if (t.userId != null) {
                    card.addView(MaterialButton(requireContext()).apply {
                        text = "Tra cứu tài khoản ${t.fullName}"
                        isAllCaps = false
                        setBackgroundColor(ContextCompat.getColor(context, R.color.rc_tint_soft))
                        setTextColor(ContextCompat.getColor(context, R.color.rc_brand_dark))
                        setOnClickListener {
                            findNavController().navigate(R.id.adminAccountDetailFragment, bundleOf("accountId" to t.userId))
                        }
                    }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (40 * dp).toInt()).apply { topMargin = (6 * dp).toInt() })
                }
                binding.llTenants.addView(card)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
