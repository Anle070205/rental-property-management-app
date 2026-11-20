package com.example.rental_property_management_app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.adapters.RoomAdapter
import com.example.rental_property_management_app.databinding.FragmentRoomListBinding
import com.example.rental_property_management_app.models.RoomResponse
import com.example.rental_property_management_app.models.RoomStatus
import com.example.rental_property_management_app.utils.Resource
import com.example.rental_property_management_app.viewmodels.RoomViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Room List Fragment
 * Module 1 - Hiển thị danh sách phòng
 */
class RoomListFragment : Fragment() {
    
    private var _binding: FragmentRoomListBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: RoomViewModel by viewModels()
    
    private lateinit var adapter: RoomAdapter
    private var allRooms: List<RoomResponse> = emptyList()
    private var selectedStatusFilter: RoomStatus? = null
    private var searchKeyword: String = ""
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRoomListBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
        setupSearchView()
        observeRoomsState()
        observeDeleteState()
        
        // Load initial data
        viewModel.loadAllRooms(size = 100)
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_refresh -> {
                    viewModel.loadAllRooms(size = 100)
                    true
                }
                R.id.action_filter -> {
                    binding.etSearch.requestFocus()
                    true
                }
                else -> false
            }
        }
    }
    
    private fun setupRecyclerView() {
        adapter = RoomAdapter(
            onItemClick = { room ->
                // Navigate to detail
                val action = RoomListFragmentDirections.actionRoomListToRoomDetail(room.id)
                findNavController().navigate(action)
            },
            onEditClick = { room ->
                // Navigate to edit
                val action = RoomListFragmentDirections.actionRoomListToRoomForm(roomId = room.id)
                findNavController().navigate(action)
            },
            onDeleteClick = { room ->
                showDeleteConfirmDialog(room)
            }
        )
        
        binding.rvRooms.adapter = adapter
    }
    
    private fun setupClickListeners() {
        // FAB - Add new room
        binding.fabAddRoom.setOnClickListener {
            val action = RoomListFragmentDirections.actionRoomListToRoomForm(roomId = 0L)
            findNavController().navigate(action)
        }
        
        // Swipe refresh
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadAllRooms(size = 100)
        }
        
        // Retry button
        binding.btnRetry.setOnClickListener {
            viewModel.loadAllRooms(size = 100)
        }

        binding.chipGroupStatus.setOnCheckedStateChangeListener { _, checkedIds ->
            selectedStatusFilter = when (checkedIds.firstOrNull()) {
                R.id.chipVacant -> RoomStatus.VACANT
                R.id.chipOccupied -> RoomStatus.OCCUPIED
                R.id.chipExpiring -> RoomStatus.EXPIRING_SOON
                R.id.chipAttention -> RoomStatus.NEEDS_ATTENTION
                else -> null
            }
            applyFilters()
        }
    }
    
    private fun setupSearchView() {
        binding.etSearch.addTextChangedListener { text ->
            searchKeyword = text?.toString()?.trim().orEmpty()
            applyFilters()
        }
    }

    private fun applyFilters() {
        val kw = searchKeyword.lowercase()
        val filtered = allRooms.filter { room ->
            val matchStatus = selectedStatusFilter == null || room.status == selectedStatusFilter
            val matchKeyword = kw.isEmpty() ||
                room.roomCode.lowercase().contains(kw) ||
                room.propertyName.lowercase().contains(kw) ||
                (room.currentTenant?.lowercase()?.contains(kw) == true)
            matchStatus && matchKeyword
        }
        if (filtered.isEmpty()) {
            adapter.submitList(emptyList())
            showEmptyState()
        } else {
            hideAllStates()
            adapter.submitList(filtered)
            binding.rvRooms.visibility = View.VISIBLE
        }
    }
    
    private fun observeRoomsState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.roomsState.collectLatest { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        showLoading()
                    }
                    is Resource.Success -> {
                        binding.swipeRefresh.isRefreshing = false
                        hideAllStates()
                        allRooms = resource.data?.content ?: emptyList()
                        applyFilters()
                    }
                    is Resource.Error -> {
                        binding.swipeRefresh.isRefreshing = false
                        hideAllStates()
                        showErrorState(resource.message ?: "Lỗi không xác định")
                    }
                }
            }
        }
    }
    
    private fun observeDeleteState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.deleteState.collectLatest { state ->
                when {


                    state.isLoading -> Unit

                    state.data != null -> {
                        Toast.makeText(context, "Đã xóa phòng", Toast.LENGTH_SHORT).show()
                        viewModel.resetDeleteState()
                        viewModel.loadAllRooms() // Reload list
                    }
                    state.error != null -> {
                        Toast.makeText(
                            context,
                            "Xóa thất bại: ${state.error}",
                            Toast.LENGTH_LONG
                        ).show()
                        viewModel.resetDeleteState()
                    }
                }
            }
        }
    }
    
    private fun showDeleteConfirmDialog(room: RoomResponse) {
        AlertDialog.Builder(requireContext())
            .setTitle("Xác nhận xóa")
            .setMessage("Bạn có chắc muốn xóa phòng ${room.roomCode}?")
            .setPositiveButton("Xóa") { _, _ ->
                viewModel.deleteRoom(room.id)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
    
    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.rvRooms.visibility = View.GONE
        binding.layoutEmpty.visibility = View.GONE
        binding.layoutError.visibility = View.GONE
    }
    
    private fun showEmptyState() {
        binding.layoutEmpty.visibility = View.VISIBLE
        binding.rvRooms.visibility = View.GONE
        binding.progressBar.visibility = View.GONE
        binding.layoutError.visibility = View.GONE
    }
    
    private fun showErrorState(message: String) {
        binding.layoutError.visibility = View.VISIBLE
        binding.tvError.text = message
        binding.rvRooms.visibility = View.GONE
        binding.progressBar.visibility = View.GONE
        binding.layoutEmpty.visibility = View.GONE
    }
    
    private fun hideAllStates() {
        binding.progressBar.visibility = View.GONE
        binding.layoutEmpty.visibility = View.GONE
        binding.layoutError.visibility = View.GONE
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
