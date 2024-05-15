package com.cavss.artravel

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.ar.core.ArCoreApk

class MainActivity : AppCompatActivity() {



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        maybeEnableArButton()
        requestPermission()
    }


    // arcore 지원여부 확인
    fun maybeEnableArButton() {
        val ablitly = ArCoreApk.getInstance().checkAvailability(this).isSupported
        Log.e("mException", "MainActivity, maybeEnableArButton // Exception : ${ablitly}")
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

    // Google Play AR 서비스가 설치되어 있는지 확인하기
//    override fun onResume() {
//        super.onResume()
//        var mUserRequestedInstall = true
//        try {
//            if (mSession == null) {
//                when (ArCoreApk.getInstance().requestInstall(this, mUserRequestedInstall)) {
//                    ArCoreApk.InstallStatus.INSTALLED -> {
//                        // Success: Safe to create the AR session.
//                        mSession = Session(this)
//                    }
//                    ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
//                        // When this method returns `INSTALL_REQUESTED`:
//                        // 1. ARCore pauses this activity.
//                        // 2. ARCore prompts the user to install or update Google Play
//                        //    Services for AR (market://details?id=com.google.ar.core).
//                        // 3. ARCore downloads the latest device profile data.
//                        // 4. ARCore resumes this activity. The next invocation of
//                        //    requestInstall() will either return `INSTALLED` or throw an
//                        //    exception if the installation or update did not succeed.
//                        mUserRequestedInstall = false
//                        return
//                    }
//                }
//            }
//        } catch (e: UnavailableUserDeclinedInstallationException) {
//            // Display an appropriate message to the user and return gracefully.
//            Toast.makeText(this, "TODO: handle exception " + e, Toast.LENGTH_LONG)
//                .show()
//            return
//        } catch (e:Exception) {
//            return  // mSession remains null, since session creation has failed.
//        }
//    }

}