package com.example.rental_property_management_app

import android.app.Application
import com.example.rental_property_management_app.utils.SharedPreferencesManager

/**
 * Application class cho RoomCare
 * Khởi tạo các singleton instances
 */
class RoomCareApplication : Application() {
    
    companion object {
        lateinit var instance: RoomCareApplication
            private set
        
        lateinit var prefsManager: SharedPreferencesManager
            private set
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        prefsManager = SharedPreferencesManager(this)
    }
}
