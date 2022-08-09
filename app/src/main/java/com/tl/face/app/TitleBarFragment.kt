package com.tl.face.app

import android.os.Bundle
import android.view.*
import com.gyf.immersionbar.ImmersionBar
import com.hjq.bar.TitleBar
import com.tl.face.R
import com.tl.face.action.TitleBarAction

abstract class TitleBarFragment<A : AppActivity> : AppFragment<A>(), TitleBarAction {


    private var titleBar: TitleBar? = null


    private var immersionBar: ImmersionBar? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val titleBar = getTitleBar()

        titleBar?.setOnTitleBarListener(this)

        if (isStatusBarEnabled()) {

            getStatusBarConfig().init()
            if (titleBar != null) {

                ImmersionBar.setTitleBar(this, titleBar)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (isStatusBarEnabled()) {

            getStatusBarConfig().init()
        }
    }


    open fun isStatusBarEnabled(): Boolean {
        return false
    }


    protected fun getStatusBarConfig(): ImmersionBar {
        if (immersionBar == null) {
            immersionBar = createStatusBarConfig()
        }
        return immersionBar!!
    }


    protected fun createStatusBarConfig(): ImmersionBar {
        return ImmersionBar.with(this)

            .statusBarDarkFont(isStatusBarDarkFont())

            .navigationBarColor(R.color.white)

            .autoDarkModeEnable(true, 0.2f)
    }


    protected open fun isStatusBarDarkFont(): Boolean {

        return getAttachActivity()!!.isStatusBarDarkFont()
    }

    override fun getTitleBar(): TitleBar? {
        if (titleBar == null || !isLoading()) {
            titleBar = obtainTitleBar(view as ViewGroup)
        }
        return titleBar
    }
}