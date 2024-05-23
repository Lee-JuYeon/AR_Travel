package com.cavss.artravel.ui.view.screen.setting

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.cavss.artravel.databinding.FragmentSettingBinding
import com.cavss.artravel.server.auth.AuthManager

class SettingFragment: Fragment() {

    private lateinit var binding : FragmentSettingBinding
    private var authManager : AuthManager? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSettingBinding.inflate(inflater, container, false)
        binding.run {

        }
        authManager = AuthManager(requireActivity())
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        authCheck()
    }

    private var profileFragment : ProfileFragment? = null
    private var authIconFragment : AuthIconFragment? = null
    private fun setFragments(isAuth : Boolean){
        try{
            if (profileFragment == null) profileFragment = ProfileFragment(authManager)
            if (authIconFragment == null) authIconFragment = AuthIconFragment(authManager)

            val manager = requireActivity().supportFragmentManager.beginTransaction()
            when(isAuth){
                true -> {
                    manager.replace(binding.authFrame.id, profileFragment ?: ProfileFragment(authManager)).commit()
                }
                false -> {
                    manager.replace(binding.authFrame.id, authIconFragment ?: AuthIconFragment(authManager)).commit()
                }
            }
        }catch (e:Exception){
            Log.e("mException", "SettingFragment, setFragments // Exception : ${e.localizedMessage}")
        }
    }
    private fun authCheck(){
        authManager.let {
//            setFragments(it.getUser())
            setFragments(false)
        }
    }
}