package com.jupond.artravel

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableApkTooOldException
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import com.google.ar.core.exceptions.UnavailableException
import com.google.ar.core.exceptions.UnavailableSdkTooOldException

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val CAMERA_PERMISSION_CODE = 1001
        private const val LOCATION_PERMISSION_CODE = 1002
        private const val MIN_OPENGL_VERSION = 3.0
    }

    // AR 관련 변수
    private lateinit var glSurfaceView: GLSurfaceView
    private var arSession: Session? = null

    // UI 요소
    private lateinit var statusTextView: TextView
    private lateinit var distanceTextView: TextView
    private lateinit var infoButton: Button

    // 위치 관련 변수
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: Location? = null

    // 모델 관련 데이터
    private val historicalSites = mutableListOf<HistoricalSite>()
    private var currentSite: HistoricalSite? = null

    // AR 렌더러
    private var renderer: SimpleARRenderer? = null

    private fun testBasicARCore() {
        try {
            // 1. ARCore 설치 확인
            val availability = ArCoreApk.getInstance().checkAvailability(this)
            if (availability.isUnsupported) {
                statusTextView.text = "이 기기는 ARCore를 지원하지 않습니다."
                return
            }

            // 2. 세션 생성 테스트
            val testSession = Session(this)

            // 3. 기본 Config 테스트
            val config = Config(testSession)
            config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE

            // 4. 세션 구성 테스트
            testSession.configure(config)

            statusTextView.text = "기본 ARCore 테스트 성공!"

            // 테스트 후 정리
            testSession.close()

        } catch (e: Exception) {
            statusTextView.text = "테스트 실패: ${e.message}"
            Log.e(TAG, "ARCore 테스트 실패", e)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate() 호출됨")

        setContentView(R.layout.activity_main)

        // UI 초기화
        initializeViews()

        // 위치 서비스 초기화
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // 샘플 역사적 장소 데이터 로드
        loadSampleHistoricalSites()

        // AR 지원 여부 확인
        if (!checkIsSupportedDeviceOrFinish()) {
            return
        }

        // 권한 요청
        requestPermissions()

        checkARCoreInstalled()
        createARSession()
        testBasicARCore()

    }

    private fun checkARCoreInstalled(): Boolean {
        val availability = ArCoreApk.getInstance().checkAvailability(this)

        when (availability) {
            ArCoreApk.Availability.SUPPORTED_INSTALLED -> {
                Log.d(TAG, "ARCore 설치됨")
                return true
            }
            ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED,
            ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD -> {
                Log.d(TAG, "ARCore가 설치되지 않았거나 오래됨")
                return false
            }
            else -> {
                Log.d(TAG, "ARCore 지원되지 않음: $availability")
                return false
            }
        }
    }

    private fun createARSession(): Session? {
        try {
            val session = Session(this)
            Log.d(TAG, "AR 세션 생성 성공")
            return session
        } catch (e: UnavailableArcoreNotInstalledException) {
            Log.e(TAG, "ARCore가 설치되지 않음", e)
            Toast.makeText(this, "ARCore를 설치해주세요", Toast.LENGTH_LONG).show()
        } catch (e: UnavailableApkTooOldException) {
            Log.e(TAG, "ARCore가 오래됨", e)
            Toast.makeText(this, "ARCore를 업데이트해주세요", Toast.LENGTH_LONG).show()
        } catch (e: UnavailableSdkTooOldException) {
            Log.e(TAG, "앱 SDK가 오래됨", e)
            Toast.makeText(this, "앱 업데이트가 필요합니다", Toast.LENGTH_LONG).show()
        } catch (e: UnavailableDeviceNotCompatibleException) {
            Log.e(TAG, "기기가 ARCore를 지원하지 않음", e)
            Toast.makeText(this, "이 기기는 AR을 지원하지 않습니다", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e(TAG, "AR 세션 생성 중 알 수 없는 오류: ${e.message}", e)
            Toast.makeText(this, "AR 초기화 오류: ${e.message}", Toast.LENGTH_LONG).show()
        }
        return null
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart() 호출됨")
    }

    private fun initializeViews() {
        try {
            glSurfaceView = findViewById(R.id.gl_surface_view)
            statusTextView = findViewById(R.id.status_text)
            distanceTextView = findViewById(R.id.distance_text)
            infoButton = findViewById(R.id.info_button)

            infoButton.setOnClickListener {
                currentSite?.let { site ->
                    showHistoricalSiteInfo(site)
                }
            }

            Log.d(TAG, "Views 초기화 완료")
        } catch (e: Exception) {
            Log.e(TAG, "Views 초기화 오류: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun requestPermissions() {
        // 카메라 권한 요청
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        } else {
            setupArCore()
        }

        // 위치 권한 요청
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_CODE
            )
        } else {
            // 위치 권한이 있으면 위치 업데이트 시작
            try {
                startLocationUpdates()
            } catch (e: Exception) {
                Log.e(TAG, "위치 업데이트 시작 오류: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            CAMERA_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setupArCore()
                } else {
                    Toast.makeText(
                        this,
                        "카메라 권한이 필요합니다",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            }
            LOCATION_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLocationUpdates()
                } else {
                    Toast.makeText(
                        this,
                        "위치 권한이 필요합니다",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun setupArCore() {
        // ARCore 초기화 확인
        val availability = ArCoreApk.getInstance().checkAvailability(this)
        if (availability.isTransient) {
            Log.i(TAG, "ARCore는 일시적으로 사용할 수 없습니다. 나중에 다시 시도합니다.")
            return
        }

        if (availability.isUnsupported) {
            Toast.makeText(this, "이 기기는 ARCore를 지원하지 않습니다.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        try {
            // ARCore 설치 요청
            when (ArCoreApk.getInstance().requestInstall(this, true)) {
                ArCoreApk.InstallStatus.INSTALLED -> {
                    // ARCore가 이미 설치되어 있음, 계속 진행
                    initARSession()
                }
                ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                    // 설치가 요청됨, 설치 후 다시 시도
                    Log.i(TAG, "ARCore 설치가 요청되었습니다.")
                    return
                }
                else -> {
                    Log.i(TAG, "ARCore 설치 상태 알 수 없음")
                    return
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "ARCore 설치 요청 중 오류 발생: ${e.message}")
            Toast.makeText(this, "ARCore 초기화 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun initARSession() {
        try {
            Log.d(TAG, "세션 초기화 시작")

            if (arSession == null) {
                // 각 단계마다 상세 로그 추가
                Log.d(TAG, "1. ARCore 설치 상태 확인")
                val availability = ArCoreApk.getInstance().checkAvailability(this)
                Log.d(TAG, "ARCore 사용 가능 여부: $availability")

                if (availability.isTransient || availability.isUnsupported) {
                    Log.e(TAG, "ARCore를 사용할 수 없음: $availability")
                    statusTextView.text = "이 기기에서 ARCore를 사용할 수 없습니다"
                    return
                }

                // ARCore 설치 요청
                Log.d(TAG, "2. ARCore 설치 요청")
                val installStatus = ArCoreApk.getInstance().requestInstall(this, true)
                Log.d(TAG, "ARCore 설치 상태: $installStatus")

                if (installStatus != ArCoreApk.InstallStatus.INSTALLED) {
                    Log.d(TAG, "ARCore가 설치되지 않음, 설치 후 다시 시도")
                    return
                }

                // 세션 생성
                Log.d(TAG, "3. 세션 생성")
                try {
                    arSession = Session(this)
                    Log.d(TAG, "세션 생성 성공: $arSession")
                } catch (e: Exception) {
                    Log.e(TAG, "세션 생성 실패: ${e.message}", e)
                    statusTextView.text = "세션 생성 실패: ${e.message}"
                    return
                }

                // 구성 전에 카메라 권한 확인
                Log.d(TAG, "4. 카메라 권한 확인")
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "카메라 권한이 없습니다")
                    return
                }

                // ARCore 세션 구성
                Log.d(TAG, "5. 세션 구성 시도")
                try {
                    val config = Config(arSession)
                    Log.d(TAG, "Config 객체 생성 성공: $config")

                    config.depthMode = Config.DepthMode.AUTOMATIC
                    config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                    config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                    config.focusMode = Config.FocusMode.AUTO
                    config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR

                    Log.d(TAG, "ARCore 세션 구성 설정 완료")

                    arSession?.configure(config)
                    Log.d(TAG, "ARCore 세션 구성 성공")

                    statusTextView.text = "AR 세션이 초기화되었습니다. 주변을 둘러보세요."

                    // GLSurfaceView 설정
                    Log.d(TAG, "6. GLSurfaceView 설정")
                    setupGLSurfaceView()

                } catch (e: Exception) {
                    Log.e(TAG, "세션 구성 중 오류 발생: ${e.message}", e)
                    e.printStackTrace()
                    // 여기서 Stack trace 전체를 로그로 출력
                    for (element in e.stackTrace) {
                        Log.e(TAG, element.toString())
                    }
                    statusTextView.text = "AR 초기화 구성 실패: ${e.message}"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "세션 초기화 중 예외 발생: ${e.message}", e)
            e.printStackTrace()
            for (element in e.stackTrace) {
                Log.e(TAG, element.toString())
            }
            statusTextView.text = "AR 초기화 실패: ${e.message}"
        }
    }
    private fun setupGLSurfaceView() {
        try {
            // 표면 뷰가 투명하도록 설정 (카메라가 보이게)
            glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0)
            glSurfaceView.holder.setFormat(android.graphics.PixelFormat.TRANSLUCENT)
            glSurfaceView.setZOrderMediaOverlay(true)  // 카메라가 제대로 보이도록 설정

            glSurfaceView.setEGLContextClientVersion(2)
            glSurfaceView.preserveEGLContextOnPause = true

            // AR 렌더러 생성
            arSession?.let {
                renderer = SimpleARRenderer(this, it)
                glSurfaceView.setRenderer(renderer)
                glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

                Log.i(TAG, "GLSurfaceView 설정 완료")
            } ?: run {
                Log.e(TAG, "렌더러 설정 실패: arSession이 null입니다.")
                statusTextView.text = "AR 렌더러 초기화 실패"
            }
        } catch (e: Exception) {
            Log.e(TAG, "GLSurfaceView 설정 중 오류: ${e.message}")
            e.printStackTrace()
            statusTextView.text = "화면 설정 오류: ${e.message}"
        }
    }

    private fun startLocationUpdates() {
        try {
            if (::fusedLocationClient.isInitialized) {  // fusedLocationClient가 초기화되었는지 확인
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                    return
                }

                // 위치 요청 설정
                val locationRequest = com.google.android.gms.location.LocationRequest.create().apply {
                    priority = com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
                    interval = 10000  // 10초마다 업데이트
                    fastestInterval = 5000  // 5초마다 최대 빈도로 업데이트
                }

                // 위치 콜백
                val locationCallback = object : com.google.android.gms.location.LocationCallback() {
                    override fun onLocationResult(locationResult: com.google.android.gms.location.LocationResult) {
                        locationResult.lastLocation?.let {
                            currentLocation = it
                            Log.d(TAG, "위치 업데이트: ${it.latitude}, ${it.longitude}")
                            updateNearbyHistoricalSites()
                        }
                    }
                }

                // 지속적인 위치 업데이트 요청
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    null
                )

                // 또한 마지막 알려진 위치도 가져옵니다
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        location?.let {
                            currentLocation = it
                            Log.d(TAG, "마지막 위치: ${it.latitude}, ${it.longitude}")
                            updateNearbyHistoricalSites()
                        } ?: run {
                            Log.w(TAG, "마지막 위치를 가져올 수 없습니다")
                            // 실제 위치를 가져올 수 없는 경우, 테스트 목적으로 가상 위치 설정
                            createTestLocation()
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "위치 가져오기 실패: ${e.message}")
                        // 실패할 경우 테스트 위치 생성
                        createTestLocation()
                    }
            } else {
                Log.e(TAG, "fusedLocationClient가 초기화되지 않았습니다")
                // 필요하다면 여기서 초기화 시도
                fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
                // 그리고 나중에 다시 시도
            }
        } catch (e: Exception) {
            Log.e(TAG, "위치 업데이트 오류: ${e.message}")
            e.printStackTrace()

            // 오류 발생 시 테스트 위치 생성
            createTestLocation()
        }
    }

    // 테스트 목적의 가상 위치 생성
    private fun createTestLocation() {
        Log.d(TAG, "테스트용 가상 위치 생성")

        // 테스트 위치 (집앞 테스트 위치와 비슷하게 설정)
        val testLocation = Location("test").apply {
            latitude = 37.578017895229664
            longitude = 126.6711298166958
        }
        currentLocation = testLocation

        // 위치 업데이트
        updateNearbyHistoricalSites()
    }


    private fun updateNearbyHistoricalSites() {
        currentLocation?.let { location ->
            // 현재 위치에서 가장 가까운 역사적 건축물 찾기
            var nearestSite: HistoricalSite? = null
            var minDistance = Double.MAX_VALUE

            for (site in historicalSites) {
                val distance = calculateDistance(
                    location.latitude, location.longitude,
                    site.latitude, site.longitude
                )

                if (distance < minDistance) {
                    minDistance = distance
                    nearestSite = site
                }
            }

            // 가장 가까운 건축물이 100m 이내일 경우 AR 모델 로드
            if (minDistance < 100.0) {
                currentSite = nearestSite

                distanceTextView.text = "거리: ${String.format("%.1f", minDistance)}m"
                statusTextView.text = "발견: ${nearestSite?.name}"
                infoButton.visibility = View.VISIBLE

                // AR 모델 표시 활성화 (렌더러에게 알림)
                renderer?.setCurrentSite(nearestSite)

            } else {
                statusTextView.text = "주변에 역사적 건축물이 없습니다 (${String.format("%.1f", minDistance)}m)"
                infoButton.visibility = View.GONE

                // AR 모델 표시 비활성화
                renderer?.setCurrentSite(null)
            }
        }
    }

    private fun showHistoricalSiteInfo(site: HistoricalSite) {
        // 건축물 정보 다이얼로그 표시 (실제 앱에서는 더 자세한 UI 필요)
        Toast.makeText(
            this,
            "${site.name} (${site.originalYear}년): ${site.description}",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        // Haversine 공식을 사용한 두 지점 간의 거리 계산 (미터 단위)
        val r = 6371e3 // 지구 반경 (미터)
        val φ1 = Math.toRadians(lat1)
        val φ2 = Math.toRadians(lat2)
        val Δφ = Math.toRadians(lat2 - lat1)
        val Δλ = Math.toRadians(lon2 - lon1)

        val a = Math.sin(Δφ / 2) * Math.sin(Δφ / 2) +
                Math.cos(φ1) * Math.cos(φ2) *
                Math.sin(Δλ / 2) * Math.sin(Δλ / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return r * c
    }

    private fun loadSampleHistoricalSites() {
        // 샘플 데이터 - modelPath를 문자열 경로로 변경
        historicalSites.add(
            HistoricalSite(
                id = "site1",
                name = "집앞 테스트",
                description = "집앞 어린이집 조형물테스트",
                latitude = 37.578017895229664,
                longitude = 126.6711298166958,
                modelPath = "build3d.obj", // assets 폴더 내 파일명
                originalYear = 1395
            )
        )

        historicalSites.add(
            HistoricalSite(
                id = "site2",
                name = "수원 화성",
                description = "정조가 1796년에 완공한 성곽으로, 전통성곽 건축기술과 서양의 과학기술이 결합된 독특한 건축물입니다.",
                latitude = 37.287568,
                longitude = 127.012888,
                modelPath = "build3d.obj", // assets 폴더 내 파일명
                originalYear = 1796
            )
        )

        // 테스트를 위해 현재 위치 근처에 가상의 유적지 추가
        currentLocation?.let { location ->
            historicalSites.add(
                HistoricalSite(
                    id = "site_nearby",
                    name = "현재 위치 테스트 건축물",
                    description = "테스트용 가상 건축물입니다. 실제 위치에 맞게 수정해야 합니다.",
                    latitude = location.latitude + 0.0001, // 약 10-20미터 거리
                    longitude = location.longitude + 0.0001,
                    modelPath = "build3d.obj", // assets 폴더 내 파일명
                    originalYear = 1800
                )
            )

            // 가까운 사이트 업데이트
            updateNearbyHistoricalSites()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume() 호출됨")

        // AR 세션이 있는 경우에만 재개
        try {
            arSession?.resume()

            // GLSurfaceView가 setRenderer()가 호출된 경우에만 onResume() 호출
            if (renderer != null) {
                glSurfaceView.onResume()
                Log.d(TAG, "GLSurfaceView.onResume() 호출됨")
            } else {
                Log.d(TAG, "렌더러가 null이므로 GLSurfaceView.onResume() 호출 건너뜀")
                // 세션이 없는 경우 권한이 있으면 초기화
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                    setupArCore()
                }
            }
        } catch (e: CameraNotAvailableException) {
            Log.e(TAG, "카메라를 사용할 수 없습니다: ${e.message}")
            statusTextView.text = "카메라를 사용할 수 없습니다"
            e.printStackTrace()
        } catch (e: Exception) {
            Log.e(TAG, "onResume 중 오류: ${e.message}")
            e.printStackTrace()
        }

        // 위치 업데이트 재개
        startLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause() 호출됨")

        try {
            arSession?.pause()

            // GLSurfaceView가 setRenderer()가 호출된 경우에만 onPause() 호출
            if (renderer != null) {
                glSurfaceView.onPause()
                Log.d(TAG, "GLSurfaceView.onPause() 호출됨")
            }
        } catch (e: Exception) {
            Log.e(TAG, "onPause 중 오류: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy() 호출됨")

        try {
            arSession?.close()
            arSession = null
        } catch (e: Exception) {
            Log.e(TAG, "onDestroy 중 오류: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun checkIsSupportedDeviceOrFinish(): Boolean {
        val openGlVersionString = (getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager)
            .deviceConfigurationInfo
            .glEsVersion

        if (java.lang.Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Toast.makeText(
                this,
                "ARCore는 OpenGL ES $MIN_OPENGL_VERSION 이상이 필요합니다",
                Toast.LENGTH_LONG
            ).show()
            finish()
            return false
        }

        return true
    }

    // 역사적 건축물 데이터 클래스
    data class HistoricalSite(
        val id: String,
        val name: String,
        val description: String,
        val latitude: Double,
        val longitude: Double,
        val modelPath: String,
        val originalYear: Int
    )
}