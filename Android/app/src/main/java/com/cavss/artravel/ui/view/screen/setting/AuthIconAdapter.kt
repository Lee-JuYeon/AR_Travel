package com.cavss.artravel.ui.view.screen.setting

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.cavss.artravel.databinding.HolderAuthIconBinding
import com.cavss.artravel.ui.custom.recyclerview.BaseDiffUtil
import com.cavss.artravel.ui.custom.recyclerview.IClickListener

class AuthIconAdapter : RecyclerView.Adapter<AuthIconViewHolder>() {

    private val items = mutableListOf<AuthIconModel>()
    fun updateList(newItems : List<AuthIconModel>?){
        try{
            val diffResult = DiffUtil.calculateDiff(
                object : BaseDiffUtil<AuthIconModel>(
                    oldList = items,
                    newList = newItems ?: mutableListOf()
                ){},
                false
            )

            diffResult.dispatchUpdatesTo(this)
            items.clear()
            items.addAll(newItems ?: mutableListOf())
        }catch (e:Exception){
            Log.e("mException", "AuthIconAdapter, updateList // Exception : ${e.message}")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AuthIconViewHolder {
        val binding  = HolderAuthIconBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AuthIconViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    private var clickListener : IClickListener<AuthIconModel>? = null
    fun setClickListener(listener : IClickListener<AuthIconModel>){
        this.clickListener = listener
    }
    override fun onBindViewHolder(holder: AuthIconViewHolder, position: Int) {
        try{
            val item = items[position]
            holder.bind(item, clickCallBack = clickListener)
        }catch (e:Exception){
            Log.e("mException", "AuthIconAdapter, onViewRecycled : Exception : ${e.localizedMessage}")
        }
    }

    override fun onViewRecycled(holder: AuthIconViewHolder) {
        try{
            super.onViewRecycled(holder)
            holder.bind(model = null, clickCallBack = null)
        }catch (e:Exception){
            Log.e("mException", "AuthIconAdapter, onViewRecycled : Exception : ${e.localizedMessage}")
        }
    }
}