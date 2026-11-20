package com.example.rental_property_management_app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.RoomCareApplication
import com.example.rental_property_management_app.databinding.FragmentLandlordHomeBinding
import com.example.rental_property_management_app.models.AttentionItemResponse
import com.example.rental_property_management_app.models.LandlordDashboardResponse
import com.example.rental_property_management_app.utils.CurrencyUtils
import com.example.rental_property_management_app.utils.Resource
import com.example.rental_property_management_app.viewmodels.AuthViewModel
import com.example.rental_property_management_app.viewmodels.DashboardViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LandlordHomeFragment : Fragment() {

    private var _binding: FragmentLandlordHomeBinding? = null
    private val binding get() = _binding!!

    private val dashboardViewModel: DashboardViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLandlordHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userName = RoomCareApplication.prefsManager.getUserName()
        if (!userName.isNullOrBlank()) {
            binding.tvWelcome.text = userName
            binding.tvAvatar.text = profileInitials(userName)
        } else {
            binding.tvAvatar.text = "CN"
        }

        setupNavigationListeners()
        observeStates()
    }

    override fun onResume() {
        super.onResume()
        val userName = RoomCareApplication.prefsManager.getUserName()
        if (!userName.isNullOrBlank()) {
            binding.tvWelcome.text = userName
            binding.tvAvatar.text = profileInitials(userName)
        }
        dashboardViewModel.loadDashboard()
    }

    private fun setupNavigationListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            dashboardViewModel.loadDashboard()
        }

        binding.btnRetry.setOnClickListener {
            dashboardViewModel.loadDashboard()
        }

        binding.btnNotifications.setOnClickListener {
            findNavController().navigate(R.id.notificationFragment)
        }
        binding.btnProfile.setOnClickListener {
            findNavController().navigate(R.id.profileFragment)
        }

        binding.btnLogout.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.rc_logout)
                .setMessage(R.string.rc_logout_confirm)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.rc_logout) { _, _ ->
                    authViewModel.logout()
                    val navOptions = NavOptions.Builder()
                        .setPopUpTo(R.id.nav_graph, true)
                        .build()
                    findNavController().navigate(R.id.loginFragment, null, navOptions)
                }
                .show()
        }

        binding.cardInvoices.setOnClickListener {
            findNavController().navigate(R.id.invoiceFragment)
        }
        binding.cardUtilities.setOnClickListener {
            findNavController().navigate(R.id.utilityFragment)
        }
        binding.cardDebts.setOnClickListener {
            findNavController().navigate(R.id.debtFragment)
        }
        binding.cardHealthScore.setOnClickListener {
            findNavController().navigate(R.id.healthScoreFragment)
        }
        binding.cardRoomManagement.setOnClickListener {
            findNavController().navigate(R.id.roomListFragment)
        }
        binding.cardTenantManagement.setOnClickListener {
            findNavController().navigate(R.id.tenantListFragment)
        }
        binding.cardPropertyManagement.setOnClickListener {
            findNavController().navigate(
                R.id.propertySelectionFragment,
                bundleOf("selectMode" to false)
            )
        }
        binding.cardBookingRequests.setOnClickListener {
            findNavController().navigate(R.id.landlordBookingListFragment)
        }
        binding.cardContracts.setOnClickListener {
            findNavController().navigate(R.id.contractListFragment)
        }
        binding.cardGuestStays.setOnClickListener {
            findNavController().navigate(R.id.guestStayListFragment)
        }
        binding.cardIncidents.setOnClickListener {
            findNavController().navigate(R.id.incidentListFragment)
        }
    }

    private fun observeStates() {
        viewLifecycleOwner.lifecycleScope.launch {
            dashboardViewModel.dashboardState.collectLatest { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        if (!binding.swipeRefresh.isRefreshing) {
                            binding.progressBar.visibility = View.VISIBLE
                        }
                        binding.layoutError.visibility = View.GONE
                    }
                    is Resource.Success -> {
                        binding.swipeRefresh.isRefreshing = false
                        binding.progressBar.visibility = View.GONE
                        binding.layoutError.visibility = View.GONE
                        binding.scrollContent.visibility = View.VISIBLE
                        resource.data?.let { bindDashboard(it) }
                    }
                    is Resource.Error -> {
                        binding.swipeRefresh.isRefreshing = false
                        binding.progressBar.visibility = View.GONE
                        binding.layoutError.visibility = View.VISIBLE
                        binding.tvError.text = resource.message ?: getString(R.string.error_loading)
                    }
                }
            }
        }
    }

    private fun bindDashboard(data: LandlordDashboardResponse) {
        binding.apply {
            tvTotalRooms.text = data.totalRooms.toString()
            tvOccupiedRooms.text = data.occupiedRooms.toString()
            tvVacantRooms.text = data.vacantRooms.toString()
            tvExpiringRooms.text = "Sắp hết hạn: ${data.expiringSoonRooms}"
            tvIssueRooms.text = "Cần chú ý: ${data.issueRooms}"
            tvAvgHealthScore.text = "Health: ${data.averageHealthScore}/100"
            tvBookingRequestsSubtitle.text = "${data.pendingBookingCount} yêu cầu chờ duyệt"

            tvTotalRevenue.text = CurrencyUtils.formatVND(data.totalRevenue)
            tvCollectedAmount.text = CurrencyUtils.formatVND(data.collectedAmount)
            tvUncollectedAmount.text = CurrencyUtils.formatVND(data.uncollectedAmount)
            tvCollectionRate.text = String.format("Tỷ lệ thu: %.1f%%", data.collectionRate)
            progressCollectionRate.progress = data.collectionRate.toInt().coerceIn(0, 100)

            chartMonthlyRevenue.setData(data.monthlyRevenues)

            bindAttentionSection(
                container = llExpiringContracts,
                emptyView = tvEmptyExpiring,
                items = data.expiringContracts,
                isExpiring = true
            )

            bindAttentionSection(
                container = llAttentionRooms,
                emptyView = tvEmptyAttention,
                items = data.attentionRooms,
                isExpiring = false
            )
        }
    }

    private fun bindAttentionSection(
        container: LinearLayout,
        emptyView: TextView,
        items: List<AttentionItemResponse>,
        isExpiring: Boolean
    ) {
        container.removeAllViews()
        if (items.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            container.visibility = View.GONE
            return
        }

        emptyView.visibility = View.GONE
        container.visibility = View.VISIBLE

        val ctx = requireContext()
        items.forEach { item ->
            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundResource(R.drawable.bg_badge_rounded)
                setPadding(28, 22, 28, 22)
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 14
                }
                layoutParams = lp
                setOnClickListener {
                    val bundle = Bundle().apply { putLong("roomId", item.roomId) }
                    findNavController().navigate(R.id.healthScoreFragment, bundle)
                }
            }

            val titleTv = TextView(ctx).apply {
                val tenantLabel = if (!item.tenantName.isNullOrBlank()) " • ${item.tenantName}" else ""
                text = "Phòng ${item.roomCode} (${item.propertyName})$tenantLabel"
                textSize = 14f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
            }

            val reasonTv = TextView(ctx).apply {
                val debtStr = if (item.unpaidDebt.signum() > 0) {
                    " • Nợ: ${CurrencyUtils.formatVND(item.unpaidDebt)}"
                } else {
                    ""
                }
                text = "${item.reason} • Health: ${item.healthScore}/100$debtStr"
                textSize = 12f
                setPadding(0, 6, 0, 0)
                setTextColor(
                    ContextCompat.getColor(
                        ctx,
                        if (isExpiring) R.color.status_expiring_soon else R.color.status_needs_attention
                    )
                )
            }

            row.addView(titleTv)
            row.addView(reasonTv)
            container.addView(row)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
