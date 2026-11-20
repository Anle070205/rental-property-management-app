package com.example.rental_property_management_app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.adapters.PropertyAdapter
import com.example.rental_property_management_app.databinding.FragmentPropertySelectionBinding
import com.example.rental_property_management_app.models.PropertyResponse
import com.example.rental_property_management_app.utils.Resource
import com.example.rental_property_management_app.viewmodels.PropertyViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class PropertySelectionFragment : Fragment() {

    private var _binding: FragmentPropertySelectionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PropertyViewModel by viewModels()
    private lateinit var propertyAdapter: PropertyAdapter
    private var selectMode: Boolean = true

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

        selectMode = arguments?.getBoolean("selectMode", true) ?: true

        setupToolbar()
        setupRecyclerView()
        setupListeners()
        observeProperties()
        observeSaveState()

        viewModel.loadProperties()
    }

    private fun setupToolbar() {
        binding.toolbar.title = if (selectMode) {
            getString(R.string.select_property)
        } else {
            getString(R.string.rc_my_properties)
        }
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        propertyAdapter = PropertyAdapter(
            onItemClick = { property ->
                if (selectMode) {
                    onPropertySelected(property)
                } else {
                    showPropertyDialog(property)
                }
            },
            onEditClick = { property ->
                showPropertyDialog(property)
            }
        )

        binding.rvProperties.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = propertyAdapter
        }
    }

    private fun setupListeners() {
        binding.fabAddProperty.setOnClickListener {
            showPropertyDialog(null)
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

    private fun observeSaveState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.saveState.collect { result ->
                result ?: return@collect
                when (result) {
                    is Resource.Loading -> Unit
                    is Resource.Success -> {
                        Snackbar.make(binding.root, getString(R.string.rc_property_saved), Snackbar.LENGTH_SHORT).show()
                        viewModel.consumeSaveState()
                    }
                    is Resource.Error -> {
                        Snackbar.make(binding.root, result.message ?: getString(R.string.error_server), Snackbar.LENGTH_LONG).show()
                        viewModel.consumeSaveState()
                    }
                }
            }
        }
    }

    private fun showPropertyDialog(existing: PropertyResponse?) {
        val ctx = requireContext()
        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 8)
        }

        val tilName = TextInputLayout(ctx).apply {
            hint = getString(R.string.property_name)
        }
        val etName = TextInputEditText(ctx).apply {
            setText(existing?.name.orEmpty())
        }
        tilName.addView(etName)
        container.addView(tilName)

        val tilAddress = TextInputLayout(ctx).apply {
            hint = getString(R.string.property_address)
            setPadding(0, 16, 0, 0)
        }
        val etAddress = TextInputEditText(ctx).apply {
            setText(existing?.address.orEmpty())
        }
        tilAddress.addView(etAddress)
        container.addView(tilAddress)

        val tilDesc = TextInputLayout(ctx).apply {
            hint = getString(R.string.description_optional)
            setPadding(0, 16, 0, 0)
        }
        val etDesc = TextInputEditText(ctx).apply {
            setText(existing?.description.orEmpty())
        }
        tilDesc.addView(etDesc)
        container.addView(tilDesc)

        val dialog = MaterialAlertDialogBuilder(ctx)
            .setTitle(if (existing == null) R.string.rc_add_property else R.string.rc_edit_property)
            .setView(container)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save, null)
            .create()

        dialog.show()
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val name = etName.text?.toString()?.trim().orEmpty()
            val address = etAddress.text?.toString()?.trim().orEmpty()
            val desc = etDesc.text?.toString()?.trim()?.ifEmpty { null }
            var ok = true
            if (name.isEmpty()) {
                tilName.error = getString(R.string.rc_required)
                ok = false
            } else {
                tilName.error = null
            }
            if (address.isEmpty()) {
                tilAddress.error = getString(R.string.rc_required)
                ok = false
            } else {
                tilAddress.error = null
            }
            if (!ok) return@setOnClickListener

            if (existing == null) {
                viewModel.createProperty(name, address, desc)
            } else {
                viewModel.updateProperty(existing.id, name, address, desc)
            }
            dialog.dismiss()
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
