package com.cavss.artravel.server.db

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Date

enum class ThemeType(val rawValue : String){
    HEALING("HEALING"),
    HISTORY("HISTORY"),
    FOODIE("FOODIE"),
    CAMPING("CAMPING"),
    ACTIVITY("ACTIVITY"),
    SURFING("SURFING"),
    MOUNTAIN("MOUNTAIN"),
    TRAIN("TRAIN"),
    LIBERAL_ARTS("LIBERAL_ARTS"),
    TEMPLE_BUDDISM("TEMPLE_BUDDISM"),
    TEMPLE_CATHOLIC("TEMPLE_CATHOLIC"),
    SPRING_FESTIVAL("SPRING_FESTIVAL"),
    SUMMER_FESTIVAL("SUMMER_FESTIVAL"),
    AUTUMN_FESTIVAL("AUTUMN_FESTIVAL"),
    WINTER_FESTIVAL("WINTER_FESTIVAL"),
    LOCAL_TRIP("LOCAL_TRIP"),
    SPA("SPA")
}

data class ReplyModel(
    val userUID : String,
    val reply : String,
    val createDate : Date
){

}

data class CardModel(
    val userUID : String,
    val cardUID : String,
    val cardTitle : String,
    val cardImageUIDs : List<String>?,
    val cardText : String,
    val save : List<String>?,
    val createDate : Date,
    val replies : List<ReplyModel>?,
    val theme : ThemeType,
    val themeUID : String,
    val themeTitle : String,
    val latitude : Double,
    val longitude : Double

){

}

data class ThemeModel(
    val userUID : String,
    val themeUID : String,
    val themeTitle : String,
    val themeType : ThemeType,
    val cards : List<String>?
) {

}

class FirebaseDB : ViewModel() {

    private val db = FirebaseDatabase.getInstance().reference
    private val THEME = "theme"
    private val CARDS = "cards"
    fun createTheme(themeModel: ThemeModel) {
        try {
            db.child(THEME).child(themeModel.themeUID).setValue(themeModel)
        } catch (e: Exception) {
            Log.e("mException", "FirebaseDB, createTheme // Exception: ${e.localizedMessage}")
        }
    }

    fun readThemes(callback: (List<ThemeModel>) -> Unit) {
        try {
            db.child(THEME).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val themes = mutableListOf<ThemeModel>()
                    for (themeSnapshot in snapshot.children) {
                        themeSnapshot.getValue(ThemeModel::class.java)?.let { themes.add(it) }
                    }
                    callback(themes)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("mException", "FirebaseDB, readThemes // DatabaseError: ${error.message}")
                }
            })
        } catch (e: Exception) {
            Log.e("mException", "FirebaseDB, readThemes // Exception: ${e.localizedMessage}")
        }
    }

    fun updateTheme(themeUID: String, updatedTheme: ThemeModel) {
        try {
            db.child(THEME).child(themeUID).setValue(updatedTheme)
        } catch (e: Exception) {
            Log.e("mException", "FirebaseDB, updateTheme // Exception: ${e.localizedMessage}")
        }
    }

    fun deleteTheme(themeUID: String) {
        try {
            db.child(THEME).child(themeUID).removeValue()
        } catch (e: Exception) {
            Log.e("mException", "FirebaseDB, deleteTheme // Exception: ${e.localizedMessage}")
        }
    }

    // add Card
    fun addCard(themeUID: String, card: CardModel) {
        try {
            // 카드가 생성되면 해당 테마에 카드 UID를 추가
            db.child(THEME).child(themeUID).child(CARDS).child(card.cardUID).push()
        } catch (e: Exception) {
            Log.e("mException", "FirebaseDB, createCard // Exception: ${e.localizedMessage}")
        }
    }



    // Update Card
    fun updateCard(cardUID: String, updatedCard: CardModel) {
        try {
            db.child(THEME).child(cardUID).setValue(updatedCard)
        } catch (e: Exception) {
            Log.e("mException", "FirebaseDB, updateCard // Exception: ${e.localizedMessage}")
        }
    }

    // Delete Card
    fun deleteCard(cardUID: String, themeUID: String) {
        try {
            db.child("cards").child(cardUID).removeValue()
            // 카드가 삭제되면 해당 테마에서 카드 UID를 제거
            db.child("themes").child(themeUID).child("cards").child(cardUID).removeValue()
        } catch (e: Exception) {
            Log.e("mException", "FirebaseDB, deleteCard // Exception: ${e.localizedMessage}")
        }
    }

    // Add Reply to Card
    fun addReply(cardUID: String, reply: ReplyModel) {
        try {
            db.child("cards").child(cardUID).child("replies").push().setValue(reply)
        } catch (e: Exception) {
            Log.e("mException", "FirebaseDB, addReply // Exception: ${e.localizedMessage}")
        }
    }
}

