package com.tl.face.manager

import android.content.Context
import android.view.View
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler
import kotlin.math.abs
import kotlin.math.min


class PickerLayoutManager private constructor(
    context: Context, orientation: Int, reverseLayout: Boolean, maxItem: Int, scale: Float, alpha: Boolean) :
    LinearLayoutManager(context, orientation, reverseLayout) {

    private val linearSnapHelper: LinearSnapHelper = LinearSnapHelper()
    private val maxItem: Int
    private val scale: Float
    private val alpha: Boolean
    private var recyclerView: RecyclerView? = null
    private var listener: OnPickerListener? = null

    init {
        this.maxItem = maxItem
        this.alpha = alpha
        this.scale = scale
    }

    override fun onAttachedToWindow(recyclerView: RecyclerView) {
        super.onAttachedToWindow(recyclerView)
        this.recyclerView = recyclerView

        this.recyclerView!!.clipToPadding = false

        linearSnapHelper.attachToRecyclerView(this.recyclerView)
    }

    override fun onDetachedFromWindow(recyclerView: RecyclerView?, recycler: Recycler?) {
        super.onDetachedFromWindow(recyclerView, recycler)
        this.recyclerView = null
    }

    override fun isAutoMeasureEnabled(): Boolean {
        return maxItem == 0
    }

    override fun onMeasure(recycler: Recycler, state: RecyclerView.State, widthSpec: Int, heightSpec: Int) {
        var width: Int = chooseSize(widthSpec, paddingLeft + paddingRight, ViewCompat.getMinimumWidth(recyclerView!!))
        var height: Int = chooseSize(heightSpec, paddingTop + paddingBottom, ViewCompat.getMinimumHeight(recyclerView!!))
        if (state.itemCount != 0 && maxItem != 0) {
            val itemView: View = recycler.getViewForPosition(0)
            measureChildWithMargins(itemView, widthSpec, heightSpec)
            if (orientation == HORIZONTAL) {
                val measuredWidth: Int = itemView.measuredWidth
                val paddingHorizontal: Int = (maxItem - 1) / 2 * measuredWidth
                recyclerView!!.setPadding(paddingHorizontal, 0, paddingHorizontal, 0)
                width = measuredWidth * maxItem
            } else if (orientation == VERTICAL) {
                val measuredHeight: Int = itemView.measuredHeight
                val paddingVertical: Int = (maxItem - 1) / 2 * measuredHeight
                recyclerView!!.setPadding(0, paddingVertical, 0, paddingVertical)
                height = measuredHeight * maxItem
            }
        }
        setMeasuredDimension(width, height)
    }

    override fun onScrollStateChanged(state: Int) {
        super.onScrollStateChanged(state)

        if (state != RecyclerView.SCROLL_STATE_IDLE) {
            return
        }
        recyclerView?.let {
            listener?.onPicked(it, getPickedPosition())
        }
    }

    override fun onLayoutChildren(recycler: Recycler, state: RecyclerView.State) {
        super.onLayoutChildren(recycler, state)
        if (itemCount < 0 || state.isPreLayout) {
            return
        }
        if (orientation == HORIZONTAL) {
            scaleHorizontalChildView()
        } else if (orientation == VERTICAL) {
            scaleVerticalChildView()
        }
    }

    override fun scrollHorizontallyBy(dx: Int, recycler: Recycler?, state: RecyclerView.State?): Int {
        scaleHorizontalChildView()
        return super.scrollHorizontallyBy(dx, recycler, state)
    }

    override fun scrollVerticallyBy(dy: Int, recycler: Recycler?, state: RecyclerView.State?): Int {
        scaleVerticalChildView()
        return super.scrollVerticallyBy(dy, recycler, state)
    }


    private fun scaleHorizontalChildView() {
        val mid: Float = width / 2.0f
        for (i in 0 until childCount) {
            val childView: View = getChildAt(i) ?: continue
            val childMid: Float =
                (getDecoratedLeft(childView) + getDecoratedRight(childView)) / 2.0f
            val scale: Float = 1.0f + (-1 * (1 - scale)) * min(mid, abs(mid - childMid)) / mid
            childView.scaleX = scale
            childView.scaleY = scale
            if (alpha) {
                childView.alpha = scale
            }
        }
    }


    private fun scaleVerticalChildView() {
        val mid: Float = height / 2.0f
        for (i in 0 until childCount) {
            val childView: View = getChildAt(i) ?: continue
            val childMid: Float = (getDecoratedTop(childView) + getDecoratedBottom(childView)) / 2.0f
            val scale: Float = 1.0f + (-1 * (1 - scale)) * (min(mid, abs(mid - childMid))) / mid
            childView.scaleX = scale
            childView.scaleY = scale
            if (alpha) {
                childView.alpha = scale
            }
        }
    }


    fun getPickedPosition(): Int {
        val itemView: View = linearSnapHelper.findSnapView(this) ?: return 0
        return getPosition(itemView)
    }


    fun setOnPickerListener(listener: OnPickerListener?) {
        this.listener = listener
    }

    interface OnPickerListener {

        /**

         * @param recyclerView
         * @param position
         */
        fun onPicked(recyclerView: RecyclerView, position: Int)
    }

    class Builder constructor(private val context: Context) {

        private var orientation: Int = VERTICAL
        private var reverseLayout: Boolean = false
        private var listener: OnPickerListener? = null
        private var maxItem: Int = 3
        private var scale: Float = 0.6f
        private var alpha: Boolean = true


        fun setOrientation(@RecyclerView.Orientation orientation: Int): Builder = apply {
            this.orientation = orientation
        }


        fun setReverseLayout(reverseLayout: Boolean): Builder = apply {
            this.reverseLayout = reverseLayout
        }


        fun setMaxItem(maxItem: Int): Builder = apply {
            this.maxItem = maxItem
        }


        fun setScale(scale: Float): Builder = apply {
            this.scale = scale
        }


        fun setAlpha(alpha: Boolean): Builder = apply {
            this.alpha = alpha
        }

        fun setOnPickerListener(listener: OnPickerListener?): Builder = apply {
            this.listener = listener
        }


        fun build(): PickerLayoutManager {
            val layoutManager = PickerLayoutManager(context, orientation, reverseLayout, maxItem, scale, alpha)
            layoutManager.setOnPickerListener(listener)
            return layoutManager
        }


        fun into(recyclerView: RecyclerView) {
            recyclerView.layoutManager = build()
        }
    }
}