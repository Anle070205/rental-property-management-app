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
import com.example.rental_property_management_app.models.RoomStatus
import com.example.rental_property_management_app.models.UpdateRoomRequest
import com.example.rental_property_management_app.utils.ValidationUtils
import com.example.rental_property_management_app.viewmodels.RoomViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class RoomFormFragment : Fragment() {

    private var _binding: FragmentRoomFormBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RoomViewModel by viewModels()
    private val args: RoomFormFragmentArgs by navArgs()

    private var selectedPropertyId: Long? = null
    private var isEditMode = false

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
        setupListeners()
        observeStates()

        // Check if edit mode
        if (args.roomId != 0L) {
            isEditMode = true
            binding.toolbar.title = getString(R.string.edit_room)
            loadRoomDetail()
        } else {
            isEditMode = false
            binding.toolbar.title = getString(R.string.add_room)
            selectedPropertyId = args.propertyId.takeIf { it != 0L }
            
            // If propertyId not passed, show property selection
            if (selectedPropertyId == null) {
                binding.propertySelectionGroup.visibility = View.VISIBLE
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

        // Listen for property selection result
        val savedState = findNavController().currentBackStackEntry?.savedStateHandle
        savedState?.getLiveData<Long>("selected_property_id")
            ?.observe(viewLifecycleOwner) { propertyId ->
                selectedPropertyId = propertyId
                val propName = savedState.get<String>("selected_property_name")
                binding.btnSelectProperty.text = if (!propName.isNullOrBlank()) {
                    "Nhà trọ: $propName"
                } else {
                    getString(R.string.property_selected)
                }
                Snackbar.make(binding.root, getString(R.string.property_selected), Snackbar.LENGTH_SHORT).show()
            }
    }

    private fun observeStates() {
        // Observe room detail for edit mode
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

        // Observe create/update result
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
                        findNavController().navigateUp()
                    }
                }
            }
        }
    }

    private fun loadRoomDetail() {
        viewModel.getRoomDetail(args.roomId)
    }

    private fun populateForm(room: com.example.rental_property_management_app.models.RoomResponse) {
        selectedPropertyId = room.propertyId
        binding.propertySelectionGroup.visibility = View.GONE

        binding.etRoomNumber.setText(room.roomCode)
        binding.etFloor.setText(room.floor.toString())
        binding.etArea.setText(room.area.toString())
        binding.etPrice.setText(room.rentPrice.toString())
        binding.etDeposit.setText(room.deposit.toString())
        binding.etMaxOccupants.setText(room.capacity.toString())
        binding.etAmenities.setText(room.amenities)
        binding.etDescription.setText(room.description)

        // Set status spinner
        val statusPosition = RoomStatus.values().indexOf(room.status)
        if (statusPosition >= 0) {
            binding.spinnerStatus.setSelection(statusPosition)
        }
    }

    private fun validateForm(): Boolean {
        var isValid = true

        // Validate property selection
        if (selectedPropertyId == null) {
            showError(getString(R.string.error_select_property))
            return false
        }

        // Validate room number
        val roomNumber = binding.etRoomNumber.text.toString().trim()
        if (roomNumber.isEmpty()) {
            binding.tilRoomNumber.error = getString(R.string.error_room_number_required)
            isValid = false
        } else {
            binding.tilRoomNumber.error = null
        }

        // Validate floor
        val floorStr = binding.etFloor.text.toString().trim()
        if (floorStr.isEmpty()) {
            binding.tilFloor.error = getString(R.string.error_floor_required)
            isValid = false
        } else {
            binding.tilFloor.error = null
        }

        // Validate area
        val areaStr = binding.etArea.text.toString().trim()
        if (areaStr.isEmpty()) {
            binding.tilArea.error = getString(R.string.error_area_required)
            isValid = false
        } else if (areaStr.toDoubleOrNull() == null || areaStr.toDouble() <= 0) {
            binding.tilArea.error = getString(R.string.error_area_invalid)
            isValid = false
        } else {
            binding.tilArea.error = null
        }

        // Validate price
        val priceStr = binding.etPrice.text.toString().trim()
        if (priceStr.isEmpty()) {
            binding.tilPrice.error = getString(R.string.error_price_required)
            isValid = false
        } else if (priceStr.toDoubleOrNull() == null || priceStr.toDouble() <= 0) {
            binding.tilPrice.error = getString(R.string.error_price_invalid)
            isValid = false
        } else {
            binding.tilPrice.error = null
        }

        // Validate max occupants
        val maxOccupantsStr = binding.etMaxOccupants.text.toString().trim()
        if (maxOccupantsStr.isEmpty()) {
            binding.tilMaxOccupants.error = getString(R.string.error_max_occupants_required)
            isValid = false
        } else if (maxOccupantsStr.toIntOrNull() == null || maxOccupantsStr.toInt() <= 0) {
            binding.tilMaxOccupants.error = getString(R.string.error_max_occupants_invalid)
            isValid = false
        } else {
            binding.tilMaxOccupants.error = null
        }

        return isValid
    }

    private fun createRoom() {
        val request = CreateRoomRequest(
            propertyId = selectedPropertyId!!,
            roomCode = binding.etRoomNumber.text.toString().trim(),
            floor = binding.etFloor.text.toString().toInt(),
            area = java.math.BigDecimal(binding.etArea.text.toString()),
            rentPrice = java.math.BigDecimal(binding.etPrice.text.toString()),
            deposit = java.math.BigDecimal(binding.etDeposit.text.toString()),
            capacity = binding.etMaxOccupants.text.toString().toInt(),
            amenities = binding.etAmenities.text.toString().trim().ifEmpty { null },
            description = binding.etDescription.text.toString().trim().ifEmpty { null },
            imageUrl = null // TODO: Phase 3 - Image upload
        )

        viewModel.createRoom(request)
    }

    private fun updateRoom() {
        val request = UpdateRoomRequest(
            roomCode = binding.etRoomNumber.text.toString().trim(),
            floor = binding.etFloor.text.toString().toInt(),
            area = java.math.BigDecimal(binding.etArea.text.toString()),
            rentPrice = java.math.BigDecimal(binding.etPrice.text.toString()),
            deposit = java.math.BigDecimal(binding.etDeposit.text.toString()),
            capacity = binding.etMaxOccupants.text.toString().toInt(),
            amenities = binding.etAmenities.text.toString().trim().ifEmpty { null },
            description = binding.etDescription.text.toString().trim().ifEmpty { null },
            imageUrl = null, // TODO: Phase 3 - Image upload
            status = RoomStatus.values()[binding.spinnerStatus.selectedItemPosition]
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
