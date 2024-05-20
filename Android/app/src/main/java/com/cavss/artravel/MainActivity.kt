package com.cavss.artravel

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log



class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestPermission()
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