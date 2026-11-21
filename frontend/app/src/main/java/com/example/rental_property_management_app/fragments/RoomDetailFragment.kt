package com.example.rental_property_management_app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.databinding.FragmentRoomDetailBinding
import com.example.rental_property_management_app.models.RoomResponse
import com.example.rental_property_management_app.utils.CurrencyUtils
import com.example.rental_property_management_app.viewmodels.RoomViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class RoomDetailFragment : Fragment() {
    
    private var _binding: FragmentRoomDetailBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: RoomViewModel by viewModels()
    private val args: RoomDetailFragmentArgs by navArgs()
    private var currentRoom: RoomResponse? = null
    
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

    override fun onResume() {
        super.onResume()
        viewModel.loadRoomDetail(args.roomId)
    }
    
    private fun setupClickListeners() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnEdit.setOnClickListener {
            val action = RoomDetailFragmentDirections.actionRoomDetailToRoomForm(args.roomId)
            findNavController().navigate(action)
        }

        binding.btnHealthScore.setOnClickListener {
            val bundle = Bundle().apply { putLong("roomId", args.roomId) }
            findNavController().navigate(R.id.healthScoreFragment, bundle)
        }

        binding.btnAddTenant.setOnClickListener {
            findNavController().navigate(
                R.id.tenantFormFragment,
                bundleOf(
                    "propertyId" to (currentRoom?.propertyId ?: 0L),
                    "roomId" to args.roomId,
                    "roomCode" to (currentRoom?.roomCode ?: "")
                )
            )
        }

        binding.btnManageRoommates.setOnClickListener {
            val contractId = currentRoom?.currentContractId ?: return@setOnClickListener
            findNavController().navigate(
                R.id.contractDetailFragment,
                bundleOf("contractId" to contractId)
            )
        }

        binding.btnRoomContracts.setOnClickListener {
            findNavController().navigate(R.id.contractListFragment, bundleOf("roomId" to args.roomId))
        }

        binding.btnRoomInvoices.setOnClickListener {
            findNavController().navigate(R.id.invoiceFragment, bundleOf("roomId" to args.roomId))
        }
        
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }
    
    private fun observeRoomDetail() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.roomDetailState.collectLatest { state ->
                when {
                    state.isLoading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.contentLayout.visibility = View.GONE
                    }
                    state.data != null -> {
                        binding.progressBar.visibility = View.GONE
                        binding.contentLayout.visibility = View.VISIBLE
                        displayRoomDetail(state.data)
                    }
                    state.error != null -> {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(context, state.error, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
    
    private fun displayRoomDetail(room: RoomResponse) {
        currentRoom = room
        binding.apply {
            tvRoomCode.text = room.roomCode
            tvFloor.text = "Tầng ${room.floor}"
            tvArea.text = "${room.area} m²"
            tvRentPrice.text = CurrencyUtils.formatVND(room.rentPrice.toLong())
            tvHealthScoreBadge.text = "Health Score: ${room.healthScore}/100"
            tvDeposit.text = "Cọc: ${CurrencyUtils.formatVND(room.deposit.toLong())}"
            tvCapacity.text = "Đang ở: ${room.currentOccupants}/${room.capacity} người"
            tvStatus.text = room.status.getDisplayName()
            tvPropertyName.text = room.propertyName
            tvAmenities.text = room.amenities ?: "Chưa có thông tin"
            val fees = listOfNotNull(
                room.electricityPrice?.let { "Điện: ${CurrencyUtils.formatVND(it)}/kWh" },
                room.waterPrice?.let { "Nước: ${CurrencyUtils.formatVND(it)}/m³" },
                room.internetFee?.let { "Wifi: ${CurrencyUtils.formatVND(it)}" },
                room.serviceFee?.let { "Dịch vụ: ${CurrencyUtils.formatVND(it)}" }
            )
            tvUtilityRates.text = if (fees.isNotEmpty()) fees.joinToString(" • ") else "Chưa khai báo đơn giá dịch vụ"
            tvDescription.text = room.description ?: "Chưa có mô tả"

            if (!room.currentTenant.isNullOrEmpty()) {
                tvTenantInfo.text = "Khách thuê chính: ${room.currentTenant}"
            } else {
                tvTenantInfo.text = "Chưa có khách thuê"
            }

            tvContractSummary.text = if (!room.currentContract.isNullOrBlank()) {
                val statusVi = when (room.currentContractStatus?.uppercase()) {
                    "ACTIVE" -> "Đang hiệu lực"
                    "SIGNED" -> "Đã ký"
                    "DRAFT" -> "Nháp"
                    "EXPIRED" -> "Hết hạn"
                    "TERMINATED" -> "Đã kết thúc"
                    else -> room.currentContractStatus
                }
                val suffix = statusVi?.takeIf { !room.currentContract.contains(it, ignoreCase = true) }
                    ?.let { " ($it)" } ?: ""
                "Hợp đồng hiện tại: ${room.currentContract}$suffix"
            } else {
                "Chưa có hợp đồng đang gắn với phòng"
            }

            tvRoommatesInfo.isVisible = room.currentRoommates.isNotEmpty()
            tvRoommatesInfo.text = if (room.currentRoommates.isNotEmpty()) {
                "Người ở cùng: ${room.currentRoommates.joinToString(", ")}"
            } else ""

            tvOccupancyNotice.isVisible = !room.occupancyMessage.isNullOrBlank()
            tvOccupancyNotice.text = room.occupancyMessage.orEmpty()

            btnAddTenant.isVisible = room.canAddPrimaryTenant
            btnManageRoommates.isVisible = room.canAddRoommate && room.currentContractId != null
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
