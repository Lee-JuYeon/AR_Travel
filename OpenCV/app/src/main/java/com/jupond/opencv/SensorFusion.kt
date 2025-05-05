package com.jupond.opencv

import org.opencv.core.Mat
import org.opencv.core.CvType
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.content.Context
import kotlin.math.cos
import kotlin.math.sin

// 칼만 필터 기반 센서 퓨전 클래스
class SensorFusion(context: Context) : SensorEventListener {
    private val TAG = "SensorFusion"

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    // 센서 데이터
    private val gravity = FloatArray(3)
    private val linearAcceleration = FloatArray(3)
    private val gyroRotation = FloatArray(3)

    // 칼만 필터 관련 변수
    private val stateEstimate = Mat(6, 1, CvType.CV_32F) // [x, y, z, roll, pitch, yaw]
    private val stateCovariance = Mat.eye(6, 6, CvType.CV_32F).mul(Mat.ones(6, 6, CvType.CV_32F), 1000.0)
    private val processNoise = Mat.eye(6, 6, CvType.CV_32F).mul(Mat.ones(6, 6, CvType.CV_32F), 0.01)
    private val measurementNoise = Mat.eye(6, 6, CvType.CV_32F).mul(Mat.ones(6, 6, CvType.CV_32F), 0.1)

    // 타임스탬프
    private var lastTimestamp: Long = 0

    init {
        // 센서 리스너 등록
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME)
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                // 저주파 필터로 중력 분리
                val alpha = 0.8f
                gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0]
                gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1]
                gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2]

                // 선형 가속도 계산
                linearAcceleration[0] = event.values[0] - gravity[0]
                linearAcceleration[1] = event.values[1] - gravity[1]
                linearAcceleration[2] = event.values[2] - gravity[2]

                // 칼만 필터 업데이트 (가속도계 데이터)
                updateKalmanFilter(event.timestamp)
            }

            Sensor.TYPE_GYROSCOPE -> {
                // 자이로스코프 각속도 저장
                gyroRotation[0] = event.values[0]
                gyroRotation[1] = event.values[1]
                gyroRotation[2] = event.values[2]

                // 칼만 필터 업데이트 (자이로스코프 데이터)
                updateKalmanFilter(event.timestamp)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // 정확도 변경 처리 (필요시 구현)
    }

    private fun updateKalmanFilter(timestamp: Long) {
        if (lastTimestamp == 0L) {
            lastTimestamp = timestamp
            return
        }

        // 시간 간격 계산 (나노초 -> 초)
        val dt = (timestamp - lastTimestamp) / 1_000_000_000.0f
        lastTimestamp = timestamp

        // 상태 전이 행렬 계산
        val F = Mat.eye(6, 6, CvType.CV_32F)
        // 위치 업데이트 (등속도 모델)
        F.put(0, 3, dt.toDouble())
        F.put(1, 4, dt.toDouble())
        F.put(2, 5, dt.toDouble())

        // 예측 단계
        stateEstimate.put(0, 0, stateEstimate.get(0, 0)[0] + stateEstimate.get(3, 0)[0] * dt)
        stateEstimate.put(1, 0, stateEstimate.get(1, 0)[0] + stateEstimate.get(4, 0)[0] * dt)
        stateEstimate.put(2, 0, stateEstimate.get(2, 0)[0] + stateEstimate.get(5, 0)[0] * dt)

        // 자이로 데이터로 각도 업데이트
        stateEstimate.put(3, 0, gyroRotation[0].toDouble())
        stateEstimate.put(4, 0, gyroRotation[1].toDouble())
        stateEstimate.put(5, 0, gyroRotation[2].toDouble())

        // 공분산 업데이트
        stateCovariance.put(0, 0, stateCovariance.get(0, 0)[0] + processNoise.get(0, 0)[0])
        // 다른 공분산 요소 업데이트 (생략)...
    }

    // 비전 데이터와 센서 데이터 융합
    fun fuseWithVision(homography: Mat?): Mat? {
        if (homography == null) {
            return null
        }

        // 센서 데이터를 활용하여 호모그래피 안정화
        val stabilizedH = homography.clone()

        // 회전 안정화 - 센서 데이터로부터 얻은 자세 정보 활용
        val roll = stateEstimate.get(3, 0)[0]
        val pitch = stateEstimate.get(4, 0)[0]
        val yaw = stateEstimate.get(5, 0)[0]

        // 단순화된 안정화 로직
        // 실제 구현에서는 센서 데이터와 호모그래피를 더 정교하게 융합해야 함
        val rotMat = Mat(3, 3, CvType.CV_64F)
        rotMat.put(0, 0, cos(yaw) * cos(pitch))
        rotMat.put(0, 1, cos(yaw) * sin(pitch) * sin(roll) - sin(yaw) * cos(roll))
        rotMat.put(0, 2, cos(yaw) * sin(pitch) * cos(roll) + sin(yaw) * sin(roll))
        rotMat.put(1, 0, sin(yaw) * cos(pitch))
        rotMat.put(1, 1, sin(yaw) * sin(pitch) * sin(roll) + cos(yaw) * cos(roll))
        rotMat.put(1, 2, sin(yaw) * sin(pitch) * cos(roll) - cos(yaw) * sin(roll))
        rotMat.put(2, 0, -sin(pitch))
        rotMat.put(2, 1, cos(pitch) * sin(roll))
        rotMat.put(2, 2, cos(pitch) * cos(roll))

        // 회전 행렬과 호모그래피 결합 (간소화된 로직)
        // 실제 구현에서는 더 복잡한 행렬 연산 필요

        return stabilizedH
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }
}