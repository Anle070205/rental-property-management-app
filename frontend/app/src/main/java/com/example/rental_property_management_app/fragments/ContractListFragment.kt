package com.example.rental_property_management_app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.adapters.ContractAdapter
import com.example.rental_property_management_app.databinding.DialogCreateContractBinding
import com.example.rental_property_management_app.databinding.FragmentContractListBinding
import com.example.rental_property_management_app.models.*
import com.example.rental_property_management_app.utils.CurrencyUtils
import com.example.rental_property_management_app.utils.Resource
import com.example.rental_property_management_app.viewmodels.ContractViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ContractListFragment : Fragment() {

    private var _binding: FragmentContractListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ContractViewModel by viewModels()
    private lateinit var adapter: ContractAdapter

    private var roomsList: List<RoomResponse> = emptyList()
    private var tenantsList: List<TenantResponse> = emptyList()
    private var preselectRoomId: Long? = null
    private var preselectTenantId: Long? = null

    private val isoDateRegex = Regex("^\\d{4}-\\d{2}-\\d{2}$")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentContractListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        preselectRoomId = arguments?.getLong("roomId", 0L)?.takeIf { it > 0L }
        preselectTenantId = arguments?.getLong("tenantId", 0L)?.takeIf { it > 0L }

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        adapter = ContractAdapter { contract ->
            findNavController().navigate(
                R.id.contractDetailFragment,
                bundleOf("contractId" to contract.id)
            )
        }
        binding.rvContracts.adapter = adapter

        binding.chipGroupStatus.setOnCheckedStateChangeListener { _, checkedIds ->
            val status = when (checkedIds.firstOrNull()) {
                R.id.chipDraft -> ContractStatus.DRAFT
                R.id.chipSigned -> ContractStatus.SIGNED
                R.id.chipActive -> ContractStatus.ACTIVE
                R.id.chipTerminated -> ContractStatus.TERMINATED
                else -> null
            }
            viewModel.loadContracts(
                roomId = preselectRoomId,
                tenantId = preselectTenantId,
                status = status
            )
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadContracts(roomId = preselectRoomId, tenantId = preselectTenantId)
        }
        binding.btnRetry.setOnClickListener {
            viewModel.loadContracts(roomId = preselectRoomId, tenantId = preselectTenantId)
        }
        binding.fabCreateContract.setOnClickListener {
            if (roomsList.isEmpty() || tenantsList.isEmpty()) {
                Snackbar.make(binding.root, "Cần có ít nhất 1 phòng và 1 khách thuê trước khi tạo hợp đồng", Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }
            showCreateContractDialog()
        }

        observeStates()
        viewModel.loadReferenceData()
        viewModel.loadContracts(roomId = preselectRoomId, tenantId = preselectTenantId)
    }

    private fun observeStates() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.roomsState.collectLatest { roomsList = it }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.tenantsState.collectLatest { tenantsList = it }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.contractsState.collectLatest { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.layoutEmpty.visibility = View.GONE
                        binding.layoutError.visibility = View.GONE
                    }
                    is Resource.Success -> {
                        binding.swipeRefresh.isRefreshing = false
                        binding.progressBar.visibility = View.GONE
                        binding.layoutError.visibility = View.GONE
                        val list = resource.data ?: emptyList()
                        if (list.isEmpty()) {
                            binding.rvContracts.visibility = View.GONE
                            binding.layoutEmpty.visibility = View.VISIBLE
                        } else {
                            binding.layoutEmpty.visibility = View.GONE
                            binding.rvContracts.visibility = View.VISIBLE
                            adapter.submitList(list)
                        }
                    }
                    is Resource.Error -> {
                        binding.swipeRefresh.isRefreshing = false
                        binding.progressBar.visibility = View.GONE
                        binding.rvContracts.visibility = View.GONE
                        binding.layoutError.visibility = View.VISIBLE
                        binding.tvError.text = resource.message ?: getString(R.string.error_loading)
                    }
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.actionState.collectLatest { resource ->
                resource ?: return@collectLatest
                when (resource) {
                    is Resource.Loading -> Unit
                    is Resource.Success -> {
                        val created = resource.data
                        Snackbar.make(
                            binding.root,
                            "Đã tạo hợp đồng nháp ${created?.contractCode ?: ""}",
                            Snackbar.LENGTH_SHORT
                        ).show()
                        viewModel.resetActionState()
                        if (created != null) {
                            findNavController().navigate(
                                R.id.contractDetailFragment,
                                bundleOf("contractId" to created.id)
                            )
                        }
                    }
                    is Resource.Error -> {
                        AlertDialog.Builder(requireContext())
                            .setTitle("Không thể tạo hợp đồng")
                            .setMessage(resource.message ?: "Dữ liệu không hợp lệ")
                            .setPositiveButton("Đóng", null)
                            .show()
                        viewModel.resetActionState()
                    }
                }
            }
        }
    }

    private fun showCreateContractDialog() {
        val db = DialogCreateContractBinding.inflate(layoutInflater)
        val roomLabels = roomsList.map { "P.${it.roomCode} - ${it.propertyName} (${CurrencyUtils.formatVND(it.rentPrice)})" }
        db.spinnerRoom.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, roomLabels).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        preselectRoomId?.let { id ->
            val idx = roomsList.indexOfFirst { it.id == id }
            if (idx >= 0) db.spinnerRoom.setSelection(idx)
        }

        val tenantLabels = tenantsList.map { "${it.fullName} - ${it.phone ?: it.email ?: "Chưa có LH"}" }
        db.spinnerTenant.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, tenantLabels).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        preselectTenantId?.let { id ->
            val idx = tenantsList.indexOfFirst { it.id == id }
            if (idx >= 0) db.spinnerTenant.setSelection(idx)
        }

        val modes = WaterBillingMode.values()
        db.spinnerWaterMode.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            modes.map { it.getDisplayName() }
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val startCal = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }
        val endCal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.YEAR, 1)
            add(Calendar.DAY_OF_MONTH, -1)
        }
        db.etStartDate.setText(fmt.format(startCal.time))
        db.etEndDate.setText(fmt.format(endCal.time))

        db.spinnerRoom.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val room = roomsList.getOrNull(position) ?: return
                db.etMonthlyRent.setText(room.rentPrice.stripTrailingZeros().toPlainString())
                db.etDeposit.setText(room.deposit.stripTrailingZeros().toPlainString())
                room.electricityPrice?.let { db.etElecPrice.setText(it.stripTrailingZeros().toPlainString()) }
                room.waterPrice?.let { db.etWaterPrice.setText(it.stripTrailingZeros().toPlainString()) }
                room.internetFee?.let { db.etInternetFee.setText(it.stripTrailingZeros().toPlainString()) }
                room.serviceFee?.let { db.etServiceFee.setText(it.stripTrailingZeros().toPlainString()) }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Tạo hợp đồng thuê (Nháp)")
            .setView(db.root)
            .setPositiveButton("Tạo nháp", null)
            .setNegativeButton(getString(R.string.cancel), null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val startDate = db.etStartDate.text?.toString()?.trim().orEmpty()
                val endDate = db.etEndDate.text?.toString()?.trim().orEmpty()
                if (!isoDateRegex.matches(startDate)) {
                    db.tilStartDate.error = "YYYY-MM-DD"
                    return@setOnClickListener
                }
                db.tilStartDate.error = null
                if (!isoDateRegex.matches(endDate)) {
                    db.tilEndDate.error = "YYYY-MM-DD"
                    return@setOnClickListener
                }
                db.tilEndDate.error = null

                val room = roomsList.getOrNull(db.spinnerRoom.selectedItemPosition) ?: return@setOnClickListener
                val tenant = tenantsList.getOrNull(db.spinnerTenant.selectedItemPosition) ?: return@setOnClickListener
                val rent = db.etMonthlyRent.text?.toString()?.trim()?.toBigDecimalOrNull() ?: room.rentPrice
                val deposit = db.etDeposit.text?.toString()?.trim()?.toBigDecimalOrNull() ?: BigDecimal.ZERO
                val elec = db.etElecPrice.text?.toString()?.trim()?.toBigDecimalOrNull()
                val water = db.etWaterPrice.text?.toString()?.trim()?.toBigDecimalOrNull()
                val mode = modes.getOrElse(db.spinnerWaterMode.selectedItemPosition) { WaterBillingMode.METERED }
                val internet = db.etInternetFee.text?.toString()?.trim()?.toBigDecimalOrNull() ?: BigDecimal.ZERO
                val garbage = db.etGarbageFee.text?.toString()?.trim()?.toBigDecimalOrNull() ?: BigDecimal.ZERO
                val service = db.etServiceFee.text?.toString()?.trim()?.toBigDecimalOrNull() ?: BigDecimal.ZERO
                val payDay = db.etPaymentDay.text?.toString()?.trim()?.toIntOrNull()?.coerceIn(1, 28) ?: 5
                val terms = db.etTerms.text?.toString()?.trim()?.ifEmpty { null }

                viewModel.createContract(
                    CreateContractRequest(
                        roomId = room.id,
                        tenantId = tenant.id,
                        startDate = startDate,
                        endDate = endDate,
                        monthlyRent = rent,
                        depositAmount = deposit,
                        electricityUnitPrice = elec,
                        waterUnitPrice = water,
                        waterBillingMode = mode,
                        internetFee = internet,
                        garbageFee = garbage,
                        serviceFee = service,
                        paymentDayOfMonth = payDay,
                        terms = terms
                    )
                )
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
