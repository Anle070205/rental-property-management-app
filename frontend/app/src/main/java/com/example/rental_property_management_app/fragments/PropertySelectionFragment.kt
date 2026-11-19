package com.example.rental_property_management_app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.adapters.PropertyAdapter
import com.example.rental_property_management_app.databinding.FragmentPropertySelectionBinding
import com.example.rental_property_management_app.models.PropertyResponse
import com.example.rental_property_management_app.viewmodels.PropertyViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class PropertySelectionFragment : Fragment() {

    private var _binding: FragmentPropertySelectionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PropertyViewModel by viewModels()
    private lateinit var propertyAdapter: PropertyAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPropertySelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        observeProperties()

        // Load properties
        viewModel.loadProperties()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        propertyAdapter = PropertyAdapter { property ->
            onPropertySelected(property)
        }

        binding.rvProperties.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = propertyAdapter
            setHasFixedSize(true)
        }
    }

    private fun observeProperties() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.propertiesState.collect { state ->
                when {
                    state.isLoading -> showLoading()
                    state.error != null -> showError(state.error)
                    state.data != null -> showProperties(state.data)
                }
            }
        }
    }

    private fun onPropertySelected(property: PropertyResponse) {
        val handle = findNavController().previousBackStackEntry?.savedStateHandle
        handle?.set("selected_property_id", property.id)
        handle?.set("selected_property_name", property.name)
        findNavController().navigateUp()
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.rvProperties.visibility = View.GONE
        binding.tvEmpty.visibility = View.GONE
    }

    private fun showProperties(properties: List<PropertyResponse>) {
        binding.progressBar.visibility = View.GONE

        if (properties.isEmpty()) {
            binding.rvProperties.visibility = View.GONE
            binding.tvEmpty.visibility = View.VISIBLE
        } else {
            binding.rvProperties.visibility = View.VISIBLE
            binding.tvEmpty.visibility = View.GONE
            propertyAdapter.submitList(properties)
        }
    }

    private fun showError(message: String) {
        binding.progressBar.visibility = View.GONE
        binding.rvProperties.visibility = View.GONE
        binding.tvEmpty.visibility = View.VISIBLE
        binding.tvEmpty.text = message
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
