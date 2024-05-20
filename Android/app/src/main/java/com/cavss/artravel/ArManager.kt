package com.cavss.artravel

import android.Manifest
import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableApkTooOldException
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import com.google.ar.core.exceptions.UnavailableException
import com.google.ar.core.exceptions.UnavailableSdkTooOldException
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.google.ar.core.*
import java.util.*

/*
ARCore : OpenGL ES가를 사용한 AR 소프트웨어 개발 키트
    ㄴ obj : 그래픽의 기본적인 정보인 UV데이터, 버텍스, 노말 등만 저장
        1. 가장 처음으로 만들어진 확장자
        2. 애니메이션 동작 저장 불가능
        3. 복잡하고 잡스러운 정보를 저장하지 않아 fbx보다 가볍고 깔끔함.

    ㄴ fbx : obj에 비해 다양한 정보를 함께 저장
        1. 애니메이션 동작 저장 가능

SceneView : OpenGL없이 ARCore앱을 쉽게 빌드할 수 있도록 만들어진 3D프레임워크
    ㄴ glb : 3d모델 정보를 바이너리의 형태로 저장
        1. 하나의 파일로 출력
        2. 기타 api를 사용하여 응용 프로그램의 런타임 처리를 최소화 (WebGL)
        3. 최적화가 이루어져 가벼움 (웹에서 자주 사용)
    ㄴ gltf : 3d모델 정보를 json형식으로 저장
        1. 여러 개의 파일 출력 -> 결과물 = 파일 그룹
        2. 기타 API를 사용하여 응용 프로그램의 런타임 처리를 최소화 (WebGL)
        3. 최적화가 이루어져 가벼움 (웹에서 자주 사용)
 */
//class ArManager : DefaultLifecycleObserver {
//
//
//    // arcore 지원여부 확인
//    fun maybeEnableArButton(context: Activity) {
//        val ablitly = ArCoreApk.getInstance().checkAvailability(context).isSupported
//        Log.e("mException", "ArManager, maybeEnableArButton // Exception : ${ablitly}")
//    }
//
//    // AR코어가 설치되었는지 최근버전을 사용하는지 확인
//    fun isARCoreUpdate(context: Activity): Boolean {
//        return when (ArCoreApk.getInstance().checkAvailability(context)) {
//            ArCoreApk.Availability.SUPPORTED_INSTALLED -> {
//                Log.e("mException", "ArManager, isARCoreUpdate // ARCore 사용가능")
//                true
//            }
//            ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD, ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED -> {
//                try {
//                    when (ArCoreApk.getInstance().requestInstall(context, true)) {
//                        ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
//                            Log.e("mException", "ArManager, isARCoreUpdate // ARCore 설치 요청됨. 현재 활동이 일시중지")
//                            false
//                        }
//                        ArCoreApk.InstallStatus.INSTALLED -> {
//                            Log.e("mException", "ArManager, isARCoreUpdate // 요청한 AR 리소스가 이미 설치됨")
//                            true
//                        }
//                    }
//                } catch (e: UnavailableException) {
//                    Log.e("mXception", "ArManager, isARCoreUpdate // ARCore 설치안됨 : ${e.localizedMessage}")
//                    false
//                }
//            }
//
//            ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE -> {
//                Log.e("mXception", "ArManager, isARCoreUpdate // 해당기기는 AR을 지원하지 않음.")
//                false
//            }
//
//            ArCoreApk.Availability.UNKNOWN_CHECKING -> {
//                Log.e("mXception", "ArManager, isARCoreUpdate // ARCore가 설치되어 있지 않으며, ARCore가 지원되는지 확인하는 쿼리가 발행됨.")
//                false
//            }
//            ArCoreApk.Availability.UNKNOWN_ERROR -> {
//                Log.e("mXception", "ArManager, isARCoreUpdate // ARCore 가용성을 확인하는 동안 내부 오류가 발생. logcat 확인필요")
//                false
//            }
//            ArCoreApk.Availability.UNKNOWN_TIMED_OUT -> {
//                Log.e("mXception", "ArManager, isARCoreUpdate // ARCore가 설치되지 않았으며 ARCore 지원 여부를 확인하는 쿼리가 시간 초과. 기기가 오프라인 상태이기 때문일 수 있습니다.")
//                false
//            }
//        }
//    }
//
//
//    private var arFragment: ArFragment? = null
//    private var cubeRenderable: ModelRenderable? = null
//
//    private var nodeA: TransformableNode? = null
//    private var nodeB: TransformableNode? = null
//
//    var greenMaterial: Material? = null
//    var originalMaterial: Material? = null
//
//    var overlapIdle = true
//    val arSession = ArSession.getInstance()
//    val scene = Scene(arSession)
//
//    /**
//     * `Session.resume()`이 호출되기 전에 세션을 구성해야 합니다.
//     * [`Session.configure`](https://developers.google.com/ar/reference/java/com/google/ar/core/Session#configure-config)
//     * 또는
//     * [`setCameraConfig`](https://developers.google.com/ar/reference/java/com/google/ar/core/Session#setCameraConfig-cameraConfig)
//     * 를 사용하세요.
//     */
//    var installRequested = false
//    var session: Session? = null
//        private set
//    fun configureSession(session : Session){
//        // Create a session config.
//        session.configure(
//            session.config?.apply {
//                // enabled인 경우 어플에서 지리정보 데이터를 가져올 수 있다.
//                geospatialMode = Config.GeospatialMode.ENABLED
//            }
//        )
//    }
//
//    fun createSession(context : AppCompatActivity) {
//        try {
//            val permissionManager = PermissionManager(context)
//            permissionManager.requestPermissions(
//                arrayOf(
//                    Manifest.permission.CAMERA,
//                    Manifest.permission.ACCESS_FINE_LOCATION
//                ), object : PermissionCallback {
//                    override fun onPermissionGranted(grantedPermission: String) {
//                        // 카메라 권한이 허용된 경우 세션을 만듭니다.
//                        if (session == null) {
//                            try {
//                                session = Session(activity, features)
//                                session!!.resume()
//                            } catch (e: CameraNotAvailableException) {
//                                Log.e(
//                                    "mException",
//                                    "ArManager, createSession // CameraNotAvailableException(카메라 사용불가) : ${e.localizedMessage}"
//                                )
//                            }
//                        }
//
//
//
//                        val latitude = 47.3514 // 위도
//                        val longitude = 8.1949 // 경도
//                        val altitude = 415.0 // 고도
//
//                        val anchor = session?.earth?.createAnchor(
//                            latitude, longitude, altitude, 0f, 0f, 0f, 1f
//                        )
//
//
//                        // Request installation if necessary.
//                        when (ArCoreApk.getInstance()
//                            .requestInstall(context, !installRequested)!!) {
//                            ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
//                                installRequested = true
//                                // tryCreateSession will be called again, so we return null for now.
//                                return null
//                            }
//
//                            ArCoreApk.InstallStatus.INSTALLED -> {
//                                // Left empty; nothing needs to be done.
//                            }
//                        }
//
//                    }
//
//                    override fun onPermissionDenied(
//                        askPermissionAgain: Boolean,
//                        deniedPermission: String
//                    ) {
//                        when (askPermissionAgain) {
//                            true -> {
//                                Log.e("mException", "권한 이전에 거절한 적 있음, 거절된 권한 :${deniedPermission}")
//                            }
//
//                            false -> {
//                                Log.e("mException", "권한 이전에 거절한 적 없음, 거절된 권한 :${deniedPermission}")
//                            }
//                        }
//                    }
//
//                })
//        }catch (e: UnavailableArcoreNotInstalledException){
//                Log.e("mException", "ArManager, createSession // Exception : ARCore 설치 필요, ${e.localizedMessage}")
//        }catch (e: UnavailableUserDeclinedInstallationException){
//            Log.e("mException", "ArManager, createSession // Exception : ARCore 설치 필요, ${e.localizedMessage}")
//        }catch (e: UnavailableApkTooOldException){
//            Log.e("mException", "ArManager, createSession // Exception : APK 업뎃 필요, ${e.localizedMessage}")
//        }catch (e: UnavailableSdkTooOldException){
//            Log.e("mException", "ArManager, createSession // Exception : SDK 업뎃 필요, ${e.localizedMessage}")
//        }catch (e: UnavailableDeviceNotCompatibleException){
//            Log.e("mException", "ArManager, createSession // Exception : 기기호환 불가, ${e.localizedMessage}")
//        }catch (e:Exception){
//            Log.e("mException", "ArManager, createSession // Exception : ${e.localizedMessage}")
//        }
//
//
//        try {
//            // Create a new ARCore session.
//            val session = Session(context)
//            configureSession(session)
//
//            val earth = session.earth
//            if (earth?.trackingState == TrackingState.TRACKING){
//                val cameraGeospatialPose = earth.cameraGeospatialPose
//                val latitude = cameraGeospatialPose.latitude
//                val longitude = cameraGeospatialPose.longitude
//                val heading = cameraGeospatialPose.heading
//            }
//
//
//
//            // Do feature-specific operations here, such as enabling depth or turning on
//            // support for Augmented Faces.
//
//
//
//            // Request installation if necessary.
//            when (ArCoreApk.getInstance().requestInstall(context, !installRequested)!!) {
//                ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
//                    installRequested = true
//                    // tryCreateSession will be called again, so we return null for now.
//                    return
//                }
//                ArCoreApk.InstallStatus.INSTALLED -> {
//                    // Left empty; nothing needs to be done.
//                }
//            }
//        }catch (e:Exception){
//            Log.e("mException", "ArManager, createSession // Exception : ${e.localizedMessage}")
//        }
//    }
//    fun pauseSession(){
//        if (session != null){
//            session?.pause()
//        }
//    }
//    fun destroySession(){
//        if (session != null){
//            session?.close()
//            session = null
//        }
//    }
//}