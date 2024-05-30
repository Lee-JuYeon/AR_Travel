package com.cavss.artravel.ui.custom.recyclerview

import android.graphics.Canvas
import androidx.recyclerview.widget.RecyclerView

class StickyHeaderItemDecoration(
    private val headerProvider: (Int) -> Boolean
) : RecyclerView.ItemDecoration() {

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDrawOver(c, parent, state)

        val child = parent.getChildAt(0) ?: return
        val position = parent.getChildAdapterPosition(child)

        if (headerProvider(position)) {
            val header = child
            c.save()
            c.translate(0f, maxOf(0, child.top).toFloat())
            header.draw(c)
            c.restore()
        }
    }
}
