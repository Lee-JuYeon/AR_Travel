package com.cavss.artravel.ui.custom.recyclerview


import androidx.recyclerview.widget.DiffUtil

abstract class BaseDiffUtil<MODEL>(
    private val oldList : MutableList<MODEL>,
    private val newList : List<MODEL>
) : DiffUtil.Callback() {

    override fun getOldListSize() = oldList.size

    override fun getNewListSize() = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}