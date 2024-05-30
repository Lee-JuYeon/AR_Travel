package com.cavss.artravel.models

import java.util.Date

data class ReplyModel(
    val userUID : String,
    val reply : String,
    val createDate : Date
){

}