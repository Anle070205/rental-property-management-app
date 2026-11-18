package com.example.rental_property_management_app.adapters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.databinding.ItemDebtBinding
import com.example.rental_property_management_app.models.RoomTenantDebtResponse
import com.example.rental_property_management_app.utils.CurrencyUtils

class DebtAdapter(
    private val onViewInvoiceClick: (RoomTenantDebtResponse) -> Unit,
    private val onViewHistoryClick: (RoomTenantDebtResponse) -> Unit,
    private val onContactClick: (RoomTenantDebtResponse) -> Unit
) : ListAdapter<RoomTenantDebtResponse, DebtAdapter.DebtViewHolder>(DebtDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DebtViewHolder {
        val binding = ItemDebtBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DebtViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DebtViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DebtViewHolder(
        private val binding: ItemDebtBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RoomTenantDebtResponse) {
            val ctx = binding.root.context
            binding.apply {
                val roomText = item.roomCode?.let { "Phòng $it" } ?: "Chưa rõ phòng"
                val tenantText = item.tenantName ?: "Chưa gắn người thuê"
                tvRoomAndTenant.text = "$roomText • $tenantText"

                val phoneText = item.tenantPhone ?: "Chưa có SĐT"
                tvPropertyAndPhone.text = "${item.propertyName ?: ""} • $phoneText"

                tvTotalDebt.text = CurrencyUtils.formatVND(item.totalDebt)
                tvInvoiceCounts.text = "${item.unpaidInvoicesCount} hóa đơn nợ (${item.overdueInvoicesCount} quá hạn)"
                tvBilledVsPaid.text = "Đã thu: ${CurrencyUtils.formatVND(item.totalPaid)} / ${CurrencyUtils.formatVND(item.totalBilled)}"

                if (item.maxDaysOverdue > 0) {
                    tvOverdueBadge.text = "Trễ ${item.maxDaysOverdue} ngày"
                    val isSevere = item.maxDaysOverdue > 7
                    tvOverdueBadge.setTextColor(
                        ContextCompat.getColor(ctx, if (isSevere) R.color.invoice_overdue else R.color.invoice_unpaid)
                    )
                    tvOverdueBadge.backgroundTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(ctx, if (isSevere) R.color.invoice_overdue_bg else R.color.invoice_unpaid_bg)
                    )
                } else {
                    tvOverdueBadge.text = "Chưa quá hạn"
                    tvOverdueBadge.setTextColor(ContextCompat.getColor(ctx, R.color.invoice_partial))
                    tvOverdueBadge.backgroundTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(ctx, R.color.invoice_partial_bg)
                    )
                }

                btnViewInvoice.setOnClickListener { onViewInvoiceClick(item) }
                btnViewHistory.setOnClickListener { onViewHistoryClick(item) }
                btnContactTenant.setOnClickListener { onContactClick(item) }
            }
        }
    }

    class DebtDiffCallback : DiffUtil.ItemCallback<RoomTenantDebtResponse>() {
        override fun areItemsTheSame(oldItem: RoomTenantDebtResponse, newItem: RoomTenantDebtResponse): Boolean {
            return oldItem.roomId == newItem.roomId && oldItem.tenantId == newItem.tenantId
        }

        override fun areContentsTheSame(oldItem: RoomTenantDebtResponse, newItem: RoomTenantDebtResponse): Boolean {
            return oldItem == newItem
        }
    }
}
