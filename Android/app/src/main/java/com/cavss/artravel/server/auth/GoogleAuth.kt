package com.cavss.artravel.server.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.registerForActivityResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.cavss.artravel.BuildConfig
import com.cavss.artravel.MainActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider

class GoogleAuth(private val activity: FragmentActivity, var auth: FirebaseAuth?) {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

    init {
        val web_client_id = BuildConfig.firebase_web_client_id
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(web_client_id) // Use your web client ID
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(activity, gso)

    }

    fun setting(fragment: Fragment){
        googleSignInLauncher = fragment.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data: Intent? = result.data
            if (result.resultCode == Activity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    val credential = GoogleAuthProvider.getCredential(account?.idToken, null)
                    auth?.signInWithCredential(credential)?.addOnCompleteListener(activity) { task ->
                        when {
                            task.isSuccessful -> {
                                Log.e("mException", "GoogleAuth, googleSignInLauncher, activityResult, signInWithCredential result : success")
                            }
                            task.isCanceled -> {
                                Log.e("mException", "GoogleAuth, googleSignInLauncher, activityResult, signInWithCredential result : canceled // Canceled : ${task.exception}")
                            }
                            task.isComplete -> {
                                Log.e("mException", "GoogleAuth, googleSignInLauncher, activityResult, signInWithCredential result : complete")
                            }
                            else -> {
                                Log.e("mException", "GoogleAuth, googleSignInLauncher, activityResult, signInWithCredential result : else // else Failure : ${task.exception}")
                            }
                        }
                    }
                } catch (e: ApiException) {
                    Log.e("mException", "GoogleAuth, googleSignInLauncher, activityResult // ApiException : ${e.message}")
                } catch (e: Exception) {
                    Log.e("mException", "GoogleAuth, googleSignInLauncher, activityResult // Exception : ${e.message}")
                }
            }
        }
    }


    fun logIn(onSuccess: (FirebaseUser?) -> Unit, onFailed: (String?) -> Unit) {
        try {
            val signInIntent = googleSignInClient.signInIntent // 로그인 인텐트 생성
            googleSignInLauncher.launch(signInIntent)
            onSuccess(auth?.currentUser)
        } catch (e: ApiException) {
            Log.e("mException", "GoogleAuth, logIn // ApiException : ${e.localizedMessage}")
            onFailed(e.localizedMessage)
        } catch (e: Exception) {
            Log.e("mException", "GoogleAuth, logIn // Exception: ${e.localizedMessage}")
            onFailed(e.localizedMessage)
        }
    }

    fun logOut(onComplete: () -> Unit) {
        try {
            if (auth == null) auth = FirebaseAuth.getInstance()
            auth?.signOut()
            googleSignInClient.signOut().addOnCompleteListener(activity) {
                onComplete()
            }
        } catch (e: Exception) {
            Log.e("mException", "GoogleAuth, logOut // Exception: ${e.localizedMessage}")
        }
    }

    fun delete(onSuccess: () -> Unit, onFailed: (String?) -> Unit) {
        try {
            if (auth == null) auth = FirebaseAuth.getInstance()
            val user = auth?.currentUser
            user?.delete()?.addOnCompleteListener(activity) { task ->
                when {
                    task.isSuccessful -> {
                        googleSignInClient.revokeAccess().addOnCompleteListener {
                            onSuccess()
                        }
                    }
                    task.isCanceled -> {
                        Log.e("mException", "GoogleAuth, delete // canceled : ${task.exception}")
                        onFailed(task.exception?.message)
                    }
                    task.isComplete -> {
                        Log.e("mException", "GoogleAuth, delete // complete")
                    }
                    else -> {
                        Log.e("mException", "GoogleAuth, delete // failure : ${task.exception}")
                        onFailed(task.exception?.message)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("mException", "GoogleAuth, delete // Exception: ${e.localizedMessage}")
            onFailed(e.localizedMessage)
        }
    }
}
