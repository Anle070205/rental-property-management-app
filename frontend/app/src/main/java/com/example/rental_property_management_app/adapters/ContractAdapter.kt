package com.example.rental_property_management_app.adapters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.rental_property_management_app.databinding.ItemContractBinding
import com.example.rental_property_management_app.models.ContractResponse
import com.example.rental_property_management_app.utils.CurrencyUtils

class ContractAdapter(
    private val onItemClick: (ContractResponse) -> Unit
) : ListAdapter<ContractResponse, ContractAdapter.ContractViewHolder>(ContractDiff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContractViewHolder {
        val binding = ItemContractBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ContractViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContractViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ContractViewHolder(
        private val binding: ItemContractBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(contract: ContractResponse) {
            val ctx = binding.root.context
            binding.apply {
                tvContractCode.text = contract.contractCode
                tvStatusBadge.text = contract.status.getDisplayName()
                tvStatusBadge.setTextColor(ContextCompat.getColor(ctx, contract.status.getTextColorRes()))
                tvStatusBadge.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(ctx, contract.status.getBgColorRes())
                )
                tvRoomProperty.text = "Phòng ${contract.roomCode} • ${contract.propertyName}"
                val accLabel = when (contract.tenantAccountStatus) {
                    "ACTIVE" -> "TK đã kích hoạt"
                    "PENDING_ACTIVATION" -> "Chờ kích hoạt"
                    else -> "Chưa cấp TK"
                }
                tvTenantName.text = "Khách đại diện: ${contract.tenantName} ($accLabel)"
                tvPeriod.text = "Hạn: ${formatDate(contract.startDate)} → ${formatDate(contract.endDate)}"
                tvMonthlyRent.text = "${CurrencyUtils.formatVND(contract.monthlyRent)}/tháng"
                root.setOnClickListener { onItemClick(contract) }
            }
        }

        private fun formatDate(iso: String): String {
            val parts = iso.split("-")
            return if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else iso
        }
    }

    class ContractDiff : DiffUtil.ItemCallback<ContractResponse>() {
        override fun areItemsTheSame(oldItem: ContractResponse, newItem: ContractResponse) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: ContractResponse, newItem: ContractResponse) = oldItem == newItem
    }
}
