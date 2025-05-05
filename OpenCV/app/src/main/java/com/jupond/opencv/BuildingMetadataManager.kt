package com.jupond.opencv

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

// 건축물 메타데이터 관리 시스템 개선
class BuildingMetadataManager(private val context: Context) {
    private val TAG = "BuildingMetadataManager"

    // 건물 메타데이터 클래스
    data class BuildingMetadata(
        val id: String,
        val name: String,
        val description: String,
        val year: Int,
        val latitude: Double,
        val longitude: Double,
        val altitude: Double,
        val orientation: Float,
        val scale: Float,
        val modelFile: String,
        val textureFile: String,
        val architecturalFeatures: List<String>,
        val historicalEvents: List<String>,
        val historyPeriods: List<HistoricalInfoManager.HistoryPeriod>
    )

    private val buildingMetadata = HashMap<String, BuildingMetadata>()

    init {
        loadMetadataFromJson()
    }

    // JSON 파일에서 메타데이터 로드
    private fun loadMetadataFromJson() {
        try {
            val inputStream = context.assets.open("buildings_metadata.json")
            val reader = BufferedReader(InputStreamReader(inputStream))
            val jsonString = reader.use { it.readText() }

            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val metadata = parseMetadata(jsonObject)
                buildingMetadata[metadata.id] = metadata
            }

            Log.d(TAG, "건물 메타데이터 로드 완료: ${buildingMetadata.size}개")

        } catch (e: Exception) {
            Log.e(TAG, "메타데이터 로드 실패: ${e.message}")
            // 오류 발생 시 샘플 데이터 추가
            addSampleMetadata()
        }
    }

    // JSON 객체에서 메타데이터 파싱
    private fun parseMetadata(jsonObject: JSONObject): BuildingMetadata {
        // JSON 파싱 로직 구현
        // 략 (실제 구현 필요)

        // 임시 구현
        return BuildingMetadata(
            id = jsonObject.getString("id"),
            name = jsonObject.getString("name"),
            description = jsonObject.getString("description"),
            year = jsonObject.getInt("year"),
            latitude = jsonObject.getDouble("latitude"),
            longitude = jsonObject.getDouble("longitude"),
            altitude = jsonObject.getDouble("altitude"),
            orientation = jsonObject.getDouble("orientation").toFloat(),
            scale = jsonObject.getDouble("scale").toFloat(),
            modelFile = jsonObject.getString("modelFile"),
            textureFile = jsonObject.getString("textureFile"),
            architecturalFeatures = parseStringArray(jsonObject.getJSONArray("architecturalFeatures")),
            historicalEvents = parseStringArray(jsonObject.getJSONArray("historicalEvents")),
            historyPeriods = parseHistoryPeriods(jsonObject.getJSONArray("historyPeriods"))
        )
    }

    // 샘플 메타데이터 추가
    private fun addSampleMetadata() {
        // 샘플 데이터 추가 로직
        // 략 (실제 구현 필요)
    }

    // 건물 ID로 메타데이터 가져오기
    fun getMetadataById(id: String): BuildingMetadata? {
        return buildingMetadata[id]
    }

    // 모든 건물 ID 목록 가져오기
    fun getAllBuildingIds(): List<String> {
        return buildingMetadata.keys.toList()
    }

    // 위치 기반 근처 건물 검색
    fun findNearbyBuildings(latitude: Double, longitude: Double, radiusInMeters: Double): List<BuildingMetadata> {
        val result = ArrayList<BuildingMetadata>()

        for (metadata in buildingMetadata.values) {
            val distance = calculateDistance(
                latitude, longitude,
                metadata.latitude, metadata.longitude
            )

            if (distance <= radiusInMeters) {
                result.add(metadata)
            }
        }

        return result
    }

    // 두 좌표 간 거리 계산 (Haversine 공식)
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        // 지구 반지름 (미터)
        val R = 6371000.0

        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val deltaLat = Math.toRadians(lat2 - lat1)
        val deltaLon = Math.toRadians(lon2 - lon1)

        val a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return R * c
    }

    // JSON 배열에서 문자열 배열 파싱
    private fun parseStringArray(jsonArray: JSONArray): List<String> {
        val result = ArrayList<String>()
        for (i in 0 until jsonArray.length()) {
            result.add(jsonArray.getString(i))
        }
        return result
    }

    // 역사적 시기 정보 파싱
    private fun parseHistoryPeriods(jsonArray: JSONArray): List<HistoricalInfoManager.HistoryPeriod> {
        val result = ArrayList<HistoricalInfoManager.HistoryPeriod>()
        for (i in 0 until jsonArray.length()) {
            val periodObj = jsonArray.getJSONObject(i)
            result.add(
                HistoricalInfoManager.HistoryPeriod(
                    periodName = periodObj.getString("periodName"),
                    startYear = periodObj.getInt("startYear"),
                    endYear = periodObj.getInt("endYear"),
                    description = periodObj.getString("description")
                )
            )
        }
        return result
    }
}