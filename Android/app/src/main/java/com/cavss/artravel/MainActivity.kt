package com.cavss.artravel

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.NavigationUI.setupActionBarWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView


class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var navHostFragment: NavHostFragment
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestPermission()

        // 하단 네비게이션 설정
        bottomNavigationView = findViewById(R.id.bottom_navigation)
        navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragmnet) as NavHostFragment
        navController = navHostFragment.navController

        NavigationUI.setupWithNavController(
            bottomNavigationView,navController
        )

        bottomNavigationView.setOnItemSelectedListener {item ->
            when(item.itemId) {
                R.id.fragment_home -> {
                    if (navController.currentDestination?.id != R.id.fragment_home) {
                        navController.navigate(R.id.fragment_home)
                    }
                    true
                }
                R.id.fragment_map -> {
                    if (navController.currentDestination?.id != R.id.fragment_map) {
                        navController.navigate(R.id.fragment_map)
                    }
                    true
                }
                R.id.fragment_profile -> {
                    if (navController.currentDestination?.id != R.id.fragment_profile) {
                        navController.navigate(R.id.fragment_profile)
                    }
                    true
                }
                else -> false
            }
        }
    }




    //카메라 권한 요청
    private fun requestPermission() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
        try {
            val permissionManager = PermissionManager(this)
            permissionManager.requestPermissions(permissions, object  : PermissionCallback {
                override fun onPermissionGranted(grantedPermission : String) {
                    Log.e("mException", "허락된 권한 : ${grantedPermission}")
                }

                override fun onPermissionDenied(askPermissionAgain: Boolean, deniedPermission : String) {
                    when(askPermissionAgain){
                        true -> {
                            Log.e("mException", "권한 이전에 거절한 적 있음, 거절된 권한 :${deniedPermission}")
                        }
                        false -> {
                            Log.e("mException", "권한 이전에 거절한 적 없음, 거절된 권한 :${deniedPermission}")
                        }
                    }
                }
            })
        }catch (e:Exception){
            Log.e("mException", "MainActivity, requestPermission // Exception : ${e.localizedMessage}")
        }
    }


}