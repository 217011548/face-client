package com.tl.face.other

import android.graphics.Canvas
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration


class GridSpaceDecoration constructor(private val space: Int) : ItemDecoration() {

    override fun onDraw(canvas: Canvas, recyclerView: RecyclerView, state: RecyclerView.State) {}

    override fun onDrawOver(canvas: Canvas, recyclerView: RecyclerView, state: RecyclerView.State) {}

    override fun getItemOffsets(rect: Rect, view: View, recyclerView: RecyclerView, state: RecyclerView.State) {
        val position: Int = recyclerView.getChildAdapterPosition(view)
        val spanCount: Int = (recyclerView.layoutManager as GridLayoutManager).spanCount


        if ((position + 1) % spanCount == 0) {
            rect.right = space
        }


        if (position < spanCount) {
            rect.top = space
        }
        rect.bottom = space
        rect.left = space
    }
}