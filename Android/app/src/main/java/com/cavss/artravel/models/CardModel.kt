package com.cavss.artravel.models

import com.cavss.artravel.types.ThemeType
import java.util.Date

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