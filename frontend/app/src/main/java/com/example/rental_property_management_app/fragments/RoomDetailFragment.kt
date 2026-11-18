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
import com.example.rental_property_management_app.databinding.FragmentRoomDetailBinding
import com.example.rental_property_management_app.models.RoomResponse
import com.example.rental_property_management_app.utils.CurrencyUtils
import com.example.rental_property_management_app.utils.Resource
import com.example.rental_property_management_app.viewmodels.RoomViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class RoomDetailFragment : Fragment() {
    
    private var _binding: FragmentRoomDetailBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: RoomViewModel by viewModels()
    private val args: RoomDetailFragmentArgs by navArgs()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRoomDetailBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupClickListeners()
        observeRoomDetail()
        
        // Load room detail
        viewModel.loadRoomDetail(args.roomId)
    }
    
    private fun setupClickListeners() {
        binding.btnEdit.setOnClickListener {
            val action = RoomDetailFragmentDirections.actionRoomDetailToRoomForm(args.roomId)
            findNavController().navigate(action)
        }
        
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }
    
    private fun observeRoomDetail() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.roomDetailState.collectLatest { resource ->
                resource ?: return@collectLatest
                
                when (resource) {
                    is Resource.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.contentLayout.visibility = View.GONE
                    }
                    is Resource.Success -> {
                        binding.progressBar.visibility = View.GONE
                        binding.contentLayout.visibility = View.VISIBLE
                        displayRoomDetail(resource.data)
                    }
                    is Resource.Error -> {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(context, resource.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
    
    private fun displayRoomDetail(room: RoomResponse) {
        binding.apply {
            tvRoomCode.text = room.roomCode
            tvFloor.text = "Tầng ${room.floor}"
            tvArea.text = "${room.area} m²"
            tvRentPrice.text = CurrencyUtils.formatVND(room.rentPrice.toLong())
            tvDeposit.text = "Cọc: ${CurrencyUtils.formatVND(room.deposit.toLong())}"
            tvCapacity.text = "Sức chứa: ${room.capacity} người"
            tvStatus.text = room.status.getDisplayName()
            tvPropertyName.text = room.propertyName
            tvAmenities.text = room.amenities ?: "Chưa có thông tin"
            tvDescription.text = room.description ?: "Chưa có mô tả"
            
            // Tenant info
            if (!room.currentTenant.isNullOrEmpty()) {
                tvTenantInfo.text = "Khách thuê: ${room.currentTenant}"
            } else {
                tvTenantInfo.text = "Chưa có khách thuê"
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
