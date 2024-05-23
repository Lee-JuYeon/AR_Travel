package com.cavss.artravel.util

import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter

object CustomBindingAdapter {

    @JvmStatic
    @BindingAdapter("app:img")
    fun ImageView.img(image : Int){
        this.setImageResource(image)
    }
}