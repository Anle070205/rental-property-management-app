package com.example.rental_property_management_app.utils

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout

/**
 * Extension functions để làm code gọn hơn
 */

// View Extensions
fun View.show() {
    visibility = View.VISIBLE
}

fun View.hide() {
    visibility = View.GONE
}

fun View.invisible() {
    visibility = View.INVISIBLE
}

fun View.enable() {
    isEnabled = true
}

fun View.disable() {
    isEnabled = false
}

// Hide keyboard
fun View.hideKeyboard() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, 0)
}

// Show keyboard
fun View.showKeyboard() {
    requestFocus()
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
}

// Context Extensions
fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

fun Context.showLongToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

// View Snackbar
fun View.showSnackbar(message: String, duration: Int = Snackbar.LENGTH_SHORT) {
    Snackbar.make(this, message, duration).show()
}

fun View.showLongSnackbar(message: String) {
    Snackbar.make(this, message, Snackbar.LENGTH_LONG).show()
}

fun View.showSnackbarWithAction(
    message: String,
    actionText: String,
    action: () -> Unit
) {
    Snackbar.make(this, message, Snackbar.LENGTH_LONG)
        .setAction(actionText) { action() }
        .show()
}

// TextInputLayout Extensions
fun TextInputLayout.getText(): String {
    return editText?.text?.toString()?.trim() ?: ""
}

fun TextInputLayout.setText(text: String) {
    editText?.setText(text)
}

fun TextInputLayout.showError(errorMessage: String?) {
    error = errorMessage
    isErrorEnabled = errorMessage != null
}

fun TextInputLayout.clearError() {
    error = null
    isErrorEnabled = false
}

// String Extensions
fun String.isValidEmail(): Boolean {
    return ValidationUtils.isValidEmail(this)
}

fun String.isValidPhone(): Boolean {
    return ValidationUtils.isValidPhone(this)
}

fun String.isValidPassword(): Boolean {
    return ValidationUtils.isValidPassword(this)
}

// Number Extensions
fun Long.toVND(): String {
    return CurrencyUtils.formatVND(this)
}

fun Double.toVND(): String {
    return CurrencyUtils.formatVND(this)
}
