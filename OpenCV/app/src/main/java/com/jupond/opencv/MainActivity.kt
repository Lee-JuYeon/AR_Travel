package com.jupond.opencv

import android.content.pm.PackageManager
import org.opencv.android.OpenCVLoader
import org.opencv.core.Mat
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.JavaCameraView
import org.opencv.core.CvType
import org.opencv.imgproc.Imgproc
import android.util.Log
import android.view.SurfaceView
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.graphics.PixelFormat
import android.opengl.GLSurfaceView
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity(), CameraBridgeViewBase.CvCameraViewListener2 {

    companion object {
        private const val TAG = "d"
        private const val CAMERA_PERMISSION_REQUEST = 100
        private const val LOCATION_PERMISSION_REQUEST = 101 // 추가된 부분

        // OpenCV 라이브러리 초기화
        init {
            if (OpenCVLoader.initDebug()) {
                Log.d(TAG, "OpenCV 초기화 성공")
            } else {
                Log.e(TAG, "OpenCV 초기화 실패")
            }
        }
    }

    // 뷰 및 컴포넌트
    private lateinit var mOpenCvCameraView: JavaCameraView
    private lateinit var glSurfaceView: GLSurfaceView
    private lateinit var statusTextView: TextView
    private lateinit var captureButton: Button

    // 기능 모듈
    private lateinit var featureTracker: FeatureTracker
    private lateinit var sensorFusion: SensorFusion
    private lateinit var modelRenderer: ModelRenderer
    private lateinit var performanceOptimizer: PerformanceOptimizer

    // 데이터 및 상태
    private var mRgba: Mat? = null
    private val isRendering = AtomicBoolean(false)
    private var currentHomography: Mat? = null
    private var isModelVisible = false

    // 건물 모델 모서리점
    private val modelCorners = MatOfPoint2f(
        Point(0.0, 0.0),
        Point(500.0, 0.0),
        Point(500.0, 500.0),
        Point(0.0, 500.0)
    )

    // 클래스 멤버 변수로 추가
    private lateinit var buildingModelManager: BuildingModelManager
    private lateinit var historicalInfoManager: HistoricalInfoManager
    private lateinit var infoButton: Button
    private lateinit var buildingInfoDialog: AlertDialog

    private lateinit var geoAnchoredARSystem: GeoAnchoredARSystem
    private lateinit var depthEstimationSystem: DepthEstimationSystem
    private var useDepthEstimation = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 뷰 초기화
        statusTextView = findViewById(R.id.status_text)
        captureButton = findViewById(R.id.capture_button)

        // 기능 모듈 초기화
        featureTracker = FeatureTracker()
        sensorFusion = SensorFusion(this)
        performanceOptimizer = PerformanceOptimizer(this)

        // 건물 모델 및 정보 관리자 초기화
        buildingModelManager = BuildingModelManager(this)
        historicalInfoManager = HistoricalInfoManager(this)

        // 정보 버튼 설정
        infoButton = findViewById(R.id.info_button)
        infoButton.setOnClickListener {
            showBuildingInfoDialog()
        }
        infoButton.visibility = View.GONE // 초기에는 숨김

        // 3D 렌더링 초기화
        setupGLView()

        // 버튼 이벤트 설정
        captureButton.setOnClickListener {
            if (isModelVisible) {
                resetTracking()
            } else {
                startModelAlignment()
            }
        }

        // 위치 권한 확인 및 요청 (세부 코드 생략)
        checkAndRequestLocationPermissions()

        // 시스템 초기화
        geoAnchoredARSystem = GeoAnchoredARSystem(this)
        depthEstimationSystem = DepthEstimationSystem()

        // 건축물 앵커 추가 (실제 위치로 수정 필요)
        geoAnchoredARSystem.addAnchor(
            "gyeongbokgung",
            37.579617, 126.977041, 38.0, // 경복궁 위치 (위도, 경도, 고도)
            0f, 1.0f // 방향, 크기
        )


        // 카메라 권한 확인
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // 권한이 없으면 요청
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST)
        } else {
            // 이미 권한이 있으면 카메라 초기화
            initializeCamera()
        }

        // 초기 상태 설정
        updateStatusText("카메라를 건물에 맞춰주세요")
    }

    private fun setupGLView() {
        // GLSurfaceView 초기화
        glSurfaceView = findViewById(R.id.gl_surface_view)
        glSurfaceView.setEGLContextClientVersion(2)
        glSurfaceView.setZOrderOnTop(true)
        glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        glSurfaceView.holder.setFormat(PixelFormat.TRANSLUCENT)

        // 렌더러 초기화 및 설정
        modelRenderer = ModelRenderer(this)
        glSurfaceView.setRenderer(modelRenderer)
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY

        // 초기에는 모델 렌더링 비활성화
        glSurfaceView.visibility = View.INVISIBLE
    }

    private fun initializeCamera() {
        mOpenCvCameraView = findViewById(R.id.opencv_camera_view)
        mOpenCvCameraView.visibility = SurfaceView.VISIBLE
        mOpenCvCameraView.setCameraPermissionGranted() // 중요: 권한 승인 상태 설정
        mOpenCvCameraView.setCvCameraViewListener(this)
        mOpenCvCameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_BACK)
        mOpenCvCameraView.enableFpsMeter() // FPS 표시 (디버깅용)
        mOpenCvCameraView.enableView()
        Log.d(TAG, "카메라 뷰 초기화 완료")
    }

    override fun onResume() {
        super.onResume()
        if (::mOpenCvCameraView.isInitialized) {
            mOpenCvCameraView.enableView()
            Log.d(TAG, "onResume: 카메라 뷰 활성화")
        }
        if (::glSurfaceView.isInitialized) {
            glSurfaceView.onResume()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::mOpenCvCameraView.isInitialized) {
            mOpenCvCameraView.disableView()
            Log.d(TAG, "onPause: 카메라 뷰 비활성화")
        }
        if (::glSurfaceView.isInitialized) {
            glSurfaceView.onPause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::mOpenCvCameraView.isInitialized) {
            mOpenCvCameraView.disableView()
            Log.d(TAG, "onDestroy: 카메라 뷰 비활성화")
        }
        if (::sensorFusion.isInitialized) {
            sensorFusion.stop()
        }

        // 추가 리소스 해제
        if (::geoAnchoredARSystem.isInitialized) {
            geoAnchoredARSystem.stop()
        }

        if (::depthEstimationSystem.isInitialized) {
            depthEstimationSystem.release()
        }

        // 메모리 정리
        mRgba?.release()
        currentHomography?.release()

    }

    private fun startModelAlignment() {
        updateStatusText("건물 인식 중...")
        captureButton.text = "재설정"

        // 특징점 트래킹 초기화
        featureTracker.reset()

        // 렌더링 활성화
        isRendering.set(true)
    }

    private fun resetTracking() {
        updateStatusText("카메라를 건물에 맞춰주세요")
        captureButton.text = "모델 표시하기"

        // 특징점 트래킹 및 모델 렌더링 리셋
        featureTracker.reset()
        isRendering.set(false)
        isModelVisible = false
        glSurfaceView.visibility = View.INVISIBLE

        // 버튼 숨기기
        infoButton.visibility = View.GONE
    }

    private fun updateStatusText(message: String) {
        runOnUiThread {
            statusTextView.text = message
        }
    }

    override fun onCameraViewStarted(width: Int, height: Int) {
        Log.d(TAG, "onCameraViewStarted: 너비=$width, 높이=$height")
        mRgba = Mat(height, width, CvType.CV_8UC4)
        captureButton.visibility = View.VISIBLE
    }

    override fun onCameraViewStopped() {
        Log.d(TAG, "onCameraViewStopped")
        mRgba?.release()
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame): Mat {
        // 성능 측정 시작
        performanceOptimizer.beginFrame()

        // 카메라 프레임 가져오기
        val rgba = inputFrame.rgba()

        // 깊이 추정 (성능에 따라 선택적 사용)
        var depthMap: Mat? = null
        if (useDepthEstimation && performanceOptimizer.shouldUseEffects()) {
            depthMap = depthEstimationSystem.estimateDepth(rgba)

            // 깊이 맵이 업데이트되면 OpenGL 깊이 텍스처 업데이트
            if (depthEstimationSystem.isDepthMapUpdated()) {
                val depthTextureData = depthEstimationSystem.getDepthTextureData()
                // 3D 렌더러에 깊이 텍스처 전달
                modelRenderer.updateDepthTexture(depthTextureData)
            }
        }

        // 현재 보이는 앵커 확인
        val visibleAnchorId = findVisibleAnchor()

        if (visibleAnchorId != null) {
            // 위치 기반 모델 행렬 계산
            val modelMatrix = geoAnchoredARSystem.calculateModelMatrix(visibleAnchorId)

            if (modelMatrix != null) {
                // 3D 모델 위치 업데이트 및 렌더링
                modelRenderer.updateModelMatrixFromGPS(modelMatrix)

                // LOD 레벨 설정 (성능 최적화)
                modelRenderer.setLODLevel(performanceOptimizer.getCurrentLODLevel())

                // 렌더링 요청
                glSurfaceView.visibility = View.VISIBLE
                glSurfaceView.requestRender()

                // 건물 정보 버튼 표시
                infoButton.visibility = View.VISIBLE
            } else {
                glSurfaceView.visibility = View.INVISIBLE
                infoButton.visibility = View.GONE
            }
        } else {
            glSurfaceView.visibility = View.INVISIBLE
            infoButton.visibility = View.GONE
        }


//        if (isRendering.get()) {
//            // 특징점 트래킹 및 호모그래피 계산
//            val homography = featureTracker.trackFeatures(rgba)
//
//            // 센서 데이터와 비전 데이터 융합
//            currentHomography = sensorFusion.fuseWithVision(homography)
//
//            // 모델 렌더링 상태 업데이트
//            val wasModelVisible = isModelVisible
//            isModelVisible = currentHomography != null
//
//            // 상태 변경 시 UI 업데이트
//            if (wasModelVisible != isModelVisible) {
//                runOnUiThread {
//                    if (isModelVisible) {
//                        updateStatusText("건물 트래킹 중...")
//                        glSurfaceView.visibility = View.VISIBLE
//                        infoButton.visibility = View.VISIBLE // 모델이 보일 때 정보 버튼 표시
//                    } else {
//                        updateStatusText("건물을 다시 찾는 중...")
//                        glSurfaceView.visibility = View.INVISIBLE
//                        infoButton.visibility = View.GONE // 모델이 안 보일 때 정보 버튼 숨김
//                    }
//                }
//            }
//
//            // 3D 모델 위치 업데이트 및 렌더링
//            if (isModelVisible) {
//                runOnUiThread {
//                    modelRenderer.updateModelMatrix(currentHomography)
//
//                    // LOD 레벨 설정 (성능 최적화)
//                    modelRenderer.setLODLevel(performanceOptimizer.getCurrentLODLevel())
//
//                    // 렌더링 요청
//                    glSurfaceView.requestRender()
//                }
//            }
//
//            // 디버깅용 특징점 및 모델 윤곽 표시
//            if (performanceOptimizer.shouldUseEffects()) {
//                featureTracker.drawModelOverlay(rgba, currentHomography, modelCorners)
//            }
//        }

        // 성능 측정 종료
        performanceOptimizer.endFrame()

        return rgba
    }


    // 현재 보이는 앵커 찾기
    private fun findVisibleAnchor(): String? {
        for (anchorId in buildingModelManager.getAllBuildingIds()) {
            if (geoAnchoredARSystem.isAnchorVisible(anchorId)) {
                return anchorId
            }
        }
        return null
    }


    // 건물 정보 다이얼로그 표시
    private fun showBuildingInfoDialog() {
        val currentModelId = buildingModelManager.getCurrentModel()?.id ?: return
        val buildingInfo = historicalInfoManager.getBuildingInfoById(currentModelId) ?: return

        // 다이얼로그 뷰 생성
        val dialogView = layoutInflater.inflate(R.layout.dialog_building_info, null)
        val titleTextView = dialogView.findViewById<TextView>(R.id.dialog_title)
        val descriptionTextView = dialogView.findViewById<TextView>(R.id.dialog_description)
        val yearTextView = dialogView.findViewById<TextView>(R.id.dialog_year)
        val featuresListView = dialogView.findViewById<ListView>(R.id.dialog_features_list)
        val eventsListView = dialogView.findViewById<ListView>(R.id.dialog_events_list)

        // 정보 설정
        titleTextView.text = buildingInfo.name
        descriptionTextView.text = buildingInfo.description
        yearTextView.text = "건립 연도: ${buildingInfo.year}년"

        // 건축적 특징 목록 설정
        val featuresAdapter = ArrayAdapter<String>(
            this,
            android.R.layout.simple_list_item_1,
            buildingInfo.architecturalFeatures
        )
        featuresListView.adapter = featuresAdapter

        // 역사적 사건 목록 설정
        val eventsAdapter = ArrayAdapter<String>(
            this,
            android.R.layout.simple_list_item_1,
            buildingInfo.historicalEvents
        )
        eventsListView.adapter = eventsAdapter

        // 다이얼로그 생성 및 표시
        val builder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("닫기") { dialog, _ -> dialog.dismiss() }

        buildingInfoDialog = builder.create()
        buildingInfoDialog.show()
    }

    // MainActivity 클래스에 추가
    private fun checkAndRequestLocationPermissions() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        val permissionsToRequest = ArrayList<String>()

        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) !=
                PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                LOCATION_PERMISSION_REQUEST
            )
        }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)



        when (requestCode) {
            CAMERA_PERMISSION_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "카메라 권한 승인됨")
                    initializeCamera()
                } else {
                    Log.e(TAG, "카메라 권한 거부됨")
                    Toast.makeText(this, "카메라 권한이 필요합니다", Toast.LENGTH_LONG).show()
                    finish()
                }
            }

            LOCATION_PERMISSION_REQUEST -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "위치 권한 승인됨")
                    // 위치 기반 시스템 초기화
                    if (::geoAnchoredARSystem.isInitialized) {
                        geoAnchoredARSystem = GeoAnchoredARSystem(this)
                    }
                } else {
                    Log.e(TAG, "위치 권한 거부됨")
                    Toast.makeText(
                        this,
                        "위치 권한이 필요합니다. 일부 기능이 제한됩니다.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}