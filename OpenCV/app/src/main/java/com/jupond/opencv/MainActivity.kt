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
import android.content.Context
import android.graphics.PixelFormat
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import org.opencv.calib3d.Calib3d
import org.opencv.core.Core
import org.opencv.core.MatOfKeyPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.features2d.ORB
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.ZipInputStream
class MainActivity : AppCompatActivity(), CameraBridgeViewBase.CvCameraViewListener2 {

    companion object {
        private const val TAG = "d공"
        private const val CAMERA_PERMISSION_REQUEST = 100
        private const val LOCATION_PERMISSION_REQUEST = 101

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
    private lateinit var infoButton: Button

    // 기능 모듈
    private lateinit var featureTracker: FeatureTracker
    private lateinit var sensorFusion: SensorFusion
    private lateinit var modelRenderer: ModelRenderer
    private lateinit var performanceOptimizer: PerformanceOptimizer
    private lateinit var buildingModelManager: BuildingModelManager
    private lateinit var historicalInfoManager: HistoricalInfoManager
    private lateinit var geoAnchoredARSystem: GeoAnchoredARSystem
    private lateinit var depthEstimationSystem: DepthEstimationSystem

    // 데이터 및 상태
    private var mRgba: Mat? = null
    private val isRendering = AtomicBoolean(false)
    private var isModelVisible = false
    private var useDepthEstimation = true
    private lateinit var buildingInfoDialog: AlertDialog

    // 건물 모델 모서리점
    private val modelCorners = MatOfPoint2f(
        Point(0.0, 0.0),
        Point(500.0, 0.0),
        Point(500.0, 500.0),
        Point(0.0, 500.0)
    )

    // 테스트 위치 좌표
    private val TEST_LATITUDE = 37.578017895229664
    private val TEST_LONGITUDE = 126.6711298166958
    private val TEST_ALTITUDE = 50.0

    // 필요한 상수들
    private val FLOOR_HEIGHT = 3.0f  // 층 높이 (미터)
    private val MODEL_BASE_SCALE = 0.2f  // 기본 모델 크기
    private val MODEL_DISTANCE = 50.0f  // 모델까지의 기본 거리
    private lateinit var sensorManager: SensorManager  // 센서 관리자

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 뷰 초기화
        initializeViews()

        // 기능 모듈 초기화
        initializeModules()

        // 권한 확인 및 요청
        checkAndRequestPermissions()

        // 모델 로드
        loadBuild3dModel()

        // 초기 상태 설정
        updateStatusText("카메라를 건물에 맞춰주세요")
    }

    private fun initializeViews() {
        statusTextView = findViewById(R.id.status_text)
        captureButton = findViewById(R.id.capture_button)
        infoButton = findViewById(R.id.info_button)

        infoButton.setOnClickListener {
            showBuildingInfoDialog()
        }
        infoButton.visibility = View.GONE

        // 버튼 이벤트 설정
        captureButton.setOnClickListener {
            if (isModelVisible) {
                resetTracking()
            } else {
                startModelAlignment()
            }
        }

        // 3D 렌더링 초기화
        setupGLView()
    }

    private fun initializeModules() {
        featureTracker = FeatureTracker()
        sensorFusion = SensorFusion(this)
        performanceOptimizer = PerformanceOptimizer(this)
        buildingModelManager = BuildingModelManager(this)
        historicalInfoManager = HistoricalInfoManager(this)
        depthEstimationSystem = DepthEstimationSystem()
        geoAnchoredARSystem = GeoAnchoredARSystem(this)

        // 센서 관리자 초기화
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        // 테스트 위치에 AR 앵커 추가
        geoAnchoredARSystem.addAnchor(
            "test_building",
            TEST_LATITUDE, TEST_LONGITUDE, TEST_ALTITUDE,
            0f, 1.0f // 방향, 크기
        )

        // 센서 기반 자동 배치 시스템 설정
        setupSensorBasedPositioning()

        // AR 앵커 정착 시스템 개선
        setupImprovedARAnchoring()
    }

    private fun setupSensorBasedPositioning() {
        // 디바이스 방향 감지 센서 활용
        val rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        if (rotationVector == null) {
            Log.e(TAG, "회전 벡터 센서를 찾을 수 없습니다. 기본 방식으로 대체합니다.")
            return
        }

        sensorManager.registerListener(object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    // 디바이스 방향 행렬 계산
                    val rotationMatrix = FloatArray(16)
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

                    // 디바이스 방향을 고려한 모델 위치 조정
                    updateModelPositionBasedOnDeviceOrientation(rotationMatrix)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
        }, rotationVector, SensorManager.SENSOR_DELAY_GAME)
    }

    private fun updateModelPositionBasedOnDeviceOrientation(deviceRotationMatrix: FloatArray) {
        // 디바이스가 바라보는 방향 벡터 계산
        val lookVector = FloatArray(4)
        val lookDir = FloatArray(4)
        lookVector[0] = 0f
        lookVector[1] = 0f
        lookVector[2] = -1f
        lookVector[3] = 0f

        Matrix.multiplyMV(lookDir, 0, deviceRotationMatrix, 0, lookVector, 0)

        // 현재 높이(38층)와 목표 높이(1층)의 차이 계산
        val heightDifference = 37 * FLOOR_HEIGHT // 층간 높이 (약 3m)

        // 모델 위치 계산 (높이차 고려)
        val pitch = Math.atan2(heightDifference.toDouble(), MODEL_DISTANCE.toDouble()).toFloat()

        // 모델 행렬 설정
        val modelMatrix = FloatArray(16)
        Matrix.setIdentityM(modelMatrix, 0)

        // 디바이스 방향에 따라 모델 위치 조정
        Matrix.translateM(modelMatrix, 0,
            lookDir[0] * MODEL_DISTANCE,
            -heightDifference + lookDir[1] * MODEL_DISTANCE,
            lookDir[2] * MODEL_DISTANCE)

        // 모델 회전 - 디바이스 방향에 맞춤
        val deviceOrientation = FloatArray(3)
        SensorManager.getOrientation(deviceRotationMatrix, deviceOrientation)

        // 적절한 회전 적용
        Matrix.rotateM(modelMatrix, 0,
            Math.toDegrees(deviceOrientation[0].toDouble()).toFloat(), 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0,
            Math.toDegrees(pitch.toDouble()).toFloat(), 1f, 0f, 0f)

        // 모델 크기 조정 - 거리에 따른 자동 크기 조정
        val scaleFactorBasedOnDistance = MODEL_BASE_SCALE * (30f / MODEL_DISTANCE)
        Matrix.scaleM(modelMatrix, 0, scaleFactorBasedOnDistance,
            scaleFactorBasedOnDistance, scaleFactorBasedOnDistance)

        // 모델 위치 업데이트
        if (isModelVisible) {
            modelRenderer.updateModelMatrixFromGPS(modelMatrix)
            glSurfaceView.requestRender()
        }
    }

    // AR 앵커 정착 시스템 구현
    private fun setupImprovedARAnchoring() {
        // 카메라 프레임에서 바닥면 특징점을 감지하여 AR 앵커 개선
        Log.d(TAG, "향상된 AR 앵커링 시스템 초기화")
    }

    // 카메라 프레임에서 바닥면 특징점 감지
    private fun detectFloorFeatures(frame: Mat): List<Point>? {
        // 실제 구현에서는 바닥면 특징점 감지 로직 추가
        // 간소화된 예시: 프레임 하단부에서 특징점 감지
        try {
            val roi = Mat(frame, Rect(0, frame.rows() * 2 / 3, frame.cols(), frame.rows() / 3))
            val features = MatOfKeyPoint()
            val detector = ORB.create(50)  // 최대 50개 특징점 감지
            detector.detect(roi, features)

            val keypoints = features.toList()
            if (keypoints.isEmpty()) {
                return null
            }

            // 특징점 좌표 추출
            val points = keypoints.map {
                Point(it.pt.x, it.pt.y + frame.rows() * 2 / 3)
            }

            roi.release()
            features.release()

            return points
        } catch (e: Exception) {
            Log.e(TAG, "바닥면 특징점 감지 실패: ${e.message}")
            return null
        }
    }

    // 바닥면 변환 행렬 계산
    private fun calculateFloorTransform(floorFeatures: List<Point>): Mat {
        // 실제 구현에서는 호모그래피 계산 로직 추가
        try {
            // 최소 4개의 점이 있어야 호모그래피 계산 가능
            if (floorFeatures.size < 4) {
                return Mat.eye(3, 3, CvType.CV_64F)
            }

            // 기준 평면 좌표 (정사각형)
            val referencePoints = MatOfPoint2f(
                Point(0.0, 0.0),
                Point(500.0, 0.0),
                Point(500.0, 500.0),
                Point(0.0, 500.0)
            )

            // 현재 특징점 중 대표점 4개 선택
            val selectedPoints = MatOfPoint2f()
            selectedPoints.fromList(floorFeatures.subList(0, 4))

            // 호모그래피 계산
            val homography = Calib3d.findHomography(
                referencePoints, selectedPoints, Calib3d.RANSAC, 5.0
            )

            if (!homography.empty()) {
                return homography
            }

            return Mat.eye(3, 3, CvType.CV_64F)
        } catch (e: Exception) {
            Log.e(TAG, "바닥면 변환 행렬 계산 실패: ${e.message}")
            return Mat.eye(3, 3, CvType.CV_64F)
        }
    }

    // GPS 위치와 시각적 특징점 정보 결합
    private fun combineGPSAndVisualAnchoring(gpsTransform: Mat, floorTransform: Mat): Mat {
        try {
            // 두 행렬의 가중 평균
            val combinedTransform = Mat(3, 3, CvType.CV_64F)
            Core.addWeighted(gpsTransform, 0.7, floorTransform, 0.3, 0.0, combinedTransform)

            return combinedTransform
        } catch (e: Exception) {
            Log.e(TAG, "변환 행렬 결합 실패: ${e.message}")
            return gpsTransform.clone() // 오류 시 GPS 기반 변환만 사용
        }
    }

    // AR 모델 앵커 업데이트
    private fun updateModelAnchor(transform: Mat) {
        try {
            val modelMatrix = FloatArray(16)
            Matrix.setIdentityM(modelMatrix, 0)

            // 호모그래피 행렬을 3x3 행렬로 처리
            val h = DoubleArray(9)
            transform.get(0, 0, h)

            // 행렬 변환
            modelMatrix[0] = h[0].toFloat()
            modelMatrix[1] = h[3].toFloat()
            modelMatrix[2] = 0f

            modelMatrix[4] = h[1].toFloat()
            modelMatrix[5] = h[4].toFloat()
            modelMatrix[6] = 0f

            modelMatrix[12] = h[2].toFloat() * 0.01f
            modelMatrix[13] = h[5].toFloat() * 0.01f
            modelMatrix[14] = -5f

            // 모델 행렬 업데이트
            if (isModelVisible) {
                modelRenderer.updateModelMatrixFromGPS(modelMatrix)
                glSurfaceView.requestRender()
            }
        } catch (e: Exception) {
            Log.e(TAG, "모델 앵커 업데이트 실패: ${e.message}")
        }
    }

    private fun setupGLView() {
        glSurfaceView = findViewById(R.id.gl_surface_view)
        glSurfaceView.setEGLContextClientVersion(2)
        glSurfaceView.setZOrderOnTop(true)
        glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        glSurfaceView.holder.setFormat(PixelFormat.TRANSLUCENT)

        modelRenderer = ModelRenderer(this)
        glSurfaceView.setRenderer(modelRenderer)
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
        glSurfaceView.visibility = View.INVISIBLE
    }

    private fun initializeCamera() {
        mOpenCvCameraView = findViewById(R.id.opencv_camera_view)
        mOpenCvCameraView.visibility = SurfaceView.VISIBLE
        mOpenCvCameraView.setCameraPermissionGranted()
        mOpenCvCameraView.setCvCameraViewListener(this)
        mOpenCvCameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_BACK)
        mOpenCvCameraView.enableFpsMeter()
        mOpenCvCameraView.enableView()
        Log.d(TAG, "카메라 뷰 초기화 완료")
    }

    private fun checkAndRequestPermissions() {
        // 카메라 권한 확인
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST)
        } else {
            initializeCamera()
        }

        // 위치 권한 확인
        checkAndRequestLocationPermissions()
    }

    private fun loadBuild3dModel() {
        try {
            val objFileName = "build3d.obj"

            // 캐시 디렉토리에 복사
            val cacheDir = File(cacheDir, "models")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            val targetFile = File(cacheDir, objFileName)

            // assets에서 파일 복사
            assets.open("models/$objFileName").use { inputStream ->
                FileOutputStream(targetFile).use { outputStream ->
                    val buffer = ByteArray(1024)
                    var length: Int
                    while (inputStream.read(buffer).also { length = it } > 0) {
                        outputStream.write(buffer, 0, length)
                    }
                }
            }

            // 모델 로드 및 설정
            val model = loadObjModel(targetFile)
            setupModelForRendering(model)

            Log.d(TAG, "OBJ 모델 직접 로드 성공: $objFileName")
        } catch (e: Exception) {
            Log.e(TAG, "모델 로딩 오류: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun loadObjModel(objFile: File): BuildingModelManager.BuildingModel {
        val vertices = ArrayList<Float>()
        val texCoords = ArrayList<Float>()
        val indices = ArrayList<Short>()

        Log.d(TAG, "OBJ 파일 로드 시작: ${objFile.absolutePath}")

        try {
            // OBJ 파일 파싱
            objFile.bufferedReader().use { reader ->
                var lineIndex = 0
                val vertexList = ArrayList<Float>()
                val texCoordList = ArrayList<Float>()

                reader.forEachLine { line ->
                    lineIndex++
                    try {
                        when {
                            line.startsWith("v ") -> {
                                val parts = line.split("\\s+".toRegex()).drop(1)
                                if (parts.size >= 3) {
                                    vertexList.add(parts[0].toFloat())
                                    vertexList.add(parts[1].toFloat())
                                    vertexList.add(parts[2].toFloat())
                                }
                            }
                            line.startsWith("vt ") -> {
                                val parts = line.split("\\s+".toRegex()).drop(1)
                                if (parts.size >= 2) {
                                    texCoordList.add(parts[0].toFloat())
                                    texCoordList.add(1 - parts[1].toFloat())
                                }
                            }
                            line.startsWith("f ") -> {
                                val parts = line.split("\\s+".toRegex()).drop(1)
                                if (parts.size >= 3) {
                                    // 삼각형화
                                    for (i in 0 until parts.size - 2) {
                                        processFaceVertex(parts[0], vertexList, texCoordList, vertices, texCoords, indices)
                                        processFaceVertex(parts[i + 1], vertexList, texCoordList, vertices, texCoords, indices)
                                        processFaceVertex(parts[i + 2], vertexList, texCoordList, vertices, texCoords, indices)
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "OBJ 파싱 오류 (라인 $lineIndex): ${e.message}")
                    }
                }
            }

            Log.d(TAG, "OBJ 파일 로드 완료. 정점 수: ${vertices.size / 3}, 인덱스 수: ${indices.size}")

            return BuildingModelManager.BuildingModel(
                id = "loaded_building",
                name = "로드된 건축 모델",
                description = "build3d.obj에서 로드된 건축 모델",
                year = 0,
                vertices = vertices.toFloatArray(),
                texCoords = texCoords.toFloatArray(),
                indices = indices.toShortArray(),
                cornerPoints = createDefaultCorners(),
                textureResId = R.drawable.ic_launcher_background
            )
        } catch (e: Exception) {
            Log.e(TAG, "OBJ 모델 로드 최종 오류: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    private fun processFaceVertex(
        faceVertex: String,
        vertexList: ArrayList<Float>,
        texCoordList: ArrayList<Float>,
        vertices: ArrayList<Float>,
        texCoords: ArrayList<Float>,
        indices: ArrayList<Short>
    ) {
        // OBJ 형식: v/vt/vn 또는 v//vn 또는 v/vt 또는 v
        val parts = faceVertex.split("/")

        // 정점 인덱스 (1부터 시작하므로 -1 필요)
        val vertexIndex = (parts[0].toInt() - 1) * 3

        // 정점 좌표 추가
        vertices.add(vertexList[vertexIndex])
        vertices.add(vertexList[vertexIndex + 1])
        vertices.add(vertexList[vertexIndex + 2])

        // 텍스처 좌표 추가 (있는 경우)
        if (parts.size > 1 && parts[1].isNotEmpty()) {
            val texCoordIndex = (parts[1].toInt() - 1) * 2
            if (texCoordIndex < texCoordList.size - 1) {
                texCoords.add(texCoordList[texCoordIndex])
                texCoords.add(texCoordList[texCoordIndex + 1])
            } else {
                texCoords.add(0f)
                texCoords.add(0f)
            }
        } else {
            texCoords.add(0f)
            texCoords.add(0f)
        }

        // 인덱스 추가
        indices.add((vertices.size / 3 - 1).toShort())
    }

    private fun createDefaultCorners(): MatOfPoint2f {
        return MatOfPoint2f(
            Point(0.0, 0.0),
            Point(500.0, 0.0),
            Point(500.0, 500.0),
            Point(0.0, 500.0)
        )
    }

    private fun setupModelForRendering(model: BuildingModelManager.BuildingModel) {
        try {
            // 버텍스 버퍼 생성
            val vbb = ByteBuffer.allocateDirect(model.vertices.size * 4)
            vbb.order(ByteOrder.nativeOrder())
            val vertexBuffer = vbb.asFloatBuffer()
            vertexBuffer.put(model.vertices)
            vertexBuffer.position(0)

            // 텍스처 좌표 버퍼 생성
            val tbb = ByteBuffer.allocateDirect(model.texCoords.size * 4)
            tbb.order(ByteOrder.nativeOrder())
            val texCoordBuffer = tbb.asFloatBuffer()
            texCoordBuffer.put(model.texCoords)
            texCoordBuffer.position(0)

            // 인덱스 버퍼 생성
            val ibb = ByteBuffer.allocateDirect(model.indices.size * 2)
            ibb.order(ByteOrder.nativeOrder())
            val indexBuffer = ibb.asShortBuffer()
            indexBuffer.put(model.indices)
            indexBuffer.position(0)

            // ModelRenderer에 데이터 전달
            runOnUiThread {
                if (::modelRenderer.isInitialized) {
                    modelRenderer.setModelData(vertexBuffer, texCoordBuffer, indexBuffer, model.indices.size)
                    Log.d(TAG, "모델 데이터 설정 완료")
                } else {
                    Log.e(TAG, "ModelRenderer가 초기화되지 않았습니다")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "모델 렌더링 설정 실패: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun checkAndRequestLocationPermissions() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        val permissionsToRequest = ArrayList<String>()

        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                AlertDialog.Builder(this)
                    .setTitle("위치 권한 필요")
                    .setMessage("AR 모델을 정확한 위치에 표시하기 위해 위치 권한이 필요합니다.")
                    .setPositiveButton("권한 요청") { _, _ ->
                        ActivityCompat.requestPermissions(
                            this,
                            permissionsToRequest.toTypedArray(),
                            LOCATION_PERMISSION_REQUEST
                        )
                    }
                    .setNegativeButton("취소") { dialog, _ ->
                        dialog.dismiss()
                        Toast.makeText(this, "위치 권한이 없어 AR 위치 기능이 제한됩니다.", Toast.LENGTH_LONG).show()
                    }
                    .create()
                    .show()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toTypedArray(),
                    LOCATION_PERMISSION_REQUEST
                )
            }
        } else {
            setupGPSBasedAR()
        }
    }

    private fun setupGPSBasedAR() {
        try {
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000, // 1초마다
                1f,   // 1미터 이동 시
                object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        Log.d(TAG, "현재 위치: ${location.latitude}, ${location.longitude}")

                        // 테스트 위치와의 거리 계산
                        val results = FloatArray(1)
                        Location.distanceBetween(
                            location.latitude, location.longitude,
                            TEST_LATITUDE, TEST_LONGITUDE,
                            results
                        )

                        val distance = results[0]
                        Log.d(TAG, "테스트 위치까지 거리: $distance 미터")

                        // 100미터 이내면 AR 모델 표시
                        runOnUiThread {
                            if (distance < 100) {
                                updateStatusText("건물 위치 근처입니다! (${distance.toInt()}m)")
                                isModelVisible = true
                                glSurfaceView.visibility = View.VISIBLE
                                infoButton.visibility = View.VISIBLE
                            } else {
                                updateStatusText("건물 위치로 이동하세요 (${distance.toInt()}m)")
                                isModelVisible = false
                                glSurfaceView.visibility = View.INVISIBLE
                                infoButton.visibility = View.GONE
                            }
                        }
                    }

                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
                }
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "위치 권한이 필요합니다: ${e.message}")
            // 테스트 목적으로 권한이 없어도 모델 표시
            enableTestMode()
        }
    }

    private fun enableTestMode() {
        Log.d(TAG, "테스트 모드 활성화")
        // 테스트 모드에서는 항상 모델 표시
        runOnUiThread {
            updateStatusText("테스트 모드 활성화")
            isModelVisible = true
            glSurfaceView.visibility = View.VISIBLE
            infoButton.visibility = View.VISIBLE
        }
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
        releaseResources()
    }

    private fun releaseResources() {
        if (::mOpenCvCameraView.isInitialized) {
            mOpenCvCameraView.disableView()
        }
        if (::sensorFusion.isInitialized) {
            sensorFusion.stop()
        }
        if (::geoAnchoredARSystem.isInitialized) {
            geoAnchoredARSystem.stop()
        }
        if (::depthEstimationSystem.isInitialized) {
            depthEstimationSystem.release()
        }

        mRgba?.release()
    }

    private fun startModelAlignment() {
        updateStatusText("건물 인식 중...")
        captureButton.text = "재설정"
        featureTracker.reset()
        isRendering.set(true)
    }

    private fun resetTracking() {
        updateStatusText("카메라를 건물에 맞춰주세요")
        captureButton.text = "모델 표시하기"
        featureTracker.reset()
        isRendering.set(false)
        isModelVisible = false
        glSurfaceView.visibility = View.INVISIBLE
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

        // 테스트 목적으로 항상 모델 표시
        if (isModelVisible) {
            // 깊이 추정 처리
            processDepthEstimation(rgba)

            // 바닥면 특징점 감지를 통한 AR 앵커 개선
            val floorFeatures = detectFloorFeatures(rgba)
            if (floorFeatures != null && floorFeatures.size >= 4) {
                val floorTransform = calculateFloorTransform(floorFeatures)

                // GPS 기반 변환 행렬 가져오기 (간소화된 예시)
                val gpsTransform = Mat.eye(3, 3, CvType.CV_64F)

                // GPS와 시각적 앵커 결합
                val combinedTransform = combineGPSAndVisualAnchoring(gpsTransform, floorTransform)

                // 모델 앵커 업데이트
                updateModelAnchor(combinedTransform)

                // 리소스 해제
                floorTransform.release()
                gpsTransform.release()
                combinedTransform.release()
            } else {
                // 바닥면을 감지하지 못한 경우 기존 방식으로 모델 표시
                displayModel()
            }
        }

        // 성능 측정 종료
        performanceOptimizer.endFrame()

        return rgba
    }

    private fun processDepthEstimation(rgba: Mat) {
        if (performanceOptimizer.shouldUseEffects() && useDepthEstimation) {
            val depthMap = depthEstimationSystem.estimateDepth(rgba)

            if (depthEstimationSystem.isDepthMapUpdated()) {
                val depthTextureData = depthEstimationSystem.getDepthTextureData()
                runOnUiThread {
                    modelRenderer.updateDepthTexture(depthTextureData)
                }
            }
        }
    }

    private fun displayModel() {
        runOnUiThread {
            try {
                // 현재 디바이스 방향 가져오기
                val rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
                if (rotationVector != null) {
                    // 이미 센서 기반 자동 배치 시스템이 작동 중이므로
                    // 여기서는 추가 작업 없이 최신 센서 데이터로 업데이트만 요청
                    val requestUpdate = true  // 센서 데이터 업데이트 요청 플래그

                    // 렌더링 요청 (자동 배치 시스템이 모델 위치와 회전을 설정함)
                    glSurfaceView.requestRender()

                    Log.d(TAG, "센서 기반 자동 배치 시스템 사용 중")
                } else {
                    // 센서를 사용할 수 없는 경우 기본 설정으로 표시
                    val modelMatrix = FloatArray(16)
                    Matrix.setIdentityM(modelMatrix, 0)

                    // 기본 위치 및 크기 설정
                    Matrix.translateM(modelMatrix, 0, 0f, 0f, -5f)
                    Matrix.scaleM(modelMatrix, 0, 0.1f, 0.1f, 0.1f)

                    // 모델 위치 업데이트
                    modelRenderer.updateModelMatrixFromGPS(modelMatrix)

                    // LOD 레벨 설정
                    modelRenderer.setLODLevel(performanceOptimizer.getCurrentLODLevel())

                    // 렌더링 요청
                    glSurfaceView.requestRender()

                    Log.d(TAG, "기본 모델 배치 사용 (센서 없음)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "모델 표시 오류: ${e.message}")
            }
        }
    }

    private fun showBuildingInfoDialog() {
        val currentModelId = buildingModelManager.getCurrentModel()?.id ?: return
        val buildingInfo = historicalInfoManager.getBuildingInfoById(currentModelId) ?: return

        val dialogView = layoutInflater.inflate(R.layout.dialog_building_info, null)
        val titleTextView = dialogView.findViewById<TextView>(R.id.dialog_title)
        val descriptionTextView = dialogView.findViewById<TextView>(R.id.dialog_description)
        val yearTextView = dialogView.findViewById<TextView>(R.id.dialog_year)
        val featuresListView = dialogView.findViewById<ListView>(R.id.dialog_features_list)
        val eventsListView = dialogView.findViewById<ListView>(R.id.dialog_events_list)

        titleTextView.text = buildingInfo.name
        descriptionTextView.text = buildingInfo.description
        yearTextView.text = "건립 연도: ${buildingInfo.year}년"

        val featuresAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            buildingInfo.architecturalFeatures
        )
        featuresListView.adapter = featuresAdapter

        val eventsAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            buildingInfo.historicalEvents
        )
        eventsListView.adapter = eventsAdapter

        val builder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("닫기") { dialog, _ -> dialog.dismiss() }

        buildingInfoDialog = builder.create()
        buildingInfoDialog.show()
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
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "위치 권한 승인됨")
                    setupGPSBasedAR()
                } else {
                    Log.e(TAG, "위치 권한 거부됨")
                    Toast.makeText(this,
                        "위치 권한이 없어도 테스트 모드로 계속 진행합니다.",
                        Toast.LENGTH_LONG
                    ).show()
                    enableTestMode()
                }
            }
        }
    }
}