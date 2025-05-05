package com.jupond.opencv

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * 건축물에 대한 역사적 정보를 관리하는 클래스
 */
class HistoricalInfoManager(private val context: Context) {
    private val TAG = "HistoricalInfoManager"

    // 건물 정보 데이터 클래스
    data class BuildingInfo(
        val id: String,
        val name: String,
        val description: String,
        val year: Int,
        val historyPeriods: List<HistoryPeriod>,
        val architecturalFeatures: List<String>,
        val historicalEvents: List<String>
    )

    // 역사적 시기 데이터 클래스
    data class HistoryPeriod(
        val periodName: String,
        val startYear: Int,
        val endYear: Int,
        val description: String
    )

    // 건물 정보 저장 맵
    private val buildingInfoMap = HashMap<String, BuildingInfo>()

    init {
        // 건물 정보 로드
        loadHistoricalInfo()
    }

    // 건물 역사 정보 로드
    private fun loadHistoricalInfo() {
        try {
            // 건물 정보 JSON 파일 로드 (실제 구현에서는 assets/historical_info.json에서 로드)
            val inputStream = context.assets.open("historical_info.json")
            val reader = BufferedReader(InputStreamReader(inputStream))
            val jsonString = reader.use { it.readText() }

            // 간소화된 JSON 파싱 (실제 구현에서는 Gson 또는 다른 JSON 라이브러리 사용 권장)
            // 여기서는 예시로 간단한 건물 정보만 하드코딩

            // 예시 건물 정보 추가
            addExampleBuildingInfo()

            Log.d(TAG, "건물 역사 정보 로드 완료: ${buildingInfoMap.size}개")

        } catch (e: Exception) {
            Log.e(TAG, "건물 역사 정보 로드 실패: ${e.message}")
            // 오류 발생 시 예시 건물 정보 추가
            addExampleBuildingInfo()
        }
    }

    // 예시 건물 정보 추가 (실제 앱에서는 JSON에서 로드)
    private fun addExampleBuildingInfo() {
        // 경복궁 정보
        val gyeongbokgungPeriods = listOf(
            HistoryPeriod(
                periodName = "조선 초기",
                startYear = 1395,
                endYear = 1592,
                description = "태조 이성계가 창건한 이후 임진왜란 이전까지의 시기"
            ),
            HistoryPeriod(
                periodName = "폐허 시기",
                startYear = 1592,
                endYear = 1867,
                description = "임진왜란으로 소실된 후 재건되지 않은 시기"
            ),
            HistoryPeriod(
                periodName = "재건 시기",
                startYear = 1867,
                endYear = 1910,
                description = "흥선대원군 주도로 재건된 시기"
            ),
            HistoryPeriod(
                periodName = "일제강점기",
                startYear = 1910,
                endYear = 1945,
                description = "일제강점기 훼손 및 변형된 시기"
            ),
            HistoryPeriod(
                periodName = "현대 복원",
                startYear = 1945,
                endYear = 2025,
                description = "광복 이후 지속적인 복원 작업이 진행된 시기"
            )
        )

        val gyeongbokgungFeatures = listOf(
            "정전(근정전) - 왕의 즉위식과 공식 행사가 열리던 공간",
            "경회루 - 연못 위에 지어진 아름다운 누각",
            "향원정 - 연못 가운데 육각형 정자",
            "아미산 - 후원의 가산",
            "교태전 - 왕비의 침전"
        )

        val gyeongbokgungEvents = listOf(
            "1395년 - 태조 이성계에 의해 창건",
            "1592년 - 임진왜란으로 전소",
            "1867년 - 흥선대원군 주도로 재건",
            "1915년 - 조선총독부 건물 건축으로 일부 훼손",
            "1990년대 - 조선총독부 건물 철거 및 본격적인 복원 시작"
        )

        val gyeongbokgungInfo = BuildingInfo(
            id = "gyeongbokgung",
            name = "경복궁",
            description = "조선왕조 제일의 법궁으로, 1395년 태조 이성계가 창건하였습니다. 북으로는 북악산, 동으로는 낙산, 서로는 인왕산, 남으로는 남산으로 둘러싸여 있어 풍수지리적으로 좋은 위치에 자리잡고 있습니다.",
            year = 1395,
            historyPeriods = gyeongbokgungPeriods,
            architecturalFeatures = gyeongbokgungFeatures,
            historicalEvents = gyeongbokgungEvents
        )

        // 불국사 정보
        val bulguksaPeriods = listOf(
            HistoryPeriod(
                periodName = "신라 시대",
                startYear = 751,
                endYear = 935,
                description = "불국사가 창건되고 번성한 시기"
            ),
            HistoryPeriod(
                periodName = "고려 시대",
                startYear = 935,
                endYear = 1392,
                description = "고려 시대 불교의 중심지로서의 시기"
            ),
            HistoryPeriod(
                periodName = "조선 시대",
                startYear = 1392,
                endYear = 1910,
                description = "조선의 숭유억불 정책으로 쇠퇴한 시기"
            ),
            HistoryPeriod(
                periodName = "현대 복원",
                startYear = 1910,
                endYear = 2025,
                description = "일제강점기 이후 현대까지 복원 과정"
            )
        )

        val bulguksaFeatures = listOf(
            "석가탑 - 신라의 전형적인 석탑 양식",
            "다보탑 - 화려한 장식의 독특한 형태",
            "청운교와 백운교 - 33계단으로 이루어진 돌다리",
            "자하문 - 불국사의 정문",
            "범영루 - 불국사 입구의 누각"
        )

        val bulguksaEvents = listOf(
            "751년 - 경덕왕 때 김대성에 의해 창건",
            "1593년 - 임진왜란으로 소실",
            "1604년 - 일부 복원 시작",
            "1969년 - 전면적인 복원 시작",
            "1995년 - 유네스코 세계문화유산 등재"
        )

        val bulguksaInfo = BuildingInfo(
            id = "bulguksa",
            name = "불국사",
            description = "신라시대에 창건된 대표적인 사찰로, 석가탑과 다보탑 등 국보급 문화재가 많이 보존되어 있습니다. 1995년 유네스코 세계문화유산으로 등재되었습니다.",
            year = 751,
            historyPeriods = bulguksaPeriods,
            architecturalFeatures = bulguksaFeatures,
            historicalEvents = bulguksaEvents
        )

        // 정보 맵에 추가
        buildingInfoMap["gyeongbokgung"] = gyeongbokgungInfo
        buildingInfoMap["bulguksa"] = bulguksaInfo
    }

    // 특정 ID의 건물 정보 가져오기
    fun getBuildingInfoById(id: String): BuildingInfo? {
        return buildingInfoMap[id]
    }

    // 모든 건물 정보 가져오기
    fun getAllBuildingInfo(): List<BuildingInfo> {
        return buildingInfoMap.values.toList()
    }

    // 건물 간략 정보 가져오기 (UI 표시용)
    fun getBuildingSummary(id: String): String {
        val info = buildingInfoMap[id] ?: return "정보가 없습니다."
        return "${info.name} (${info.year}년)\n${info.description}"
    }

    // 특정 건물의 시기별 정보 가져오기
    fun getBuildingPeriodInfo(id: String, periodIndex: Int): String {
        val info = buildingInfoMap[id] ?: return "정보가 없습니다."
        if (periodIndex < 0 || periodIndex >= info.historyPeriods.size) {
            return "해당 시기 정보가 없습니다."
        }

        val period = info.historyPeriods[periodIndex]
        return "${period.periodName} (${period.startYear}년-${period.endYear}년)\n${period.description}"
    }
}