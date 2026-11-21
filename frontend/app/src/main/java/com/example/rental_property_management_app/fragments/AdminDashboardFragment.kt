package com.example.rental_property_management_app.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
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
import com.example.rental_property_management_app.RoomCareApplication
import com.example.rental_property_management_app.databinding.FragmentAdminDashboardBinding
import com.example.rental_property_management_app.models.AdminDashboardResponse
import com.example.rental_property_management_app.utils.Resource
import com.example.rental_property_management_app.utils.showSnackbar
import com.example.rental_property_management_app.viewmodels.AdminViewModel
import kotlinx.coroutines.launch

class AdminDashboardFragment : Fragment() {
    private var _binding: FragmentAdminDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AdminViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val cachedName = RoomCareApplication.prefsManager.getUserName() ?: "Admin"
        binding.tvGreeting.text = "Xin chào, $cachedName"

        binding.swipeRefresh.setOnRefreshListener { viewModel.loadDashboard() }
        binding.cardTotalAccounts.setOnClickListener { findNavController().navigate(R.id.adminAccountListFragment) }
        binding.cardLandlords.setOnClickListener { findNavController().navigate(R.id.adminLandlordListFragment) }
        binding.cardProperties.setOnClickListener { findNavController().navigate(R.id.adminPropertyListFragment) }
        binding.btnViewReports.setOnClickListener { findNavController().navigate(R.id.adminReportListFragment) }

        observeDashboard()
        viewModel.loadDashboard()
    }

    private fun observeDashboard() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.dashboard.collect { res ->
                when (res) {
                    is Resource.Loading -> binding.swipeRefresh.isRefreshing = true
                    is Resource.Success -> {
                        binding.swipeRefresh.isRefreshing = false
                        res.data?.let { bindData(it) }
                    }
                    is Resource.Error -> {
                        binding.swipeRefresh.isRefreshing = false
                        view?.showSnackbar(res.message ?: "Không thể tải tổng quan")
                    }
                    null -> Unit
                }
            }
        }
    }

    private fun bindData(data: AdminDashboardResponse) {
        binding.tvGreeting.text = "Xin chào, ${data.adminName}"
        binding.tvTotalAccounts.text = data.totalAccounts.toString()
        binding.tvLandlords.text = data.landlordCount.toString()
        binding.tvTenants.text = data.tenantCount.toString()
        binding.tvProperties.text = data.propertyCount.toString()
        binding.tvRooms.text = data.totalRooms.toString()
        binding.tvOccupiedRooms.text = "${data.occupiedRooms} đang thuê"
        binding.tvAdmins.text = data.adminCount.toString()

        binding.tvAccountStatusLine.text = "${data.activeAccounts} hoạt động · ${data.lockedAccounts} bị khóa"
        val pct = data.activePercentage.toInt().coerceIn(0, 100)
        binding.progressActiveRatio.progress = pct
        binding.tvActivePctLine.text = "${data.activePercentage}% tài khoản đang hoạt động"

        renderBars(data)
        renderActivities(data)
    }

    private fun renderBars(data: AdminDashboardResponse) {
        binding.llChartBars.removeAllViews()
        val points = data.monthlyGrowth
        if (points.isEmpty()) return
        val maxCount = (points.maxOfOrNull { it.count } ?: 1L).coerceAtLeast(1L)
        val dp = resources.displayMetrics.density
        val maxBarPx = (82 * dp).toInt()
        val mint = ContextCompat.getColor(requireContext(), R.color.rc_tint)
        val brand = ContextCompat.getColor(requireContext(), R.color.rc_brand)
        val ink = ContextCompat.getColor(requireContext(), R.color.rc_ink)
        val muted = ContextCompat.getColor(requireContext(), R.color.rc_muted)

        points.forEachIndexed { idx, pt ->
            val col = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
            }
            val tvVal = TextView(requireContext()).apply {
                text = pt.count.toString()
                textSize = 10f
                setTextColor(ink)
            }
            val barH = ((pt.count.toDouble() / maxCount.toDouble()) * maxBarPx).toInt().coerceAtLeast((10 * dp).toInt())
            val bar = View(requireContext()).apply {
                setBackgroundColor(if (idx == points.lastIndex) brand else mint)
                layoutParams = LinearLayout.LayoutParams((24 * dp).toInt(), barH).apply {
                    topMargin = (4 * dp).toInt()
                    bottomMargin = (4 * dp).toInt()
                }
            }
            val tvLbl = TextView(requireContext()).apply {
                text = pt.label
                textSize = 11f
                setTextColor(if (idx == points.lastIndex) brand else muted)
            }
            col.addView(tvVal)
            col.addView(bar)
            col.addView(tvLbl)
            binding.llChartBars.addView(col)
        }
    }

    private fun renderActivities(data: AdminDashboardResponse) {
        binding.llRecentActivities.removeAllViews()
        val dp = resources.displayMetrics.density
        if (data.recentActivities.isEmpty()) {
            binding.llRecentActivities.addView(TextView(requireContext()).apply {
                text = "Chưa có hoạt động gần đây"
                textSize = 13f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.rc_muted))
            })
            return
        }
        data.recentActivities.forEach { act ->
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, (6 * dp).toInt(), 0, (6 * dp).toInt())
                setOnClickListener {
                    if (act.type == "REPORT" && act.refId != null) {
                        findNavController().navigate(R.id.adminReportDetailFragment, bundleOf("reportId" to act.refId))
                    } else if (act.type == "ACCOUNT" && act.refId != null) {
                        findNavController().navigate(R.id.adminAccountDetailFragment, bundleOf("accountId" to act.refId))
                    }
                }
            }
            row.addView(TextView(requireContext()).apply {
                text = "${act.timeLabel} · ${act.title}"
                textSize = 13f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.rc_ink))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            })
            row.addView(TextView(requireContext()).apply {
                text = act.subtitle
                textSize = 12f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.rc_muted))
            })
            val divider = View(requireContext()).apply {
                setBackgroundColor(Color.parseColor("#DDE5E2"))
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1).apply {
                    topMargin = (6 * dp).toInt()
                }
            }
            binding.llRecentActivities.addView(row)
            binding.llRecentActivities.addView(divider)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
