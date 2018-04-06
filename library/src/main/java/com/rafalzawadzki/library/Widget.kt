package com.rafalzawadzki.library

import android.content.Context
import android.view.LayoutInflater
import android.view.View

abstract class Widget(var context: Context) {

    var isShown: Boolean = false

    val view: View by lazy {
        LayoutInflater.from(context).inflate(layout, null, false)
    }
    var widgetReady: (() -> Unit)? = null

    abstract val layout: Int

    open fun onShow() {
        widgetReady?.invoke()
        isShown = true
    }

    open fun onHide() {
        isShown = false
    }

    open fun onDestroy() {

    }
}
