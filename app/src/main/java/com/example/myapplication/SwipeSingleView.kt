package com.example.myapplication

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import com.example.myapplication.databinding.SwipeViewBinding

class SwipeSingleView : SwipeView, ScratchView.IRevealListener {

    private val binding = SwipeViewBinding.inflate(LayoutInflater.from(context), this, true)

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

    init {
        setWillNotDraw(false)
        binding.scratchView.setRevealListener(this)
    }

    override fun onRevealed(scratchView: ScratchView?) {

    }

    override fun onRevealPercentChangedListener(scratchView: ScratchView?, percent: Float) {
        if (percent >= 1.0f && !disableScratch) {
            disableScratch = true
            listener?.onSwipeFinished()
        }
    }

    override fun handleSwipe(event: MotionEvent?) {
        event?.apply {
            val newX = x - binding.scratchView.x
            val newY = y - binding.scratchView.y
            setLocation(newX, newY)
            binding.scratchView.onTouchEvent(this)
        }
    }
}
