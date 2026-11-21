package com.example.rental_property_management_app.utils

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.rental_property_management_app.R
import com.google.android.material.button.MaterialButton

object AdminUiHelper {

    fun showConfirmDialog(
        context: Context,
        title: String,
        message: String,
        confirmText: String,
        isDanger: Boolean = false,
        cancelText: String = "Hủy",
        onConfirm: () -> Unit
    ) {
        val dp = context.resources.displayMetrics.density
        val dialog = Dialog(context)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.rc_panel)
            val pad = (20 * dp).toInt()
            setPadding(pad, pad, pad, pad)
        }

        val eyebrow = TextView(context).apply {
            text = "ROOMCARE / XÁC NHẬN"
            textSize = 11f
            setTextColor(ContextCompat.getColor(context, R.color.rc_brand))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        val tvTitle = TextView(context).apply {
            text = title
            textSize = 19f
            setTextColor(ContextCompat.getColor(context, R.color.rc_ink))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, (6 * dp).toInt(), 0, (6 * dp).toInt())
        }
        val tvMsg = TextView(context).apply {
            text = message
            textSize = 13.5f
            setTextColor(ContextCompat.getColor(context, R.color.rc_muted))
            setPadding(0, 0, 0, (16 * dp).toInt())
        }
        val btnConfirm = MaterialButton(context).apply {
            text = confirmText
            isAllCaps = false
            cornerRadius = (12 * dp).toInt()
            setTextColor(Color.WHITE)
            setBackgroundColor(
                ContextCompat.getColor(
                    context,
                    if (isDanger) R.color.rc_danger else R.color.rc_brand
                )
            )
            setOnClickListener {
                dialog.dismiss()
                onConfirm()
            }
        }
        val btnCancel = MaterialButton(context, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = cancelText
            isAllCaps = false
            cornerRadius = (12 * dp).toInt()
            strokeWidth = 0
            setBackgroundColor(ContextCompat.getColor(context, R.color.rc_tint_soft))
            setTextColor(ContextCompat.getColor(context, R.color.rc_brand_dark))
            setOnClickListener { dialog.dismiss() }
        }

        card.addView(eyebrow)
        card.addView(tvTitle)
        card.addView(tvMsg)
        card.addView(btnConfirm, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, (48 * dp).toInt()
        ))
        card.addView(btnCancel, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, (46 * dp).toInt()
        ).apply { topMargin = (8 * dp).toInt() })

        dialog.setContentView(card)
        dialog.window?.setLayout((context.resources.displayMetrics.widthPixels * 0.90).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setGravity(Gravity.CENTER)
        dialog.show()
    }

    fun showAudienceDialog(
        context: Context,
        counts: com.example.rental_property_management_app.models.AdminBroadcastAudienceCounts?,
        onSelected: (String) -> Unit
    ) {
        val dp = context.resources.displayMetrics.density
        val dialog = Dialog(context)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.rc_panel)
            val pad = (20 * dp).toInt()
            setPadding(pad, pad, pad, pad)
        }
        card.addView(TextView(context).apply {
            text = "ROOMCARE / XÁC NHẬN"
            textSize = 11f
            setTextColor(ContextCompat.getColor(context, R.color.rc_brand))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        card.addView(TextView(context).apply {
            text = "Chọn đối tượng nhận"
            textSize = 19f
            setTextColor(ContextCompat.getColor(context, R.color.rc_ink))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, (6 * dp).toInt(), 0, (4 * dp).toInt())
        })
        card.addView(TextView(context).apply {
            text = "Chỉ gửi đến các tài khoản đang hoạt động. Chọn một nhóm bên dưới."
            textSize = 13f
            setTextColor(ContextCompat.getColor(context, R.color.rc_muted))
            setPadding(0, 0, 0, (12 * dp).toInt())
        })

        val options = listOf(
            "ALL" to "Toàn hệ thống · ${counts?.allActive ?: 0}",
            "ADMIN" to "ADMIN · ${counts?.adminActive ?: 0}",
            "LANDLORD" to "LANDLORD · ${counts?.landlordActive ?: 0}",
            "TENANT" to "TENANT · ${counts?.tenantActive ?: 0}"
        )
        options.forEachIndexed { idx, (code, label) ->
            val btn = MaterialButton(context).apply {
                text = label
                isAllCaps = false
                cornerRadius = (12 * dp).toInt()
                if (idx == 0) {
                    setBackgroundColor(ContextCompat.getColor(context, R.color.rc_brand))
                    setTextColor(Color.WHITE)
                } else {
                    setBackgroundColor(ContextCompat.getColor(context, R.color.rc_tint_soft))
                    setTextColor(ContextCompat.getColor(context, R.color.rc_brand_dark))
                }
                setOnClickListener {
                    dialog.dismiss()
                    onSelected(code)
                }
            }
            card.addView(btn, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, (46 * dp).toInt()
            ).apply { bottomMargin = (8 * dp).toInt() })
        }

        val btnCancel = MaterialButton(context).apply {
            text = "Hủy"
            isAllCaps = false
            cornerRadius = (12 * dp).toInt()
            setBackgroundColor(ContextCompat.getColor(context, R.color.rc_tint_soft))
            setTextColor(ContextCompat.getColor(context, R.color.rc_muted))
            setOnClickListener { dialog.dismiss() }
        }
        card.addView(btnCancel, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, (44 * dp).toInt()
        ))

        dialog.setContentView(card)
        dialog.window?.setLayout((context.resources.displayMetrics.widthPixels * 0.90).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }

    fun formatDateShort(iso: String?): String {
        if (iso.isNullOrBlank()) return "--"
        return try {
            val datePart = iso.substring(0, 10).split("-")
            "${datePart[2]}/${datePart[1]}/${datePart[0]}"
        } catch (_: Exception) {
            iso.take(10)
        }
    }

    fun formatDateTimeShort(iso: String?): String {
        if (iso.isNullOrBlank()) return "--"
        return try {
            val d = formatDateShort(iso)
            val t = if (iso.length >= 16) iso.substring(11, 16) else ""
            if (t.isNotEmpty()) "$d · $t" else d
        } catch (_: Exception) {
            iso.take(16)
        }
    }
}
