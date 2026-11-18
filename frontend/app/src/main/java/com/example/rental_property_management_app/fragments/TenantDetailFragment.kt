package com.example.rental_property_management_app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.rental_property_management_app.databinding.FragmentTenantDetailBinding
import com.example.rental_property_management_app.models.TenantResponse
import com.example.rental_property_management_app.utils.DateUtils
import com.example.rental_property_management_app.utils.Resource
import com.example.rental_property_management_app.viewmodels.TenantViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TenantDetailFragment : Fragment() {
    
    private var _binding: FragmentTenantDetailBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: TenantViewModel by viewModels()
    private val args: TenantDetailFragmentArgs by navArgs()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTenantDetailBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupClickListeners()
        observeTenantDetail()
        
        viewModel.loadTenantDetail(args.tenantId)
    }
    
    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }
    
    private fun observeTenantDetail() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.tenantDetailState.collectLatest { resource ->
                resource ?: return@collectLatest
                
                when (resource) {
                    is Resource.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.contentLayout.visibility = View.GONE
                    }
                    is Resource.Success -> {
                        binding.progressBar.visibility = View.GONE
                        binding.contentLayout.visibility = View.VISIBLE
                        displayTenantDetail(resource.data)
                    }
                    is Resource.Error -> {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(context, resource.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
    
    private fun displayTenantDetail(tenant: TenantResponse) {
        binding.apply {
            tvTenantName.text = tenant.fullName
            tvPhone.text = tenant.phone
            tvEmail.text = tenant.email ?: "Chưa có email"
            tvIdentity.text = "CMND/CCCD: ${tenant.identityNumber}"
            tvDateOfBirth.text = "Ngày sinh: ${tenant.dateOfBirth ?: "Chưa có"}"
            tvAddress.text = "Địa chỉ: ${tenant.address ?: "Chưa có"}"
            tvEmergencyContact.text = "Liên hệ khẩn cấp: ${tenant.emergencyContact ?: "Chưa có"}"
            tvPropertyName.text = "Nhà trọ: ${tenant.propertyName}"
            tvCurrentRoom.text = "Phòng: ${tenant.currentRoom ?: "Chưa thuê phòng"}"
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
