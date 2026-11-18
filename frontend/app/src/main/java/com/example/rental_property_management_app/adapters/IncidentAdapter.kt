package com.example.rental_property_management_app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.databinding.ItemIncidentBinding
import com.example.rental_property_management_app.models.Incident

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
        holder.binding.title.text = incident.title
        holder.binding.status.text = holder.itemView.resources.getStringArray(R.array.rc_incident_status)[incident.status.ordinal]
        holder.itemView.setOnClickListener { open(incident.id) }
    }
}
