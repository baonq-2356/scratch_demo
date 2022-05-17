package com.example.myapplication

import android.animation.Animator
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout

/**
 * @{SwipeView}
 * Created by nguyen.van.bac on 19,April,2021
 */
abstract class SwipeView : FrameLayout, View.OnTouchListener {

    var listener: SwipeListener? = null

//    private val preBitmap = BitmapFactory.decodeResource(resources, R.drawable.coin_swipe)
//    private var bitmap: Bitmap = Bitmap.createScaledBitmap(preBitmap,
//        (preBitmap.width * 1.5).toInt(), (preBitmap.height * 1.5).toInt(), true)

    private val paint = Paint()
    protected var xR = -1f
    protected var yR = -1f
    protected var disableScratch = false
    private val screenH by lazy {
        context?.resources?.displayMetrics?.heightPixels?.toFloat() ?: 0f
    }
    private var isAbleToReveal = false

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes)

    fun runAnimation(result: String) {
//        getCardSwipe().playApng(result, isLoop = true, isFullScreen = true)
//        visibility = View.VISIBLE
//        val jumpY = -20f
//        doAnimation(
//            longArrayOf(500, 50, 50),
//            arrayOf(
//                Path().apply {
//                    moveTo(0f, -screenH)
//                    lineTo(0f, 0f)
//                },
//                Path().apply {
//                    moveTo(0f, 0f)
//                    lineTo(0f, jumpY)
//                },
//                Path().apply {
//                    moveTo(0f, jumpY)
//                    lineTo(0f, 0f)
//                }
//            ),
//            0
//        ) {
        isAbleToReveal = true
//        }
    }

    private fun doAnimation(durations: LongArray, paths: Array<Path>, index: Int, onFinished: () -> Unit) {
        if (index >= durations.size || index >= paths.size) {
            onFinished()
            return
        }
        ObjectAnimator.ofFloat(this, View.X, View.Y, paths[index])
            .apply {
                duration = durations[index]
                start()
                addListener(object : Animator.AnimatorListener {
                    override fun onAnimationStart(animation: Animator?) {
                    }

                    override fun onAnimationEnd(animation: Animator?) {
                        doAnimation(durations, paths, index + 1, onFinished)
                    }

                    override fun onAnimationCancel(animation: Animator?) {
                    }

                    override fun onAnimationRepeat(animation: Animator?) {
                    }

                })
            }
    }

//    override fun dispatchDraw(canvas: Canvas?) {
//        super.dispatchDraw(canvas)
//        if (xR >= 0 && yR >= 0) {
//            canvas?.drawBitmap(bitmap, xR - bitmap.width / 2, yR - bitmap.height / 2, paint)
//        }
//    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        if (disableScratch || !isAbleToReveal) {
            removeCoin()
            return false
        }
        when(event?.action) {
            MotionEvent.ACTION_MOVE -> {
                xR = event.x
                yR = event.y
                postInvalidate()
            }
            MotionEvent.ACTION_UP -> {
                removeCoin()
            }
        }
        handleSwipe(event)
        return true
    }

    protected abstract fun handleSwipe(event: MotionEvent?)

    protected fun removeCoin() {
        xR = -1f
        yR = -1f
        postInvalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        isAbleToReveal = false
    }
}

