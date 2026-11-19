package com.example.rental_property_management_app.adapters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.databinding.ItemHealthScoreBinding
import com.example.rental_property_management_app.models.RoomHealthScoreResponse

class HealthScoreAdapter(
    private val onRoomScoreClick: (RoomHealthScoreResponse) -> Unit
) : ListAdapter<RoomHealthScoreResponse, HealthScoreAdapter.HealthScoreViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HealthScoreViewHolder {
        val binding = ItemHealthScoreBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HealthScoreViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HealthScoreViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class HealthScoreViewHolder(
        private val binding: ItemHealthScoreBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RoomHealthScoreResponse) {
            val ctx = binding.root.context
            val (fgColorRes, bgColorRes) = when (item.ratingLevel) {
                "GOOD" -> R.color.health_good to R.color.health_good_bg
                "WARNING" -> R.color.health_warning to R.color.health_warning_bg
                else -> R.color.health_risk to R.color.health_risk_bg
            }
            val fgColor = ContextCompat.getColor(ctx, fgColorRes)
            val bgColor = ContextCompat.getColor(ctx, bgColorRes)

            binding.apply {
                tvScoreCircle.text = item.totalScore.toString()
                tvScoreCircle.setTextColor(fgColor)
                tvScoreCircle.backgroundTintList = ColorStateList.valueOf(bgColor)

                tvRoomTitle.text = "Phòng ${item.roomCode} • ${item.propertyName}"
                tvTenantAndStatus.text = buildString {
                    append(item.roomStatus.getDisplayName())
                    if (!item.tenantName.isNullOrBlank()) {
                        append(" • ${item.tenantName}")
                    }
                }

                tvRatingBadge.text = item.ratingLabel
                tvRatingBadge.setTextColor(fgColor)
                tvRatingBadge.backgroundTintList = ColorStateList.valueOf(bgColor)

                tvTrend.text = when (item.trend) {
                    "UP" -> "↑ +${item.scoreDelta} điểm"
                    "DOWN" -> "↓ ${item.scoreDelta} điểm"
                    else -> "→ Ổn định"
                }
                tvTrend.setTextColor(
                    ContextCompat.getColor(
                        ctx,
                        when (item.trend) {
                            "UP" -> R.color.health_good
                            "DOWN" -> R.color.health_risk
                            else -> R.color.text_secondary
                        }
                    )
                )

                progressScore.progress = item.totalScore
                progressScore.setIndicatorColor(fgColor)

                tvFactorSummary.text = item.factors.joinToString(" • ") { f ->
                    "${shortFactorName(f.factorName)}: ${f.score}/${f.maxScore}"
                }

                tvDeductionOrTip.text = if (item.reasonsForDeduction.isNotEmpty()) {
                    "Lưu ý: ${item.reasonsForDeduction.first()}"
                } else {
                    "Gợi ý: ${item.recommendations.firstOrNull() ?: "Đang vận hành tốt"}"
                }

                root.setOnClickListener { onRoomScoreClick(item) }
            }
        }

        private fun shortFactorName(name: String): String {
            return when {
                name.contains("Sự cố", ignoreCase = true) || name.contains("Tình trạng", ignoreCase = true) -> "Sự cố"
                name.contains("Thanh toán") -> "Thanh toán"
                name.contains("Hợp đồng") -> "Hợp đồng"
                name.contains("điện nước", ignoreCase = true) -> "Điện nước"
                else -> "Vận hành"
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<RoomHealthScoreResponse>() {
        override fun areItemsTheSame(oldItem: RoomHealthScoreResponse, newItem: RoomHealthScoreResponse): Boolean {
            return oldItem.roomId == newItem.roomId
        }

        override fun areContentsTheSame(oldItem: RoomHealthScoreResponse, newItem: RoomHealthScoreResponse): Boolean {
            return oldItem == newItem
        }
    }
}
