package com.cavss.artravel.ui.view.screen.travel

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.cavss.artravel.R
import com.cavss.artravel.databinding.FragmentTravelBinding
import com.cavss.artravel.ui.custom.viewpager.BaseFragmentAdapter
import com.cavss.artravel.ui.view.screen.travel.map.MapFragment
import com.cavss.artravel.ui.view.screen.travel.theme.ThemeFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class TravelFragment : Fragment(){

    private var mapFragment : MapFragment? = null
    private var themeFragment : ThemeFragment? = null

    private fun setTabLayoutWithViewPager2(tabLayout: TabLayout, viewPager2: ViewPager2){
        try{
            mapFragment = MapFragment()
            themeFragment = ThemeFragment()
            viewPager2.let {
                var viewpagerAdapter =  object : BaseFragmentAdapter.Adapter(requireActivity()){
                    override fun setFragmentList(): List<Fragment> {
                        return listOf<Fragment>(
                            themeFragment ?: ThemeFragment(),
                            mapFragment ?: MapFragment()
                        )
                    }
                }
                it.adapter = viewpagerAdapter
                it.isUserInputEnabled = false // 스크롤로 프래그먼트 이동 억제
            }

            tabLayout.let {
                it.tabMode = TabLayout.MODE_FIXED
                it.tabGravity = TabLayout.GRAVITY_FILL
            }
            TabLayoutMediator(binding.tablayout, binding.viewpager2){ tab, position ->
                val THEME_FRAGMENT = 0
                val MAP_FRAGMENT = 1
                when(position){
                    THEME_FRAGMENT -> {
                        tab.text = requireContext().getString(R.string.fragment_theme_title)
                    }
                    MAP_FRAGMENT -> {
                        tab.text = requireContext().getString(R.string.fragment_map_title)
                    }
                }
            }.attach()
        }catch (e:Exception){
            Log.e("mException", "JobFragment, setTabLayoutWithViewPager2 // Exception : ${e.localizedMessage}")
        }
    }
    private lateinit var binding : FragmentTravelBinding
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentTravelBinding.inflate(inflater, container, false)
        binding.run {
            setTabLayoutWithViewPager2(
                tabLayout = this@run.tablayout,
                viewPager2 = this@run.viewpager2
            )
        }
        return binding.root
    }

}