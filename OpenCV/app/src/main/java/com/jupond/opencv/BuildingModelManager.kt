package com.jupond.opencv

import android.content.Context
import android.util.Log
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.*
import kotlin.collections.ArrayList

/**
 * 역사적 건축물 모델을 관리하는 클래스
 * 다양한 건물 모델 및 그에 관련된 정보를 로드하고 관리합니다.
 */
class BuildingModelManager(private val context: Context) {
    private val TAG = "BuildingModelManager"

    // 건물 모델 데이터 클래스
    data class BuildingModel(
        val id: String,
        val name: String,
        val description: String,
        val year: Int,
        val vertices: FloatArray,
        val texCoords: FloatArray,
        val indices: ShortArray,
        val cornerPoints: MatOfPoint2f,
        val textureResId: Int
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as BuildingModel
            if (id != other.id) return false

            return true
        }

        override fun hashCode(): Int {
            return id.hashCode()
        }
    }

    // 로드된 모델들
    private val loadedModels = HashMap<String, BuildingModel>()

    // 현재 선택된 모델
    private var currentModel: BuildingModel? = null

    init {
        // 모델 정보 로드
        loadModelMetadata()
    }

    // 모델 메타데이터 로드
    private fun loadModelMetadata() {
        try {
            // 건물 목록 JSON 파일 로드 (실제 구현에서는 assets/buildings.json에서 로드)
            val inputStream = context.assets.open("buildings.json")
            val reader = BufferedReader(InputStreamReader(inputStream))
            val jsonString = reader.use { it.readText() }

            // 간소화된 JSON 파싱 (실제 구현에서는 Gson 또는 다른 JSON 라이브러리 사용 권장)
            // 여기서는 예시로 간단한 건물 정보만 하드코딩

            // 예시 건물 추가
            addExampleBuildings()

            Log.d(TAG, "건물 모델 메타데이터 로드 완료: ${loadedModels.size}개")

        } catch (e: Exception) {
            Log.e(TAG, "건물 메타데이터 로드 실패: ${e.message}")
            // 오류 발생 시 예시 건물 추가
            addExampleBuildings()
        }
    }

    // 예시 건물 정보 추가 (실제 앱에서는 JSON에서 로드)
    private fun addExampleBuildings() {
        // 간단한 건물 모델 (큐브) 생성
        val cubeVertices = floatArrayOf(
            // 전면
            -1.0f,  1.0f,  1.0f,
            1.0f,  1.0f,  1.0f,
            1.0f, -1.0f,  1.0f,
            -1.0f, -1.0f,  1.0f,

            // 후면
            -1.0f,  1.0f, -1.0f,
            1.0f,  1.0f, -1.0f,
            1.0f, -1.0f, -1.0f,
            -1.0f, -1.0f, -1.0f
        )

        val cubeTexCoords = floatArrayOf(
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 1.0f,

            1.0f, 0.0f,
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f
        )

        val cubeIndices = shortArrayOf(
            // 전면
            0, 1, 2,
            2, 3, 0,

            // 우측면
            1, 5, 6,
            6, 2, 1,

            // 후면
            5, 4, 7,
            7, 6, 5,

            // 좌측면
            4, 0, 3,
            3, 7, 4,

            // 상단면
            4, 5, 1,
            1, 0, 4,

            // 하단면
            3, 2, 6,
            6, 7, 3
        )

        // 예시 건물 1 (경복궁)
        val gyeongbokgungCorners = MatOfPoint2f(
            Point(0.0, 0.0),
            Point(500.0, 0.0),
            Point(500.0, 300.0),
            Point(0.0, 300.0)
        )

        val gyeongbokgung = BuildingModel(
            id = "gyeongbokgung",
            name = "경복궁",
            description = "조선시대 건축된 왕궁으로, 1395년에 처음 건립되었습니다.",
            year = 1395,
            vertices = cubeVertices,
            texCoords = cubeTexCoords,
            indices = cubeIndices,
            cornerPoints = gyeongbokgungCorners,
            textureResId = R.drawable.ic_launcher_background // 리소스에 추가 필요
        )

        // 예시 건물 2 (불국사)
        val bulguksaCorners = MatOfPoint2f(
            Point(0.0, 0.0),
            Point(400.0, 0.0),
            Point(400.0, 250.0),
            Point(0.0, 250.0)
        )

        val bulguksa = BuildingModel(
            id = "bulguksa",
            name = "불국사",
            description = "신라시대 건립된 불교 사찰로, 유네스코 세계문화유산입니다.",
            year = 751,
            vertices = cubeVertices,
            texCoords = cubeTexCoords,
            indices = cubeIndices,
            cornerPoints = bulguksaCorners,
            textureResId = R.drawable.ic_launcher_background // 리소스에 추가 필요
        )

        // 모델 맵에 추가
        loadedModels["gyeongbokgung"] = gyeongbokgung
        loadedModels["bulguksa"] = bulguksa

        // 초기 모델 설정
        currentModel = gyeongbokgung
    }

    // 특정 ID의 건물 모델 가져오기
    fun getModelById(id: String): BuildingModel? {
        return loadedModels[id]
    }

    // 현재 선택된 모델 가져오기
    fun getCurrentModel(): BuildingModel? {
        return currentModel
    }

    // 현재 모델 변경
    fun setCurrentModel(id: String): Boolean {
        return if (loadedModels.containsKey(id)) {
            currentModel = loadedModels[id]
            true
        } else {
            false
        }
    }

    // 모든 건물 ID 목록 가져오기
    fun getAllBuildingIds(): List<String> {
        return loadedModels.keys.toList()
    }

    // 모든 건물 모델 가져오기
    fun getAllBuildings(): List<BuildingModel> {
        return loadedModels.values.toList()
    }

    // 모델의 버텍스 데이터를 FloatBuffer로 변환
    fun getVertexBuffer(model: BuildingModel): FloatBuffer {
        val buffer = ByteBuffer.allocateDirect(model.vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()

        buffer.put(model.vertices)
        buffer.position(0)
        return buffer
    }

    // 모델의 텍스처 좌표 데이터를 FloatBuffer로 변환
    fun getTexCoordBuffer(model: BuildingModel): FloatBuffer {
        val buffer = ByteBuffer.allocateDirect(model.texCoords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()

        buffer.put(model.texCoords)
        buffer.position(0)
        return buffer
    }

    // 모델의 인덱스 데이터를 ShortBuffer로 변환
    fun getIndexBuffer(model: BuildingModel): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(model.indices.size * 2)
            .order(ByteOrder.nativeOrder())

        for (index in model.indices) {
            buffer.putShort(index)
        }

        buffer.position(0)
        return buffer
    }

    // 건물 모델 LOD 버전 생성 (폴리곤 수 감소)
    fun createLODModel(model: BuildingModel, lodLevel: Int): BuildingModel {
        // 실제 구현에서는 세분화 수준을 줄인 모델 생성
        // 여기서는 간단히 원본 모델을 반환
        return model
    }
}