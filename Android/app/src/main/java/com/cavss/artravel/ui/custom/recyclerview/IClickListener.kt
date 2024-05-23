package com.cavss.artravel.ui.custom.recyclerview

interface IClickListener<MODEL> {
    fun onItemClick(model : MODEL, position : Int)
}

interface IClickEventListener {
    fun onItemClick(model : Any, position : Int)
}
