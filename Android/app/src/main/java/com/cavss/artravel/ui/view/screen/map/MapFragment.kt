package com.cavss.artravel.ui.view.screen.map

import android.Manifest
import android.graphics.Rect
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.cavss.artravel.MainActivity
import com.cavss.artravel.PermissionCallback
import com.cavss.artravel.PermissionManager
import com.cavss.artravel.databinding.FragmentMapBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay


class MapFragment : Fragment(), MapListener {

    private lateinit var binding : FragmentMapBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMapBinding.inflate(inflater,container,false)
        binding.run {
            lifecycleOwner = viewLifecycleOwner
        }
        requestPermission()
        return binding.root
    }


    lateinit var iMapController: IMapController
    lateinit var mMyLocationOverlay: MyLocationNewOverlay
    private fun setMap(mapView : MapView){
        try{
            mapView.let{
                it.setMultiTouchControls(true)
                it.mapCenter // 중심점 속성에 접근
                it.setTileSource(TileSourceFactory.MAPNIK)
                it.getLocalVisibleRect(Rect()) // 현재 보이는 지도 뷰의 사각 영역을 가져옴
            }

            mMyLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(requireContext()), mapView)
            iMapController = mapView.controller

            mMyLocationOverlay.enableMyLocation() // 사용자의 위치 추적을 시작
            mMyLocationOverlay.enableFollowLocation() // 사용자의 위치가 변경될 때마다 지도를 중심으로 이동
            mMyLocationOverlay.isDrawAccuracyEnabled = true // 위치 정확도 원을 그림
            mMyLocationOverlay.runOnFirstFix {
                CoroutineScope(Dispatchers.Main).launch {
                    iMapController.setCenter(mMyLocationOverlay.myLocation) // 지도의 중심을 사용자의 위치로 설정
                    iMapController.animateTo(mMyLocationOverlay.myLocation) // 지도를 사용자의 위치로 애니메이션화
                }
            }

            iMapController.setZoom(15.0)

            mapView.let {
                it.overlays.add(mMyLocationOverlay)
                addMarker(51.5074, -0.1278)
                it.addMapListener(this)
            }



        }catch(e:Exception){
            Log.e("mException", "MapFragment, setMap // Exception : ${e.localizedMessage}")
        }
    }

    private fun addMarker(latitude : Double, longtitude : Double){
        try{
            // 마커 추가
            val startMarker = Marker(binding.placeMap)
            startMarker.position = GeoPoint(latitude, longtitude) // 마커 위치
            startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            startMarker.title = "London"
            binding.placeMap.overlays.add(startMarker) // 마커 추가
        }catch (e:Exception){
            Log.e("mException", "MapFragment, addMarker // Exception : ${e.localizedMessage}")
        }
    }
    //카메라 권한 요청
    private fun requestPermission() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        )
        try {
            val permissionManager = PermissionManager(requireActivity() as MainActivity)
            permissionManager.requestPermissions(permissions, object  : PermissionCallback {
                override fun onPermissionGranted(grantedPermission : String) {
                    Log.e("mException", "허락된 권한 : ${grantedPermission}")
                    setMap(binding.placeMap)
                }

                override fun onPermissionDenied(askPermissionAgain: Boolean, deniedPermission : String) {
                    when(askPermissionAgain){
                        true -> {
                            Log.e("mException", "권한 이전에 거절한 적 있음, 거절된 권한 :${deniedPermission}")
                        }
                        false -> {
                            Log.e("mException", "권한 이전에 거절한 적 없음, 거절된 권한 :${deniedPermission}")
                        }
                    }
                }
            })
        }catch (e:Exception){
            Log.e("mException", "MainActivity, requestPermission // Exception : ${e.localizedMessage}")
        }
    }

    override fun onResume() {
        super.onResume()
        Configuration.getInstance().load(requireActivity(),
            PreferenceManager.getDefaultSharedPreferences(requireActivity()));
        binding.placeMap.onResume()
    }

    override fun onPause() {
        super.onPause()
        Configuration.getInstance().save(requireActivity(),
            PreferenceManager.getDefaultSharedPreferences(requireActivity()));
        binding.placeMap.onPause()
    }

    override fun onScroll(event: ScrollEvent?): Boolean {
        // 지도가 스크롤될 때 호출되는 이벤트 처리
        // 여기에 필요한 로직을 추가하세요.
        return true // 이벤트 처리가 완료되었음을 나타내는 Boolean 값을 반환합니다.
    }

    override fun onZoom(event: ZoomEvent?): Boolean {
        // 지도가 확대 또는 축소될 때 호출되는 이벤트 처리
        // 여기에 필요한 로직을 추가하세요.
        return true // 이벤트 처리가 완료되었음을 나타내는 Boolean 값을 반환합니다.
    }

}