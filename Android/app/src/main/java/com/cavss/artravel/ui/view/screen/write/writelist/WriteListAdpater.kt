package com.cavss.artravel.ui.view.screen.write.writelist

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.cavss.artravel.databinding.HeaderThemeWriteBinding
import com.cavss.artravel.databinding.HolderThemeWriteBinding
import com.cavss.artravel.models.CardModel
import com.cavss.artravel.ui.custom.recyclerview.BaseDiffUtil
import com.cavss.artravel.ui.custom.recyclerview.IClickListener

class WriteListAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_ITEM = 1
    }

    private var list = mutableListOf<CardModel>()
    private var iClick: IClickListener<CardModel>? = null
    private var headerHolder: HeaderThemeWriteBinding? = null

    fun updateList(newItems: List<CardModel>?) {
        try {
            val diffResult = DiffUtil.calculateDiff(
                object : BaseDiffUtil<CardModel>(
                    oldList = list,
                    newList = newItems ?: mutableListOf()
                ) {}, false
            )

            diffResult.dispatchUpdatesTo(this)
            list.clear()
            list.addAll(newItems ?: mutableListOf())
        } catch (e: Exception) {
            Log.e("mException", "WriteListAdapter, updateList // Exception : ${e.message}")
        }
    }

    fun setOnClick(iclick: IClickListener<CardModel>?) {
        this.iClick = iclick
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) VIEW_TYPE_HEADER else VIEW_TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                headerHolder = HeaderThemeWriteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                WriteListHeader(headerHolder!!)
            }
            VIEW_TYPE_ITEM -> {
                val holder = HolderThemeWriteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                WriteListViewHolder(holder)
            }
            else -> {
                Log.e("mException", "WriteListAdapter, onCreateViewHolder // error : 알 수 없는 viewholder타입임")
                val holder = HolderThemeWriteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                WriteListViewHolder(holder)
            }
        }
    }

    override fun getItemCount(): Int = list.size + 1 // 헤더를 포함한 항목 수

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is WriteListHeader -> {
                // 헤더 바인딩 작업
            }
            is WriteListViewHolder -> {
                holder.bind(list[position - 1], iClick) // 헤더로 인한 인덱스 조정
            }
            else -> {
                Log.e("mException", "WriteListAdapter, onBindViewHolder // error : 알 수 없는 viewholder타입임")
                (holder as WriteListViewHolder).bind(list[position - 1], iClick) // 헤더로 인한 인덱스 조정
            }
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        when (holder) {
            is WriteListHeader -> {
                // 헤더 재활용 작업
            }
            is WriteListViewHolder -> {
                holder.bind(null, null)
            }
            else -> {
                Log.e("mException", "WriteListAdapter, onViewRecycled // error : 알 수 없는 viewholder타입임")
                (holder as WriteListViewHolder).bind(null, null)
            }
        }
    }

    fun getHeaderHolder(): HeaderThemeWriteBinding? {
        return headerHolder
    }
}
