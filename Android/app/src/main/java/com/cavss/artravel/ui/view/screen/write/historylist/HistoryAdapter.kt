package com.cavss.artravel.ui.view.screen.write.historylist

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.cavss.artravel.databinding.HolderThemeHistoryBinding
import com.cavss.artravel.databinding.HolderThemeWriteBinding
import com.cavss.artravel.models.CardModel
import com.cavss.artravel.models.ThemeModel
import com.cavss.artravel.ui.custom.recyclerview.BaseDiffUtil
import com.cavss.artravel.ui.custom.recyclerview.IClickListener
import com.cavss.artravel.ui.view.screen.write.writelist.WriteListViewHolder

class HistoryAdapter : RecyclerView.Adapter<HistoryViewHolder>(){

    private var list = mutableListOf<ThemeModel>()
    fun updateList(newItems : List<ThemeModel>?){
        try{
            val diffResult = DiffUtil.calculateDiff(
                object : BaseDiffUtil<ThemeModel>(
                    oldList = list,
                    newList = newItems ?: mutableListOf()
                ){},
                false
            )

            val headerModel = ThemeModel(null,"MAKE_NEW_THEME","새 테마 만들기",null,null)

            diffResult.dispatchUpdatesTo(this)
            list.clear()
            list.add(0, headerModel)
            list.addAll(newItems ?: mutableListOf())
        }catch (e:Exception){
            Log.e("mException", "HistoryAdapter, updateList // Exception : ${e.message}")
        }
    }

    private var iClick : IClickListener<ThemeModel>? = null
    fun setOnClick(iclick : IClickListener<ThemeModel>?){
        this.iClick = iclick
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = HolderThemeHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistoryViewHolder(binding)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(list[position], iClick)
    }

    override fun onViewRecycled(holder: HistoryViewHolder) {
        super.onViewRecycled(holder)
        holder.bind(null, null)
    }
}