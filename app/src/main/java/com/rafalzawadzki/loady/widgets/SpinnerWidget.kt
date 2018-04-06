package com.rafalzawadzki.loady.widgets

import android.content.Context
import android.content.res.Resources
import android.util.TypedValue
import android.view.View
import android.widget.ProgressBar
import com.rafalzawadzki.library.Widget
import com.rafalzawadzki.loady.R

class SpinnerWidget(context: Context,
                    private var progressSize: Int = Size.SIZE_SMALL)
    : Widget(context) {

    override val layout = R.layout.loady_widget_spinner

    override fun onShow() {
        super.onShow()
        setSize()
    }

    fun setSize() {
        val bar = view.findViewById<View>(R.id.progressBar) as ProgressBar
        bar.layoutParams.height = dpToPx(progressSize)
        bar.layoutParams.width = dpToPx(progressSize)
        bar.invalidate()
    }

    object Size {
        const val SIZE_SMALL = 20
        const val SIZE_MEDIUM = 40
        const val SIZE_BIG = 70
    }

    private fun dpToPx(dpValue: Int): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpValue.toFloat(),
                Resources.getSystem().displayMetrics).toInt()
    }
}
