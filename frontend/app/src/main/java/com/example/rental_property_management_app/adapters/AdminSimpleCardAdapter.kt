package com.example.rental_property_management_app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.databinding.ItemAdminSimpleCardBinding

data class AdminCardRow(
    val id: Long,
    val badgeTop: String? = null,
    val title: String,
    val sub1: String,
    val sub2: String? = null,
    val leftTag: String,
    val rightTag: String,
    val isDangerRight: Boolean = false,
    val actionLabel: String? = null
)

class AdminSimpleCardAdapter(
    private val onClick: (AdminCardRow) -> Unit
) : RecyclerView.Adapter<AdminSimpleCardAdapter.VH>() {

    private val items = mutableListOf<AdminCardRow>()

    fun submitList(newItems: List<AdminCardRow>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemAdminSimpleCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val row = items[position]
        val b = holder.binding
        val ctx = b.root.context
        b.tvBadgeTop.isVisible = !row.badgeTop.isNullOrBlank()
        b.tvBadgeTop.text = row.badgeTop
        b.tvTitle.text = row.title
        b.tvSub1.text = row.sub1
        b.tvSub2.isVisible = !row.sub2.isNullOrBlank()
        b.tvSub2.text = row.sub2
        b.tvLeftTag.text = row.leftTag
        b.tvRightTag.text = row.rightTag
        b.tvRightTag.setTextColor(
            ContextCompat.getColor(ctx, if (row.isDangerRight) R.color.rc_danger else R.color.rc_brand)
        )
        b.btnCardAction.isVisible = !row.actionLabel.isNullOrBlank()
        b.btnCardAction.text = row.actionLabel
        b.root.setOnClickListener { onClick(row) }
        b.btnCardAction.setOnClickListener { onClick(row) }
    }

    override fun getItemCount(): Int = items.size

    class VH(val binding: ItemAdminSimpleCardBinding) : RecyclerView.ViewHolder(binding.root)
}
