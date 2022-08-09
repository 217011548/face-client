package com.tl.face.app

import android.content.Context
import android.view.View
import androidx.annotation.IntRange
import androidx.annotation.LayoutRes
import com.tl.base.BaseAdapter
import java.util.*


abstract class AppAdapter<T> constructor(context: Context) :
    BaseAdapter<AppAdapter<T>.AppViewHolder>(context) {

    private var dataSet: MutableList<T> = ArrayList()


    private var pageNumber = 1


    private var lastPage = false


    private var tag: Any? = null

    override fun getItemCount(): Int {
        return getCount()
    }


    open fun getCount(): Int {
        return dataSet.size
    }


    open fun setData(data: MutableList<T>?) {
        if (data == null) {
            dataSet.clear()
        } else {
            dataSet = data
        }
        notifyDataSetChanged()
    }


    open fun getData(): MutableList<T> {
        return dataSet
    }


    open fun addData(data: MutableList<T>?) {
        if (data == null || data.isEmpty()) {
            return
        }
        dataSet.addAll(data)
        notifyItemRangeInserted(dataSet.size - data.size, data.size)
    }

    open fun clearData() {
        dataSet.clear()
        notifyDataSetChanged()
    }


    open fun containsItem(@IntRange(from = 0) position: Int): Boolean {
        return containsItem(getItem(position))
    }


    open fun containsItem(item: T?): Boolean {
        return if (item == null) {
            false
        } else dataSet.contains(item)
    }


    open fun getItem(@IntRange(from = 0) position: Int): T {
        return dataSet[position]
    }


    open fun setItem(@IntRange(from = 0) position: Int, item: T) {
        dataSet[position] = item
        notifyItemChanged(position)
    }


    open fun addItem(item: T) {
        addItem(dataSet.size, item)
    }

    open fun addItem(@IntRange(from = 0) position: Int, item: T) {
        var finalPosition = position
        if (finalPosition < dataSet.size) {
            dataSet.add(finalPosition, item)
        } else {
            dataSet.add(item)
            finalPosition = dataSet.size - 1
        }
        notifyItemInserted(finalPosition)
    }


    open fun removeItem(item: T) {
        val index = dataSet.indexOf(item)
        if (index != -1) {
            removeItem(index)
        }
    }

    open fun removeItem(@IntRange(from = 0) position: Int) {
        dataSet.removeAt(position)
        notifyItemRemoved(position)
    }


    open fun getPageNumber(): Int {
        return pageNumber
    }


    open fun setPageNumber(@IntRange(from = 0) number: Int) {
        pageNumber = number
    }


    open fun isLastPage(): Boolean {
        return lastPage
    }


    open fun setLastPage(last: Boolean) {
        lastPage = last
    }


    open fun getTag(): Any? {
        return tag
    }


    open fun setTag(tag: Any) {
        this.tag = tag
    }

    abstract inner class AppViewHolder : BaseViewHolder {

        constructor(@LayoutRes id: Int) : super(id)

        constructor(itemView: View) : super(itemView)
    }

    inner class SimpleViewHolder : AppViewHolder {

        constructor(@LayoutRes id: Int) : super(id)

        constructor(itemView: View) : super(itemView)

        override fun onBindView(position: Int) {}
    }
}