package com.cavss.artravel.ui.view.screen.setting

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cavss.artravel.R
import com.cavss.artravel.databinding.FragmentSettingAuthiconBinding
import com.cavss.artravel.server.auth.AuthManager
import com.cavss.artravel.ui.custom.recyclerview.IClickListener
import com.google.firebase.auth.FirebaseUser

class AuthIconFragment(var authManager: AuthManager?) : Fragment() {

    private lateinit var binding : FragmentSettingAuthiconBinding
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentSettingAuthiconBinding.inflate(inflater, container, false)
        binding.run {
            authManager?.googleInit(this@AuthIconFragment)
            setAuthIconView(recyclerView = authRecyclerview)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
//                                    authManager.firebaseCreate(
//                                        setEmail = "redpond2@naver.com",
//                                        setPassword = "vlwk!)1928",
//                                        onSuccess = { firebaseUser: FirebaseUser? ->
//                                            Log.e("mException", "계정생성 완료 : ${firebaseUser}")
//                                        },
//                                        onFailed = { reason : String? ->
//                                            Log.e("mException", "계정생성 실패 : ${reason}")
//                                        }
//                                    )
                                    authManager?.googleDelete(
                                        onSuccess = {
                                            Log.e("mException", "계정 삭제")
                                        },
                                        onFailed = { reason : String? ->
                                            Log.e("mException", "계정 삭제 실패 : ${reason}" )
                                        }
                                    )
                                }
                                "GOOGLE" -> {
                                    authManager?.googleLogin(
                                        onSuccess = { firebaseUser: FirebaseUser? ->
                                            Log.e("mException", "계정생성 완료 : ${firebaseUser}")
                                        },
                                        onFailed = { reason : String? ->
                                            Log.e("mException", "계정생성 실패 : ${reason}")
                                        }
                                    )
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
}