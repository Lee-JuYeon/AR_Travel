package com.cavss.artravel.util

import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.cavss.artravel.R
import com.cavss.artravel.types.ThemeType

object CustomBindingAdapter {

    @JvmStatic
    @BindingAdapter("img")
    fun ImageView.img(image : Int){
        this.setImageResource(image)
    }

    @JvmStatic
    @BindingAdapter("imgURL")
    fun ImageView.imgURL(image : String?){
        when(image){
            "ADD_PHOTO" -> {
                this.setImageResource(R.drawable.image_google)
            }
            else -> {
                Glide.with(this)
                    .load(R.drawable.ic_launcher_background)
                    .placeholder(R.drawable.ic_launcher_background) // Glide 로 이미지 로딩을 시작하기 전에 보여줄 이미지
                    .error(R.drawable.ic_launcher_background) // 리소스를 불러오다가 에러가 발생했을 때 보여줄 이미지
                    .fallback(R.drawable.ic_launcher_background) //  load할 url이 null인 경우 등 비어있을 때 보여줄 이미지
                    .skipMemoryCache(true) // 메모리에 캐싱하지 않으려면 true
                    .diskCacheStrategy(DiskCacheStrategy.NONE) //  디스크에 캐싱하지 않으려면 DiskCacheStrategy.NONE
                    .into(this)
            }
        }
    }

    @JvmStatic
    @BindingAdapter("themeType")
    fun TextView.themeType(type : ThemeType?){
        this.text = type?.rawValue ?: ""
    }
}