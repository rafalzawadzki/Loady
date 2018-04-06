package com.rafalzawadzki.loady.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RSRuntimeException;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.Log;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

public class BlurEngine {

    private Context ctx;
    private BlurAsyncTask bluringTask;
    private BlurListener blurListener;
    private ImageView blurredBackgroundView;

    static final float DEFAULT_BLUR_DOWN_SCALE_FACTOR = 4.0f;
    static final int DEFAULT_BLUR_RADIUS = 5;
    static final boolean DEFAULT_DIMMING_POLICY = false;
    static final boolean DEFAULT_DEBUG_POLICY = false;
    static final boolean DEFAULT_ACTION_BAR_BLUR = false;
    static final boolean DEFAULT_USE_RENDERSCRIPT = false;
    private static final String TAG = BlurEngine.class.getSimpleName();

    private boolean mDebugEnable = false;
    private float mDownScaleFactor = DEFAULT_BLUR_DOWN_SCALE_FACTOR;
    private int mBlurRadius = DEFAULT_BLUR_RADIUS;
    private int mBlurOverlayColor = com.rafalzawadzki.library.R.color.loady_white_light;
    private int mAnimationDuration = 300;
    private boolean mUseRenderScript;



    public interface BlurListener {
        void onBlurred(Bitmap bmp);
    }


    /**
     * Constructor.
     *
     * @param ctx Context
     */
    public BlurEngine(Context ctx) {
        this.ctx = ctx.getApplicationContext();
    }

    /**
     * Must be linked to the original lifecycle.
     */
    public void destroy() {
        if (bluringTask != null) {
            bluringTask.cancel(true);
        }
        bluringTask = null;
    }

    /**
     * Enable / disable debug mode.
     * <p/>
     * LogCat and graphical information directly on blurred screen.
     *
     * @param enable true to display log of LogCat.
     */
    public void debug(boolean enable) {
        mDebugEnable = enable;
    }

    /**
     * Apply custom down scale factor.
     * <p/>
     * By default down scale factor is set to
     * {@link BlurEngine#DEFAULT_BLUR_DOWN_SCALE_FACTOR}
     * <p/>
     * Higher down scale factor will increase blurring speed but reduce final rendering quality.
     *
     * @param factor customized down scale factor, must be at least 1.0 ( no down scale applied )
     */
    public void setDownScaleFactor(float factor) {
        if (factor >= 1.0f) {
            mDownScaleFactor = factor;
        } else {
            mDownScaleFactor = 1.0f;
        }
    }

    /**
     * Apply custom blur radius.
     * <p/>
     * By default blur radius is set to
     * {@link BlurEngine#DEFAULT_BLUR_RADIUS}
     *
     * @param radius custom radius used to blur.
     */
    public void setBlurRadius(int radius) {
        if (radius >= 0) {
            mBlurRadius = radius;
        } else {
            mBlurRadius = 0;
        }
    }

    /**
     * Set use of RenderScript
     * <p/>
     * By default RenderScript is set to
     * {@link BlurEngine#DEFAULT_USE_RENDERSCRIPT}
     * <p/>
     * Don't forget to add those lines to your build.gradle
     * <pre>
     *  defaultConfig {
     *  ...
     *  renderscriptTargetApi 22
     *  renderscriptSupportModeEnabled true
     *  ...
     *  }
     * </pre>
     *
     * @param useRenderScript use of RenderScript
     */

    public void setUseRenderScript(boolean useRenderScript) {
        mUseRenderScript = useRenderScript;
    }

    /**
     * Blur the given bitmap and add it to the activity.
     *
     * @param bkg  should be a bitmap of the background.
     */
    private Bitmap renderBlur(Bitmap bkg) {
        long startMs = System.currentTimeMillis();

        Bitmap overlay = null;

        int width = (int)(bkg.getWidth() / mDownScaleFactor);
        int height = (int)(bkg.getHeight() / mDownScaleFactor);

        // Render script doesn't work with RGB_565
        /*if (mUseRenderScript) {
            overlay = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        } else {
            overlay = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        }*/

        overlay = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        //scale and draw background view on the canvas overlay
        Canvas canvas = new Canvas(overlay);
        canvas.drawColor(Color.WHITE);
        canvas.scale(1 / mDownScaleFactor, 1 / mDownScaleFactor);
        Paint paint = new Paint();
        paint.setFlags(Paint.FILTER_BITMAP_FLAG);
        canvas.drawBitmap(bkg, 0, 0, paint);
        canvas.drawColor(ctx.getResources().getColor(mBlurOverlayColor));

        //apply fast blur on overlay
        if (mUseRenderScript) {
            overlay = renderscriptBlur(overlay, mBlurRadius, true, ctx);
        } else {
            overlay = fastBlur(overlay, mBlurRadius, true);
        }

        if (mDebugEnable) {
            String blurTime = (System.currentTimeMillis() - startMs) + " ms";
            Log.d(TAG, "Blur method : " + (mUseRenderScript ? "RenderScript" : "FastBlur"));
            Log.d(TAG, "Radius : " + mBlurRadius);
            Log.d(TAG, "Down Scale Factor : " + mDownScaleFactor);
            Log.d(TAG, "Blurred achieved of : " + blurTime);
            Log.d(TAG, "Allocation : " + bkg.getRowBytes() + "ko (screen capture) + "
                    + overlay.getRowBytes() + "ko (blurred bitmap)"
                    + (!mUseRenderScript ? " + temp buff " + overlay.getRowBytes() + "ko." : ""));
        }

        bkg.recycle();
        bkg = null;

        return overlay;
    }

    /**
     * Removed the blurred view from the view hierarchy.
     */
    private void removeBlurredView() {
        if (blurredBackgroundView != null) {
            ViewGroup parent = (ViewGroup) blurredBackgroundView.getParent();
            if (parent != null) {
                parent.removeView(blurredBackgroundView);
            }
            blurredBackgroundView = null;
        }
    }

    /**
     * Async task used to process blur out of ui thread
     */
    private class BlurAsyncTask extends AsyncTask<Void, Void, Bitmap> {

        private Bitmap blurredBmp;

        public BlurAsyncTask(Bitmap blurredBmp){
            this.blurredBmp = blurredBmp;
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            if (!isCancelled() && !blurredBmp.isRecycled()) {
                return renderBlur(blurredBmp);
            } else {
                return null;
            }
        }

        @Override
        @SuppressLint("NewApi")
        protected void onPostExecute(Bitmap result) {
            super.onPostExecute(result);

            if (blurListener != null) {
                blurListener.onBlurred(result);
                blurListener = null;
            }
            blurredBmp.recycle();
            blurredBmp = null;
        }
    }

    public BlurEngine blur(final ViewGroup blurredView, final BlurListener callback){
        blurListener = callback;

        blurredView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                blurredView.getViewTreeObserver().removeOnPreDrawListener(this);

                Bitmap bmp;// = Bitmap.createBitmap(blurredView.getWidth(), blurredView.getHeight(), );

                try {
                    blurredView.setDrawingCacheEnabled(true);
                    blurredView.buildDrawingCache(true);



                    bmp = blurredView.getDrawingCache(true).copy(blurredView.getDrawingCache(true).getConfig(), true);

                    blurredView.setDrawingCacheEnabled(false);

                    bluringTask = new BlurAsyncTask(bmp);
                    bluringTask.execute();

                } catch (Exception e){
                    callback.onBlurred(null);
                } catch (OutOfMemoryError e){
                    callback.onBlurred(null);
                }

                /*blurredView.destroyDrawingCache();
                blurredView.setDrawingCacheEnabled(false);*/

                return true;
            }
        });

        return this;
    }


    // ----------------------------------------------------------------------------- Actual blurring

    @SuppressLint("NewApi")
    public static Bitmap fastBlur(Bitmap sentBitmap, int radius, boolean canReuseInBitmap) {

        if (radius < 1) {
            return (null);
        }

        Bitmap bitmap;
        if (canReuseInBitmap || (sentBitmap.getConfig() == Bitmap.Config.RGB_565)) {
            // if RenderScript is used and bitmap is of RGB_565, it will
            // necessarily be copied when converting to ARGB_8888
            bitmap = sentBitmap;
        } else {
            bitmap = sentBitmap.copy(sentBitmap.getConfig(), true);
        }


        // Stack Blur v1.0 from
        // http://www.quasimondo.com/StackBlurForCanvas/StackBlurDemo.html
        //
        // Java Author: Mario Klingemann <mario at quasimondo.com>
        // http://incubator.quasimondo.com
        // created Feburary 29, 2004
        // Android port : Yahel Bouaziz <yahel at kayenko.com>
        // http://www.kayenko.com
        // ported april 5th, 2012

        // This is a compromise between Gaussian Blur and Box blur
        // It creates much better looking blurs than Box Blur, but is
        // 7x faster than my Gaussian Blur implementation.
        //
        // I called it Stack Blur because this describes best how this
        // filter works internally: it creates a kind of moving stack
        // of colors whilst scanning through the image. Thereby it
        // just has to add one new block of color to the right side
        // of the stack and remove the leftmost color. The remaining
        // colors on the topmost layer of the stack are either added on
        // or reduced by one, depending on if they are on the right or
        // on the left side of the stack.
        //
        // If you are using this algorithm of your code please add
        // the following line:
        //
        // Stack Blur Algorithm by Mario Klingemann <mario@quasimondo.com>

        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        int[] pix = new int[w * h];
        bitmap.getPixels(pix, 0, w, 0, 0, w, h);

        int wm = w - 1;
        int hm = h - 1;
        int wh = w * h;
        int div = radius + radius + 1;

        int r[] = new int[wh];
        int g[] = new int[wh];
        int b[] = new int[wh];
        int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
        int vmin[] = new int[Math.max(w, h)];

        int divsum = (div + 1) >> 1;
        divsum *= divsum;
        int dv[] = new int[256 * divsum];
        for (i = 0; i < 256 * divsum; i++) {
            dv[i] = (i / divsum);
        }

        yw = yi = 0;

        int[][] stack = new int[div][3];
        int stackpointer;
        int stackstart;
        int[] sir;
        int rbs;
        int r1 = radius + 1;
        int routsum, goutsum, boutsum;
        int rinsum, ginsum, binsum;

        for (y = 0; y < h; y++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            for (i = -radius; i <= radius; i++) {
                p = pix[yi + Math.min(wm, Math.max(i, 0))];
                sir = stack[i + radius];
                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);
                rbs = r1 - Math.abs(i);
                rsum += sir[0] * rbs;
                gsum += sir[1] * rbs;
                bsum += sir[2] * rbs;
                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }
            }
            stackpointer = radius;

            for (x = 0; x < w; x++) {

                r[yi] = dv[rsum];
                g[yi] = dv[gsum];
                b[yi] = dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (y == 0) {
                    vmin[x] = Math.min(x + radius + 1, wm);
                }
                p = pix[yw + vmin[x]];

                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[(stackpointer) % div];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi++;
            }
            yw += w;
        }
        for (x = 0; x < w; x++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            yp = -radius * w;
            for (i = -radius; i <= radius; i++) {
                yi = Math.max(0, yp) + x;

                sir = stack[i + radius];

                sir[0] = r[yi];
                sir[1] = g[yi];
                sir[2] = b[yi];

                rbs = r1 - Math.abs(i);

                rsum += r[yi] * rbs;
                gsum += g[yi] * rbs;
                bsum += b[yi] * rbs;

                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }

                if (i < hm) {
                    yp += w;
                }
            }
            yi = x;
            stackpointer = radius;
            for (y = 0; y < h; y++) {
                // Preserve alpha channel: ( 0xff000000 & pix[yi] )
                pix[yi] = (0xff000000 & pix[yi]) | (dv[rsum] << 16) | (dv[gsum] << 8) | dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (x == 0) {
                    vmin[y] = Math.min(y + r1, hm) * w;
                }
                p = x + vmin[y];

                sir[0] = r[p];
                sir[1] = g[p];
                sir[2] = b[p];

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[stackpointer];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi += w;
            }
        }

        bitmap.setPixels(pix, 0, w, 0, 0, w, h);

        return (bitmap);
    }

    public static Bitmap renderscriptBlur(Bitmap sentBitmap, int radius, boolean canReuseInBitmap, Context context) {
        Bitmap bitmap;

        if (canReuseInBitmap) {
            bitmap = sentBitmap;
        } else {
            bitmap = sentBitmap.copy(sentBitmap.getConfig(), true);
        }

        if (bitmap.getConfig() == Bitmap.Config.RGB_565) {
            // RenderScript hates RGB_565 so we convert it to ARGB_8888
            // (see http://stackoverflow.com/questions/21563299/
            // defect-of-image-with-scriptintrinsicblur-from-support-library)
            bitmap = convertRGB565toARGB888(bitmap);
        }

        try {
            final RenderScript rs = RenderScript.create(context);
            final Allocation input = Allocation.createFromBitmap(rs, bitmap, Allocation.MipmapControl.MIPMAP_NONE,
                    Allocation.USAGE_SCRIPT);
            final Allocation output = Allocation.createTyped(rs, input.getType());
            final ScriptIntrinsicBlur script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
            script.setRadius(radius);
            script.setInput(input);
            script.forEach(output);
            output.copyTo(bitmap);
            return bitmap;
        } catch (RSRuntimeException e) {
            Log.e(TAG, "RenderScript known error : https://code.google.com/p/android/issues/detail?id=71347 "
                    + "continue with the FastBlur approach.");
        }

        return null;
    }

    private static Bitmap convertRGB565toARGB888(Bitmap bitmap) {
        return bitmap.copy(Bitmap.Config.ARGB_8888, true);
    }
}
