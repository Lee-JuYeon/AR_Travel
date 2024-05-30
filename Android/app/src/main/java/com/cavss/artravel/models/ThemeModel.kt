package com.cavss.artravel.models

import com.cavss.artravel.types.ThemeType

data class ThemeModel(
    val userUID : String?,
    val themeUID : String?,
    val themeTitle : String,
    val themeType : ThemeType?,
    val cards : List<CardModel>?
) {

}