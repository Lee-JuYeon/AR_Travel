package com.cavss.artravel.ui.view.screen.setting.auth

import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import com.cavss.artravel.databinding.HolderAuthIconBinding
import com.cavss.artravel.ui.custom.recyclerview.IClickListener

class AuthIconViewHolder(private val binding : HolderAuthIconBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(model : AuthIconModel?, clickCallBack : IClickListener<AuthIconModel>?){
        try {
            binding.let {
                it.model = model
                it.position = adapterPosition
                it.clickCallback = clickCallBack
                it.executePendingBindings()
            }
        }catch (e:Exception){
            Log.e("mException", "AuthIconViewholder, bind // Exception : ${e.localizedMessage}")
        }
    }
}