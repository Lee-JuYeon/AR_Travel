package com.jupond.opencv

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.hardware.SensorManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.opengl.Matrix
import android.util.Log

// GPS 기반 AR 앵커링 시스템:
class GeoAnchoredARSystem(
    private val context: Context
) : LocationListener, SensorEventListener {

    private val TAG = "GeoAnchoredARSystem"

    // 위치 관리자
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    // 센서 관리자
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    // 센서 데이터
    private val accelerometerData = FloatArray(3)
    private val magnetometerData = FloatArray(3)
    private val gyroscopeData = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    // 현재 위치 및 방향
    private var currentLocation: Location? = null
    private var currentBearing = 0f
    private var currentPitch = 0f

    // AR 모델 앵커
    private val modelAnchors = HashMap<String, GeoAnchor>()

    // 앵커 클래스
    data class GeoAnchor(
        val id: String,
        val latitude: Double,
        val longitude: Double,
        val altitude: Double,
        val orientation: Float,
        val scale: Float
    )

    init {
        // 위치 업데이트 요청
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000, // 1초마다
                0.5f, // 0.5미터 이동 시
                this
            )

            // 센서 리스너 등록
            sensorManager.registerListener(
                this,
                accelerometer,
                SensorManager.SENSOR_DELAY_GAME
            )
            sensorManager.registerListener(
                this,
                magnetometer,
                SensorManager.SENSOR_DELAY_GAME
            )
            sensorManager.registerListener(
                this,
                gyroscope,
                SensorManager.SENSOR_DELAY_GAME
            )

            Log.d(TAG, "GeoAnchoredARSystem 초기화 완료")
        } catch (e: SecurityException) {
            Log.e(TAG, "위치 권한이 필요합니다: ${e.message}")
        }
    }

    // AR 앵커 추가
    fun addAnchor(id: String, latitude: Double, longitude: Double, altitude: Double, orientation: Float, scale: Float) {
        val anchor = GeoAnchor(id, latitude, longitude, altitude, orientation, scale)
        modelAnchors[id] = anchor
        Log.d(TAG, "앵커 추가됨: $id, 위치: ($latitude, $longitude)")
    }

    // 위치 기반 모델 변환 행렬 계산
    fun calculateModelMatrix(anchorId: String): FloatArray? {
        val anchor = modelAnchors[anchorId] ?: return null
        val location = currentLocation ?: return null

        // 현재 위치와 앵커 위치 간의 거리 및 방향 계산
        val results = FloatArray(3)
        Location.distanceBetween(
            location.latitude, location.longitude,
            anchor.latitude, anchor.longitude,
            results
        )

        val distance = results[0] // 미터 단위 거리
        val bearing = results[1] // 방위각

        // 모델 행렬 생성 (실제 구현에서는 더 복잡한 계산 필요)
        val modelMatrix = FloatArray(16)
        Matrix.setIdentityM(modelMatrix, 0)

        // 현재 위치 기준으로 모델 위치 설정
        val bearingRad = Math.toRadians((bearing - currentBearing).toDouble())
        val dx = distance * Math.sin(bearingRad)
        val dz = distance * Math.cos(bearingRad)

        // 고도 차이 계산 (해발고도 - 현재고도)
        val dy = anchor.altitude - (location.altitude ?: 0.0)

        // 모델 위치 및 회전 설정
        Matrix.translateM(modelMatrix, 0, dx.toFloat(), dy.toFloat(), -dz.toFloat())
        Matrix.rotateM(modelMatrix, 0, anchor.orientation - currentBearing, 0f, 1f, 0f)
        Matrix.scaleM(modelMatrix, 0, anchor.scale, anchor.scale, anchor.scale)

        return modelMatrix
    }

    // 특정 앵커가 현재 가시권 내에 있는지 확인
    fun isAnchorVisible(anchorId: String): Boolean {
        val anchor = modelAnchors[anchorId] ?: return false
        val location = currentLocation ?: return false

        // 거리 계산
        val results = FloatArray(3)
        Location.distanceBetween(
            location.latitude, location.longitude,
            anchor.latitude, anchor.longitude,
            results
        )

        val distance = results[0]
        val bearing = results[1]

        // 사용자의 방향과 앵커 방향의 차이 계산
        val bearingDiff = Math.abs(bearing - currentBearing)
        val normalizedBearingDiff = Math.min(bearingDiff, 360 - bearingDiff)

        // 거리가 너무 멀거나 시야각을 벗어나면 보이지 않음
        return distance < 300 && normalizedBearingDiff < 45
    }

    // LocationListener 구현
    override fun onLocationChanged(location: Location) {
        currentLocation = location
        Log.d(TAG, "위치 업데이트: ${location.latitude}, ${location.longitude}")
    }

    // SensorEventListener 구현
    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, accelerometerData, 0, 3)
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, magnetometerData, 0, 3)
            }
            Sensor.TYPE_GYROSCOPE -> {
                System.arraycopy(event.values, 0, gyroscopeData, 0, 3)
            }
        }

        // 센서 데이터를 사용하여 기기 방향 계산
        if (SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerData, magnetometerData)) {
            SensorManager.getOrientation(rotationMatrix, orientationAngles)

            // 라디안에서 도로 변환
            currentBearing = Math.toDegrees(orientationAngles[0].toDouble()).toFloat() * -1
            currentPitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // 센서 정확도 변경 처리 (필요한 경우)
    }

    // 리소스 해제
    fun stop() {
        try {
            locationManager.removeUpdates(this)
            sensorManager.unregisterListener(this)
        } catch (e: Exception) {
            Log.e(TAG, "리소스 해제 중 오류: ${e.message}")
        }
    }
}