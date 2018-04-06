package com.rafalzawadzki.library

import android.content.Context
import android.support.annotation.LayoutRes

class LayoutWidget(context: Context, @LayoutRes layoutRes: Int) : Widget(context) {

    override val layout = layoutRes

}
