package com.cavss.artravel.server.auth

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class AuthManager(var activity : FragmentActivity) {

    private var auth : FirebaseAuth? = null
    private var firebaseAuth : com.cavss.artravel.server.auth.FirebaseAuth? = null
    private var googleAuth : GoogleAuth? = null
    val firebaseApp = FirebaseApp.getInstance()

    init {
        if (auth == null) auth = FirebaseAuth.getInstance(firebaseApp)
        firebaseAuth = FirebaseAuth(auth = auth)
        googleAuth = GoogleAuth(activity, auth)
    }

    fun getAuth() : FirebaseAuth?{
        if (auth == null) auth = FirebaseAuth.getInstance(firebaseApp)
        return auth
    }
    fun getUser() : Boolean {
        return try {
            if (auth == null) auth = FirebaseAuth.getInstance()
            auth?.currentUser != null
        } catch (e: Exception) {
            Log.e("mException", "AuthManager, getUser // Exception : ${e.localizedMessage}")
            false
        }
    }


    fun googleInit(fragment : Fragment){
        googleAuth?.setting(fragment)
    }
    fun googleLogin(onSuccess: (FirebaseUser?) -> Unit, onFailed: (String?) -> Unit){
        googleAuth?.logIn(onSuccess, onFailed)
    }
    fun googleLogOut(onComplete: () -> Unit){
        googleAuth?.logOut(onComplete)
    }
    fun googleDelete(onSuccess: () -> Unit, onFailed: (String?) -> Unit){
        googleAuth?.delete(onSuccess, onFailed)
    }




    fun firebaseLogin(setEmail : String, setPassword : String, onSuccess : (FirebaseUser?) -> Unit, onFailed : (String?) -> Unit){
        firebaseAuth?.logIn(setEmail, setPassword, onSuccess, onFailed)
    }
    fun firebaseLogOut(onComplete: () -> Unit){
        firebaseAuth?.logOut(onComplete)
    }
    fun firebaseCreate(setEmail : String, setPassword : String, onSuccess : (FirebaseUser?) -> Unit, onFailed : (String?) -> Unit){
        firebaseAuth?.register(setEmail, setPassword, onSuccess, onFailed)
    }
    fun firebaseDelete(onSuccess: () -> Unit, onFailed: (String?) -> Unit){
        firebaseAuth?.deleteAccount(onSuccess, onFailed)
    }

}