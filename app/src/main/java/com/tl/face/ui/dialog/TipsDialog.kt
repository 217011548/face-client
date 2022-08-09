package com.tl.face.ui.dialog

import android.content.Context
import android.text.TextUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.tl.base.BaseDialog
import com.tl.base.action.AnimAction
import com.tl.face.R


class TipsDialog {

    companion object {
        const val ICON_FINISH: Int = R.drawable.tips_finish_ic
        const val ICON_ERROR: Int = R.drawable.tips_error_ic
        const val ICON_WARNING: Int = R.drawable.tips_warning_ic
    }

    class Builder(context: Context) : BaseDialog.Builder<Builder>(context),
        Runnable, BaseDialog.OnShowListener {

        private val messageView: TextView? by lazy { findViewById(R.id.tv_tips_message) }
        private val iconView: ImageView? by lazy { findViewById(R.id.iv_tips_icon) }

        private var duration = 2000

        init {
            setContentView(R.layout.tips_dialog)
            setAnimStyle(AnimAction.ANIM_TOAST)
            setBackgroundDimEnabled(false)
            setCancelable(false)
            addOnShowListener(this)
        }

        fun setIcon(@DrawableRes id: Int): Builder = apply {
            iconView?.setImageResource(id)
        }

        fun setDuration(duration: Int): Builder = apply {
            this.duration = duration
        }

        fun setMessage(@StringRes id: Int): Builder = apply {
            setMessage(getString(id))
        }

        fun setMessage(text: CharSequence?): Builder = apply {
            messageView?.text = text
        }

        override fun create(): BaseDialog {

            requireNotNull(iconView?.drawable) { "The display type must be specified" }

            require(!TextUtils.isEmpty(messageView?.text.toString())) { "Dialog message not null" }
            return super.create()
        }

        override fun onShow(dialog: BaseDialog?) {

            postDelayed(this, duration.toLong())
        }

        override fun run() {
            if (!isShowing()) {
                return
            }
            dismiss()
        }
    }
}