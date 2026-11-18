package com.example.rental_property_management_app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.adapters.HealthScoreAdapter
import com.example.rental_property_management_app.databinding.FragmentHealthScoreBinding
import com.example.rental_property_management_app.models.RoomHealthScoreResponse
import com.example.rental_property_management_app.utils.Resource
import com.example.rental_property_management_app.viewmodels.HealthScoreViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HealthScoreFragment : Fragment() {

    private var _binding: FragmentHealthScoreBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HealthScoreViewModel by viewModels()
    private lateinit var adapter: HealthScoreAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHealthScoreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        setupListeners()
        observeStates()

        viewModel.loadHealthScores()

        val initialRoomId = arguments?.getLong("roomId", 0L) ?: 0L
        if (initialRoomId > 0L) {
            viewModel.loadRoomHealthScore(initialRoomId)
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        adapter = HealthScoreAdapter { item ->
            viewModel.loadRoomHealthScore(item.roomId)
        }
        binding.rvHealthScores.adapter = adapter
    }

    private fun setupListeners() {
        binding.chipGroupRating.setOnCheckedStateChangeListener { _, checkedIds ->
            val rating = when (checkedIds.firstOrNull()) {
                R.id.chipGood -> "GOOD"
                R.id.chipWarning -> "WARNING"
                R.id.chipRisk -> "RISK"
                else -> null
            }
            viewModel.loadHealthScores(ratingLevel = rating)
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadHealthScores()
        }

        binding.btnRetry.setOnClickListener {
            viewModel.loadHealthScores()
        }
    }

    private fun observeStates() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.overviewState.collectLatest { resource ->
                when (resource) {
                    is Resource.Loading -> showLoading()
                    is Resource.Success -> {
                        binding.swipeRefresh.isRefreshing = false
                        hideStates()
                        val overview = resource.data
                        if (overview != null) {
                            binding.tvAvgScore.text = "${overview.averageScore}/100"
                            binding.tvGoodCount.text = "Tốt: ${overview.goodRoomsCount} phòng"
                            binding.tvWarningCount.text = "Theo dõi: ${overview.warningRoomsCount} phòng"
                            binding.tvRiskCount.text = "Rủi ro: ${overview.riskRoomsCount} phòng"
                            binding.tvFormulaNote.text = overview.formulaSummary ?: ""

                            if (overview.roomScores.isEmpty()) {
                                binding.tvEmpty.visibility = View.VISIBLE
                                binding.rvHealthScores.visibility = View.GONE
                            } else {
                                binding.tvEmpty.visibility = View.GONE
                                binding.rvHealthScores.visibility = View.VISIBLE
                                adapter.submitList(overview.roomScores)
                            }
                        }
                    }
                    is Resource.Error -> {
                        binding.swipeRefresh.isRefreshing = false
                        hideStates()
                        binding.layoutError.visibility = View.VISIBLE
                        binding.tvError.text = resource.message ?: getString(R.string.error_loading)
                        binding.rvHealthScores.visibility = View.GONE
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.roomScoreState.collectLatest { resource ->
                resource ?: return@collectLatest
                when (resource) {
                    is Resource.Loading -> Unit
                    is Resource.Success -> {
                        resource.data?.let { showRoomHealthDetailDialog(it) }
                        viewModel.resetRoomScoreState()
                    }
                    is Resource.Error -> {
                        Snackbar.make(binding.root, resource.message ?: "Không tải được chi tiết phòng", Snackbar.LENGTH_SHORT).show()
                        viewModel.resetRoomScoreState()
                    }
                }
            }
        }
    }

    private fun showRoomHealthDetailDialog(detail: RoomHealthScoreResponse) {
        val message = buildString {
            append("Điểm tổng: ${detail.totalScore}/100 (${detail.ratingLabel})\n")
            val trendLabel = when (detail.trend) {
                "UP" -> "Tăng (+${detail.scoreDelta})"
                "DOWN" -> "Giảm (${detail.scoreDelta})"
                else -> "Ổn định"
            }
            append("Xu hướng: $trendLabel\n\n")

            append("CÁC YẾU TỐ ĐÁNH GIÁ:\n")
            detail.factors.forEach { f ->
                append("• ${f.factorName}: ${f.score}/${f.maxScore} (${f.weightPercent}%)\n  ${f.detail}\n")
            }

            if (detail.reasonsForDeduction.isNotEmpty()) {
                append("\nLÝ DO ĐIỂM GIẢM:\n")
                detail.reasonsForDeduction.forEach { r -> append("• $r\n") }
            }

            if (detail.recommendations.isNotEmpty()) {
                append("\nGỢI Ý CẢI THIỆN:\n")
                detail.recommendations.forEach { tip -> append("• $tip\n") }
            }

            if (!detail.formulaNote.isNullOrBlank()) {
                append("\n(${detail.formulaNote})")
            }
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Health Score - Phòng ${detail.roomCode}")
            .setMessage(message)
            .setPositiveButton("Đóng", null)
            .setNeutralButton("Xem phòng") { _, _ ->
                val bundle = Bundle().apply { putLong("roomId", detail.roomId) }
                findNavController().navigate(R.id.roomDetailFragment, bundle)
            }
            .show()
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.rvHealthScores.visibility = View.GONE
        binding.tvEmpty.visibility = View.GONE
        binding.layoutError.visibility = View.GONE
    }

    private fun hideStates() {
        binding.progressBar.visibility = View.GONE
        binding.tvEmpty.visibility = View.GONE
        binding.layoutError.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
