package com.example.rental_property_management_app.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Manager để lưu và lấy dữ liệu từ SharedPreferences
 * Dùng để lưu token, user info, etc.
 */
class SharedPreferencesManager(context: Context) {
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        Constants.PrefsKeys.PREFS_NAME,
        Context.MODE_PRIVATE
    )
    
    private val editor: SharedPreferences.Editor = sharedPreferences.edit()
    
    // Token Management
    fun saveAccessToken(token: String) {
        editor.putString(Constants.PrefsKeys.KEY_ACCESS_TOKEN, token).apply()
    }
    
    fun getAccessToken(): String? {
        return sharedPreferences.getString(Constants.PrefsKeys.KEY_ACCESS_TOKEN, null)
    }
    
    fun saveRefreshToken(token: String) {
        editor.putString(Constants.PrefsKeys.KEY_REFRESH_TOKEN, token).apply()
    }
    
    fun getRefreshToken(): String? {
        return sharedPreferences.getString(Constants.PrefsKeys.KEY_REFRESH_TOKEN, null)
    }
    
    // User Info Management
    fun saveUserId(userId: Long) {
        editor.putLong(Constants.PrefsKeys.KEY_USER_ID, userId).apply()
    }
    
    fun getUserId(): Long {
        return sharedPreferences.getLong(Constants.PrefsKeys.KEY_USER_ID, -1L)
    }
    
    fun saveUserRole(role: String) {
        editor.putString(Constants.PrefsKeys.KEY_USER_ROLE, role).apply()
    }
    
    fun getUserRole(): String? {
        return sharedPreferences.getString(Constants.PrefsKeys.KEY_USER_ROLE, null)
    }
    
    fun saveUserName(name: String) {
        editor.putString(Constants.PrefsKeys.KEY_USER_NAME, name).apply()
    }
    
    fun getUserName(): String? {
        return sharedPreferences.getString(Constants.PrefsKeys.KEY_USER_NAME, null)
    }
    
    fun saveUserEmail(email: String) {
        editor.putString(Constants.PrefsKeys.KEY_USER_EMAIL, email).apply()
    }
    
    fun getUserEmail(): String? {
        return sharedPreferences.getString(Constants.PrefsKeys.KEY_USER_EMAIL, null)
    }
    
    // Login Status
    fun setLoggedIn(isLoggedIn: Boolean) {
        editor.putBoolean(Constants.PrefsKeys.KEY_IS_LOGGED_IN, isLoggedIn).apply()
    }
    
    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(Constants.PrefsKeys.KEY_IS_LOGGED_IN, false)
    }
    
    // Save all user data at once
    fun saveUserData(
        accessToken: String,
        refreshToken: String?,
        userId: Long,
        role: String,
        name: String,
        email: String
    ) {
        editor.apply {
            putString(Constants.PrefsKeys.KEY_ACCESS_TOKEN, accessToken)
            refreshToken?.let { putString(Constants.PrefsKeys.KEY_REFRESH_TOKEN, it) }
            putLong(Constants.PrefsKeys.KEY_USER_ID, userId)
            putString(Constants.PrefsKeys.KEY_USER_ROLE, role)
            putString(Constants.PrefsKeys.KEY_USER_NAME, name)
            putString(Constants.PrefsKeys.KEY_USER_EMAIL, email)
            putBoolean(Constants.PrefsKeys.KEY_IS_LOGGED_IN, true)
        }.apply()
    }
    
    // Clear all data (logout)
    fun clearAllData() {
        editor.clear().apply()
    }
    
    // Clear only tokens
    fun clearTokens() {
        editor.remove(Constants.PrefsKeys.KEY_ACCESS_TOKEN)
            .remove(Constants.PrefsKeys.KEY_REFRESH_TOKEN)
            .apply()
    }
}
