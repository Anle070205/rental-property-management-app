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
import com.example.rental_property_management_app.databinding.FragmentAdminPropertyDetailBinding
import com.example.rental_property_management_app.models.AdminPropertyItem
import com.example.rental_property_management_app.utils.Resource
import com.example.rental_property_management_app.utils.showSnackbar
import com.example.rental_property_management_app.viewmodels.AdminViewModel
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class AdminPropertyDetailFragment : Fragment() {
    private var _binding: FragmentAdminPropertyDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AdminViewModel by viewModels()
    private var landlordId: Long? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminPropertyDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val propertyId = arguments?.getLong("propertyId", 0L) ?: 0L
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnViewOwner.setOnClickListener {
            landlordId?.let {
                findNavController().navigate(R.id.adminLandlordDetailFragment, bundleOf("landlordId" to it))
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.propertyDetail.collect { res ->
                when (res) {
                    is Resource.Success -> res.data?.let { bindItem(it) }
                    is Resource.Error -> view.showSnackbar(res.message ?: "Không thể tải chi tiết nhà trọ")
                    else -> Unit
                }
            }
        }
        viewModel.loadPropertyDetail(propertyId)
    }

    private fun bindItem(p: AdminPropertyItem) {
        landlordId = p.landlordId
        binding.tvPropName.text = p.name
        binding.tvPropCodeStatus.text = "${p.propertyCode} · ${p.statusLabel}"
        binding.tvAddress.text = "Địa chỉ: ${p.address.orEmpty()}"
        binding.tvOwner.text = "Chủ sở hữu: ${p.landlordName} · ${p.landlordCode}"
        binding.tvRoomTenantCount.text = "${p.totalRooms} phòng · ${p.tenantCount} người thuê"
        binding.tvRoomBreakdown.text = "${p.occupiedRooms} đang thuê · ${p.vacantRooms} trống · ${p.maintenanceRooms} bảo trì"
        binding.tvOccupancyPct.text = "Tỷ lệ lấp đầy: ${p.occupancyPercent}%"

        val dp = resources.displayMetrics.density
        binding.llOccupiedRooms.removeAllViews()
        if (p.occupiedRoomLookups.isEmpty()) {
            binding.llOccupiedRooms.addView(TextView(requireContext()).apply { text = "Chưa có phòng đang thuê" })
        } else {
            p.occupiedRoomLookups.forEach { r ->
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
                    text = "P.${r.roomCode} · Tầng ${r.floor ?: "-"}"
                    textSize = 14f
                    setTextColor(ContextCompat.getColor(context, R.color.rc_ink))
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                })
                card.addView(TextView(requireContext()).apply {
                    text = "Người thuê: ${r.tenantName}"
                    textSize = 12f
                    setTextColor(ContextCompat.getColor(context, R.color.rc_muted))
                })
                if (r.tenantUserId != null) {
                    card.addView(MaterialButton(requireContext()).apply {
                        text = "Tra cứu người thuê"
                        isAllCaps = false
                        setBackgroundColor(ContextCompat.getColor(context, R.color.rc_tint_soft))
                        setTextColor(ContextCompat.getColor(context, R.color.rc_brand_dark))
                        setOnClickListener {
                            findNavController().navigate(R.id.adminAccountDetailFragment, bundleOf("accountId" to r.tenantUserId))
                        }
                    }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (40 * dp).toInt()).apply { topMargin = (6 * dp).toInt() })
                }
                binding.llOccupiedRooms.addView(card)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
