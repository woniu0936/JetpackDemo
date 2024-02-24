package com.demo.jetpack.navigation.home

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.demo.jetpack.R
import com.demo.jetpack.core.extension.viewBindings
import com.demo.jetpack.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {

    private val mBinding: ActivityHomeBinding by viewBindings(ActivityHomeBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        mBinding.navMain.setupWithNavController(navHostFragment.navController)
    }

}