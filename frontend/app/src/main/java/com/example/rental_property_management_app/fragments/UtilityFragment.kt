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
import com.example.rental_property_management_app.adapters.UtilityRecordAdapter
import com.example.rental_property_management_app.databinding.FragmentUtilityBinding
import com.example.rental_property_management_app.models.*
import com.example.rental_property_management_app.utils.CurrencyUtils
import com.example.rental_property_management_app.utils.Resource
import com.example.rental_property_management_app.viewmodels.UtilityViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class UtilityFragment : Fragment() {

    private var _binding: FragmentUtilityBinding? = null
    private val binding get() = _binding!!

    private val viewModel: UtilityViewModel by viewModels()
    private lateinit var adapter: UtilityRecordAdapter

    private var roomsList: List<RoomResponse> = emptyList()
    private var editingRecordId: Long? = null
    private var suppressPrevIndexAutoFill = false
    private val monthRegex = Regex("^\\d{4}-(0[1-9]|1[0-2])$")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUtilityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        setupFormWatchers()
        setupListeners()
        observeStates()

        val defaultMonth = SimpleDateFormat("yyyy-MM", Locale.US).format(Calendar.getInstance().time)
        binding.etBillingMonth.setText(defaultMonth)

        viewModel.loadRooms()
        viewModel.loadRecords()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        adapter = UtilityRecordAdapter { record ->
            enterEditMode(record)
        }
        binding.rvUtilityRecords.adapter = adapter
    }

    private fun setupFormWatchers() {
        listOf(
            binding.etPrevElectric,
            binding.etCurrElectric,
            binding.etElectricPrice,
            binding.etPrevWater,
            binding.etCurrWater,
            binding.etWaterPrice,
            binding.etInternetFee,
            binding.etServiceFee
        ).forEach { et ->
            et.addTextChangedListener { recalculatePreview() }
        }

        binding.switchAdjustment.setOnCheckedChangeListener { _, isChecked ->
            binding.tilAdjustmentReason.visibility = if (isChecked) View.VISIBLE else View.GONE
            recalculatePreview()
        }
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadRecords()
        }

        binding.btnCancelEdit.setOnClickListener {
            exitEditMode()
        }

        binding.btnSaveUtility.setOnClickListener {
            submitUtilityForm()
        }
    }

    private fun observeStates() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.roomsState.collectLatest { rooms ->
                roomsList = rooms
                setupRoomSpinner(rooms)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.previousIndexState.collectLatest { prev ->
                if (prev == null || editingRecordId != null || suppressPrevIndexAutoFill) return@collectLatest
                binding.etPrevElectric.setText(prev.suggestedPrevElectricIndex.stripTrailingZeros().toPlainString())
                binding.etPrevWater.setText(prev.suggestedPrevWaterIndex.stripTrailingZeros().toPlainString())
                binding.etElectricPrice.setText(prev.lastElectricUnitPrice.stripTrailingZeros().toPlainString())
                binding.etWaterPrice.setText(prev.lastWaterUnitPrice.stripTrailingZeros().toPlainString())
                binding.etInternetFee.setText(prev.lastInternetFee.stripTrailingZeros().toPlainString())
                binding.etServiceFee.setText(prev.lastServiceFee.stripTrailingZeros().toPlainString())
                recalculatePreview()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.recordsState.collectLatest { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                    is Resource.Success -> {
                        binding.swipeRefresh.isRefreshing = false
                        binding.progressBar.visibility = View.GONE
                        val list = resource.data ?: emptyList()
                        if (list.isEmpty()) {
                            binding.tvEmptyRecords.visibility = View.VISIBLE
                            binding.rvUtilityRecords.visibility = View.GONE
                        } else {
                            binding.tvEmptyRecords.visibility = View.GONE
                            binding.rvUtilityRecords.visibility = View.VISIBLE
                            adapter.submitList(list)
                        }
                    }
                    is Resource.Error -> {
                        binding.swipeRefresh.isRefreshing = false
                        binding.progressBar.visibility = View.GONE
                        Snackbar.make(binding.root, resource.message ?: "Lỗi tải dữ liệu", Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.saveState.collectLatest { resource ->
                resource ?: return@collectLatest
                when (resource) {
                    is Resource.Loading -> {
                        binding.btnSaveUtility.isEnabled = false
                    }
                    is Resource.Success -> {
                        binding.btnSaveUtility.isEnabled = true
                        val wasEditing = editingRecordId != null
                        exitEditMode()
                        Snackbar.make(
                            binding.root,
                            if (wasEditing) "Đã cập nhật chỉ số và đồng bộ hóa đơn" else "Đã lưu chỉ số điện nước thành công",
                            Snackbar.LENGTH_LONG
                        ).show()
                        viewModel.resetSaveState()
                        viewModel.loadRecords()
                    }
                    is Resource.Error -> {
                        binding.btnSaveUtility.isEnabled = true
                        AlertDialog.Builder(requireContext())
                            .setTitle("Không thể lưu bản ghi")
                            .setMessage(resource.message ?: "Dữ liệu không hợp lệ")
                            .setPositiveButton("Đóng", null)
                            .show()
                        viewModel.resetSaveState()
                    }
                }
            }
        }
    }

    private fun setupRoomSpinner(rooms: List<RoomResponse>) {
        val labels = rooms.map { "Phòng ${it.roomCode} - ${it.propertyName}" }
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, labels)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerRoom.adapter = spinnerAdapter

        binding.spinnerRoom.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (editingRecordId != null) return
                val room = rooms.getOrNull(position) ?: return
                val month = binding.etBillingMonth.text?.toString()?.trim()
                viewModel.loadPreviousIndex(room.id, month)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun enterEditMode(record: UtilityRecordResponse) {
        editingRecordId = record.id
        suppressPrevIndexAutoFill = true

        binding.tvFormHeader.text = "Chỉnh sửa điện nước P.${record.roomCode} (${record.billingMonth})"
        binding.btnCancelEdit.visibility = View.VISIBLE
        binding.btnSaveUtility.text = getString(R.string.utility_update)

        val roomIdx = roomsList.indexOfFirst { it.id == record.roomId }
        if (roomIdx >= 0) {
            binding.spinnerRoom.setSelection(roomIdx)
        }
        binding.spinnerRoom.isEnabled = false
        binding.etBillingMonth.setText(record.billingMonth)
        binding.etBillingMonth.isEnabled = false

        binding.etPrevElectric.setText(record.prevElectricIndex.stripTrailingZeros().toPlainString())
        binding.etCurrElectric.setText(record.currElectricIndex.stripTrailingZeros().toPlainString())
        binding.etElectricPrice.setText(record.electricUnitPrice.stripTrailingZeros().toPlainString())

        binding.etPrevWater.setText(record.prevWaterIndex.stripTrailingZeros().toPlainString())
        binding.etCurrWater.setText(record.currWaterIndex.stripTrailingZeros().toPlainString())
        binding.etWaterPrice.setText(record.waterUnitPrice.stripTrailingZeros().toPlainString())

        binding.etInternetFee.setText(record.internetFee.stripTrailingZeros().toPlainString())
        binding.etServiceFee.setText(record.serviceFee.stripTrailingZeros().toPlainString())
        binding.switchAdjustment.isChecked = record.isAdjustment
        binding.etAdjustmentReason.setText(record.adjustmentReason ?: "")
        binding.etNotes.setText(record.notes ?: "")

        recalculatePreview()
        suppressPrevIndexAutoFill = false
    }

    private fun exitEditMode() {
        editingRecordId = null
        binding.tvFormHeader.text = "Ghi chỉ số điện, nước & dịch vụ"
        binding.btnCancelEdit.visibility = View.GONE
        binding.btnSaveUtility.text = getString(R.string.utility_save)
        binding.spinnerRoom.isEnabled = true
        binding.etBillingMonth.isEnabled = true
        binding.switchAdjustment.isChecked = false
        binding.etAdjustmentReason.setText("")
        binding.etNotes.setText("")

        val selectedPos = binding.spinnerRoom.selectedItemPosition
        val room = roomsList.getOrNull(selectedPos)
        if (room != null) {
            viewModel.loadPreviousIndex(room.id, binding.etBillingMonth.text?.toString()?.trim())
        }
    }

    private fun recalculatePreview() {
        val prevElec = parseDec(binding.etPrevElectric.text?.toString())
        val currElec = parseDec(binding.etCurrElectric.text?.toString())
        val elecPrice = parseDec(binding.etElectricPrice.text?.toString())

        val prevWater = parseDec(binding.etPrevWater.text?.toString())
        val currWater = parseDec(binding.etCurrWater.text?.toString())
        val waterPrice = parseDec(binding.etWaterPrice.text?.toString())

        val internetFee = parseDec(binding.etInternetFee.text?.toString())
        val serviceFee = parseDec(binding.etServiceFee.text?.toString())

        val isAdj = binding.switchAdjustment.isChecked
        val elecUsage = if (currElec >= prevElec) currElec - prevElec else if (isAdj) currElec else BigDecimal.ZERO
        val waterUsage = if (currWater >= prevWater) currWater - prevWater else if (isAdj) currWater else BigDecimal.ZERO

        val elecAmount = elecUsage.multiply(elecPrice).setScale(0, RoundingMode.HALF_UP)
        val waterAmount = waterUsage.multiply(waterPrice).setScale(0, RoundingMode.HALF_UP)
        val servicesTotal = internetFee.add(serviceFee)
        val grandTotal = elecAmount.add(waterAmount).add(servicesTotal)

        binding.tvCalcElectric.text = "Điện (${elecUsage.stripTrailingZeros().toPlainString()} kWh): ${CurrencyUtils.formatVND(elecAmount)}"
        binding.tvCalcWater.text = "Nước (${waterUsage.stripTrailingZeros().toPlainString()} m³): ${CurrencyUtils.formatVND(waterAmount)}"
        binding.tvCalcServices.text = "Internet + Dịch vụ: ${CurrencyUtils.formatVND(servicesTotal)}"
        binding.tvCalcTotal.text = CurrencyUtils.formatVND(grandTotal)
    }

    private fun submitUtilityForm() {
        val room = roomsList.getOrNull(binding.spinnerRoom.selectedItemPosition)
        if (room == null) {
            Snackbar.make(binding.root, "Vui lòng chọn phòng", Snackbar.LENGTH_SHORT).show()
            return
        }

        val billingMonth = binding.etBillingMonth.text?.toString()?.trim().orEmpty()
        if (!monthRegex.matches(billingMonth)) {
            binding.tilBillingMonth.error = "Định dạng YYYY-MM"
            return
        }
        binding.tilBillingMonth.error = null

        val prevElec = parseDec(binding.etPrevElectric.text?.toString())
        val currElec = parseDec(binding.etCurrElectric.text?.toString())
        val elecPrice = parseDec(binding.etElectricPrice.text?.toString())
        val prevWater = parseDec(binding.etPrevWater.text?.toString())
        val currWater = parseDec(binding.etCurrWater.text?.toString())
        val waterPrice = parseDec(binding.etWaterPrice.text?.toString())
        val internetFee = parseDec(binding.etInternetFee.text?.toString())
        val serviceFee = parseDec(binding.etServiceFee.text?.toString())
        val isAdjustment = binding.switchAdjustment.isChecked
        val adjustmentReason = binding.etAdjustmentReason.text?.toString()?.trim()?.ifEmpty { null }
        val notes = binding.etNotes.text?.toString()?.trim()?.ifEmpty { null }

        if ((currElec < prevElec || currWater < prevWater) && (!isAdjustment || adjustmentReason.isNullOrBlank())) {
            AlertDialog.Builder(requireContext())
                .setTitle("Chỉ số không hợp lệ")
                .setMessage("Chỉ số mới đang nhỏ hơn chỉ số cũ. Nếu thay/reset đồng hồ, hãy bật chế độ Điều chỉnh đồng hồ và nhập lý do.")
                .setPositiveButton("Đã hiểu", null)
                .show()
            return
        }

        val editId = editingRecordId
        if (editId != null) {
            viewModel.updateUtility(
                editId,
                UpdateUtilityRequest(
                    prevElectricIndex = prevElec,
                    currElectricIndex = currElec,
                    electricUnitPrice = elecPrice,
                    prevWaterIndex = prevWater,
                    currWaterIndex = currWater,
                    waterUnitPrice = waterPrice,
                    internetFee = internetFee,
                    serviceFee = serviceFee,
                    isAdjustment = isAdjustment,
                    adjustmentReason = adjustmentReason,
                    notes = notes
                )
            )
        } else {
            viewModel.createUtility(
                CreateUtilityRequest(
                    roomId = room.id,
                    billingMonth = billingMonth,
                    prevElectricIndex = prevElec,
                    currElectricIndex = currElec,
                    electricUnitPrice = elecPrice,
                    prevWaterIndex = prevWater,
                    currWaterIndex = currWater,
                    waterUnitPrice = waterPrice,
                    internetFee = internetFee,
                    serviceFee = serviceFee,
                    isAdjustment = isAdjustment,
                    adjustmentReason = adjustmentReason,
                    notes = notes
                )
            )
        }
    }

    private fun parseDec(text: String?): BigDecimal {
        val cleaned = text?.trim().orEmpty()
        if (cleaned.isEmpty()) return BigDecimal.ZERO
        return cleaned.toBigDecimalOrNull()?.max(BigDecimal.ZERO) ?: BigDecimal.ZERO
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
