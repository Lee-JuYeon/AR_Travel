package com.cavss.artravel.ui.view.screen.setting.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.cavss.artravel.databinding.FragmentSettingProfileBinding
import com.cavss.artravel.server.auth.AuthManager

class ProfileFragment() : Fragment() {

    private lateinit var binding : FragmentSettingProfileBinding
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentSettingProfileBinding.inflate(inflater, container, false)
        binding.run {

        }
        return binding.root
    }


}