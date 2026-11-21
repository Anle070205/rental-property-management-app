package com.example.rental_property_management_app.fragments

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.RoomCareApplication
import com.example.rental_property_management_app.databinding.FragmentIncidentDetailBinding
import com.example.rental_property_management_app.models.Incident
import com.example.rental_property_management_app.models.IncidentStatus
import com.example.rental_property_management_app.models.UpdateIncidentStatusRequest
import com.example.rental_property_management_app.utils.DateUtils
import com.example.rental_property_management_app.utils.Resource
import com.example.rental_property_management_app.viewmodels.IncidentViewModel
import kotlinx.coroutines.launch

class IncidentDetailFragment : Fragment(R.layout.fragment_incident_detail) {
    private var _binding: FragmentIncidentDetailBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val viewModel: IncidentViewModel by viewModels()
    private val incidentId get() = requireArguments().getLong("incidentId")
    private var isLandlord: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentIncidentDetailBinding.bind(view)
        isLandlord = RoomCareApplication.prefsManager.getUserRole().equals("LANDLORD", ignoreCase = true)

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.retry.setOnClickListener { viewModel.loadDetail(incidentId, isLandlord) }
        binding.supplement.setOnClickListener {
            findNavController().navigate(R.id.incidentFormFragment, bundleOf("incidentId" to incidentId))
        }
        binding.btnMarkInProgress.setOnClickListener { updateStatus(IncidentStatus.IN_PROGRESS) }
        binding.btnMarkDone.setOnClickListener { updateStatus(IncidentStatus.DONE) }

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

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.save.collect { res ->
                when (res) {
                    is Resource.Success -> {
                        Toast.makeText(requireContext(), "Đã cập nhật trạng thái sự cố", Toast.LENGTH_SHORT).show()
                        viewModel.clearSaveState()
                    }
                    is Resource.Error -> {
                        Toast.makeText(requireContext(), res.message ?: "Không thể cập nhật", Toast.LENGTH_LONG).show()
                        viewModel.clearSaveState()
                    }
                    else -> Unit
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadDetail(incidentId, isLandlord)
    }

    private fun updateStatus(status: IncidentStatus) {
        val resp = binding.etLandlordResponse.text?.toString()?.trim()?.ifEmpty { null }
        viewModel.updateLandlordStatus(incidentId, UpdateIncidentStatusRequest(status, resp))
    }

    private fun display(incident: Incident) {
        binding.title.text = incident.title
        val roomInfo = listOfNotNull(
            incident.roomCode?.takeIf { it.isNotBlank() }?.let { "Phòng $it" },
            incident.tenantName?.takeIf { it.isNotBlank() }?.let { "Người gửi: $it" }
        )
        binding.roomMeta.isVisible = roomInfo.isNotEmpty()
        binding.roomMeta.text = roomInfo.joinToString(" · ")

        binding.description.text = incident.description
        binding.type.text = resources.getStringArray(R.array.rc_incident_types)[incident.type.ordinal]
        binding.priority.text = resources.getStringArray(R.array.rc_incident_priorities)[incident.priority.ordinal]
        binding.created.text = getString(R.string.rc_incident_created, date(incident.createdAt))
        binding.updated.text = getString(R.string.rc_incident_updated, date(incident.updatedAt))
        binding.image.isVisible = !incident.imageUrl.isNullOrBlank()
        binding.image.text = incident.imageUrl
        binding.response.text = incident.landlordResponse ?: getString(R.string.rc_no_landlord_response)

        val ctx = requireContext()
        val doneColor = ContextCompat.getColor(ctx, R.color.success)
        val activeColor = ContextCompat.getColor(ctx, R.color.warning)
        val mutedColor = ContextCompat.getColor(ctx, R.color.rc_muted)

        binding.stepReceivedPill.setBackgroundResource(R.drawable.rc_badge_success)
        binding.stepReceivedPill.setTextColor(doneColor)

        if (incident.status == IncidentStatus.IN_PROGRESS) {
            binding.stepInProgressPill.setBackgroundResource(R.drawable.rc_badge_warning)
            binding.stepInProgressPill.setTextColor(activeColor)
        } else if (incident.status == IncidentStatus.DONE) {
            binding.stepInProgressPill.setBackgroundResource(R.drawable.rc_badge_success)
            binding.stepInProgressPill.setTextColor(doneColor)
            binding.stepDonePill.setBackgroundResource(R.drawable.rc_badge_success)
            binding.stepDonePill.setTextColor(doneColor)
        } else {
            binding.stepInProgressPill.setBackgroundResource(R.drawable.rc_badge)
            binding.stepInProgressPill.setTextColor(mutedColor)
            binding.stepDonePill.setBackgroundResource(R.drawable.rc_badge)
            binding.stepDonePill.setTextColor(mutedColor)
        }

        binding.steps.text = resources.getStringArray(R.array.rc_incident_status)
            .mapIndexed { index, label ->
                getString(
                    if (index <= incident.status.ordinal) R.string.rc_incident_step_done else R.string.rc_incident_step_pending,
                    label
                )
            }.joinToString("\n")

        binding.supplement.isVisible = !isLandlord && incident.status != IncidentStatus.DONE
        binding.layoutLandlordControls.isVisible = isLandlord
        binding.btnMarkInProgress.isEnabled = incident.status != IncidentStatus.DONE
        binding.btnMarkDone.isEnabled = incident.status != IncidentStatus.DONE
        if (binding.etLandlordResponse.text.isNullOrBlank() && !incident.landlordResponse.isNullOrBlank()) {
            binding.etLandlordResponse.setText(incident.landlordResponse)
        }
    }

    private fun date(value: String) = DateUtils.parseApiDateTime(value)?.let(DateUtils::formatDateTime) ?: value

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
