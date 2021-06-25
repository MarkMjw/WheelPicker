package sh.tyy.wheelpicker

import android.content.Context
import android.graphics.Camera
import android.graphics.Canvas
import android.graphics.Matrix
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class WheelPickerRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {
    private val camera: Camera = Camera()
    private val wheelMatrix: Matrix = Matrix()
    private val snapHelper = WheelSnapHelper()
    var isEnabledHapticFeedback: Boolean = true
    private var hapticFeedbackLastTriggerPosition: Int = 0
    var currentPosition: Int = NO_POSITION
        private set

    init {
        val linearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        linearLayoutManager.stackFromEnd = true
        layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        snapHelper.attachToRecyclerView(this)
        setHasFixedSize(true)
        addItemDecoration(OffsetItemDecoration())
    }

    override fun onScrolled(dx: Int, dy: Int) {
        super.onScrolled(dx, dy)

        val visibleCenterItemPosition = visibleCenterItemPosition()
        if (visibleCenterItemPosition == NO_POSITION) {
            return
        }

        if (isEnabledHapticFeedback && hapticFeedbackLastTriggerPosition != visibleCenterItemPosition) {
            hapticFeedbackLastTriggerPosition = visibleCenterItemPosition
            performHapticFeedback(
                HapticFeedbackConstants.KEYBOARD_TAP,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
        }
    }

    override fun onScrollStateChanged(state: Int) {
        super.onScrollStateChanged(state)
        if (state == SCROLL_STATE_IDLE) {
            currentPosition = visibleCenterItemPosition()
        }
    }

    private fun visibleCenterItemPosition(): Int {
        val linearLayoutManager = (layoutManager as? LinearLayoutManager) ?: return NO_POSITION

        val firstIndex = linearLayoutManager.findFirstVisibleItemPosition()
        val lastIndex = linearLayoutManager.findLastVisibleItemPosition()
        for (i in firstIndex until lastIndex) {
            val holder = findViewHolderForAdapterPosition(i) ?: continue
            val child: View = holder.itemView
            val centerY: Int = height / 2
            if (child.top <= centerY && child.bottom >= centerY) {
                return i
            }
        }
        return NO_POSITION
    }

    // reference: https://github.com/devilist/RecyclerWheelPicker/blob/master/recyclerwheelpicker/src/main/java/com/devilist/recyclerwheelpicker/widget/RecyclerWheelPicker.java
    override fun drawChild(canvas: Canvas?, child: View?, drawingTime: Long): Boolean {
        if (canvas == null || child == null) {
            return super.drawChild(canvas, child, drawingTime)
        }

        val centerY = (height - paddingBottom - paddingTop) / 2
        val childCenterY = child.top + child.height / 2F
        val factor = (centerY - childCenterY) * 1f / centerY
        val alphaFactor = 1 - 0.7f * abs(factor)
        child.alpha = alphaFactor * alphaFactor * alphaFactor
        val scaleFactor = 1 - 0.3f * abs(factor)
        child.scaleX = scaleFactor
        child.scaleY = scaleFactor

        val rotateRadius: Float = (2.0F * centerY / PI).toFloat()
        val rad = (centerY - childCenterY) * 1f / rotateRadius
        val offsetY = centerY - childCenterY - rotateRadius * sin(rad) * 1.3F
        child.translationY = offsetY

        canvas.save()
        camera.save()
        camera.translate(0F, 0F, rotateRadius * (1 - cos(rad)))
        camera.rotateX(rad * 180 / Math.PI.toFloat())
        camera.getMatrix(wheelMatrix)
        camera.restore()
        wheelMatrix.preTranslate(-child.width / 2F, -childCenterY)
        wheelMatrix.postTranslate(child.width / 2F, childCenterY)
        canvas.concat(wheelMatrix)
        val result = super.drawChild(canvas, child, drawingTime)
        canvas.restore()
        return result
    }
}