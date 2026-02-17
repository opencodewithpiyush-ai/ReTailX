package com.rajatt7z.retailx

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import com.rajatt7z.retailx.databinding.ActivityAdminDashboardBinding

import com.rajatt7z.retailx.utils.BaseActivity

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AdminDashboardActivity : BaseActivity() {

    private lateinit var binding: ActivityAdminDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        // Navigation is handled by NavHostFragment in XML
        
        val navigateTo = intent.getStringExtra("NAVIGATE_TO")
        if (navigateTo == "CHANGE_PASSWORD") {
            val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            val bundle = Bundle()
            bundle.putBoolean("IS_RESET_MODE", true)
            navHostFragment.navController.navigate(R.id.changePasswordFragment, bundle)
        }
    }
}
