package com.example.rental_property_management_app.fragments

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.databinding.FragmentIncidentDetailBinding
import com.example.rental_property_management_app.models.Incident
import com.example.rental_property_management_app.models.IncidentStatus
import com.example.rental_property_management_app.utils.DateUtils
import com.example.rental_property_management_app.utils.Resource
import com.example.rental_property_management_app.viewmodels.IncidentViewModel
import kotlinx.coroutines.launch

class IncidentDetailFragment : Fragment(R.layout.fragment_incident_detail) {
    private var _binding: FragmentIncidentDetailBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val viewModel: IncidentViewModel by viewModels()
    private val incidentId get() = requireArguments().getLong("incidentId")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentIncidentDetailBinding.bind(view)
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.retry.setOnClickListener { viewModel.loadDetail(incidentId) }
        binding.supplement.setOnClickListener {
            findNavController().navigate(R.id.incidentFormFragment, bundleOf("incidentId" to incidentId))
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.detail.collect { result ->
                binding.progress.isVisible = result is Resource.Loading
                binding.error.isVisible = result is Resource.Error
                binding.retry.isVisible = result is Resource.Error
                binding.error.text = result.message
                binding.content.isVisible = result is Resource.Success
                result.data?.let(::display)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadDetail(incidentId)
    }

    private fun display(incident: Incident) {
        binding.title.text = incident.title
        binding.description.text = incident.description
        binding.type.text = resources.getStringArray(R.array.rc_incident_types)[incident.type.ordinal]
        binding.priority.text = resources.getStringArray(R.array.rc_incident_priorities)[incident.priority.ordinal]
        binding.created.text = getString(R.string.rc_incident_created, date(incident.createdAt))
        binding.updated.text = getString(R.string.rc_incident_updated, date(incident.updatedAt))
        binding.image.isVisible = !incident.imageUrl.isNullOrBlank()
        binding.image.text = incident.imageUrl
        binding.response.text = incident.landlordResponse ?: getString(R.string.rc_no_landlord_response)
        binding.steps.text = resources.getStringArray(R.array.rc_incident_status)
            .mapIndexed { index, label ->
                getString(if (index <= incident.status.ordinal) R.string.rc_incident_step_done else R.string.rc_incident_step_pending, label)
            }.joinToString("\n")
        binding.supplement.isVisible = incident.status != IncidentStatus.DONE
    }

    private fun date(value: String) = DateUtils.parseApiDateTime(value)?.let(DateUtils::formatDateTime) ?: value

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
