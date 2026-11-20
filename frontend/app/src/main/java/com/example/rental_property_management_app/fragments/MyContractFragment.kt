package com.example.rental_property_management_app.fragments

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.databinding.FragmentMyContractBinding
import com.example.rental_property_management_app.models.MyContract
import com.example.rental_property_management_app.utils.Constants
import com.example.rental_property_management_app.utils.CurrencyUtils
import com.example.rental_property_management_app.utils.DateUtils
import com.example.rental_property_management_app.utils.Resource
import com.example.rental_property_management_app.viewmodels.MyContractViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class MyContractFragment : Fragment(R.layout.fragment_my_contract) {
    private var _binding: FragmentMyContractBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val viewModel: MyContractViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentMyContractBinding.bind(view)
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.retry.setOnClickListener { viewModel.load() }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.contract.collect { result ->
                val contract = result.data
                binding.progress.isVisible = result is Resource.Loading
                binding.retry.isVisible = result is Resource.Error
                binding.status.text = result.message ?: getString(R.string.rc_no_contract)
                binding.status.isVisible = result is Resource.Error || (result is Resource.Success && contract?.id == null)
                binding.content.isVisible = contract?.id != null
                if (contract?.id != null) display(contract)
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.renewal.collect { result ->
                binding.renew.isEnabled = result !is Resource.Loading && result !is Resource.Success
                binding.renewalStatus.isVisible = result is Resource.Success
                if (result is Resource.Error) {
                    Snackbar.make(view, result.message.orEmpty(), Snackbar.LENGTH_LONG).show()
                    viewModel.clearRenewalError()
                }
            }
        }
    }

    private fun display(contract: MyContract) {
        binding.sample.isVisible = Constants.USE_MOCK_TENANT_DATA
        val status = when (contract.status) {
            "DRAFT" -> "Bản nháp"
            "SIGNED" -> "Đã ký (Chờ hiệu lực)"
            "ACTIVE" -> getString(R.string.rc_contract_active)
            "EXPIRED" -> getString(R.string.rc_contract_expired)
            "TERMINATED" -> getString(R.string.rc_contract_terminated)
            "PENDING" -> getString(R.string.rc_contract_pending)
            else -> contract.status.orEmpty()
        }
        binding.contractStatus.text = getString(R.string.rc_contract_status, status)
        val metaParts = listOfNotNull(
            contract.contractCode?.let { "Mã HĐ: $it" },
            contract.roomCode?.let { rc ->
                val prop = contract.propertyName?.let { " • $it" } ?: ""
                "Phòng $rc$prop"
            },
            contract.landlordName?.let { ln ->
                val phone = contract.landlordPhone?.let { " ($it)" } ?: ""
                "Chủ trọ: $ln$phone"
            }
        )
        binding.tvContractMeta.text = metaParts.joinToString("\n")
        binding.tvContractMeta.isVisible = metaParts.isNotEmpty()

        binding.startDate.text = getString(R.string.rc_contract_start, date(contract.startDate))
        binding.endDate.text = getString(R.string.rc_contract_end, date(contract.endDate))
        binding.rent.text = getString(R.string.rc_room_rent, contract.rentPrice?.let(CurrencyUtils::formatVND).orEmpty())
        binding.deposit.text = getString(R.string.rc_contract_deposit, contract.deposit?.let(CurrencyUtils::formatVND).orEmpty())

        val rateLines = listOfNotNull(
            contract.electricityUnitPrice?.let { "• Đơn giá điện: ${CurrencyUtils.formatVND(it)}/kWh" },
            contract.waterUnitPrice?.let {
                val mode = when (contract.waterBillingMode) {
                    "FIXED" -> "Khoán tháng"
                    "PER_PERSON" -> "Theo đầu người"
                    else -> "Theo m³"
                }
                "• Đơn giá nước ($mode): ${CurrencyUtils.formatVND(it)}"
            },
            contract.internetFee?.let { "• Internet: ${CurrencyUtils.formatVND(it)}" },
            contract.garbageFee?.let { "• Phí rác: ${CurrencyUtils.formatVND(it)}" },
            contract.serviceFee?.let { "• Dịch vụ chung: ${CurrencyUtils.formatVND(it)}" },
            contract.paymentDayOfMonth?.let { "• Hạn thanh toán hàng tháng: Ngày $it" }
        )
        binding.tvContractRates.isVisible = rateLines.isNotEmpty()
        binding.tvContractRates.text = rateLines.joinToString("\n")

        val hasRoommates = contract.roommates.isNotEmpty()
        binding.tvRoommatesHeader.isVisible = hasRoommates
        binding.tvContractRoommates.isVisible = hasRoommates
        if (hasRoommates) {
            binding.tvContractRoommates.text = contract.roommates.joinToString("\n") { rm ->
                val rel = rm.relationship?.let { " ($it)" } ?: ""
                val phone = rm.phone?.takeIf { it.isNotBlank() }?.let { " - $it" } ?: ""
                "• ${rm.fullName}$rel$phone"
            }
        }

        binding.terms.text = contract.terms
        binding.renew.isVisible = contract.status == "ACTIVE" && contract.id != null
        binding.renew.setOnClickListener {
            val id = contract.id ?: return@setOnClickListener
            MaterialAlertDialogBuilder(requireContext()).setMessage(R.string.rc_contract_confirm_renew)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.rc_contract_renew) { _, _ -> viewModel.requestRenewal(id) }
                .show()
        }
    }

    private fun date(value: String?) = value?.let { DateUtils.parseApiDate(it)?.let(DateUtils::formatDate) ?: it }.orEmpty()

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
