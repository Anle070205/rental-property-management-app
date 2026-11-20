package com.example.rental_property_management_app.utils

import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.widget.EditText

/**
 * Gắn 6 ô EditText nhập OTP: tự động chuyển focus sang ô tiếp theo, quay lại khi nhấn xóa
 * và tự chia đều chuỗi 6 chữ số nếu người dùng dán từ Gmail / SMS.
 */
class OtpBoxGroup(
    private val boxes: List<EditText>,
    private val onOtpChanged: (String) -> Unit = {},
    private val onCompleted: (String) -> Unit = {}
) {
    private var updatingProgrammatically = false

    init {
        require(boxes.size == 6) { "OtpBoxGroup requires exactly 6 EditTexts" }
        boxes.forEachIndexed { index, editText ->
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable?) {
                    if (updatingProgrammatically) return
                    val text = s?.toString().orEmpty()
                    val digitsOnly = text.filter { it.isDigit() }
                    if (digitsOnly.length > 1) {
                        fillDigitsStartingAt(index, digitsOnly)
                        return
                    }
                    if (text != digitsOnly) {
                        updatingProgrammatically = true
                        editText.setText(digitsOnly)
                        editText.setSelection(digitsOnly.length)
                        updatingProgrammatically = false
                    }
                    if (digitsOnly.length == 1 && index < boxes.lastIndex) {
                        boxes[index + 1].requestFocus()
                    }
                    notifyState()
                }
            })

            editText.setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN) {
                    if (editText.text.isNullOrEmpty() && index > 0) {
                        boxes[index - 1].apply {
                            text?.clear()
                            requestFocus()
                        }
                        notifyState()
                        return@setOnKeyListener true
                    }
                }
                false
            }
        }
    }

    fun getOtp(): String = boxes.joinToString("") { it.text?.toString()?.trim().orEmpty() }

    fun clear() {
        updatingProgrammatically = true
        boxes.forEach { it.text?.clear() }
        updatingProgrammatically = false
        boxes.firstOrNull()?.requestFocus()
        notifyState()
    }

    private fun fillDigitsStartingAt(startIndex: Int, digits: String) {
        updatingProgrammatically = true
        val start = if (digits.length >= 6) 0 else startIndex
        var cursor = start
        for (ch in digits) {
            if (cursor > boxes.lastIndex) break
            boxes[cursor].setText(ch.toString())
            cursor++
        }
        updatingProgrammatically = false
        val focusIdx = (cursor - 1).coerceIn(0, boxes.lastIndex)
        boxes[focusIdx].requestFocus()
        boxes[focusIdx].setSelection(boxes[focusIdx].text?.length ?: 0)
        notifyState()
    }

    private fun notifyState() {
        val current = getOtp()
        onOtpChanged(current)
        if (current.length == 6 && current.all { it.isDigit() }) {
            onCompleted(current)
        }
    }
}
