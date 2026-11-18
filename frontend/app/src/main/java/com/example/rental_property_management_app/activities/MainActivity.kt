package com.example.rental_property_management_app.activities


import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.navOptions
import androidx.navigation.fragment.NavHostFragment

import com.example.rental_property_management_app.databinding.ActivityMainBinding
import com.example.rental_property_management_app.R
import com.example.rental_property_management_app.RoomCareApplication
import com.example.rental_property_management_app.network.RetrofitClient
import kotlinx.coroutines.launch
class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Khởi tạo View Binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Setup Navigation Controller
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        lifecycleScope.launchWhenResumed {
            RetrofitClient.expiredToken.collect { token ->
                if (token == null) return@collect
                RetrofitClient.consumeExpiredToken(token)
                val preferences = RoomCareApplication.prefsManager
                if (preferences.getAccessToken() == token) {
                    preferences.clearAllData()
                    if (navController.currentDestination?.id != R.id.loginFragment) {
                        navController.navigate(R.id.loginFragment, null, navOptions {
                            popUpTo(R.id.nav_graph) { inclusive = true }
                        })
                    }
                }
            }
        }
        
        // Setup ActionBar với Navigation (Đã tắt vì dùng NoActionBar theme)
        // setupActionBarWithNavController(navController)
    }
    
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
