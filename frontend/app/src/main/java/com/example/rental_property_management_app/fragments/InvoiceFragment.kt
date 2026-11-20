package com.example.rental_property_management_app.fragments

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
import com.example.rental_property_management_app.adapters.InvoiceAdapter
import com.example.rental_property_management_app.databinding.DialogBatchCreateInvoiceBinding
import com.example.rental_property_management_app.databinding.DialogCreateInvoiceBinding
import com.example.rental_property_management_app.databinding.FragmentInvoiceBinding
import com.example.rental_property_management_app.models.*
import com.example.rental_property_management_app.utils.CurrencyUtils
import com.example.rental_property_management_app.utils.Resource
import com.example.rental_property_management_app.viewmodels.InvoiceViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class InvoiceFragment : Fragment() {

    private var _binding: FragmentInvoiceBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InvoiceViewModel by viewModels()
    private lateinit var adapter: InvoiceAdapter

    private var roomsList: List<RoomResponse> = emptyList()
    private var propertiesList: List<PropertyResponse> = emptyList()
    private var tenantsList: List<TenantResponse> = emptyList()
    private var preselectRoomId: Long? = null
    private var preselectContractId: Long? = null
    private var activePreviewLabel: android.widget.TextView? = null
    private var activePreviewTotalLabel: android.widget.TextView? = null

    private val billingMonthRegex = Regex("^\\d{4}-(0[1-9]|1[0-2])$")
    private val isoDateRegex = Regex("^\\d{4}-\\d{2}-\\d{2}$")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInvoiceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        preselectRoomId = arguments?.getLong("roomId", 0L)?.takeIf { it > 0L }
        preselectContractId = arguments?.getLong("contractId", 0L)?.takeIf { it > 0L }

        setupToolbar()
        setupRecyclerView()
        setupFilters()
        setupClickListeners()
        observeStates()

        viewModel.loadReferenceData()
        viewModel.loadInvoices(roomId = preselectRoomId)
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        adapter = InvoiceAdapter { invoice ->
            onInvoiceSelected(invoice)
        }
        binding.rvInvoices.adapter = adapter
    }

    private fun onInvoiceSelected(invoice: InvoiceResponse) {
        val bundle = Bundle().apply { putLong("invoiceId", invoice.id) }
        val navController = findNavController()
        if (navController.graph.findNode(R.id.invoiceDetailFragment) != null) {
            navController.navigate(R.id.invoiceDetailFragment, bundle)
        } else {
            Snackbar.make(
                binding.root,
                "Hóa đơn ${invoice.invoiceCode} - ${CurrencyUtils.formatVND(invoice.totalAmount)}",
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    private fun setupFilters() {
        binding.btnMonthFilter.setOnClickListener {
            showMonthFilterDialog()
        }

        binding.chipGroupStatus.setOnCheckedStateChangeListener { _, checkedIds ->
            val status = when (checkedIds.firstOrNull()) {
                R.id.chipDraft -> InvoiceStatus.DRAFT
                R.id.chipUnpaid -> InvoiceStatus.UNPAID
                R.id.chipPartial -> InvoiceStatus.PARTIALLY_PAID
                R.id.chipOverdue -> InvoiceStatus.OVERDUE
                R.id.chipPaid -> InvoiceStatus.PAID
                R.id.chipCancelled -> InvoiceStatus.CANCELLED
                else -> null
            }
            viewModel.loadInvoices(status = status)
        }
    }

    private fun setupClickListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadInvoices()
        }

        binding.btnRetry.setOnClickListener {
            viewModel.loadInvoices()
        }

        binding.btnCreateSingle.setOnClickListener {
            if (roomsList.isEmpty()) {
                showMessage("Chưa có dữ liệu phòng để tạo hóa đơn")
                return@setOnClickListener
            }
            showCreateSingleInvoiceDialog()
        }

        binding.btnCreateBatch.setOnClickListener {
            if (roomsList.isEmpty()) {
                showMessage("Chưa có dữ liệu phòng để tạo hóa đơn hàng loạt")
                return@setOnClickListener
            }
            showBatchCreateDialog()
        }
    }

    private fun observeStates() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.roomsState.collectLatest { rooms ->
                roomsList = rooms
                setupRoomFilterSpinner(rooms)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.propertiesState.collectLatest { props ->
                propertiesList = props
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.tenantsState.collectLatest { tenants ->
                tenantsList = tenants
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.invoicesState.collectLatest { resource ->
                when (resource) {
                    is Resource.Loading -> showLoading()
                    is Resource.Success -> {
                        binding.swipeRefresh.isRefreshing = false
                        hideStates()
                        val list = resource.data ?: emptyList()
                        updateSummaryStrip(list)
                        if (list.isEmpty()) {
                            binding.layoutEmpty.visibility = View.VISIBLE
                            binding.rvInvoices.visibility = View.GONE
                        } else {
                            binding.layoutEmpty.visibility = View.GONE
                            binding.rvInvoices.visibility = View.VISIBLE
                            adapter.submitList(list)
                        }
                    }
                    is Resource.Error -> {
                        binding.swipeRefresh.isRefreshing = false
                        hideStates()
                        binding.layoutError.visibility = View.VISIBLE
                        binding.tvError.text = resource.message ?: getString(R.string.error_loading)
                        binding.rvInvoices.visibility = View.GONE
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.createInvoiceState.collectLatest { resource ->
                resource ?: return@collectLatest
                when (resource) {
                    is Resource.Loading -> Unit
                    is Resource.Success -> {
                        showMessage("Tạo hóa đơn ${resource.data?.invoiceCode ?: ""} thành công")
                        viewModel.resetCreateInvoiceState()
                        viewModel.loadInvoices()
                    }
                    is Resource.Error -> {
                        showErrorDialog("Không thể tạo hóa đơn", resource.message ?: "Dữ liệu không hợp lệ")
                        viewModel.resetCreateInvoiceState()
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.previewState.collectLatest { resource ->
                resource ?: return@collectLatest
                when (resource) {
                    is Resource.Loading -> Unit
                    is Resource.Success -> {
                        val p = resource.data
                        if (p != null) {
                            activePreviewTotalLabel?.text = CurrencyUtils.formatVND(p.totalAmount)
                            activePreviewLabel?.visibility = View.VISIBLE
                            activePreviewLabel?.text = buildString {
                                append("Dự tính kỳ ${p.billingMonth} (${p.contractCode ?: "Theo phòng"}):\n")
                                append("• Phòng: ${CurrencyUtils.formatVND(p.roomRent)}\n")
                                append("• Điện: ${CurrencyUtils.formatVND(p.electricityAmount)}\n")
                                append("• Nước (${p.waterBillingMode ?: "METERED"}): ${CurrencyUtils.formatVND(p.waterAmount)}\n")
                                append("• Internet: ${CurrencyUtils.formatVND(p.internetAmount)} | Rác: ${CurrencyUtils.formatVND(p.garbageFee)} | DV: ${CurrencyUtils.formatVND(p.serviceAmount)}\n")
                                if (p.previousDebt.signum() > 0) append("• Nợ kỳ trước: ${CurrencyUtils.formatVND(p.previousDebt)}\n")
                                append("⇒ TỔNG: ${CurrencyUtils.formatVND(p.totalAmount)}")
                            }
                        }
                        viewModel.resetPreviewState()
                    }
                    is Resource.Error -> {
                        activePreviewLabel?.visibility = View.VISIBLE
                        activePreviewLabel?.text = resource.message ?: "Không xem trước được hóa đơn"
                        viewModel.resetPreviewState()
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.batchCreateState.collectLatest { resource ->
                resource ?: return@collectLatest
                when (resource) {
                    is Resource.Loading -> Unit
                    is Resource.Success -> {
                        val result = resource.data
                        if (result != null) {
                            val details = buildString {
                                append("Đã tạo thành công: ${result.createdCount} hóa đơn.\n")
                                append("Bỏ qua: ${result.skippedCount} phòng.")
                                if (result.skippedReasons.isNotEmpty()) {
                                    append("\n\nChi tiết bỏ qua:\n• ")
                                    append(result.skippedReasons.joinToString("\n• "))
                                }
                            }
                            AlertDialog.Builder(requireContext())
                                .setTitle("Kết quả tạo hóa đơn hàng loạt")
                                .setMessage(details)
                                .setPositiveButton("Đóng", null)
                                .show()
                        }
                        viewModel.resetBatchCreateState()
                        viewModel.loadInvoices()
                    }
                    is Resource.Error -> {
                        showErrorDialog("Tạo hàng loạt thất bại", resource.message ?: "Có lỗi xảy ra")
                        viewModel.resetBatchCreateState()
                    }
                }
            }
        }
    }

    private fun setupRoomFilterSpinner(rooms: List<RoomResponse>) {
        val labels = mutableListOf(getString(R.string.invoice_all_rooms))
        labels.addAll(rooms.map { "P.${it.roomCode} (${it.propertyName})" })

        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, labels)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerRoomFilter.adapter = spinnerAdapter

        binding.spinnerRoomFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedRoomId = if (position == 0) null else rooms.getOrNull(position - 1)?.id
                if (selectedRoomId != viewModel.currentRoomIdFilter) {
                    viewModel.loadInvoices(roomId = selectedRoomId)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun showMonthFilterDialog() {
        val options = mutableListOf(getString(R.string.invoice_all_months))
        val cal = Calendar.getInstance()
        val monthFormat = SimpleDateFormat("yyyy-MM", Locale.US)
        cal.add(Calendar.MONTH, 1)
        for (i in 0 until 12) {
            options.add(monthFormat.format(cal.time))
            cal.add(Calendar.MONTH, -1)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Chọn kỳ hóa đơn")
            .setItems(options.toTypedArray()) { _, which ->
                val selectedMonth = if (which == 0) null else options[which]
                binding.btnMonthFilter.text = selectedMonth ?: getString(R.string.invoice_all_months)
                viewModel.loadInvoices(month = selectedMonth)
            }
            .show()
    }

    private fun updateSummaryStrip(invoices: List<InvoiceResponse>) {
        val total = invoices.fold(BigDecimal.ZERO) { acc, inv -> acc + inv.totalAmount }
        val paid = invoices.fold(BigDecimal.ZERO) { acc, inv -> acc + inv.paidAmount }
        val remaining = invoices.fold(BigDecimal.ZERO) { acc, inv -> acc + inv.remainingAmount }

        binding.tvSummaryTotal.text = CurrencyUtils.formatVND(total)
        binding.tvSummaryPaid.text = CurrencyUtils.formatVND(paid)
        binding.tvSummaryRemaining.text = CurrencyUtils.formatVND(remaining)
    }

    private fun showCreateSingleInvoiceDialog() {
        val dialogBinding = DialogCreateInvoiceBinding.inflate(layoutInflater)

        val roomLabels = roomsList.map { "Phòng ${it.roomCode} - ${it.propertyName} (${CurrencyUtils.formatVND(it.rentPrice)})" }
        val roomAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, roomLabels)
        roomAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dialogBinding.spinnerRoom.adapter = roomAdapter

        val tenantOptions = mutableListOf("Không chọn / tự lấy theo phòng")
        tenantOptions.addAll(tenantsList.map {
            val contact = it.phone?.takeIf { p -> p.isNotBlank() } ?: it.email?.takeIf { e -> e.isNotBlank() } ?: "Chưa có SĐT"
            "${it.fullName} - $contact"
        })
        val tenantAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, tenantOptions)
        tenantAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dialogBinding.spinnerTenant.adapter = tenantAdapter

        preselectRoomId?.let { targetId ->
            val idx = roomsList.indexOfFirst { it.id == targetId }
            if (idx >= 0) dialogBinding.spinnerRoom.setSelection(idx)
        }

        val defaultMonth = SimpleDateFormat("yyyy-MM", Locale.US).format(Calendar.getInstance().time)
        val dueCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 7) }
        val defaultDueDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(dueCal.time)
        dialogBinding.etBillingMonth.setText(defaultMonth)
        dialogBinding.etDueDate.setText(defaultDueDate)

        fun updatePreviewTotal() {
            val rent = parseBigDecimal(dialogBinding.etRoomRent.text?.toString())
            val elec = parseBigDecimal(dialogBinding.etElectricity.text?.toString())
            val water = parseBigDecimal(dialogBinding.etWater.text?.toString())
            val internet = parseBigDecimal(dialogBinding.etInternet.text?.toString())
            val garbage = parseBigDecimal(dialogBinding.etGarbageFee.text?.toString())
            val service = parseBigDecimal(dialogBinding.etService.text?.toString())
            val other = parseBigDecimal(dialogBinding.etOtherFees.text?.toString())
            val prevDebt = parseBigDecimal(dialogBinding.etPreviousDebt.text?.toString())
            val discount = parseBigDecimal(dialogBinding.etDiscount.text?.toString())
            val total = (rent + elec + water + internet + garbage + service + other + prevDebt - discount)
                .max(BigDecimal.ZERO)
            dialogBinding.tvPreviewTotal.text = CurrencyUtils.formatVND(total)
        }

        fun buildRequestOrShowError(): CreateInvoiceRequest? {
            val billingMonth = dialogBinding.etBillingMonth.text?.toString()?.trim().orEmpty()
            val dueDate = dialogBinding.etDueDate.text?.toString()?.trim().orEmpty()

            if (!billingMonthRegex.matches(billingMonth)) {
                dialogBinding.tilBillingMonth.error = "Kỳ hóa đơn phải theo định dạng YYYY-MM"
                return null
            }
            dialogBinding.tilBillingMonth.error = null

            if (!isoDateRegex.matches(dueDate)) {
                dialogBinding.tilDueDate.error = "Hạn thanh toán phải theo định dạng YYYY-MM-DD"
                return null
            }
            dialogBinding.tilDueDate.error = null

            val selectedRoom = roomsList.getOrNull(dialogBinding.spinnerRoom.selectedItemPosition)
            if (selectedRoom == null) {
                showMessage("Vui lòng chọn phòng")
                return null
            }

            val tenantPos = dialogBinding.spinnerTenant.selectedItemPosition
            val selectedTenantId = if (tenantPos > 0) tenantsList.getOrNull(tenantPos - 1)?.id else null

            val rent = parseBigDecimalOrNull(dialogBinding.etRoomRent.text?.toString())
            val elec = parseBigDecimalOrNull(dialogBinding.etElectricity.text?.toString())
            val water = parseBigDecimalOrNull(dialogBinding.etWater.text?.toString())
            val internet = parseBigDecimalOrNull(dialogBinding.etInternet.text?.toString())
            val garbage = parseBigDecimalOrNull(dialogBinding.etGarbageFee.text?.toString())
            val service = parseBigDecimalOrNull(dialogBinding.etService.text?.toString())
            val other = parseBigDecimal(dialogBinding.etOtherFees.text?.toString())
            val prevDebt = parseBigDecimalOrNull(dialogBinding.etPreviousDebt.text?.toString())
            val discount = parseBigDecimal(dialogBinding.etDiscount.text?.toString())

            if ((rent != null && rent < BigDecimal.ZERO) || (elec != null && elec < BigDecimal.ZERO) ||
                (water != null && water < BigDecimal.ZERO) || (internet != null && internet < BigDecimal.ZERO) ||
                (garbage != null && garbage < BigDecimal.ZERO) || (service != null && service < BigDecimal.ZERO) ||
                other < BigDecimal.ZERO || (prevDebt != null && prevDebt < BigDecimal.ZERO) ||
                discount < BigDecimal.ZERO
            ) {
                showErrorDialog("Dữ liệu không hợp lệ", "Các khoản tiền không được nhỏ hơn 0")
                return null
            }

            return CreateInvoiceRequest(
                roomId = selectedRoom.id,
                tenantId = selectedTenantId,
                contractId = if (selectedRoom.id == preselectRoomId) preselectContractId else null,
                billingMonth = billingMonth,
                dueDate = dueDate,
                roomRent = rent,
                electricityAmount = elec,
                waterAmount = water,
                internetAmount = internet,
                garbageFee = garbage,
                serviceAmount = service,
                otherFees = other,
                previousDebt = prevDebt,
                discountAmount = discount,
                useUtilityRecord = dialogBinding.cbUseUtilityRecord.isChecked,
                autoCarryPreviousDebt = dialogBinding.cbAutoCarryDebt.isChecked,
                saveAsDraft = dialogBinding.cbSaveAsDraft.isChecked,
                notes = dialogBinding.etNotes.text?.toString()?.trim()?.ifEmpty { null }
            )
        }

        dialogBinding.spinnerRoom.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val room = roomsList.getOrNull(position) ?: return
                dialogBinding.etRoomRent.setText(room.rentPrice.stripTrailingZeros().toPlainString())
                updatePreviewTotal()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        listOf(
            dialogBinding.etRoomRent,
            dialogBinding.etElectricity,
            dialogBinding.etWater,
            dialogBinding.etInternet,
            dialogBinding.etGarbageFee,
            dialogBinding.etService,
            dialogBinding.etOtherFees,
            dialogBinding.etPreviousDebt,
            dialogBinding.etDiscount
        ).forEach { editText ->
            editText.addTextChangedListener { updatePreviewTotal() }
        }

        activePreviewLabel = dialogBinding.tvPreviewBreakdown
        activePreviewTotalLabel = dialogBinding.tvPreviewTotal

        dialogBinding.btnPreviewInvoice.setOnClickListener {
            val req = buildRequestOrShowError() ?: return@setOnClickListener
            viewModel.previewInvoice(req)
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Tạo hóa đơn mới")
            .setView(dialogBinding.root)
            .setPositiveButton("Tạo hóa đơn", null)
            .setNegativeButton(getString(R.string.cancel), null)
            .create()

        dialog.setOnDismissListener {
            activePreviewLabel = null
            activePreviewTotalLabel = null
        }

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val request = buildRequestOrShowError() ?: return@setOnClickListener
                viewModel.createInvoice(request)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun showBatchCreateDialog() {
        val dialogBinding = DialogBatchCreateInvoiceBinding.inflate(layoutInflater)

        val propLabels = mutableListOf("Tất cả nhà trọ")
        propLabels.addAll(propertiesList.map { it.name })
        val propAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, propLabels)
        propAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dialogBinding.spinnerProperty.adapter = propAdapter

        val defaultMonth = SimpleDateFormat("yyyy-MM", Locale.US).format(Calendar.getInstance().time)
        val dueCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 7) }
        val defaultDueDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(dueCal.time)
        dialogBinding.etBatchMonth.setText(defaultMonth)
        dialogBinding.etBatchDueDate.setText(defaultDueDate)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Tạo hóa đơn hàng loạt")
            .setView(dialogBinding.root)
            .setPositiveButton("Tạo hàng loạt", null)
            .setNegativeButton(getString(R.string.cancel), null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val month = dialogBinding.etBatchMonth.text?.toString()?.trim().orEmpty()
                val dueDate = dialogBinding.etBatchDueDate.text?.toString()?.trim().orEmpty()

                if (!billingMonthRegex.matches(month)) {
                    dialogBinding.tilBatchMonth.error = "Kỳ hóa đơn phải theo định dạng YYYY-MM"
                    return@setOnClickListener
                }
                dialogBinding.tilBatchMonth.error = null

                if (!isoDateRegex.matches(dueDate)) {
                    dialogBinding.tilBatchDueDate.error = "Hạn thanh toán phải theo định dạng YYYY-MM-DD"
                    return@setOnClickListener
                }
                dialogBinding.tilBatchDueDate.error = null

                val propPos = dialogBinding.spinnerProperty.selectedItemPosition
                val propertyId = if (propPos > 0) propertiesList.getOrNull(propPos - 1)?.id else null

                val request = BatchCreateInvoiceRequest(
                    propertyId = propertyId,
                    billingMonth = month,
                    dueDate = dueDate,
                    defaultInternetFee = parseBigDecimal(dialogBinding.etDefaultInternet.text?.toString()),
                    defaultServiceFee = parseBigDecimal(dialogBinding.etDefaultService.text?.toString()),
                    otherFees = parseBigDecimal(dialogBinding.etBatchOtherFees.text?.toString()),
                    onlyOccupiedRooms = dialogBinding.cbOnlyOccupied.isChecked,
                    notes = dialogBinding.etBatchNotes.text?.toString()?.trim()?.ifEmpty { null }
                )

                viewModel.batchCreateInvoices(request)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun parseBigDecimal(value: String?): BigDecimal {
        val cleaned = value?.trim().orEmpty()
        if (cleaned.isEmpty()) return BigDecimal.ZERO
        return cleaned.toBigDecimalOrNull() ?: BigDecimal.ZERO
    }

    private fun parseBigDecimalOrNull(value: String?): BigDecimal? {
        val cleaned = value?.trim().orEmpty()
        if (cleaned.isEmpty()) return null
        return cleaned.toBigDecimalOrNull()
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.rvInvoices.visibility = View.GONE
        binding.layoutEmpty.visibility = View.GONE
        binding.layoutError.visibility = View.GONE
    }

    private fun hideStates() {
        binding.progressBar.visibility = View.GONE
        binding.layoutEmpty.visibility = View.GONE
        binding.layoutError.visibility = View.GONE
    }

    private fun showMessage(msg: String) {
        Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
    }

    private fun showErrorDialog(title: String, msg: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(msg)
            .setPositiveButton("Đóng", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
