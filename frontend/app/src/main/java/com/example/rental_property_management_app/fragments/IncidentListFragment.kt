package com.example.rental_property_management_app.fragments

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.adapters.IncidentAdapter
import com.example.rental_property_management_app.databinding.FragmentIncidentListBinding
import com.example.rental_property_management_app.models.IncidentStatus
import com.example.rental_property_management_app.viewmodels.IncidentViewModel
import kotlinx.coroutines.launch

class IncidentListFragment : Fragment(R.layout.fragment_incident_list) {
    private var _binding: FragmentIncidentListBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val viewModel: IncidentViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentIncidentListBinding.bind(view)
        val nav = findNavController()
        val adapter = IncidentAdapter { nav.navigate(R.id.incidentDetailFragment, bundleOf("incidentId" to it)) }
        binding.items.layoutManager = LinearLayoutManager(requireContext())
        binding.items.adapter = adapter
        binding.toolbar.setNavigationOnClickListener { nav.navigateUp() }
        binding.create.setOnClickListener { nav.navigate(R.id.incidentFormFragment) }
        binding.more.setOnClickListener { viewModel.more() }
        binding.retry.setOnClickListener { refresh() }
        binding.swipe.setOnRefreshListener { refresh() }
        binding.filter.adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_dropdown_item,
            listOf(getString(R.string.rc_all_incidents)) + resources.getStringArray(R.array.rc_incident_status)
        )
        binding.filter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) = refresh()
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.list.collect { state ->
                adapter.submitList(state.items)
                binding.swipe.isRefreshing = state.loading
                binding.more.isVisible = !state.loading && state.nextPage != null
                binding.retry.isVisible = state.error != null
                binding.status.isVisible = !state.loading && (state.error != null || state.items.isEmpty())
                binding.status.text = state.error ?: getString(R.string.rc_no_incidents)
            }
        }
    }

    private fun refresh() = viewModel.loadList(IncidentStatus.entries.getOrNull(binding.filter.selectedItemPosition - 1))

    override fun onDestroyView() {
        binding.items.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
