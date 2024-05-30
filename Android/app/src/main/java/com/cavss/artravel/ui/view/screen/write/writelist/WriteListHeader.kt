package com.cavss.artravel.ui.view.screen.write.writelist

import android.util.Log
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.cavss.artravel.databinding.HeaderThemeWriteBinding
class WriteListHeader(private val binding : HeaderThemeWriteBinding): RecyclerView.ViewHolder(binding.root) {

    fun setHeaderUI(uiSetting : (HeaderThemeWriteBinding) -> Unit){
        try{
            uiSetting(binding)
        }catch (e:Exception){
            Log.e("mException", "WriteListHeader, setHeaderUI // Exception : ${e.localizedMessage}")
        }
    }
}