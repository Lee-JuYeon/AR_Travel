package com.cavss.artravel.ui.view.screen.setting.auth

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cavss.artravel.R
import com.cavss.artravel.databinding.FragmentSettingAuthiconBinding
import com.cavss.artravel.interfaces.IButtonClick
import com.cavss.artravel.ui.custom.recyclerview.IClickListener
import com.cavss.artravel.vm.AuthVM
import com.google.firebase.auth.FirebaseUser

class AuthIconFragment() : Fragment() {

    private lateinit var binding: FragmentSettingAuthiconBinding
    private var iLoginClick : IButtonClick? = null
    private var iRegisterClick : IButtonClick? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentSettingAuthiconBinding.inflate(inflater, container, false)
        binding.run {
            setAuthIconView(recyclerView = authRecyclerview)
            iclick = iLoginClick
            registerButton = iRegisterClick
            lifecycleOwner = viewLifecycleOwner
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        firebaseLogIn()
        firebaseRegister()
    }

    private fun setAuthIconView(recyclerView: RecyclerView){
        try{
            val authIconAdapter = AuthIconAdapter()
            authIconAdapter.let {
                it.updateList(listOf(
                    AuthIconModel(image = R.drawable.ic_launcher_foreground, loginWay = "LOCAL"),
                    AuthIconModel(image = R.drawable.image_google, loginWay = "GOOGLE")
                ))
                it.setClickListener(
                    listener = object : IClickListener<AuthIconModel> {
                        override fun onItemClick(model: AuthIconModel, position: Int) {
                            when(model.loginWay){
                                "LOCAL" -> {
                                    binding.registerContainer.visibility = View.VISIBLE
                                }
                                "GOOGLE" -> {
                                    googleLogin()
                                }
                            }
                        }
                    }
                )
            }

            recyclerView.apply {
                adapter = authIconAdapter
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(requireActivity()).apply{
                    orientation = LinearLayoutManager.HORIZONTAL
                    isItemPrefetchEnabled = false
                }
                setItemViewCacheSize(0)
            }
        }catch (e:Exception){
            Log.e("mException", "AuthIconFragment, setAuthIconView // Exception : ${e.localizedMessage}")
        }
    }

    private val authVM : AuthVM by activityViewModels()
    private fun firebaseLogIn(){
        try{
            iLoginClick = object : IButtonClick {
                override fun onIClick() {
                    authVM.firebaseLogin(
                        setEmail = binding.email.text.toString(),
                        setPassword = binding.password.text.toString(),
                        onSuccess = { firebaseUser: FirebaseUser? ->

                        },
                        onFailed = { reason : String? ->
                            Log.e("mException", "계정생성 실패 : ${reason}")
                        }
                    )
                }
            }
        }catch (e:Exception){
            Log.e("mException", "AuthIconFragment, firebaseLogIn // Exception : ${e.localizedMessage}")
        }
    }
    private fun firebaseRegister(){
        try{
            iRegisterClick = object : IButtonClick {
                override fun onIClick() {
                    authVM.firebaseRegister(
                        setEmail = binding.registerEmail.text.toString(),
                        setPassword = binding.registerPassword.text.toString(),
                        onSuccess = { firebaseUser: FirebaseUser? ->

                        },
                        onFailed = { reason : String? ->
                            Log.e("mException", "계정생성 실패 : ${reason}")
                        }
                    )
                }
            }
        }catch (e:Exception){
            Log.e("mException", "AuthIconFragment, firebaseRegister // Exception : ${e.localizedMessage}")
        }
    }
    private fun googleLogin(){
        try{
            authVM.googleLogin(
                onSuccess = { firebaseUser: FirebaseUser? ->

                },
                onFailed = { reason : String? ->
                    Log.e("mException", "계정생성 실패 : ${reason}")
                }
            )
        }catch (e:Exception){
            Log.e("mException", "AuthIconFragment, googleLogin // Exception : ${e.localizedMessage}")
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        iRegisterClick = null
        iLoginClick = null
    }
}