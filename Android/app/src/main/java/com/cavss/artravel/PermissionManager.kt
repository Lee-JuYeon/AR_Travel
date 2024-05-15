package com.cavss.artravel
import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.util.Log

interface PermissionCallback {
    fun onPermissionGranted(grantedPermission : String)
    fun onPermissionDenied(askPermissionAgain: Boolean, deniedPermission : String)
}

class PermissionManager(private val activity: AppCompatActivity) {

    companion object {
        const val PERMISSION_REQUEST_CODE = 1
    }

    fun requestPermissions(permissions: Array<String>, callback: PermissionCallback) {
        try {
            permissions.forEach { permission : String ->
                when (ContextCompat.checkSelfPermission(activity, permission)) {
                    PackageManager.PERMISSION_GRANTED -> {
                        callback.onPermissionGranted(grantedPermission = permission)
                    }
                    PackageManager.PERMISSION_DENIED -> {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                            // 사용자가 이전에 권한 요청을 거부했고, 권한 요청 이유를 설명할 필요가 있는 경우
                            // 이 부분에서 권한 요청에 대한 설명을 사용자에게 표시할 수 있습니다.
                            ActivityCompat.requestPermissions(activity, permissions, PERMISSION_REQUEST_CODE)
                            callback.onPermissionDenied(true, permission)
                        } else {
                            //처음 요청하는 경우 그냥 권한 요청
                            ActivityCompat.requestPermissions(activity, permissions, PERMISSION_REQUEST_CODE)
                            callback.onPermissionDenied(false, permission)
                        }
                    }
                }
            }

        } catch (e: Exception) {
            Log.e("mException", "PermissionManager, requestPermissions // Exception: ${e.localizedMessage}")
        }
    }
}
