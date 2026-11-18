package com.example.rental_property_management_app.fragments

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.databinding.FragmentMyRoomBinding
import com.example.rental_property_management_app.models.MyRoom
import com.example.rental_property_management_app.utils.Constants
import com.example.rental_property_management_app.utils.CurrencyUtils
import com.example.rental_property_management_app.utils.Resource
import com.example.rental_property_management_app.viewmodels.MyRoomViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class MyRoomFragment : Fragment(R.layout.fragment_my_room) {
    private var _binding: FragmentMyRoomBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val viewModel: MyRoomViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentMyRoomBinding.bind(view)
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.retry.setOnClickListener { viewModel.load() }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.room.collect { result ->
                val room = result.data
                binding.progress.isVisible = result is Resource.Loading
                binding.retry.isVisible = result is Resource.Error
                binding.status.text = result.message ?: getString(R.string.rc_no_active_room)
                binding.status.isVisible = result is Resource.Error || (result is Resource.Success && room?.roomCode == null)
                binding.content.isVisible = room?.roomCode != null
                if (room?.roomCode != null) display(room)
            }
        }
    }

    private fun display(room: MyRoom) {
        binding.sample.isVisible = Constants.USE_MOCK_TENANT_DATA
        binding.roomCode.text = room.roomCode
        binding.address.text = getString(R.string.rc_room_address, room.address.orEmpty())
        binding.rentPrice.text = getString(R.string.rc_room_rent, room.rentPrice?.let(CurrencyUtils::formatVND).orEmpty())
        binding.amenities.text = getString(R.string.rc_room_amenities, room.amenities.joinToString(", "))
        binding.landlord.text = getString(R.string.rc_room_landlord, listOfNotNull(room.landlordName, room.landlordPhone).joinToString(" · "))
        val phone = room.landlordPhone
        binding.dial.isVisible = !phone.isNullOrBlank()
        binding.dial.setOnClickListener {
            if (!phone.isNullOrBlank()) {
                try {
                    startActivity(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phone, null)))
                } catch (_: ActivityNotFoundException) {
                    Snackbar.make(binding.root, R.string.rc_dial_unavailable, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
