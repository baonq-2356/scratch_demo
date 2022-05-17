package com.example.myapplication

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.os.AsyncTask
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import java.nio.ByteBuffer
import kotlin.math.abs

class ScratchView: AppCompatImageView {

    interface IRevealListener {
        fun onRevealed(scratchView: ScratchView?)
        fun onRevealPercentChangedListener(scratchView: ScratchView?, percent: Float)
    }

    val STROKE_WIDTH = 12f
    private var SCREEN_HEIGHT = 0f

    private var mX = 0f
    private  var mY= 0f
    private val TOUCH_TOLERANCE = 4f

    private var isValidCoordinate = false

    /**
     * Bitmap holding the scratch region.
     */
    private var mScratchBitmap: Bitmap? = null

    /**
     * Drawable canvas area through which the scratchable area is drawn.
     */
    private var mCanvas: Canvas? = null

    /**
     * Path holding the erasing path done by the user.
     */
    private var mErasePath: Path = Path()

    /**
     * Path to indicate where the user have touched.
     */
    private var mTouchPath: Path = Path()

    /**
     * Paint properties for drawing the scratch area.
     */
    private var mBitmapPaint: Paint = Paint()

    /**
     * Paint properties for erasing the scratch region.
     */
    private var mErasePaint: Paint = Paint()

    /**
     * Gradient paint properties that lies as a background for scratch region.
     */
    private var mGradientBgPaint: Paint = Paint()
    private var alphaPaint = true

    /**
     * Sample Drawable bitmap having the scratch pattern.
     */
    private var mDrawable: BitmapDrawable? = null


    /**
     * Listener object callback reference to send back the callback when the text has been revealed.
     */
    private var mRevealListener: IRevealListener? = null

    /**
     * Reveal percent value.
     */
    private var mRevealPercent = 0f

    /**
     * Thread Count
     */
    private var mThreadCount = 0

    lateinit var scratchBitmap: Bitmap

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        readAttrs(attrs)
    }
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        readAttrs(attrs)
    }


    /**
     * Initialises the paint drawing elements.
     */
    private fun readAttrs(attrs: AttributeSet?) {
        mErasePaint.isAntiAlias = true
        mErasePaint.isDither = true
        mErasePaint.color = Color.BLUE
        mErasePaint.style = Paint.Style.STROKE
        mErasePaint.strokeJoin = Paint.Join.BEVEL
        mErasePaint.strokeCap = Paint.Cap.ROUND
        mErasePaint.xfermode = PorterDuffXfermode(
//            PorterDuff.Mode.CLEAR
            PorterDuff.Mode.SRC_IN
        )
        mGradientBgPaint = Paint()
        mErasePath = Path()
        mBitmapPaint = Paint(Paint.DITHER_FLAG)
    }

    /**
     * Set the strokes width based on the parameter multiplier.
     *
     * @param multiplier can be 1,2,3 and so on to set the stroke width of the paint.
     */
//    private fun setStrokeWidth(multiplier: Int) {
//        mErasePaint.strokeWidth = multiplier * STROKE_WIDTH
//    }
    private fun setStrokeWidth(width: Float) {
        mErasePaint.strokeWidth = width
    }


    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        setStrokeWidth(height.toFloat())
//        scratchBitmap = BitmapFactory.decodeResource(resources, R.drawable.card_swipe)
//        scratchBitmap = Bitmap.createScaledBitmap(
//            scratchBitmap,
//            width, height, false
//        )
        scratchBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        mDrawable = BitmapDrawable(resources, scratchBitmap).apply {
            setTileModeXY(Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }

        mScratchBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            mCanvas = Canvas(this)
        }
        val rect = Rect(0, 0, width, height)
        mDrawable?.bounds = rect
        val startGradientColor: Int = ContextCompat.getColor(context, R.color.white)
        val endGradientColor: Int = ContextCompat.getColor(context, R.color.white)
        mGradientBgPaint.shader = LinearGradient(
            0f, 0f, 0f,
            height.toFloat(), startGradientColor, endGradientColor, Shader.TileMode.MIRROR
        )
        mCanvas?.apply {
            this.drawRect(rect, mGradientBgPaint)
            mDrawable?.draw(this)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        mScratchBitmap?.apply {
            canvas.drawBitmap(this, 0f, 0f, mBitmapPaint)
        }
        canvas.drawPath(mErasePath, mErasePaint)
    }

    private fun touchStart(x: Float, y: Float) {
        mErasePath.reset()
        mErasePath.moveTo(x, y)
        mX = x
        mY = y
    }

    /**
     * clears the scratch area to reveal the hidden image.
     */
    fun clear() {
        val bounds = getViewBounds()
        var left = bounds[0]
        var top = bounds[1]
        var right = bounds[2]
        var bottom = bounds[3]
        val width = right - left
        val height = bottom - top
        val centerX = left + width / 2
        val centerY = top + height / 2
        left = centerX - width / 2
        top = centerY - height / 2
        right = left + width
        bottom = top + height
        val paint = Paint()
        paint.xfermode = PorterDuffXfermode(
            PorterDuff.Mode.CLEAR
        )
        mCanvas?.drawRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), paint)
        checkRevealed()
        invalidate()
    }

    fun mask() {
        clear()
        mRevealPercent = 0f
        mCanvas?.drawBitmap(scratchBitmap, 0f, 0f, mBitmapPaint)
        invalidate()
    }

    private fun touchMove(x: Float, y: Float) {
        val dx = abs(x - mX)
        val dy: Float = abs(y - mY)
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mErasePath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2)
            mX = x
            mY = y
            drawPath()
        }
        mTouchPath.reset()
        mTouchPath.addCircle(mX, mY, 30f, Path.Direction.CW)
    }

    private fun drawPath() {
        mErasePath.lineTo(mX, mY)
        // commit the path to our offscreen
        mCanvas?.drawPath(mErasePath, mErasePaint)
        // kill this so we don't double draw
        mTouchPath.reset()
        mErasePath.reset()
        mErasePath.moveTo(mX, mY)
        checkRevealed()
    }

    private fun touchUp() {
        drawPath()
    }

    private fun checkCoordinatesReveal(y: Float): Boolean {
        return false
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        alphaPaint = false
        val x = event.x
        val y = event.y
        Log.d("NQB", "onTouchEvent: $x - $y")
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
//                Log.d("NQB", "ACTION_DOWN")
//                if (checkCoordinatesReveal(y)) {
                touchStart(x, y)
                invalidate()
//                }
            }
            MotionEvent.ACTION_MOVE -> {
//                Log.d("NQB", "ACTION_MOVE")
//                if (checkCoordinatesReveal(y)) {
                touchMove(x, y)
                invalidate()
//                }
            }
            MotionEvent.ACTION_UP -> {
//                Log.d("NQB", "ACTION_UP")
                touchUp()
                invalidate()
            }
            else -> {
            }
        }
        return true
    }

    fun setRevealListener(listener: IRevealListener?) {
        mRevealListener = listener
    }

    fun isRevealed(): Boolean {
        return mRevealPercent >= 1.0
    }

    private fun checkRevealed() {
        if (!isRevealed() && mRevealListener != null) {
            val bounds = getViewBounds()
            val left = bounds[0]
            val top = bounds[1]
            val width = bounds[2] - left
            val height = bounds[3] - top
            if (mThreadCount > 1) {
                return
            }
            mThreadCount++
            object : AsyncTask<Int?, Void?, Float>() {

                override fun doInBackground(vararg params: Int?): Float {
                    return try {
                        val left = params[0] ?: 0
                        val top = params[1] ?: 0
                        val width = params[2] ?: 0
                        val height = params[3] ?: 0
                        val croppedBitmap =
                            Bitmap.createBitmap(mScratchBitmap!!, left, top, width, height)
                        getTransparentPixelPercent(croppedBitmap)
                    } finally {
                        mThreadCount--
                    }
                }

                /**
                 * Finds the percentage of pixels that do are empty.
                 *
                 * @param bitmap input bitmap
                 * @return a value between 0.0 to 1.0 . Note the method will return 0.0 if either of bitmaps are null nor of same size.
                 */
                fun getTransparentPixelPercent(bitmap: Bitmap?): Float {
                    if (bitmap == null) {
                        return 0f
                    }
                    val buffer = ByteBuffer.allocate(bitmap.height * bitmap.rowBytes)
                    bitmap.copyPixelsToBuffer(buffer)
                    val array = buffer.array()
                    val len = array.size
                    var count = 0
                    for (i in 0 until len) {
                        if (array[i].toInt() == 0) {
                            count++
                        }
                    }
                    return count.toFloat() / len
                }

                override fun onPostExecute(percentRevealed: Float) {

                    // check if not revealed before.
                    if (!isRevealed()) {
                        val oldValue = mRevealPercent
                        mRevealPercent = percentRevealed
                        if (oldValue != percentRevealed) {
                            mRevealListener?.onRevealPercentChangedListener(
                                this@ScratchView,
                                percentRevealed
                            )
                        }

                        // if now revealed.
                        if (isRevealed()) {
                            mRevealListener?.onRevealed(this@ScratchView)
                        }
                    }
                }
            }.execute(left, top, width, height)
        }
    }

    private fun getViewBounds(): IntArray {
        val left = 0
        val top = 0
        val width = width
        val height = height
        return intArrayOf(left, top, left + width, top + height)
    }
}

