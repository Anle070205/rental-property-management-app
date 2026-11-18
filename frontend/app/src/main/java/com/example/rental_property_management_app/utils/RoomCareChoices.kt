package com.example.rental_property_management_app.utils

import android.view.LayoutInflater
import android.view.View
import com.example.rental_property_management_app.R
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

internal fun ChipGroup.populateRoomCareChoices(labels: Array<String>, selected: Int) {
    val inflater = LayoutInflater.from(context)
    labels.forEach { label ->
        val chip = (inflater.inflate(R.layout.item_roomcare_choice, this, false) as Chip).apply {
            id = View.generateViewId()
            text = label
        }
        addView(chip)
    }
    getChildAt(selected.takeIf { it in labels.indices } ?: 0)?.let { check(it.id) }
}

internal fun ChipGroup.selectedRoomCareChoice(): Int =
    (0 until childCount).firstOrNull { getChildAt(it).id == checkedChipId } ?: 0
