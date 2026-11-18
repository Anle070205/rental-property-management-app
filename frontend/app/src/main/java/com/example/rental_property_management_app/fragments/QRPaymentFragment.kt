package com.example.rental_property_management_app.fragments

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.databinding.FragmentQrPaymentBinding
import com.example.rental_property_management_app.utils.CurrencyUtils
import com.example.rental_property_management_app.utils.Resource
import com.example.rental_property_management_app.viewmodels.TenantPaymentQr
import com.example.rental_property_management_app.viewmodels.TenantPaymentViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class QRPaymentFragment : Fragment(R.layout.fragment_qr_payment) {
    private var _binding: FragmentQrPaymentBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val viewModel: TenantPaymentViewModel by viewModels()
    private val invoiceId get() = requireArguments().getLong("invoiceId")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentQrPaymentBinding.bind(view)
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.retry.setOnClickListener { viewModel.loadQr(invoiceId) }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.qr.collect { result ->
                binding.progress.isVisible = result is Resource.Loading
                binding.error.isVisible = result is Resource.Error
                binding.retry.isVisible = result is Resource.Error
                binding.content.isVisible = result is Resource.Success
                binding.error.text = result.message
                result.data?.let(::display)
            }
        }
        viewModel.loadQr(invoiceId)
    }

    private fun display(value: TenantPaymentQr) {
        val info = value.info
        binding.sample.isVisible = info.isSample
        binding.instructions.isVisible = !info.isSample
        binding.qr.isVisible = value.image != null
        binding.qr.setImageBitmap(value.image)
        binding.qrError.isVisible = value.image == null
        binding.bank.text = getString(R.string.rc_payment_bank, info.bankName)
        binding.account.text = getString(R.string.rc_payment_account, info.accountNumber)
        binding.amount.text = getString(R.string.rc_payment_amount, CurrencyUtils.formatVND(info.amount))
        binding.reference.text = getString(R.string.rc_payment_content, info.transferContent)
        binding.copyAccount.setOnClickListener { copy(R.string.rc_payment_copy_account, info.accountNumber) }
        binding.copyReference.setOnClickListener { copy(R.string.rc_payment_copy_content, info.transferContent) }
    }

    private fun copy(label: Int, value: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(getString(label), value))
        Snackbar.make(binding.root, R.string.rc_payment_copied, Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
