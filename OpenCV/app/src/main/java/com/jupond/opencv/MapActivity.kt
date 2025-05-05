// MapActivity.kt
package com.jupond.opencv

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

// 향상된 사용자 인터페이스 (맵 통합)
class MapActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private lateinit var mMap: GoogleMap
    private lateinit var buildingMetadataManager: BuildingMetadataManager
    private lateinit var startARButton: Button

    private var selectedBuildingId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)


    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // 마커 클릭 리스너 설정
        mMap.setOnMarkerClickListener(this)

        // 건물 위치에 마커 추가
        addBuildingMarkers()

        // 초기 카메라 위치 설정 (예: 서울)
        val seoul = LatLng(37.5665, 126.9780)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(seoul, 10f))
    }

    private fun addBuildingMarkers() {
        for (buildingId in buildingMetadataManager.getAllBuildingIds()) {
            val metadata = buildingMetadataManager.getMetadataById(buildingId) ?: continue

            val position = LatLng(metadata.latitude, metadata.longitude)
            val marker = mMap.addMarker(
                MarkerOptions()
                    .position(position)
                    .title(metadata.name)
                    .snippet("${metadata.year}년 건립")
            )

            // 마커에 건물 ID 태그 지정
            marker?.tag = buildingId
        }
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        // 마커 클릭 시 건물 선택
        selectedBuildingId = marker.tag as String?

        // AR 버튼 표시
        startARButton.visibility = View.VISIBLE

        // 기본 동작 수행 (정보창 표시)
        return false
    }

    private fun startARView() {
        if (selectedBuildingId != null) {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("BUILDING_ID", selectedBuildingId)
            startActivity(intent)
        }
    }
}