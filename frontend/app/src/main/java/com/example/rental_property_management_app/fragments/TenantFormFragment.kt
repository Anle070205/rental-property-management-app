package com.example.rental_property_management_app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.core.os.bundleOf
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.databinding.FragmentTenantFormBinding
import com.example.rental_property_management_app.models.CreateTenantRequest
import com.example.rental_property_management_app.models.UpdateTenantRequest
import com.example.rental_property_management_app.utils.CccdQrParser
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
            if (args.propertyId > 0L) {
                selectedPropertyId = args.propertyId
                binding.selectProperty.text = if (args.roomCode.isNotBlank()) {
                    "Nhà trọ đã chọn • Phòng ${args.roomCode}"
                } else {
                    getString(R.string.property_selected)
                }
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

        binding.btnPasteCccdQr.setOnClickListener {
            showCccdQrInputDialog()
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

    private fun showCccdQrInputDialog() {
        val input = EditText(requireContext()).apply {
            hint = "VD: 079200001234||Nguyễn Văn An|15081998|Nam|TP.HCM|01012022"
            minLines = 2
            setPadding(36, 24, 36, 24)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Nhập nhanh từ chuỗi QR CCCD")
            .setMessage("Dán chuỗi QR quét từ CCCD gắn chip để tự điền họ tên, số CCCD, ngày sinh và địa chỉ:")
            .setView(input)
            .setPositiveButton("Giải mã") { _, _ ->
                val parsed = CccdQrParser.parse(input.text?.toString())
                if (parsed == null) {
                    showError("Chuỗi QR CCCD không hợp lệ. Định dạng chuẩn dùng dấu |")
                } else {
                    binding.etIdCard.setText(parsed.identityNumber)
                    binding.etFullName.setText(parsed.fullName)
                    parsed.address?.let { binding.etNotes.setText(it) }
                    parsed.dateOfBirthIso?.let { iso ->
                        try {
                            selectedDateOfBirth = DateUtils.parseApiDate(iso)
                            selectedDateOfBirth?.let { binding.etMoveInDate.setText(dateFormat.format(it)) }
                        } catch (_: Exception) {
                        }
                    }
                    binding.cbCccdConsent.isChecked = true
                    Snackbar.make(binding.root, "Đã điền thông tin từ QR CCCD", Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
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
                if (!isEditMode && !list.isNullOrEmpty()) {
                    val matched = selectedPropertyId?.let { id -> list.find { it.id == id } }
                    val chosen = matched ?: if (selectedPropertyId == null) list.first() else null
                    if (chosen != null) {
                        selectedPropertyId = chosen.id
                        selectedPropertyName = chosen.name
                        binding.selectProperty.text = if (args.roomCode.isNotBlank()) {
                            "Nhà trọ: ${chosen.name} • Phòng ${args.roomCode}"
                        } else {
                            "Nhà trọ: ${chosen.name}"
                        }
                    }
                }
            }
        }

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
                        val created = state.data
                        if (isEditMode) {
                            Snackbar.make(
                                binding.root,
                                getString(R.string.tenant_updated),
                                Snackbar.LENGTH_SHORT
                            ).show()
                            findNavController().navigateUp()
                        } else {
                            AlertDialog.Builder(requireContext())
                                .setTitle("Đã thêm người thuê")
                                .setMessage(
                                    "Đã lưu hồ sơ ${created.fullName}. Tiếp tục cấp tài khoản đăng nhập RoomCare cho người thuê này?"
                                )
                                .setCancelable(false)
                                .setPositiveButton("Cấp tài khoản ngay") { _, _ ->
                                    val navOptions = NavOptions.Builder()
                                        .setPopUpTo(R.id.tenantFormFragment, true)
                                        .build()
                                    findNavController().navigate(
                                        R.id.tenantDetailFragment,
                                        bundleOf(
                                            "tenantId" to created.id,
                                            "openProvision" to true,
                                            "roomId" to args.roomId
                                        ),
                                        navOptions
                                    )
                                }
                                .setNegativeButton("Xem chi tiết") { _, _ ->
                                    val navOptions = NavOptions.Builder()
                                        .setPopUpTo(R.id.tenantFormFragment, true)
                                        .build()
                                    findNavController().navigate(
                                        R.id.tenantDetailFragment,
                                        bundleOf(
                                            "tenantId" to created.id,
                                            "roomId" to args.roomId
                                        ),
                                        navOptions
                                    )
                                }
                                .show()
                        }
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
        binding.etPhone.setText(tenant.phone.orEmpty())
        binding.etEmail.setText(tenant.email.orEmpty())
        binding.etIdCard.setText(tenant.identityNumber.orEmpty())
        binding.cbCccdConsent.isChecked = !tenant.identityNumber.isNullOrBlank()

        tenant.dateOfBirth?.let { dateStr ->
            try {
                selectedDateOfBirth = DateUtils.parseApiDate(dateStr)
                selectedDateOfBirth?.let {
                    binding.etMoveInDate.setText(dateFormat.format(it))
                }
            } catch (_: Exception) {
            }
        }

        binding.etEmergencyContact.setText(tenant.emergencyContact.orEmpty())
        binding.etNotes.setText(tenant.address.orEmpty())
    }

    private fun validateForm(): Boolean {
        var isValid = true

        if (selectedPropertyId == null) {
            showError(getString(R.string.error_select_property))
            return false
        }

        val fullName = binding.etFullName.text.toString().trim()
        if (fullName.isEmpty()) {
            binding.tilFullName.error = getString(R.string.error_full_name_required)
            isValid = false
        } else {
            binding.tilFullName.error = null
        }

        val phone = binding.etPhone.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        if (phone.isEmpty() && email.isEmpty()) {
            binding.tilPhone.error = "Nhập số điện thoại hoặc email"
            binding.tilEmail.error = "Nhập email hoặc số điện thoại"
            isValid = false
        } else {
            if (phone.isNotEmpty() && !ValidationUtils.isValidPhone(phone)) {
                binding.tilPhone.error = getString(R.string.error_phone_invalid)
                isValid = false
            } else {
                binding.tilPhone.error = null
            }

            if (email.isNotEmpty() && !ValidationUtils.isValidEmail(email)) {
                binding.tilEmail.error = getString(R.string.error_email_invalid)
                isValid = false
            } else {
                binding.tilEmail.error = null
            }
        }

        val idCard = binding.etIdCard.text.toString().trim()
        if (idCard.isNotEmpty() && !binding.cbCccdConsent.isChecked) {
            binding.tilIdCard.error = "Cần tick xác nhận khách đồng ý lưu CCCD"
            isValid = false
        } else {
            binding.tilIdCard.error = null
        }

        binding.tilMoveInDate.error = null
        return isValid
    }

    private fun createTenant() {
        val request = CreateTenantRequest(
            propertyId = selectedPropertyId!!,
            fullName = binding.etFullName.text.toString().trim(),
            phone = binding.etPhone.text.toString().trim().ifEmpty { null },
            email = binding.etEmail.text.toString().trim().ifEmpty { null },
            identityNumber = binding.etIdCard.text.toString().trim().ifEmpty { null },
            consentConfirmed = binding.cbCccdConsent.isChecked,
            dateOfBirth = selectedDateOfBirth?.let { DateUtils.formatToApiDate(it) },
            address = binding.etNotes.text.toString().trim().ifEmpty { null },
            emergencyContact = binding.etEmergencyContact.text.toString().trim().ifEmpty { null }
        )

        viewModel.createTenant(request)
    }

    private fun updateTenant() {
        val request = UpdateTenantRequest(
            fullName = binding.etFullName.text.toString().trim(),
            phone = binding.etPhone.text.toString().trim().ifEmpty { null },
            email = binding.etEmail.text.toString().trim().ifEmpty { null },
            identityNumber = binding.etIdCard.text.toString().trim().ifEmpty { null },
            consentConfirmed = binding.cbCccdConsent.isChecked,
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
