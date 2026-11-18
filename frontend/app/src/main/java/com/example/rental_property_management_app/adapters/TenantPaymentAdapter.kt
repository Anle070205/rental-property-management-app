package com.example.rental_property_management_app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.databinding.ItemTenantPaymentBinding
import com.example.rental_property_management_app.models.TenantPayment
import com.example.rental_property_management_app.utils.CurrencyUtils
import com.example.rental_property_management_app.utils.DateUtils

class TenantPaymentAdapter :
    ListAdapter<TenantPayment, TenantPaymentAdapter.Holder>(object : DiffUtil.ItemCallback<TenantPayment>() {
        override fun areItemsTheSame(old: TenantPayment, new: TenantPayment) = old.id == new.id
        override fun areContentsTheSame(old: TenantPayment, new: TenantPayment) = old == new
    }) {
    class Holder(val binding: ItemTenantPaymentBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, type: Int) =
        Holder(ItemTenantPaymentBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = getItem(position)
        with(holder.binding) {
            val context = root.context
            amount.text = CurrencyUtils.formatVND(item.amount)
            time.text = item.paidAt?.let { DateUtils.parseApiDateTime(it)?.let(DateUtils::formatDateTime) ?: it }
                ?: context.getString(R.string.rc_payment_time_missing)
            method.text = when (item.method) {
                "CASH" -> context.getString(R.string.rc_payment_method_cash)
                "BANK_TRANSFER", "TRANSFER" -> context.getString(R.string.rc_payment_method_transfer)
                else -> item.method
            }
            status.text = when (item.status) {
                "CONFIRMED" -> context.getString(R.string.rc_payment_status_confirmed)
                "PENDING" -> context.getString(R.string.rc_payment_status_pending)
                "FAILED" -> context.getString(R.string.rc_payment_status_failed)
                else -> item.status
            }
        }
    }
}
