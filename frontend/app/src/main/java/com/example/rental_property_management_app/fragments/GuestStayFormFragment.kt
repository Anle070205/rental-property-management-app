package com.example.rental_property_management_app.fragments

import android.Manifest
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.databinding.FragmentGuestStayFormBinding
import com.example.rental_property_management_app.models.CreateGuestStayRequest
import com.example.rental_property_management_app.utils.CccdQrParser
import com.example.rental_property_management_app.utils.Resource
import com.example.rental_property_management_app.viewmodels.GuestStayViewModel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class GuestStayFormFragment : Fragment(R.layout.fragment_guest_stay_form) {

    private var _binding: FragmentGuestStayFormBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val viewModel: GuestStayViewModel by viewModels()

    private var qrVerified = false
    private var selectedDobIso: String? = null
    private val startCal: Calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 18)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
    }
    private val endCal: Calendar = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 10)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
    }

    private val apiDateTimeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
    private val displayDateTimeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    private val scanQrLauncher = registerForActivityResult(ScanContract()) { result ->
        val raw = result.contents
        if (raw == null) {
            Toast.makeText(requireContext(), "Đã hủy quét QR CCCD", Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }
        applyCccdPayload(raw)
    }

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                launchQrScanner()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Chưa cấp quyền Camera. Bạn có thể dán chuỗi QR hoặc nhập thông tin thủ công.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentGuestStayFormBinding.bind(view)

        updateDateTimeFields()

        binding.btnScanCccdCamera.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            ) {
                launchQrScanner()
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        binding.btnPasteCccdQr.setOnClickListener { showPasteQrDialog() }

        binding.etGuestDob.setOnClickListener { showDobPicker() }
        binding.etStayStart.setOnClickListener {
            pickDateTime(startCal) { updateDateTimeFields() }
        }
        binding.etStayEnd.setOnClickListener {
            pickDateTime(endCal) { updateDateTimeFields() }
        }

        binding.btnSubmitGuestStay.setOnClickListener { submitForm() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.actionState.collect { res ->
                binding.progressSubmit.isVisible = res is Resource.Loading
                binding.btnSubmitGuestStay.isEnabled = res !is Resource.Loading
                when (res) {
                    is Resource.Success -> {
                        Toast.makeText(
                            requireContext(),
                            res.message ?: "Đã gửi khai báo khách lưu trú",
                            Toast.LENGTH_LONG
                        ).show()
                        viewModel.resetActionState()
                        findNavController().popBackStack()
                    }
                    is Resource.Error -> {
                        Toast.makeText(requireContext(), res.message, Toast.LENGTH_LONG).show()
                        viewModel.resetActionState()
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun launchQrScanner() {
        val options = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt("Đưa mã QR trên thẻ CCCD gắn chip vào khung hình")
            setBeepEnabled(false)
            setOrientationLocked(true)
        }
        scanQrLauncher.launch(options)
    }

    private fun showPasteQrDialog() {
        val input = EditText(requireContext()).apply {
            hint = "VD: 079200001234||Lê Minh Khách|10051999|Nam|TP.HCM|01012022"
            minLines = 2
            setPadding(36, 24, 36, 24)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Nhập từ chuỗi QR CCCD")
            .setMessage("Dán chuỗi QR đọc từ thẻ CCCD (hoặc dữ liệu test hợp lệ) để tự động điền biểu mẫu:")
            .setView(input)
            .setPositiveButton("Giải mã") { _, _ ->
                applyCccdPayload(input.text.toString())
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun applyCccdPayload(raw: String) {
        val parsed = CccdQrParser.parse(raw)
        if (parsed == null) {
            qrVerified = false
            binding.tvQrScanStatus.isVisible = true
            binding.tvQrScanStatus.setBackgroundResource(R.drawable.rc_badge_warning)
            binding.tvQrScanStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.warning))
            binding.tvQrScanStatus.text =
                "Mã QR không đúng định dạng CCCD gắn chip hỗ trợ. Vui lòng quét lại hoặc nhập thủ công."
            return
        }

        qrVerified = true
        binding.etGuestId.setText(parsed.identityNumber)
        binding.etGuestName.setText(parsed.fullName)
        selectedDobIso = parsed.dateOfBirthIso
        binding.etGuestDob.setText(parsed.dateOfBirthIso.orEmpty())
        binding.etGuestGender.setText(parsed.gender.orEmpty())

        binding.tvQrScanStatus.isVisible = true
        binding.tvQrScanStatus.setBackgroundResource(R.drawable.rc_badge_success)
        binding.tvQrScanStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.success))
        binding.tvQrScanStatus.text =
            "Đã đọc thông tin từ mã QR CCCD. Vui lòng kiểm tra lại và xác nhận trước khi gửi."

        // Highlight step 2
        binding.tvStep2.setBackgroundResource(R.drawable.rc_badge)
        binding.tvStep2.setTextColor(ContextCompat.getColor(requireContext(), R.color.rc_brand))
    }

    private fun showDobPicker() {
        val cal = Calendar.getInstance()
        cal.set(1998, 0, 1)
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val iso = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                selectedDobIso = iso
                binding.etGuestDob.setText(iso)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun pickDateTime(target: Calendar, onUpdated: () -> Unit) {
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                TimePickerDialog(
                    requireContext(),
                    { _, hourOfDay, minute ->
                        target.set(year, month, dayOfMonth, hourOfDay, minute, 0)
                        onUpdated()
                    },
                    target.get(Calendar.HOUR_OF_DAY),
                    target.get(Calendar.MINUTE),
                    true
                ).show()
            },
            target.get(Calendar.YEAR),
            target.get(Calendar.MONTH),
            target.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateDateTimeFields() {
        binding.etStayStart.setText(displayDateTimeFormat.format(startCal.time))
        binding.etStayEnd.setText(displayDateTimeFormat.format(endCal.time))
    }

    private fun submitForm() {
        val name = binding.etGuestName.text?.toString()?.trim().orEmpty()
        val idNum = binding.etGuestId.text?.toString()?.trim().orEmpty()
        val gender = binding.etGuestGender.text?.toString()?.trim()?.ifEmpty { null }
        val phone = binding.etGuestPhone.text?.toString()?.trim()?.ifEmpty { null }
        val note = binding.etNote.text?.toString()?.trim()?.ifEmpty { null }

        var valid = true
        if (name.isBlank()) {
            binding.tilGuestName.error = "Vui lòng nhập họ và tên khách"
            valid = false
        } else {
            binding.tilGuestName.error = null
        }

        if (!Regex("^(\\d{9}|\\d{12})$").matches(idNum)) {
            binding.tilGuestId.error = "Số CCCD/CMND phải gồm 9 hoặc 12 chữ số"
            valid = false
        } else {
            binding.tilGuestId.error = null
        }

        if (!endCal.time.after(startCal.time)) {
            binding.tilStayEnd.error = "Thời gian kết thúc phải sau thời gian bắt đầu"
            valid = false
        } else {
            binding.tilStayEnd.error = null
        }

        if (!binding.cbConsent.isChecked) {
            Toast.makeText(
                requireContext(),
                "Vui lòng xác nhận khách đồng ý cung cấp thông tin CCCD",
                Toast.LENGTH_SHORT
            ).show()
            valid = false
        }

        if (!valid) return

        val relation = when (binding.chipGroupRelation.checkedChipId) {
            R.id.chipRelative -> "RELATIVE"
            R.id.chipOther -> "OTHER"
            else -> "FRIEND"
        }

        val req = CreateGuestStayRequest(
            guestFullName = name,
            guestIdentityNumber = idNum,
            guestDateOfBirth = selectedDobIso,
            guestGender = gender,
            guestPhone = phone,
            relationship = relation,
            stayStartAt = apiDateTimeFormat.format(startCal.time),
            stayEndAt = apiDateTimeFormat.format(endCal.time),
            note = note,
            consentConfirmed = true,
            qrVerified = qrVerified
        )
        viewModel.createGuestStay(req)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
