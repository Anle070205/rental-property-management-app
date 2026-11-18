package com.example.rental_property_management_app.fragments

internal fun profileInitials(name: String): String =
    name.trim().split(Regex("\\s+")).filter(String::isNotBlank).takeLast(2)
        .joinToString("") { it.take(1).uppercase() }
