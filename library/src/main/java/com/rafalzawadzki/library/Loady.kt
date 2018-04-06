package com.rafalzawadzki.library

import android.support.annotation.LayoutRes
import android.util.ArrayMap
import android.view.View
import android.view.ViewGroup

object Loady {

    private val builders = ArrayMap<Int, Builder>()

    fun of(anchor: ViewGroup): Builder {
        var builder: Builder? = builders[anchor.id]
        if (builder == null) {
            builder = Builder(anchor)
            builders[anchor.id] = builder
        }
        return builder
    }

    class Builder(private val anchor: ViewGroup) {

        private val VIEWTAG = "Loady"
        private lateinit var widget: Widget

        fun with(@LayoutRes layoutRes: Int): Builder {
            this.widget = LayoutWidget(anchor.context, layoutRes)
            return this
        }

        fun with(widget: Widget): Builder {
            this.widget = widget
            return this
        }

        fun show() {
            anchor.tag = anchor.id
            widget.view.tag = VIEWTAG
            widget.widgetReady = this::widgetReady

            widget.onShow()
        }

        fun hide() {
            if (!::widget.isInitialized) {
                return
            }

            widget.onHide()

            val loadingView = anchor.findViewWithTag<View>(VIEWTAG)
            loadingView?.let {
                anchor.removeView(it)
            }

            anchor.invalidate()
        }

        fun remove() {
            hide()
            builders.remove(anchor.id)
        }

        fun isShown() = widget.isShown

        fun toggle() = if (isShown()) hide() else show()

        private fun widgetReady() {
            builders[anchor.id]?.let {
                if (anchor.parent !== anchor) anchor.addView(widget.view)
                anchor.invalidate()
            }
        }
    }
}
