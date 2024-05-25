package com.cavss.artravel

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.cavss.artravel.databinding.ActivityMainBinding
import com.cavss.artravel.vm.AuthVM
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private var authVM : AuthVM? = null
    private fun setInit(){
        try {
            authVM = ViewModelProvider(this@MainActivity)[AuthVM::class.java]
            authVM?.let {
                it.setInit(this@MainActivity)
                it.setUserExist(it.isUserExist())
            }
        } catch (e: Exception) {
            Log.e("mException", "MainActivity, setInit // Exception: ${e.localizedMessage}", e)
        }
    }


    private lateinit var binding : ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setInit()

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.run{
            setBottomNavigation()
        }
        setContentView(binding.root)
        requestPermission()
    }


    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var navHostFragment: NavHostFragment
    private lateinit var navController: NavController
    private fun setBottomNavigation(){
        try{
            // 하단 네비게이션 설정
            bottomNavigationView = findViewById(R.id.bottom_navigation)
            navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragmnet) as NavHostFragment
            navController = navHostFragment.navController

            NavigationUI.setupWithNavController(
                bottomNavigationView,navController
            )

            bottomNavigationView.setOnItemSelectedListener {  item ->
                when(item.itemId) {
                    R.id.fragment_home -> {
                        changeFragment(R.id.fragment_home)
                        true
                    }
                    R.id.fragment_map -> {
                        changeFragment(R.id.fragment_map)
                        true
                    }
                    R.id.fragment_setting -> {
                        changeFragment(R.id.fragment_setting)
                        true
                    }
                    else -> false
                }
            }
        }catch (e:Exception){
            Log.e("mException", "MainActivity, setBottomNavigation // Exception : ${e.localizedMessage}")
        }
    }

    private fun changeFragment(fragment : Int){
        try{
            if (navController.currentDestination?.id != fragment) {
                navController.navigate(fragment)
            }
        }catch (e:Exception){
            Log.e("mException", "MainActivity, changeFragment // Exception : ${e.localizedMessage}")
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