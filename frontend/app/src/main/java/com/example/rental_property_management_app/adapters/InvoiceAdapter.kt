package com.example.rental_property_management_app.adapters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.rental_property_management_app.databinding.ItemInvoiceBinding
import com.example.rental_property_management_app.models.InvoiceResponse
import com.example.rental_property_management_app.utils.CurrencyUtils

class InvoiceAdapter(
    private val onInvoiceClick: (InvoiceResponse) -> Unit
) : ListAdapter<InvoiceResponse, InvoiceAdapter.InvoiceViewHolder>(InvoiceDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InvoiceViewHolder {
        val binding = ItemInvoiceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return InvoiceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: InvoiceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class InvoiceViewHolder(
        private val binding: ItemInvoiceBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(invoice: InvoiceResponse) {
            val ctx = binding.root.context
            binding.apply {
                tvRoomAndMonth.text = "Phòng ${invoice.roomCode} • Kỳ ${formatBillingMonth(invoice.billingMonth)}"
                tvPropertyAndCode.text = "${invoice.propertyName} • ${invoice.invoiceCode}"
                tvTenantName.text = if (!invoice.tenantName.isNullOrBlank()) {
                    "Người thuê: ${invoice.tenantName}"
                } else {
                    "Chưa gắn người thuê"
                }

                val overdueText = if (invoice.daysOverdue > 0 && invoice.remainingAmount.signum() > 0) {
                    " (Trễ ${invoice.daysOverdue} ngày)"
                } else {
                    ""
                }
                tvDueDate.text = "Hạn: ${formatDateDisplay(invoice.dueDate)}$overdueText"

                tvTotalAmount.text = CurrencyUtils.formatVND(invoice.totalAmount)
                tvPaidAmount.text = CurrencyUtils.formatVND(invoice.paidAmount)
                tvRemainingAmount.text = CurrencyUtils.formatVND(invoice.remainingAmount)

                tvStatusBadge.text = invoice.status.getDisplayName()
                tvStatusBadge.setTextColor(ContextCompat.getColor(ctx, invoice.status.getTextColorRes()))
                tvStatusBadge.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(ctx, invoice.status.getBgColorRes())
                )

                root.setOnClickListener { onInvoiceClick(invoice) }
            }
        }

        private fun formatBillingMonth(month: String): String {
            val parts = month.split("-")
            return if (parts.size == 2) "${parts[1]}/${parts[0]}" else month
        }

        private fun formatDateDisplay(isoDate: String): String {
            val parts = isoDate.split("-")
            return if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else isoDate
        }
    }

    class InvoiceDiffCallback : DiffUtil.ItemCallback<InvoiceResponse>() {
        override fun areItemsTheSame(oldItem: InvoiceResponse, newItem: InvoiceResponse): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: InvoiceResponse, newItem: InvoiceResponse): Boolean {
            return oldItem == newItem
        }
    }
}
