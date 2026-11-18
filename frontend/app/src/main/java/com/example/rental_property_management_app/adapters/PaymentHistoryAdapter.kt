package com.example.rental_property_management_app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.rental_property_management_app.databinding.ItemPaymentHistoryBinding
import com.example.rental_property_management_app.models.PaymentResponse
import com.example.rental_property_management_app.utils.CurrencyUtils

class PaymentHistoryAdapter :
    ListAdapter<PaymentResponse, PaymentHistoryAdapter.PaymentViewHolder>(PaymentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentViewHolder {
        val binding = ItemPaymentHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PaymentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PaymentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PaymentViewHolder(
        private val binding: ItemPaymentHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(payment: PaymentResponse) {
            binding.apply {
                tvPaymentAmount.text = "+ ${CurrencyUtils.formatVND(payment.amount)}"
                tvPaymentMethod.text = when (payment.paymentMethod) {
                    "CASH" -> "Tiền mặt"
                    "BANK_TRANSFER" -> "Chuyển khoản"
                    else -> payment.paymentMethod
                }
                tvPaidAt.text = "Thời gian: ${payment.paidAt.replace("T", " ").take(16)}"

                if (!payment.transactionCode.isNullOrBlank()) {
                    tvTransactionCode.visibility = View.VISIBLE
                    tvTransactionCode.text = "Mã giao dịch / biên nhận: ${payment.transactionCode}"
                } else {
                    tvTransactionCode.visibility = View.GONE
                }

                if (!payment.receiptNote.isNullOrBlank()) {
                    tvReceiptNote.visibility = View.VISIBLE
                    tvReceiptNote.text = "Ghi chú: ${payment.receiptNote}"
                } else {
                    tvReceiptNote.visibility = View.GONE
                }
            }
        }
    }

    class PaymentDiffCallback : DiffUtil.ItemCallback<PaymentResponse>() {
        override fun areItemsTheSame(oldItem: PaymentResponse, newItem: PaymentResponse): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PaymentResponse, newItem: PaymentResponse): Boolean {
            return oldItem == newItem
        }
    }
}
