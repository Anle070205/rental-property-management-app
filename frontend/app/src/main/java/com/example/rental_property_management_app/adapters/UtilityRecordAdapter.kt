package com.example.rental_property_management_app.adapters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.databinding.ItemUtilityRecordBinding
import com.example.rental_property_management_app.models.UtilityRecordResponse
import com.example.rental_property_management_app.utils.CurrencyUtils

class UtilityRecordAdapter(
    private val onEditClick: (UtilityRecordResponse) -> Unit
) : ListAdapter<UtilityRecordResponse, UtilityRecordAdapter.UtilityViewHolder>(UtilityDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UtilityViewHolder {
        val binding = ItemUtilityRecordBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return UtilityViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UtilityViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class UtilityViewHolder(
        private val binding: ItemUtilityRecordBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(record: UtilityRecordResponse) {
            val ctx = binding.root.context
            binding.apply {
                tvRoomAndMonth.text = "Phòng ${record.roomCode} • Kỳ ${formatMonth(record.billingMonth)}"
                tvPropertyName.text = record.propertyName

                val elecAdj = if (record.isAdjustment) " (Điều chỉnh)" else ""
                tvElectricDetail.text = "Điện$elecAdj: ${record.prevElectricIndex.stripTrailingZeros().toPlainString()} → " +
                    "${record.currElectricIndex.stripTrailingZeros().toPlainString()} " +
                    "(${record.electricUsage.stripTrailingZeros().toPlainString()} kWh × " +
                    "${CurrencyUtils.formatVND(record.electricUnitPrice)}) = ${CurrencyUtils.formatVND(record.electricityAmount)}"

                tvWaterDetail.text = "Nước: ${record.prevWaterIndex.stripTrailingZeros().toPlainString()} → " +
                    "${record.currWaterIndex.stripTrailingZeros().toPlainString()} " +
                    "(${record.waterUsage.stripTrailingZeros().toPlainString()} m³ × " +
                    "${CurrencyUtils.formatVND(record.waterUnitPrice)}) = ${CurrencyUtils.formatVND(record.waterAmount)}"

                tvServiceDetail.text = "Internet: ${CurrencyUtils.formatVND(record.internetFee)} • Dịch vụ: ${CurrencyUtils.formatVND(record.serviceFee)}"
                tvTotalUtility.text = "Tổng: ${CurrencyUtils.formatVND(record.totalUtilityAmount)}"

                if (!record.historyLog.isNullOrBlank()) {
                    tvHistorySummary.visibility = View.VISIBLE
                    tvHistorySummary.text = "Lịch sử: ${record.historyLog.lines().lastOrNull().orEmpty()}"
                } else {
                    tvHistorySummary.visibility = View.GONE
                }

                if (record.invoiceLocked) {
                    tvInvoiceLockBadge.text = "Đã chốt (${record.linkedInvoiceCode ?: "PAID"})"
                    tvInvoiceLockBadge.setTextColor(ContextCompat.getColor(ctx, R.color.invoice_paid))
                    tvInvoiceLockBadge.backgroundTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(ctx, R.color.invoice_paid_bg)
                    )
                    btnEditRecord.isEnabled = false
                    btnEditRecord.alpha = 0.4f
                } else {
                    tvInvoiceLockBadge.text = if (record.linkedInvoiceCode != null) {
                        "Gắn ${record.linkedInvoiceCode} (Chưa chốt)"
                    } else {
                        "Chưa lập hóa đơn"
                    }
                    tvInvoiceLockBadge.setTextColor(ContextCompat.getColor(ctx, R.color.invoice_partial))
                    tvInvoiceLockBadge.backgroundTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(ctx, R.color.invoice_partial_bg)
                    )
                    btnEditRecord.isEnabled = true
                    btnEditRecord.alpha = 1.0f
                }

                btnEditRecord.setOnClickListener {
                    if (!record.invoiceLocked) {
                        onEditClick(record)
                    }
                }
            }
        }

        private fun formatMonth(month: String): String {
            val parts = month.split("-")
            return if (parts.size == 2) "${parts[1]}/${parts[0]}" else month
        }
    }

    class UtilityDiffCallback : DiffUtil.ItemCallback<UtilityRecordResponse>() {
        override fun areItemsTheSame(oldItem: UtilityRecordResponse, newItem: UtilityRecordResponse): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: UtilityRecordResponse, newItem: UtilityRecordResponse): Boolean {
            return oldItem == newItem
        }
    }
}
