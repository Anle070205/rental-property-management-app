package com.example.rental_property_management_app.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.models.MonthlyRevenueResponse
import java.math.BigDecimal

/**
 * Lightweight bar chart showing monthly billed revenue vs collected amount.
 */
class MonthlyRevenueChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var data: List<MonthlyRevenueResponse> = emptyList()

    private val billedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.chart_billed)
    }

    private val collectedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.chart_collected)
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.divider)
        strokeWidth = 2f
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_secondary)
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }

    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_primary)
        textSize = 24f
        textAlign = Paint.Align.CENTER
    }

    private val barRect = RectF()

    fun setData(items: List<MonthlyRevenueResponse>) {
        data = items
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        if (data.isEmpty()) {
            canvas.drawText(
                "Chưa có dữ liệu doanh thu theo tháng",
                w / 2f,
                h / 2f,
                labelPaint
            )
            return
        }

        val topPad = 40f
        val bottomPad = 48f
        val sidePad = 24f
        val chartHeight = (h - topPad - bottomPad).coerceAtLeast(40f)
        val baselineY = topPad + chartHeight

        canvas.drawLine(sidePad, baselineY, w - sidePad, baselineY, gridPaint)
        canvas.drawLine(sidePad, topPad + chartHeight / 2f, w - sidePad, topPad + chartHeight / 2f, gridPaint)

        val maxVal = data.maxOf { maxOf(it.totalBilled, it.totalCollected) }
            .max(BigDecimal.ONE)
            .toFloat()

        val groupWidth = (w - sidePad * 2f) / data.size
        val barWidth = (groupWidth * 0.28f).coerceIn(14f, 44f)
        val gap = 8f

        data.forEachIndexed { index, item ->
            val centerX = sidePad + groupWidth * index + groupWidth / 2f
            val billedH = (item.totalBilled.toFloat() / maxVal) * chartHeight
            val collectedH = (item.totalCollected.toFloat() / maxVal) * chartHeight

            // Billed bar (left)
            val left1 = centerX - gap / 2f - barWidth
            barRect.set(left1, baselineY - billedH, left1 + barWidth, baselineY)
            canvas.drawRoundRect(barRect, 6f, 6f, billedPaint)

            // Collected bar (right)
            val left2 = centerX + gap / 2f
            barRect.set(left2, baselineY - collectedH, left2 + barWidth, baselineY)
            canvas.drawRoundRect(barRect, 6f, 6f, collectedPaint)

            // Value label above tallest bar
            val topY = (baselineY - maxOf(billedH, collectedH) - 10f).coerceAtLeast(24f)
            canvas.drawText(shortMillion(item.totalCollected), centerX, topY, valuePaint)

            // Month caption
            canvas.drawText(item.label, centerX, h - 12f, labelPaint)
        }
    }

    private fun shortMillion(amount: BigDecimal): String {
        val v = amount.toDouble()
        return when {
            v >= 1_000_000 -> String.format("%.1ftr", v / 1_000_000.0)
            v >= 1_000 -> String.format("%.0fk", v / 1_000.0)
            else -> v.toInt().toString()
        }
    }
}
