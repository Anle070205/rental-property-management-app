package com.example.rental_property_management_app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.databinding.FragmentRoomFormBinding
import com.example.rental_property_management_app.models.CreateRoomRequest
import com.example.rental_property_management_app.models.RoomResponse
import com.example.rental_property_management_app.models.RoomStatus
import com.example.rental_property_management_app.models.UpdateRoomRequest
import com.example.rental_property_management_app.viewmodels.PropertyViewModel
import com.example.rental_property_management_app.viewmodels.RoomViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.math.BigDecimal

class RoomFormFragment : Fragment() {

    private var _binding: FragmentRoomFormBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RoomViewModel by viewModels()
    private val propertyViewModel: PropertyViewModel by viewModels()
    private val args: RoomFormFragmentArgs by navArgs()

    private var selectedPropertyId: Long? = null
    private var selectedPropertyName: String? = null
    private var hasUserPickedProperty = false
    private var isEditMode = false
    private var isSyncingChips = false

    private val quickAmenities = listOf(
        "Điều hòa",
        "Bình nóng lạnh",
        "Giường",
        "Tủ quần áo",
        "Bàn học",
        "Wifi",
        "Tủ lạnh",
        "Ban công",
        "Kệ bếp"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRoomFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupStatusSpinner()
        setupAmenityChips()
        setupListeners()
        observeStates()

        if (args.roomId != 0L) {
            isEditMode = true
            binding.toolbar.title = getString(R.string.edit_room)
            binding.tilStatus.visibility = View.VISIBLE
            loadRoomDetail()
        } else {
            isEditMode = false
            binding.toolbar.title = getString(R.string.add_room)
            binding.tilStatus.visibility = View.GONE
            if (selectedPropertyId == null) {
                selectedPropertyId = args.propertyId.takeIf { it != 0L }
            }

            if (args.propertyId == 0L) {
                binding.propertySelectionGroup.visibility = View.VISIBLE
                if (!selectedPropertyName.isNullOrBlank()) {
                    binding.btnSelectProperty.text = "Nhà trọ: $selectedPropertyName"
                }
                if (selectedPropertyId == null) {
                    propertyViewModel.loadProperties()
                }
            } else {
                binding.propertySelectionGroup.visibility = View.GONE
            }
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupStatusSpinner() {
        val statuses = RoomStatus.values().map { status ->
            when (status) {
                RoomStatus.VACANT -> getString(R.string.status_vacant)
                RoomStatus.OCCUPIED -> getString(R.string.status_occupied)
                RoomStatus.EXPIRING_SOON -> getString(R.string.status_expiring_soon)
                RoomStatus.NEEDS_ATTENTION -> getString(R.string.status_needs_attention)
            }
        }

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, statuses)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerStatus.adapter = adapter
    }

    private fun setupAmenityChips() {
        binding.chipGroupAmenities.removeAllViews()
        quickAmenities.forEach { amenity ->
            val chip = Chip(requireContext()).apply {
                text = amenity
                isCheckable = true
                isCheckedIconVisible = true
                setOnCheckedChangeListener { _, _ ->
                    if (!isSyncingChips) {
                        syncAmenitiesTextFromChips()
                    }
                }
            }
            binding.chipGroupAmenities.addView(chip)
        }
    }

    private fun syncAmenitiesTextFromChips() {
        val currentTokens = binding.etAmenities.text?.toString()
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.toMutableList()
            ?: mutableListOf()

        val customTokens = currentTokens.filter { token ->
            quickAmenities.none { it.equals(token, ignoreCase = true) }
        }

        val selectedQuick = mutableListOf<String>()
        for (i in 0 until binding.chipGroupAmenities.childCount) {
            val chip = binding.chipGroupAmenities.getChildAt(i) as? Chip ?: continue
            if (chip.isChecked) {
                selectedQuick.add(chip.text.toString())
            }
        }

        val combined = (selectedQuick + customTokens).distinctBy { it.lowercase() }
        binding.etAmenities.setText(combined.joinToString(", "))
    }

    private fun syncChipsFromAmenitiesText(amenitiesText: String?) {
        val tokens = amenitiesText.orEmpty()
            .split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .toSet()

        isSyncingChips = true
        for (i in 0 until binding.chipGroupAmenities.childCount) {
            val chip = binding.chipGroupAmenities.getChildAt(i) as? Chip ?: continue
            chip.isChecked = tokens.contains(chip.text.toString().lowercase())
        }
        isSyncingChips = false
    }

    private fun setupListeners() {
        binding.btnSelectProperty.setOnClickListener {
            val action = RoomFormFragmentDirections.actionRoomFormToPropertySelection()
            findNavController().navigate(action)
        }

        binding.btnSave.setOnClickListener {
            if (validateForm()) {
                if (isEditMode) {
                    updateRoom()
                } else {
                    createRoom()
                }
            }
        }

        val savedState = findNavController().currentBackStackEntry?.savedStateHandle
        savedState?.getLiveData<Long>("selected_property_id")
            ?.observe(viewLifecycleOwner) { propertyId ->
                hasUserPickedProperty = true
                selectedPropertyId = propertyId
                val propName = savedState.get<String>("selected_property_name")
                selectedPropertyName = propName
                binding.btnSelectProperty.text = if (!propName.isNullOrBlank()) {
                    "Nhà trọ: $propName"
                } else {
                    getString(R.string.property_selected)
                }
            }
    }

    private fun observeStates() {
        viewLifecycleOwner.lifecycleScope.launch {
            propertyViewModel.propertiesState.collect { state ->
                val list = state.data
                if (!isEditMode && !hasUserPickedProperty && selectedPropertyId == null && !list.isNullOrEmpty()) {
                    val first = list.first()
                    selectedPropertyId = first.id
                    selectedPropertyName = first.name
                    binding.btnSelectProperty.text = "Nhà trọ: ${first.name}"
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.roomDetailState.collect { state ->
                when {
                    state.isLoading -> showLoading()
                    state.error != null -> {
                        hideLoading()
                        showError(state.error)
                    }
                    state.data != null -> {
                        hideLoading()
                        populateForm(state.data)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.createUpdateState.collect { state ->
                when {
                    state.isLoading -> showLoading()
                    state.error != null -> {
                        hideLoading()
                        showError(state.error)
                    }
                    state.data != null -> {
                        hideLoading()
                        Snackbar.make(
                            binding.root,
                            if (isEditMode) getString(R.string.room_updated) else getString(R.string.room_created),
                            Snackbar.LENGTH_SHORT
                        ).show()
                        viewModel.resetCreateUpdateState()
                        findNavController().navigateUp()
                    }
                }
            }
        }
    }

    private fun loadRoomDetail() {
        viewModel.getRoomDetail(args.roomId)
    }

    private fun populateForm(room: RoomResponse) {
        selectedPropertyId = room.propertyId
        binding.propertySelectionGroup.visibility = View.GONE
        binding.tvDefaultHealthScore.text = "Health Score hiện tại: ${room.healthScore}/100"

        binding.etRoomNumber.setText(room.roomCode)
        binding.etFloor.setText(room.floor.toString())
        binding.etArea.setText(room.area.stripTrailingZeros().toPlainString())
        binding.etPrice.setText(room.rentPrice.stripTrailingZeros().toPlainString())
        binding.etDeposit.setText(room.deposit.stripTrailingZeros().toPlainString())
        binding.etMaxOccupants.setText(room.capacity.toString())
        binding.etElectricityPrice.setText(room.electricityPrice?.stripTrailingZeros()?.toPlainString().orEmpty())
        binding.etWaterPrice.setText(room.waterPrice?.stripTrailingZeros()?.toPlainString().orEmpty())
        binding.etInternetFee.setText(room.internetFee?.stripTrailingZeros()?.toPlainString().orEmpty())
        binding.etServiceFee.setText(room.serviceFee?.stripTrailingZeros()?.toPlainString().orEmpty())
        binding.etAmenities.setText(room.amenities.orEmpty())
        syncChipsFromAmenitiesText(room.amenities)
        binding.etImageUrl.setText(room.imageUrl.orEmpty())
        binding.etDescription.setText(room.description.orEmpty())

        val statusPosition = RoomStatus.values().indexOf(room.status)
        if (statusPosition >= 0) {
            binding.spinnerStatus.setSelection(statusPosition)
        }
    }

    private fun validateForm(): Boolean {
        var isValid = true

        if (selectedPropertyId == null) {
            showError(getString(R.string.error_select_property))
            return false
        }

        val roomNumber = binding.etRoomNumber.text.toString().trim()
        if (roomNumber.isEmpty()) {
            binding.tilRoomNumber.error = getString(R.string.error_room_number_required)
            isValid = false
        } else {
            binding.tilRoomNumber.error = null
        }

        val floorStr = binding.etFloor.text.toString().trim()
        val floorInt = floorStr.toIntOrNull()
        if (floorStr.isEmpty()) {
            binding.tilFloor.error = getString(R.string.error_floor_required)
            isValid = false
        } else if (floorInt == null || floorInt < 0) {
            binding.tilFloor.error = getString(R.string.rc_error_floor_invalid)
            isValid = false
        } else {
            binding.tilFloor.error = null
        }

        val areaStr = binding.etArea.text.toString().trim()
        val areaVal = areaStr.toBigDecimalOrNull()
        if (areaStr.isEmpty()) {
            binding.tilArea.error = getString(R.string.error_area_required)
            isValid = false
        } else if (areaVal == null || areaVal <= BigDecimal.ZERO) {
            binding.tilArea.error = getString(R.string.error_area_invalid)
            isValid = false
        } else {
            binding.tilArea.error = null
        }

        val priceStr = binding.etPrice.text.toString().trim()
        val priceVal = priceStr.toBigDecimalOrNull()
        if (priceStr.isEmpty()) {
            binding.tilPrice.error = getString(R.string.error_price_required)
            isValid = false
        } else if (priceVal == null || priceVal <= BigDecimal.ZERO) {
            binding.tilPrice.error = getString(R.string.error_price_invalid)
            isValid = false
        } else {
            binding.tilPrice.error = null
        }

        val depositStr = binding.etDeposit.text.toString().trim()
        val depositVal = depositStr.toBigDecimalOrNull()
        if (depositStr.isEmpty()) {
            binding.tilDeposit.error = getString(R.string.rc_error_deposit_required)
            isValid = false
        } else if (depositVal == null || depositVal < BigDecimal.ZERO) {
            binding.tilDeposit.error = getString(R.string.rc_error_deposit_invalid)
            isValid = false
        } else {
            binding.tilDeposit.error = null
        }

        val maxOccupantsStr = binding.etMaxOccupants.text.toString().trim()
        val maxOccupantsInt = maxOccupantsStr.toIntOrNull()
        if (maxOccupantsStr.isEmpty()) {
            binding.tilMaxOccupants.error = getString(R.string.error_max_occupants_required)
            isValid = false
        } else if (maxOccupantsInt == null || maxOccupantsInt <= 0) {
            binding.tilMaxOccupants.error = getString(R.string.error_max_occupants_invalid)
            isValid = false
        } else {
            binding.tilMaxOccupants.error = null
        }

        isValid = validateOptionalNonNegative(binding.tilElectricityPrice, binding.etElectricityPrice.text?.toString()) && isValid
        isValid = validateOptionalNonNegative(binding.tilWaterPrice, binding.etWaterPrice.text?.toString()) && isValid
        isValid = validateOptionalNonNegative(binding.tilInternetFee, binding.etInternetFee.text?.toString()) && isValid
        isValid = validateOptionalNonNegative(binding.tilServiceFee, binding.etServiceFee.text?.toString()) && isValid

        val imageUrl = binding.etImageUrl.text?.toString()?.trim().orEmpty()
        if (imageUrl.isNotEmpty() && !Regex("https?://\\S+").matches(imageUrl)) {
            binding.tilImageUrl.error = getString(R.string.rc_incident_invalid_link)
            isValid = false
        } else {
            binding.tilImageUrl.error = null
        }

        return isValid
    }

    private fun validateOptionalNonNegative(
        til: com.google.android.material.textfield.TextInputLayout,
        raw: String?
    ): Boolean {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) {
            til.error = null
            return true
        }
        val num = text.toBigDecimalOrNull()
        return if (num == null || num < BigDecimal.ZERO) {
            til.error = getString(R.string.rc_error_amount_invalid)
            false
        } else {
            til.error = null
            true
        }
    }

    private fun optionalMoney(text: String?): BigDecimal? =
        text?.trim()?.takeIf { it.isNotEmpty() }?.toBigDecimalOrNull()

    private fun createRoom() {
        val request = CreateRoomRequest(
            propertyId = selectedPropertyId!!,
            roomCode = binding.etRoomNumber.text.toString().trim(),
            floor = binding.etFloor.text.toString().trim().toInt(),
            area = BigDecimal(binding.etArea.text.toString().trim()),
            rentPrice = BigDecimal(binding.etPrice.text.toString().trim()),
            deposit = BigDecimal(binding.etDeposit.text.toString().trim()),
            capacity = binding.etMaxOccupants.text.toString().trim().toInt(),
            amenities = binding.etAmenities.text.toString().trim().ifEmpty { null },
            description = binding.etDescription.text.toString().trim().ifEmpty { null },
            imageUrl = binding.etImageUrl.text.toString().trim().ifEmpty { null },
            electricityPrice = optionalMoney(binding.etElectricityPrice.text?.toString()),
            waterPrice = optionalMoney(binding.etWaterPrice.text?.toString()),
            internetFee = optionalMoney(binding.etInternetFee.text?.toString()),
            serviceFee = optionalMoney(binding.etServiceFee.text?.toString())
        )

        viewModel.createRoom(request)
    }

    private fun updateRoom() {
        val request = UpdateRoomRequest(
            roomCode = binding.etRoomNumber.text.toString().trim(),
            floor = binding.etFloor.text.toString().trim().toInt(),
            area = BigDecimal(binding.etArea.text.toString().trim()),
            rentPrice = BigDecimal(binding.etPrice.text.toString().trim()),
            deposit = BigDecimal(binding.etDeposit.text.toString().trim()),
            capacity = binding.etMaxOccupants.text.toString().trim().toInt(),
            amenities = binding.etAmenities.text.toString().trim().ifEmpty { null },
            description = binding.etDescription.text.toString().trim().ifEmpty { null },
            imageUrl = binding.etImageUrl.text.toString().trim().ifEmpty { null },
            status = RoomStatus.values()[binding.spinnerStatus.selectedItemPosition],
            electricityPrice = optionalMoney(binding.etElectricityPrice.text?.toString()),
            waterPrice = optionalMoney(binding.etWaterPrice.text?.toString()),
            internetFee = optionalMoney(binding.etInternetFee.text?.toString()),
            serviceFee = optionalMoney(binding.etServiceFee.text?.toString())
        )

        viewModel.updateRoom(args.roomId, request)
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.scrollView.visibility = View.GONE
    }

    private fun hideLoading() {
        binding.progressBar.visibility = View.GONE
        binding.scrollView.visibility = View.VISIBLE
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
