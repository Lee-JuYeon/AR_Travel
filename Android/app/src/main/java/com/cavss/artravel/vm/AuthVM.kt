package com.cavss.artravel.vm

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cavss.artravel.server.auth.AuthManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class AuthVM : ViewModel() {

    private var authManager : AuthManager? = null


    fun getAuth() : FirebaseAuth? = authManager?.getAuth()
    fun isUserExist() : Boolean = authManager?.isUserExist() ?: false

    fun setInit(setFragmentActivity : FragmentActivity){
        try{
            authManager = AuthManager(activity = setFragmentActivity)
        }catch (e:Exception){
            Log.e("mException", "AuthVM , setInit // Exception : ${e.localizedMessage}")
        }
    }
    fun firebaseLogin(setEmail : String, setPassword : String, onSuccess : (FirebaseUser?) -> Unit, onFailed : (String?) -> Unit){
        try{
            authManager?.let {
                it.firebaseLogin(
                    setEmail,
                    setPassword,
                    onSuccess = { firebaseUser: FirebaseUser? ->
                        setUserExist(true)
                        onSuccess(firebaseUser)
                    },
                    onFailed
                )
            }
        }catch (e:Exception){
            Log.e("mException", "AuthVM , firebaseLogin // Exception : ${e.localizedMessage}")
        }
    }
    fun firebaseRegister(setEmail : String, setPassword : String, onSuccess : (FirebaseUser?) -> Unit, onFailed : (String?) -> Unit){
        try{
            authManager?.firebaseCreate(
                setEmail,
                setPassword,
                onSuccess = { firebaseUser: FirebaseUser? ->
                    setUserExist(true)
                    onSuccess(firebaseUser)
                },
                onFailed
            )
        }catch (e:Exception){
            Log.e("mException", "AuthVM , firebaseRegister // Exception : ${e.localizedMessage}")
        }
    }
    fun firebaseLogout(){
        try{
            authManager?.firebaseLogOut {

            }
        }catch (e:Exception){
            Log.e("mException", "AuthVM , firebaseLogout // Exception : ${e.localizedMessage}")
        }
    }
    fun firebaseDelete(onSuccess: () -> Unit, onFailed: (String?) -> Unit){
        try{
            authManager?.firebaseDelete(onSuccess, onFailed)
        }catch (e:Exception){
            Log.e("mException", "AuthVM , firebaseDelete // Exception : ${e.localizedMessage}")
        }
    }

    fun googleInit(fragment: Fragment){
        try{
            authManager?.googleInit(fragment)
        }catch (e:Exception){
            Log.e("mException", "AuthVM , googleInit // Exception : ${e.localizedMessage}")
        }
    }

    fun googleLogin(onSuccess: (FirebaseUser?) -> Unit, onFailed: (String?) -> Unit){
        try{
            authManager?.googleLogin(
                onSuccess = { firebaseUser: FirebaseUser? ->
                    setUserExist(true)
                    onSuccess(firebaseUser)
                },
                onFailed
            )
        }catch (e:Exception){
            Log.e("mException", "AuthVM , googleLogin // Exception : ${e.localizedMessage}")
        }
    }
    fun googleLogOut(onComplete: () -> Unit){
        try{
            authManager?.googleLogOut(onComplete)
        }catch (e:Exception){
            Log.e("mException", "AuthVM , googleLogOut // Exception : ${e.localizedMessage}")
        }
    }

    fun googleDelete(onSuccess: () -> Unit, onFailed: (String?) -> Unit){
        try{
            authManager?.googleDelete(onSuccess,onFailed)
        }catch (e:Exception){
            Log.e("mException", "AuthVM , googleDelete // Exception : ${e.localizedMessage}")
        }
    }


    private val _isUserObserving : MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)
    fun setUserExist(userExist : Boolean){
        _isUserObserving.postValue(userExist)
    }
    val getUserObserving : LiveData<Boolean>
        get() = _isUserObserving

    override fun onCleared() {
        super.onCleared()
        authManager = null
    }
}