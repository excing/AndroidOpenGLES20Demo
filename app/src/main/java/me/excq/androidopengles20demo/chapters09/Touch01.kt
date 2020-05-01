package me.excq.androidopengles20demo.chapters09

import android.graphics.PointF
import android.view.MotionEvent

/**
 * 在 MyRenderer08 中使用，
 * 该类计算方式为：不停的刷新上一指针位置，用于与当前指针进行计算。
 *
 * Touch02 的实现方式则是只记录第一指针的位置，具体请看 Touch02 源码。
 */
class Touch01(var pointerTouchCallback: PointerTouchCallback): MainActivity.ITouch {
    private var pointer = mutableMapOf<Int, PointF>()

    override fun onTouch(
        event: MotionEvent,
        action: Int,
        x: Float,
        y: Float
    ) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val index = event.actionIndex
                pointer[event.getPointerId(index)] = PointF(event.getX(index), event.getY(index))
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                pointer.remove(event.getPointerId(event.actionIndex))
            }
            MotionEvent.ACTION_MOVE -> {
                for ((k, v) in pointer) {
                    val index = event.findPointerIndex(k)
                    val pointerX = event.getX(index)
                    val pointerY = event.getY(index)

                    val xoffset = v.x - pointerX
                    val yoffset = pointerY - v.y

                    v.x = pointerX
                    v.y = pointerY

                    pointerTouchCallback.onTouch(pointerX, pointerY, xoffset, yoffset)
                }
            }
        }
    }
}