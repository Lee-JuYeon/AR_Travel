package com.cavss.artravel.ui.custom.recyclerview

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView


abstract class BaseAdapters{
    abstract class RecyclerAdapter<MODEL : Any, BIND : ViewDataBinding>() : RecyclerView.Adapter<BaseViewHolder<MODEL, BIND>>(){
        private val items = mutableListOf<MODEL>()
        //        abstract fun getDiffUtil(oldList: List<MODEL>, newList: List<MODEL>) : BaseDiffUtil<MODEL>
        fun updateList(newItems : List<MODEL>?){
            try{
                val diffResult = DiffUtil.calculateDiff(
                    object : BaseDiffUtil<MODEL>(
                        oldList = items,
                        newList = newItems ?: mutableListOf()
                    ){},
                    false
                )

                diffResult.dispatchUpdatesTo(this)
                items.clear()
                items.addAll(newItems ?: mutableListOf())

                Log.d("mDebug", "BaseAdapter, RecyclerAdapter, updateList // updateList success")
            }catch (e:Exception){
                Log.e("mException", "BaseAdapter, RecyclerAdapter, updateList // Exception : ${e.message}")
            }
        }
        abstract fun setViewHolderXmlFileName(viewType: Int): Int // TODO: ViewHolder의 XML파일을 넣는 곳.
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<MODEL, BIND> {
            val binding : BIND = DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                setViewHolderXmlFileName(viewType),
                parent,
                false
            )
            return object : BaseViewHolder<MODEL, BIND>(binding = binding){}
        }

        abstract fun setViewHolderVariable(position: Int, model : MODEL?) : List<Pair<Int, Any>>
        override fun onBindViewHolder(holder: BaseViewHolder<MODEL, BIND>, position: Int) {
            try{
                holder.let {
//                    it.bind(items[position], position, clickListener!!)
                    it.bindVariable(setViewHolderVariable(position, items[position]))
                    Log.d("mDebug", "BaseAdapter, RecyclerAdapter, onBindViewHolder // binding success")
                }
            }catch (e:Exception){
                Log.e("mException", "BaseAdapter, RecyclerAdapter, onBindViewHolder // Exception : ${e.message}")
            }
        }

        override fun getItemCount() = items.size

        override fun onViewRecycled(holder: BaseViewHolder<MODEL, BIND>) {
            try{
                super.onViewRecycled(holder)
                holder.let {
                    it.bindVariable(setViewHolderVariable(0, null))
                    Log.d("mDebug", "BaseAdapter, RecyclerAdapter, onViewRecycled // null success")
                }
            }catch (e:Exception){
                Log.e("mException", "BaseAdapter, RecyclerAdapter, onViewRecycled // Exception : ${e.message}")
            }
        }
    }
}