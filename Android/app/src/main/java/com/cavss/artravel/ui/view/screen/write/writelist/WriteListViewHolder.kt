package com.cavss.artravel.ui.view.screen.write.writelist

import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import com.cavss.artravel.databinding.HolderThemeWriteBinding
import com.cavss.artravel.models.CardModel
import com.cavss.artravel.ui.custom.recyclerview.IClickListener

class WriteListViewHolder(private val binding : HolderThemeWriteBinding): RecyclerView.ViewHolder(binding.root) {
    fun bind(model : CardModel?, clickCallback : IClickListener<CardModel>?){
        try{
            binding.clickCallback = clickCallback
            binding.model = model
            binding.position = adapterPosition
        }catch (e:Exception){
            Log.e("mException", "WriteListViewHolder, bind // Exception : ${e.localizedMessage}")
        }
    }
}