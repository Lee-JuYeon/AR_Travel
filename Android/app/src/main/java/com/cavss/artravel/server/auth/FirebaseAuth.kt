package com.cavss.artravel.server.auth

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class FirebaseAuth (var auth : FirebaseAuth?){


    fun logIn(setEmail : String, setPassword : String, onSuccess : (FirebaseUser?) -> Unit, onFailed : (String?) -> Unit){
        try{
            //auth 객체 초기화
            if (auth == null) auth = FirebaseAuth.getInstance()
            if (setEmail == null) {
                // 이메일, 비밀번호 Null 처리,
                // 이메일, 비밀번호 양식 처리
                onFailed("Email must not be empty")
                return
            }else if (setPassword == null){
                onFailed("password must not be empty")
                return
            }

            auth?.signInWithEmailAndPassword(setEmail,setPassword)?.addOnCompleteListener { task: Task<AuthResult> ->
                when {
                    task.isSuccessful -> onSuccess(task.result?.user) // 성공
                    task.isCanceled -> onFailed(task.exception?.message) // 실패
                    task.isComplete -> Log.e("mException", "AuthManager, logIn // task. completed") // 완료
                    else -> onFailed(task.exception?.message)
                }
            }
        }catch (e:Exception){
            Log.e("mException", "FirebaseAuth, logIn // Exception : ${e.localizedMessage}")
            onFailed(e.localizedMessage)
        }
    }

    fun logOut(onComplete: () -> Unit) {
        try {
            if (auth == null) auth = FirebaseAuth.getInstance()
            auth?.signOut()
            onComplete()
        } catch (e: Exception) {
            Log.e("mException", "FirebaseAuth, logOut // Exception: ${e.localizedMessage}")
        }
    }


    fun register(setEmail : String, setPassword : String, onSuccess : (FirebaseUser?) -> Unit, onFailed : (String?) -> Unit){
        try{
            //auth 객체 초기화
            if (auth == null) auth = FirebaseAuth.getInstance()
            if (setEmail == null) {
                // 이메일, 비밀번호 Null 처리,
                // 이메일, 비밀번호 양식 처리
                onFailed("Email must not be empty")
                return
            }else if (setPassword == null){
                onFailed("password must not be empty")
                return
            }

            auth?.createUserWithEmailAndPassword(setEmail,setPassword)?.addOnCompleteListener { task ->
                when {
                    task.isSuccessful -> onSuccess(task.result?.user) // 성공
                    task.isCanceled -> onFailed(task.exception?.message) // 실패
                    task.isComplete -> Log.e("mException", "FirebaseAuth, register // task. completed") // 완료
                    else -> onFailed(task.exception?.message)
                }
            }
        }catch (e:Exception){
            Log.e("mException", "FirebaseAuth, register // Exception : ${e.localizedMessage}")
            onFailed(e.localizedMessage)
        }
    }

    fun deleteAccount(onSuccess: () -> Unit, onFailed: (String?) -> Unit) {
        try {
            if (auth == null) auth = FirebaseAuth.getInstance()
            val user = auth?.currentUser
            user?.delete()?.addOnCompleteListener { task ->
                when {
                    task.isSuccessful -> onSuccess() // Success
                    task.isCanceled -> onFailed(task.exception?.message) // Failure
                    task.isComplete -> Log.e("mException", "FirebaseAuth, deleteAccount // task. completed") // Complete
                    else -> onFailed(task.exception?.message)
                }
            }
        } catch (e: Exception) {
            Log.e("mException", "FirebaseAuth, deleteAccount // Exception: ${e.localizedMessage}")
            onFailed(e.localizedMessage)
        }
    }
}