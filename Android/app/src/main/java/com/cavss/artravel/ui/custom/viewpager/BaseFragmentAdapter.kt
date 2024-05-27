package com.cavss.artravel.ui.custom.viewpager

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

abstract class BaseFragmentAdapter {
    abstract class Adapter(FRAGMENT_ACTIVITY : FragmentActivity) : FragmentStateAdapter(FRAGMENT_ACTIVITY){
        abstract fun setFragmentList() : List<Fragment>

        override fun createFragment(position: Int): Fragment {
            return setFragmentList()[position]
        }

        override fun getItemCount(): Int {
            return setFragmentList().size
        }
    }
}
