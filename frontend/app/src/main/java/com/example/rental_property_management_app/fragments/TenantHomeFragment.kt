package com.example.rental_property_management_app.fragments

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.databinding.FragmentTenantHomeBinding
import com.example.rental_property_management_app.utils.Constants
import com.example.rental_property_management_app.utils.CurrencyUtils
import com.example.rental_property_management_app.utils.DateUtils
import com.example.rental_property_management_app.utils.Resource
import com.example.rental_property_management_app.viewmodels.TenantHomeViewModel
import kotlinx.coroutines.launch

class TenantHomeFragment : Fragment(R.layout.fragment_tenant_home) {
    private var _binding: FragmentTenantHomeBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val viewModel: TenantHomeViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentTenantHomeBinding.bind(view)
        val nav = findNavController()
        binding.room.setOnClickListener { nav.navigate(R.id.action_tenantHome_to_room) }
        binding.btnBrowseProperties.visibility = View.GONE
        binding.btnMyBookings.visibility = View.GONE
        binding.contract.setOnClickListener { nav.navigate(R.id.action_tenantHome_to_contract) }
        binding.invoices.setOnClickListener { nav.navigate(R.id.action_tenantHome_to_invoices) }
        binding.profile.setOnClickListener { nav.navigate(R.id.profileFragment) }
        binding.notificationBell.setOnClickListener { nav.navigate(R.id.notificationFragment) }
        binding.notifications.setOnClickListener { nav.navigate(R.id.notificationFragment) }
        binding.incidents.setOnClickListener { nav.navigate(R.id.incidentListFragment) }
        binding.btnGuestStays.setOnClickListener { nav.navigate(R.id.guestStayListFragment) }
        binding.retry.setOnClickListener { viewModel.refresh() }
        binding.sample.isVisible = Constants.USE_MOCK_TENANT_DATA
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.dashboard.collect { result ->
                binding.progress.isVisible = result is Resource.Loading
                binding.error.isVisible = result is Resource.Error
                binding.error.text = result.message
                binding.retry.isVisible = result is Resource.Error
                result.data?.let {
                    binding.greeting.text = it.fullName
                    binding.avatar.text = profileInitials(it.fullName)
                    binding.roomCode.text = it.roomCode ?: getString(R.string.rc_no_active_room)
                    val hasRoom = !it.roomCode.isNullOrBlank()
                    binding.roomStatusBadge.text = getString(
                        if (hasRoom) R.string.status_occupied else R.string.rc_no_room_badge
                    )
                    binding.roomStatusBadge.setBackgroundResource(
                        if (hasRoom) R.drawable.rc_badge_success else R.drawable.rc_badge
                    )
                    binding.roomStatusBadge.setTextColor(
                        ContextCompat.getColor(requireContext(), if (hasRoom) R.color.success else R.color.rc_brand)
                    )
                    binding.amount.text = it.amountDue?.let(CurrencyUtils::formatVND)
                        ?: getString(R.string.rc_no_amount_due)
                    binding.dueDate.isVisible = it.dueDate != null
                    binding.dueDate.text = it.dueDate?.let { value ->
                        getString(R.string.rc_tenant_due_date, DateUtils.parseApiDate(value)?.let(DateUtils::formatDate) ?: value)
                    }
                    binding.notifications.text = getString(R.string.rc_notifications_with_count, it.unreadCount)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
