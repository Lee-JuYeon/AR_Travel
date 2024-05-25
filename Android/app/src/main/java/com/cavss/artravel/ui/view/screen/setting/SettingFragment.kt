package com.cavss.artravel.ui.view.screen.setting

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.cavss.artravel.databinding.FragmentSettingBinding
import com.cavss.artravel.server.auth.AuthManager
import com.cavss.artravel.ui.view.screen.setting.auth.AuthIconFragment
import com.cavss.artravel.ui.view.screen.setting.profile.ProfileFragment
import com.cavss.artravel.vm.AuthVM

class SettingFragment: Fragment() {

    private lateinit var binding : FragmentSettingBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSettingBinding.inflate(inflater, container, false)
        binding.run {
            lifecycleOwner = viewLifecycleOwner
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        settingAuthVM()
    }

    override fun onStart() {
        super.onStart()
        setFragments()
    }

    private val authVM : AuthVM by activityViewModels()
    private fun settingAuthVM(){
        try{
            authVM.let {
                it.googleInit(this@SettingFragment)
            }
        }catch (e:Exception){
            Log.e("mException", "SettingFragment, settingAuthVM // Exception : ${e.localizedMessage}")
        }
    }

    private var profileFragment : ProfileFragment? = null
    private var authIconFragment : AuthIconFragment? = null
    private fun setFragments(){
        try{
            if (profileFragment == null) profileFragment = ProfileFragment()
            if (authIconFragment == null) authIconFragment = AuthIconFragment()

            val manager = requireActivity().supportFragmentManager

            authVM.getUserObserving.observe(requireActivity()) { isUserExist: Boolean ->
                when (isUserExist) {
                    true -> {
                        manager.beginTransaction()
                            .replace(binding.authFrame.id, profileFragment ?: ProfileFragment())
                            .commit()
                    }
                    false -> {
                        manager.beginTransaction()
                            .replace(binding.authFrame.id, authIconFragment ?: AuthIconFragment())
                            .commit()
                    }
                }
            }
        }catch (e:Exception){
            Log.e("mException", "SettingFragment, setFragments // Exception : ${e.localizedMessage}")
        }
    }
}