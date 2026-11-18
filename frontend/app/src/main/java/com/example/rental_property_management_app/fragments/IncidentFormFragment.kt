package com.example.rental_property_management_app.fragments

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.databinding.FragmentIncidentFormBinding
import com.example.rental_property_management_app.models.*
import com.example.rental_property_management_app.utils.Resource
import com.example.rental_property_management_app.utils.populateRoomCareChoices
import com.example.rental_property_management_app.utils.selectedRoomCareChoice
import com.example.rental_property_management_app.viewmodels.IncidentViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class IncidentFormFragment : Fragment(R.layout.fragment_incident_form) {
    private var _binding: FragmentIncidentFormBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val viewModel: IncidentViewModel by viewModels()
    private val incidentId get() = arguments?.getLong("incidentId") ?: 0L

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentIncidentFormBinding.bind(view)
        val supplement = incidentId != 0L
        binding.toolbar.setTitle(if (supplement) R.string.rc_supplement_incident else R.string.rc_report_incident)
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.initialFields.isVisible = !supplement
        binding.type.populateRoomCareChoices(resources.getStringArray(R.array.rc_incident_types),
            savedInstanceState?.getInt(TYPE) ?: 0)
        binding.priority.populateRoomCareChoices(resources.getStringArray(R.array.rc_incident_priorities),
            savedInstanceState?.getInt(PRIORITY) ?: IncidentPriority.MEDIUM.ordinal)
        binding.submit.setOnClickListener { submit(supplement) }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.save.collect { result ->
                binding.progress.isVisible = result is Resource.Loading
                binding.submit.isEnabled = result !is Resource.Loading
                binding.error.isVisible = result is Resource.Error
                binding.error.text = result?.message
                if (result is Resource.Success) {
                    Snackbar.make(requireActivity().findViewById(android.R.id.content),
                        R.string.rc_incident_saved, Snackbar.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
            }
        }
    }

    private fun submit(supplement: Boolean) {
        val title = binding.title.text.toString().trim()
        val description = binding.description.text.toString().trim()
        val image = binding.image.text.toString().trim().ifBlank { null }
        if (image != null && !Regex("https?://\\S+").matches(image)) {
            binding.image.error = getString(R.string.rc_incident_invalid_link)
            return
        }
        if (supplement) {
            if (description.isBlank() && image == null) {
                binding.description.error = getString(R.string.rc_incident_requires_supplement)
                return
            }
            viewModel.supplement(incidentId, UpdateIncidentRequest(description.ifBlank { null }, image))
        } else {
            if (title.isBlank() || description.isBlank()) {
                if (title.isBlank()) binding.title.error = getString(R.string.rc_required)
                if (description.isBlank()) binding.description.error = getString(R.string.rc_required)
                return
            }
            viewModel.create(CreateIncidentRequest(
                IncidentType.entries[binding.type.selectedRoomCareChoice()], title, description,
                IncidentPriority.entries[binding.priority.selectedRoomCareChoice()], image
            ))
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        _binding?.let {
            outState.putInt(TYPE, it.type.selectedRoomCareChoice())
            outState.putInt(PRIORITY, it.priority.selectedRoomCareChoice())
        }
        super.onSaveInstanceState(outState)
    }

    private companion object {
        const val TYPE = "incident_type"
        const val PRIORITY = "incident_priority"
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
