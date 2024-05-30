package com.cavss.artravel.ui.view.screen.write.historylist

import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import com.cavss.artravel.databinding.HolderThemeHistoryBinding
import com.cavss.artravel.models.CardModel
import com.cavss.artravel.models.ThemeModel
import com.cavss.artravel.ui.custom.recyclerview.IClickListener

class HistoryViewHolder(private val binding : HolderThemeHistoryBinding ): RecyclerView.ViewHolder(binding.root) {
    fun bind(model : ThemeModel?, clickCallback : IClickListener<ThemeModel>?){
        try{
            binding.clickCallback = clickCallback
            binding.model = model
            binding.position = adapterPosition
        }catch (e:Exception){
            Log.e("mException", "WriteListViewHolder, bind // Exception : ${e.localizedMessage}")
        }
    }
}