package com.tl.face.http.model

import com.tl.face.http.model.HttpListData.ListBean
import kotlin.math.ceil


class HttpListData<T> : HttpData<ListBean<T?>?>() {

    class ListBean<T> {


        private val pageIndex: Int = 0


        private val pageSize: Int = 0


        private val totalNumber: Int = 0


        private val items: MutableList<T?>? = null


        fun isLastPage(): Boolean {
            return ceil((totalNumber.toFloat() / pageSize.toFloat())) <= pageIndex
        }

        fun getTotalNumber(): Int {
            return totalNumber
        }

        fun getPageIndex(): Int {
            return pageIndex
        }

        fun getPageSize(): Int {
            return pageSize
        }

        fun getItems(): MutableList<T?>? {
            return items
        }
    }
}