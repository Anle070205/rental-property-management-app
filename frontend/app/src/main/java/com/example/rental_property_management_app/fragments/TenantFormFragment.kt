package com.example.rental_property_management_app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.databinding.FragmentTenantFormBinding
import com.example.rental_property_management_app.models.CreateTenantRequest
import com.example.rental_property_management_app.models.UpdateTenantRequest
import com.example.rental_property_management_app.utils.DateUtils
import com.example.rental_property_management_app.utils.ValidationUtils
import com.example.rental_property_management_app.viewmodels.PropertyViewModel
import com.example.rental_property_management_app.viewmodels.TenantViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TenantFormFragment : Fragment() {

    private var _binding: FragmentTenantFormBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TenantViewModel by viewModels()
    private val propertyViewModel: PropertyViewModel by viewModels()
    private val args: TenantFormFragmentArgs by navArgs()

    private var selectedPropertyId: Long? = null
    private var selectedPropertyName: String? = null
    private var isEditMode = false
    private var selectedDateOfBirth: Date? = null

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTenantFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupListeners()
        observeStates()

        // Check if edit mode
        if (args.tenantId != 0L) {
            isEditMode = true
            binding.toolbar.title = getString(R.string.edit_tenant)
            loadTenantDetail()
        } else {
            isEditMode = false
            binding.toolbar.title = getString(R.string.add_tenant)

            binding.selectProperty.visibility = View.VISIBLE
            val navController = findNavController()
            binding.selectProperty.setOnClickListener {
                navController.navigate(R.id.propertySelectionFragment)
            }
            val savedState = navController.currentBackStackEntry?.savedStateHandle
            savedState?.getLiveData<Long>("selected_property_id")?.observe(viewLifecycleOwner) { propId ->
                selectedPropertyId = propId
                val propName = savedState.get<String>("selected_property_name")
                binding.selectProperty.text = if (!propName.isNullOrBlank()) {
                    "Nhà trọ: $propName"
                } else {
                    getString(R.string.property_selected)
                }
            }

            if (args.roomId != 0L) {
                selectedPropertyId = args.roomId
            }
            propertyViewModel.loadProperties()
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupListeners() {
        binding.etMoveInDate.setOnClickListener {
            showDatePicker()
        }

        binding.btnSave.setOnClickListener {
            if (validateForm()) {
                if (isEditMode) {
                    updateTenant()
                } else {
                    createTenant()
                }
            }
        }
    }

    private fun showDatePicker() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Chọn ngày sinh")
            .setSelection(selectedDateOfBirth?.time ?: MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            selectedDateOfBirth = Date(selection)
            binding.etMoveInDate.setText(dateFormat.format(selectedDateOfBirth!!))
        }

        datePicker.show(parentFragmentManager, "DATE_PICKER")
    }

    private fun observeStates() {
        viewLifecycleOwner.lifecycleScope.launch {
            propertyViewModel.propertiesState.collect { state ->
                val list = state.data
                if (!isEditMode && selectedPropertyId == null && !list.isNullOrEmpty()) {
                    val first = list.first()
                    selectedPropertyId = first.id
                    selectedPropertyName = first.name
                    binding.selectProperty.text = "Nhà trọ: ${first.name}"
                }
            }
        }

        // Observe tenant detail for edit mode
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.tenantDetailState.collect { state ->
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
                            if (isEditMode) getString(R.string.tenant_updated) else getString(R.string.tenant_created),
                            Snackbar.LENGTH_SHORT
                        ).show()
                        findNavController().navigateUp()
                    }
                }
            }
        }
    }

    private fun loadTenantDetail() {
        viewModel.getTenantDetail(args.tenantId)
    }

    private fun populateForm(tenant: com.example.rental_property_management_app.models.TenantResponse) {
        selectedPropertyId = tenant.propertyId
        binding.etFullName.setText(tenant.fullName)
        binding.etPhone.setText(tenant.phone)
        binding.etEmail.setText(tenant.email)
        binding.etIdCard.setText(tenant.identityNumber)

        tenant.dateOfBirth?.let { dateStr ->
            try {
                selectedDateOfBirth = DateUtils.parseApiDate(dateStr)
                selectedDateOfBirth?.let {
                    binding.etMoveInDate.setText(dateFormat.format(it))
                }
            } catch (_: Exception) {
            }
        }

        binding.etEmergencyContact.setText(tenant.emergencyContact)
        binding.etNotes.setText(tenant.address)
    }

    private fun validateForm(): Boolean {
        var isValid = true

        if (selectedPropertyId == null) {
            showError(getString(R.string.error_select_property))
            return false
        }

        // Validate full name
        val fullName = binding.etFullName.text.toString().trim()
        if (fullName.isEmpty()) {
            binding.tilFullName.error = getString(R.string.error_full_name_required)
            isValid = false
        } else {
            binding.tilFullName.error = null
        }

        // Validate phone
        val phone = binding.etPhone.text.toString().trim()
        if (phone.isEmpty()) {
            binding.tilPhone.error = getString(R.string.error_phone_required)
            isValid = false
        } else if (!ValidationUtils.isValidPhone(phone)) {
            binding.tilPhone.error = getString(R.string.error_phone_invalid)
            isValid = false
        } else {
            binding.tilPhone.error = null
        }

        // Validate email (optional but must be valid if provided)
        val email = binding.etEmail.text.toString().trim()
        if (email.isNotEmpty() && !ValidationUtils.isValidEmail(email)) {
            binding.tilEmail.error = getString(R.string.error_email_invalid)
            isValid = false
        } else {
            binding.tilEmail.error = null
        }

        // Validate ID card
        val idCard = binding.etIdCard.text.toString().trim()
        if (idCard.isEmpty()) {
            binding.tilIdCard.error = getString(R.string.error_id_card_required)
            isValid = false
        } else {
            binding.tilIdCard.error = null
        }

        // Validate date of birth
        if (selectedDateOfBirth == null) {
            binding.tilMoveInDate.error = "Vui lòng chọn ngày sinh"
            isValid = false
        } else {
            binding.tilMoveInDate.error = null
        }

        return isValid
    }

    private fun createTenant() {
        val request = CreateTenantRequest(
            propertyId = selectedPropertyId!!,
            fullName = binding.etFullName.text.toString().trim(),
            phone = binding.etPhone.text.toString().trim(),
            email = binding.etEmail.text.toString().trim().ifEmpty { null },
            identityNumber = binding.etIdCard.text.toString().trim(),
            dateOfBirth = selectedDateOfBirth?.let { DateUtils.formatToApiDate(it) },
            address = binding.etNotes.text.toString().trim().ifEmpty { null },
            emergencyContact = binding.etEmergencyContact.text.toString().trim().ifEmpty { null }
        )

        viewModel.createTenant(request)
    }

    private fun updateTenant() {
        val request = UpdateTenantRequest(
            fullName = binding.etFullName.text.toString().trim(),
            phone = binding.etPhone.text.toString().trim(),
            email = binding.etEmail.text.toString().trim().ifEmpty { null },
            identityNumber = binding.etIdCard.text.toString().trim(),
            dateOfBirth = selectedDateOfBirth?.let { DateUtils.formatToApiDate(it) },
            address = binding.etNotes.text.toString().trim().ifEmpty { null },
            emergencyContact = binding.etEmergencyContact.text.toString().trim().ifEmpty { null }
        )

        viewModel.updateTenant(args.tenantId, request)
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
