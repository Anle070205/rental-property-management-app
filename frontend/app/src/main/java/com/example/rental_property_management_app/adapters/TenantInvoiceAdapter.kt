package com.example.rental_property_management_app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.databinding.ItemTenantInvoiceBinding
import com.example.rental_property_management_app.models.TenantInvoice
import com.example.rental_property_management_app.utils.CurrencyUtils

class TenantInvoiceAdapter(private val open: (Long) -> Unit) :
    ListAdapter<TenantInvoice, TenantInvoiceAdapter.Holder>(object : DiffUtil.ItemCallback<TenantInvoice>() {
        override fun areItemsTheSame(old: TenantInvoice, new: TenantInvoice) = old.id == new.id
        override fun areContentsTheSame(old: TenantInvoice, new: TenantInvoice) = old == new
    }) {
    class Holder(val binding: ItemTenantInvoiceBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, type: Int) =
        Holder(ItemTenantInvoiceBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val invoice = getItem(position)
        with(holder.binding) {
            code.text = invoice.invoiceCode
            month.text = root.context.getString(R.string.rc_invoice_month, invoice.billingMonth)
            status.text = root.resources.getStringArray(R.array.rc_invoice_status)[invoice.status.ordinal]
            amount.text = CurrencyUtils.formatVND(invoice.totalAmount)
            root.setOnClickListener { open(invoice.id) }
        }
    }
}
