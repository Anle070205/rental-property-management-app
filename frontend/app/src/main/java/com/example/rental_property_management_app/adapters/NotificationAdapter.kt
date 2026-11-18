package com.example.rental_property_management_app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.models.NotificationType
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.rental_property_management_app.databinding.ItemNotificationBinding
import com.example.rental_property_management_app.models.NotificationItem
import com.example.rental_property_management_app.utils.DateUtils

class NotificationAdapter(private val onClick: (NotificationItem) -> Unit) :
    ListAdapter<NotificationItem, NotificationAdapter.Holder>(object : DiffUtil.ItemCallback<NotificationItem>() {
        override fun areItemsTheSame(old: NotificationItem, new: NotificationItem) = old.id == new.id
        override fun areContentsTheSame(old: NotificationItem, new: NotificationItem) = old == new
    }) {
    class Holder(val binding: ItemNotificationBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = getItem(position)
        with(holder.binding) {
            title.text = item.title
            content.text = item.content
            date.text = DateUtils.parseApiDateTime(item.createdAt)?.let(DateUtils::formatDateTime)
            unread.isVisible = !item.isRead
            val (icon, background, tint) = when (item.type) {
                NotificationType.INVOICE -> Triple(R.drawable.ic_card, R.color.rc_icon_red_bg, R.color.rc_danger)
                NotificationType.CONTRACT -> Triple(R.drawable.ic_description, R.color.rc_icon_yellow_bg, R.color.rc_icon_yellow)
                NotificationType.INCIDENT, NotificationType.MAINTENANCE ->
                    Triple(R.drawable.ic_info, R.color.rc_icon_blue_bg, R.color.rc_icon_blue)
                NotificationType.PAYMENT -> Triple(R.drawable.ic_check, R.color.rc_tint, R.color.rc_brand)
                NotificationType.SYSTEM -> Triple(R.drawable.rc_bell, R.color.rc_tint, R.color.rc_brand)
            }
            typeIcon.setImageResource(icon)
            typeIcon.backgroundTintList = ContextCompat.getColorStateList(root.context, background)
            typeIcon.imageTintList = ContextCompat.getColorStateList(root.context, tint)
            root.setOnClickListener { onClick(item) }
        }
    }
}
