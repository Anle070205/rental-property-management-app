package com.example.rental_property_management_app.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.adapters.DebtAdapter
import com.example.rental_property_management_app.databinding.FragmentDebtBinding
import com.example.rental_property_management_app.models.RoomResponse
import com.example.rental_property_management_app.models.RoomTenantDebtResponse
import com.example.rental_property_management_app.utils.CurrencyUtils
import com.example.rental_property_management_app.utils.Resource
import com.example.rental_property_management_app.viewmodels.DebtViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DebtFragment : Fragment() {

    private var _binding: FragmentDebtBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DebtViewModel by viewModels()
    private lateinit var adapter: DebtAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDebtBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        setupFilters()
        observeStates()

        viewModel.loadRooms()
        viewModel.loadDebts()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        adapter = DebtAdapter(
            onViewInvoiceClick = { debtItem -> openInvoicePickerOrDetail(debtItem) },
            onViewHistoryClick = { debtItem -> showDebtHistory(debtItem) },
            onContactClick = { debtItem -> showContactOptions(debtItem) }
        )
        binding.rvDebts.adapter = adapter
    }

    private fun setupFilters() {
        binding.etSearch.addTextChangedListener { editable ->
            val kw = editable?.toString()?.trim()
            if (kw != viewModel.currentKeyword) {
                viewModel.loadDebts(keyword = kw)
            }
        }

        binding.btnMonthFilter.setOnClickListener {
            showMonthPicker()
        }

        binding.chipGroupOverdue.setOnCheckedStateChangeListener { _, checkedIds ->
            val level = when (checkedIds.firstOrNull()) {
                R.id.chipNotDue -> "NOT_DUE"
                R.id.chipOverdue1To7 -> "OVERDUE_1_7"
                R.id.chipOverdue8To30 -> "OVERDUE_8_30"
                R.id.chipOverdue30Plus -> "OVERDUE_30_PLUS"
                else -> null
            }
            viewModel.loadDebts(overdueLevel = level)
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadDebts()
        }

        binding.btnRetry.setOnClickListener {
            viewModel.loadDebts()
        }
    }

    private fun observeStates() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.roomsState.collectLatest { rooms ->
                setupRoomSpinner(rooms)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.debtsState.collectLatest { resource ->
                when (resource) {
                    is Resource.Loading -> showLoading()
                    is Resource.Success -> {
                        binding.swipeRefresh.isRefreshing = false
                        hideAllStates()
                        val summary = resource.data
                        if (summary != null) {
                            binding.tvTotalDebtAmount.text = CurrencyUtils.formatVND(summary.totalDebtAmount)
                            binding.tvOverdueCount.text = "${summary.totalOverdueInvoices} hóa đơn quá hạn"
                            binding.tvRoomsWithDebt.text = "${summary.roomsWithDebtCount} phòng đang còn nợ"

                            if (summary.items.isEmpty()) {
                                binding.tvEmpty.visibility = View.VISIBLE
                                binding.rvDebts.visibility = View.GONE
                            } else {
                                binding.tvEmpty.visibility = View.GONE
                                binding.rvDebts.visibility = View.VISIBLE
                                adapter.submitList(summary.items)
                            }
                        }
                    }
                    is Resource.Error -> {
                        binding.swipeRefresh.isRefreshing = false
                        hideAllStates()
                        binding.layoutError.visibility = View.VISIBLE
                        binding.tvError.text = resource.message ?: getString(R.string.error_loading)
                        binding.rvDebts.visibility = View.GONE
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.tenantDebtDetailState.collectLatest { resource ->
                resource ?: return@collectLatest
                when (resource) {
                    is Resource.Loading -> Unit
                    is Resource.Success -> {
                        val detail = resource.data
                        if (detail != null) {
                            presentHistoryDialog(detail)
                        }
                        viewModel.resetTenantDebtDetailState()
                    }
                    is Resource.Error -> {
                        Snackbar.make(binding.root, resource.message ?: "Không tải được lịch sử công nợ", Snackbar.LENGTH_SHORT).show()
                        viewModel.resetTenantDebtDetailState()
                    }
                }
            }
        }
    }

    private fun setupRoomSpinner(rooms: List<RoomResponse>) {
        val labels = mutableListOf(getString(R.string.invoice_all_rooms))
        labels.addAll(rooms.map { "P.${it.roomCode} (${it.propertyName})" })
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, labels)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerRoomFilter.adapter = spinnerAdapter

        binding.spinnerRoomFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val roomId = if (position == 0) null else rooms.getOrNull(position - 1)?.id
                if (roomId != viewModel.currentRoomId) {
                    viewModel.loadDebts(roomId = roomId)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun showMonthPicker() {
        val options = mutableListOf(getString(R.string.invoice_all_months))
        val cal = Calendar.getInstance()
        val fmt = SimpleDateFormat("yyyy-MM", Locale.US)
        cal.add(Calendar.MONTH, 1)
        for (i in 0 until 12) {
            options.add(fmt.format(cal.time))
            cal.add(Calendar.MONTH, -1)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Lọc theo kỳ hóa đơn")
            .setItems(options.toTypedArray()) { _, which ->
                val month = if (which == 0) null else options[which]
                binding.btnMonthFilter.text = month ?: getString(R.string.invoice_all_months)
                viewModel.loadDebts(month = month)
            }
            .show()
    }

    private fun openInvoicePickerOrDetail(item: RoomTenantDebtResponse) {
        val invoices = item.unpaidInvoices
        if (invoices.isEmpty()) {
            item.latestInvoiceId?.let { navigateToInvoiceDetail(it) }
            return
        }
        if (invoices.size == 1) {
            navigateToInvoiceDetail(invoices[0].id)
            return
        }

        val labels = invoices.map {
            "Kỳ ${it.billingMonth} (${it.invoiceCode}) - Còn ${CurrencyUtils.formatVND(it.remainingAmount)}"
        }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Chọn hóa đơn Phòng ${item.roomCode ?: ""}")
            .setItems(labels) { _, which ->
                navigateToInvoiceDetail(invoices[which].id)
            }
            .show()
    }

    private fun navigateToInvoiceDetail(invoiceId: Long) {
        val bundle = Bundle().apply { putLong("invoiceId", invoiceId) }
        findNavController().navigate(R.id.invoiceDetailFragment, bundle)
    }

    private fun showDebtHistory(item: RoomTenantDebtResponse) {
        if (item.tenantId != null) {
            viewModel.loadTenantDebtDetail(item.tenantId)
        } else {
            presentHistoryDialog(item)
        }
    }

    private fun presentHistoryDialog(item: RoomTenantDebtResponse) {
        val message = if (item.debtHistory.isEmpty()) {
            "Chưa có biến động công nợ"
        } else {
            item.debtHistory.joinToString("\n\n") { h ->
                val sign = if (h.amount.signum() >= 0) "+" else ""
                "[${h.occurredDate}] ${h.description}\nBiến động: $sign${CurrencyUtils.formatVND(h.amount)} • Dư nợ sau GD: ${CurrencyUtils.formatVND(h.balanceAfter)}"
            }
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Lịch sử công nợ - ${item.tenantName ?: "P.${item.roomCode ?: ""}"}")
            .setMessage(message)
            .setPositiveButton("Đóng", null)
            .show()
    }

    private fun showContactOptions(item: RoomTenantDebtResponse) {
        val phone = item.tenantPhone
        if (phone.isNullOrBlank() && item.tenantId == null) {
            Snackbar.make(binding.root, "Chưa có thông tin người thuê cho phòng này", Snackbar.LENGTH_SHORT).show()
            return
        }

        val builder = AlertDialog.Builder(requireContext())
            .setTitle(item.tenantName ?: "Thông tin liên hệ")
            .setMessage("Phòng: ${item.roomCode ?: "-"}\nSĐT: ${phone ?: "Chưa cập nhật"}\nTổng nợ: ${CurrencyUtils.formatVND(item.totalDebt)}")

        if (!phone.isNullOrBlank()) {
            builder.setPositiveButton("Gọi điện") { _, _ ->
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                startActivity(intent)
            }
        }

        if (item.tenantId != null) {
            builder.setNeutralButton("Hồ sơ người thuê") { _, _ ->
                val bundle = Bundle().apply { putLong("tenantId", item.tenantId) }
                findNavController().navigate(R.id.tenantDetailFragment, bundle)
            }
        }

        builder.setNegativeButton(getString(R.string.cancel), null)
        builder.show()
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.rvDebts.visibility = View.GONE
        binding.tvEmpty.visibility = View.GONE
        binding.layoutError.visibility = View.GONE
    }

    private fun hideAllStates() {
        binding.progressBar.visibility = View.GONE
        binding.tvEmpty.visibility = View.GONE
        binding.layoutError.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
