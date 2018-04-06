package com.rafalzawadzki.loady.widgets

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.view.ViewGroup
import android.widget.ImageView
import com.rafalzawadzki.library.Widget
import com.rafalzawadzki.loady.R

class BlurringTileWidget(context: Context,
                         private val blurredParent: ViewGroup,
                         private var blurQuality: Int = BLUR_QUALITY_LOW)
    : Widget(context) {

    override val layout = R.layout.loady_widget_blurring_tile

    private var blurTarget: ImageView = view.findViewById(R.id.blur)
    private val blurEngine: BlurEngine by lazy { BlurEngine(context) }

    override fun onShow() {
        blurEngine.setDownScaleFactor(if (blurQuality == BLUR_QUALITY_LOW) 4.0f else 1.0f)
        blurEngine.blur(blurredParent) { result ->
            result?.let {
                blurTarget.setImageBitmap(result)
                blurTarget.scaleType = ImageView.ScaleType.CENTER_CROP
                blurTarget.setImageDrawable(BitmapDrawable(context.resources, result))
                super@BlurringTileWidget.onShow()
            }
        }
    }

    override fun onHide() {
        super.onHide()
        blurEngine.destroy()
    }

    companion object {
        const val BLUR_QUALITY_LOW = 0
        const val BLUR_QUALITY_HIGH = 1
    }
}





