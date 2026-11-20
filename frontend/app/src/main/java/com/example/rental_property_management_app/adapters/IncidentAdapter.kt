package com.example.rental_property_management_app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.databinding.ItemIncidentBinding
import com.example.rental_property_management_app.models.Incident
import com.example.rental_property_management_app.models.IncidentStatus
import com.example.rental_property_management_app.utils.DateUtils

class IncidentAdapter(private val open: (Long) -> Unit) :
    ListAdapter<Incident, IncidentAdapter.Holder>(object : DiffUtil.ItemCallback<Incident>() {
        override fun areItemsTheSame(old: Incident, new: Incident) = old.id == new.id
        override fun areContentsTheSame(old: Incident, new: Incident) = old == new
    }) {
    class Holder(val binding: ItemIncidentBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, type: Int) =
        Holder(ItemIncidentBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val incident = getItem(position)
        val ctx = holder.itemView.context
        val res = holder.itemView.resources
        holder.binding.title.text = incident.title

        val typeLabel = res.getStringArray(R.array.rc_incident_types).getOrElse(incident.type.ordinal) { incident.type.name }
        val priLabel = res.getStringArray(R.array.rc_incident_priorities).getOrElse(incident.priority.ordinal) { incident.priority.name }
        val metaParts = listOfNotNull(
            incident.roomCode?.takeIf { it.isNotBlank() }?.let { "Phòng $it" },
            incident.tenantName?.takeIf { it.isNotBlank() },
            typeLabel,
            priLabel
        )
        holder.binding.meta.text = metaParts.joinToString(" · ")
        holder.binding.preview.text = incident.description
        holder.binding.createdAt.text =
            DateUtils.parseApiDateTime(incident.createdAt)?.let(DateUtils::formatDateTime)
                ?: incident.createdAt.replace("T", " ").take(16)

        holder.binding.status.text = res.getStringArray(R.array.rc_incident_status)[incident.status.ordinal]
        val (badgeRes, colorRes, boxRes) = when (incident.status) {
            IncidentStatus.DONE -> Triple(R.drawable.rc_badge_success, R.color.success, R.drawable.rc_icon_box_tint)
            IncidentStatus.IN_PROGRESS -> Triple(R.drawable.rc_badge_warning, R.color.warning, R.drawable.rc_icon_box_warning)
            IncidentStatus.RECEIVED -> Triple(R.drawable.rc_badge, R.color.rc_brand, R.drawable.rc_icon_box_soft)
        }
        val color = ContextCompat.getColor(ctx, colorRes)
        holder.binding.status.setBackgroundResource(badgeRes)
        holder.binding.status.setTextColor(color)
        holder.binding.iconBox.setBackgroundResource(boxRes)
        holder.binding.iconView.setColorFilter(color)

        holder.itemView.setOnClickListener { open(incident.id) }
    }
}
