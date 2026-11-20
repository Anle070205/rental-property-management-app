package com.example.rental_property_management_app.activities


import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
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
        setupTenantNavigation()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
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
        }
        
        // Setup ActionBar với Navigation (Đã tắt vì dùng NoActionBar theme)
        // setupActionBarWithNavController(navController)
    }
    
    private fun setupTenantNavigation() {
        val bars = WindowCompat.getInsetsController(window, binding.root)
        val defaultStatusColor = window.statusBarColor
        val defaultLightStatusBar = bars.isAppearanceLightStatusBars
        val roomCareStatusColor = ContextCompat.getColor(this, R.color.rc_canvas)
        val darkSplashScreens = setOf(R.id.splashFragment)
        var activeMenuRole: String? = null

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val role = RoomCareApplication.prefsManager.getUserRole()
            if (role != activeMenuRole) {
                binding.tenantNavigation.menu.clear()
                val menuRes = if (role == "LANDLORD") {
                    R.menu.landlord_navigation
                } else {
                    R.menu.tenant_navigation
                }
                binding.tenantNavigation.inflateMenu(menuRes)
                activeMenuRole = role
            }
            val lightScreen = destination.id !in darkSplashScreens
            window.statusBarColor = if (lightScreen) roomCareStatusColor else defaultStatusColor
            bars.isAppearanceLightStatusBars = lightScreen || defaultLightStatusBar
            val item = binding.tenantNavigation.menu.findItem(destination.id)
            binding.tenantNavigation.isVisible = item != null &&
                (role == "TENANT" || role == "LANDLORD")
            if (binding.tenantNavigation.isVisible) item?.isChecked = true
        }
        binding.tenantNavigation.setOnItemSelectedListener { item ->
            val role = RoomCareApplication.prefsManager.getUserRole()
            val home = if (role == "LANDLORD") R.id.landlordDashboardFragment else R.id.tenantDashboardFragment
            if (navController.currentDestination?.id == item.itemId) return@setOnItemSelectedListener true
            if (item.itemId == home) {
                navController.popBackStack(home, false)
            } else {
                navController.navigate(item.itemId, null, navOptions {
                    popUpTo(home)
                    launchSingleTop = true
                })
            }
            true
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
