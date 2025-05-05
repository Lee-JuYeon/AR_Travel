package com.jupond.opencv

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileReader
import java.io.BufferedReader

// 성능최적화 관리자.
class PerformanceOptimizer(private val context: Context) {
    private val TAG = "PerformanceOptimizer"

    // 성능 메트릭
    private var frameTimeHistory = ArrayList<Long>()
    private var lastFrameTime = 0L
    private var averageFrameTime = 0L

    // 발열 관련 변수
    private var temperatureReadings = ArrayList<Float>()
    private var isOverheating = false

    // 기기 사양 정보
    private val isLowEndDevice: Boolean
    private val isMidRangeDevice: Boolean
    private val isHighEndDevice: Boolean

    // 최적화 설정
    private var currentLODLevel = 1
    private var targetFPS = 30
    private var useEffects = true

    init {
        // 기기 사양 감지
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val memoryInfo = android.app.ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val totalMemory = memoryInfo.totalMem / (1024 * 1024) // MB 단위
        val cpuCores = Runtime.getRuntime().availableProcessors()

        Log.d(TAG, "Device memory: $totalMemory MB, CPU cores: $cpuCores")

        // 기기 등급 결정
        isLowEndDevice = totalMemory < 2048 || cpuCores <= 4
        isMidRangeDevice = totalMemory in 2048..3072 && cpuCores in 5..6
        isHighEndDevice = totalMemory > 3072 && cpuCores > 6

        // 기기 사양에 따른 초기 설정
        if (isLowEndDevice) {
            currentLODLevel = 2 // 저품질
            targetFPS = 20
            useEffects = false
        } else if (isMidRangeDevice) {
            currentLODLevel = 1 // 중간 품질
            targetFPS = 30
            useEffects = true
        } else {
            currentLODLevel = 0 // 고품질
            targetFPS = 60
            useEffects = true
        }

        Log.d(TAG, "Device classification: Low=$isLowEndDevice, Mid=$isMidRangeDevice, High=$isHighEndDevice")
        Log.d(TAG, "Initial settings: LOD=$currentLODLevel, Target FPS=$targetFPS, Effects=$useEffects")
    }

    // 프레임 시간 측정 및 성능 조정
    fun beginFrame() {
        lastFrameTime = System.nanoTime()
    }

    fun endFrame() {
        val currentTime = System.nanoTime()
        val frameDuration = (currentTime - lastFrameTime) / 1_000_000 // 밀리초 단위

        // 프레임 시간 기록 (최대 60개 샘플)
        frameTimeHistory.add(frameDuration)
        if (frameTimeHistory.size > 60) {
            frameTimeHistory.removeAt(0)
        }

        // 평균 프레임 시간 계산
        averageFrameTime = frameTimeHistory.average().toLong()

        // 필요시 성능 자동 조정
        if (frameTimeHistory.size >= 30) {
            adjustPerformanceSettings()
        }

        // 발열 체크 (10초마다)
        if (frameTimeHistory.size % 300 == 0) {
            checkDeviceTemperature()
        }
    }

    // 성능 설정 자동 조정
    private fun adjustPerformanceSettings() {
        // 목표 프레임 시간 (1000ms / targetFPS)
        val targetFrameTime = 1000 / targetFPS

        // 현재 성능이 목표에 못미치면 품질 낮추기
        if (averageFrameTime > targetFrameTime * 1.2) {
            if (currentLODLevel < 2) {
                currentLODLevel++
                Log.d(TAG, "성능 최적화: 품질 낮춤 (LOD $currentLODLevel)")
            }

            // 심각한 성능 문제면 효과 비활성화
            if (averageFrameTime > targetFrameTime * 1.5) {
                useEffects = false
                Log.d(TAG, "성능 최적화: 효과 비활성화")
            }
        }
        // 성능이 매우 좋으면 품질 높이기 (발열 상태가 아닐 때만)
        else if (averageFrameTime < targetFrameTime * 0.7 && !isOverheating) {
            if (currentLODLevel > 0) {
                currentLODLevel--
                Log.d(TAG, "성능 최적화: 품질 높임 (LOD $currentLODLevel)")
            } else if (!useEffects) {
                useEffects = true
                Log.d(TAG, "성능 최적화: 효과 활성화")
            }
        }
    }

    // 기기 발열 체크 - API 레벨 23 (마시멜로우) 호환 버전
    private fun checkDeviceTemperature() {
        try {
            // 마시멜로우에서는 /sys/class/thermal에서 값 읽기 시도
            val thermalFiles = File("/sys/class/thermal").listFiles { file ->
                file.name.startsWith("thermal_zone")
            }

            if (thermalFiles != null && thermalFiles.isNotEmpty()) {
                var maxTemp = 0.0f

                for (file in thermalFiles) {
                    try {
                        val tempFile = File(file, "temp")
                        if (tempFile.exists() && tempFile.canRead()) {
                            val tempString = BufferedReader(FileReader(tempFile)).use { it.readLine() }
                            val temp = tempString.trim().toFloat() / 1000.0f // 원시값은 밀리도 단위
                            if (temp > maxTemp) {
                                maxTemp = temp
                            }
                        }
                    } catch (e: Exception) {
                        // 일부 열 센서 파일은 읽지 못할 수 있음
                        Log.e(TAG, "온도 파일 읽기 실패: ${e.message}")
                    }
                }

                if (maxTemp > 0.0f) {
                    temperatureReadings.add(maxTemp)

                    if (temperatureReadings.size > 10) {
                        temperatureReadings.removeAt(0)
                    }

                    // 발열 상태 확인
                    val avgTemp = temperatureReadings.average().toFloat()
                    val prevOverheating = isOverheating
                    isOverheating = avgTemp > 45.0f

                    if (isOverheating != prevOverheating) {
                        if (isOverheating) {
                            Log.d(TAG, "발열 감지: 성능 제한 적용 (온도: $avgTemp°C)")
                            currentLODLevel = 2
                            useEffects = false
                            targetFPS = 20
                        } else {
                            Log.d(TAG, "발열 감소: 성능 제한 해제 (온도: $avgTemp°C)")
                            // 원래 설정 복원
                            if (isHighEndDevice) {
                                currentLODLevel = 0
                                targetFPS = 60
                                useEffects = true
                            } else if (isMidRangeDevice) {
                                currentLODLevel = 1
                                targetFPS = 30
                                useEffects = true
                            }
                        }
                    }
                } else {
                    // 온도 센서를 읽을 수 없는 경우 대체 방법 사용
                    useAlternativeTemperatureEstimation()
                }
            } else {
                // 온도 센서 파일이 없는 경우 대체 방법 사용
                useAlternativeTemperatureEstimation()
            }
        } catch (e: Exception) {
            Log.e(TAG, "온도 읽기 실패: ${e.message}")
            // 오류 발생 시 대체 방법 사용
            useAlternativeTemperatureEstimation()
        }
    }

    // 대체 온도 추정 방법 (CPU 사용률, 배터리 온도 등으로 추정)
    private fun useAlternativeTemperatureEstimation() {
        try {
            // 프레임 시간을 기반으로 발열 추정
            if (averageFrameTime > 50) { // 20fps 미만은 과부하 상태로 간주
                temperatureReadings.add(46.0f) // 발열 상태로 추정
            } else {
                temperatureReadings.add(35.0f) // 정상 상태로 추정
            }

            if (temperatureReadings.size > 10) {
                temperatureReadings.removeAt(0)
            }

            // 발열 상태 확인
            val avgTemp = temperatureReadings.average().toFloat()
            val prevOverheating = isOverheating
            isOverheating = avgTemp > 45.0f

            if (isOverheating != prevOverheating) {
                if (isOverheating) {
                    Log.d(TAG, "발열 감지(추정): 성능 제한 적용")
                    currentLODLevel = 2
                    useEffects = false
                    targetFPS = 20
                } else {
                    Log.d(TAG, "발열 감소(추정): 성능 제한 해제")
                    // 원래 설정 복원
                    if (isHighEndDevice) {
                        currentLODLevel = 0
                        targetFPS = 60
                        useEffects = true
                    } else if (isMidRangeDevice) {
                        currentLODLevel = 1
                        targetFPS = 30
                        useEffects = true
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "대체 온도 추정 실패: ${e.message}")
        }
    }

    // 현재 LOD 레벨 가져오기
    fun getCurrentLODLevel(): Int {
        return currentLODLevel
    }

    // 효과 사용 여부 확인
    fun shouldUseEffects(): Boolean {
        return useEffects
    }

    // 타겟 FPS 가져오기
    fun getTargetFPS(): Int {
        return targetFPS
    }

    // 발열 상태 확인
    fun isDeviceOverheating(): Boolean {
        return isOverheating
    }

    // 메모리 사용량 최적화
    fun optimizeMemoryUsage() {
        System.gc() // 가비지 컬렉션 요청
    }
}