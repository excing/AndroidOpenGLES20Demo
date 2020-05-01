package me.excq.androidopengles20demo.chapters09

import android.graphics.PointF
import android.view.MotionEvent
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 在 MyRenderer08 中使用，
 * 该类计算方式为：记录第一指针的位置，用于与当前指针进行计算。
 *
 * Touch01 的实现方式则是不停的刷新上一指针位置，具体请看 Touch01 源码。
 *
 * 使用一个协程来实现这指针不动的情况下，
 */
class Touch02(
    var sensitivity: Float = 0.05f,
    private var pointerTouchCallback: PointerTouchCallback
) : MainActivity.ITouch {

    private var pointer = mutableMapOf<Int, TouchPointer>()

    /**
     * sendTouchData 协程开关
     */
    private var on = false

    override fun onTouch(
        event: MotionEvent,
        action: Int,
        x: Float,
        y: Float
    ) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                addPointer(event)
                sendTouchData()
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                addPointer(event)
            }
            MotionEvent.ACTION_POINTER_UP -> {
                removePointer(event)
            }
            MotionEvent.ACTION_UP -> {
                cancelTouchData()
                removePointer(event)
            }
            MotionEvent.ACTION_MOVE -> {
                for ((k, v) in pointer) {
                    val index = event.findPointerIndex(k)

                    v.latest.x = event.getX(index)
                    v.latest.y = event.getY(index)

                    /**
                     * 这里不需要手动调用一次回调函数了，
                     * 因为在 sendTouchData 方法里启用了一个协程，
                     * 这个协程每隔 16ms 自动调用一次回调函数，
                     * 因此在 Move 时，只需要更新指针数据即可。
                     *
                     * 这里说明一下，MotionEvent.ACTION_DOWN 事件只在第一个指针按下时触发，
                     * 而 MotionEvent.ACTION_UP 是在最后一个指针离开时触发，
                     * 因此可以根据这两个事件，对协程进行简单的管理，即启动和释放。
                     */
//                    pointerTouchCallback.onTouch(pointerX, pointerY, xOffset, yOffset)
                }
            }
        }
    }

    private fun sendTouchData() {
        on = true
        GlobalScope.launch {
            while (on) {
                delay(16L) // 16ms
                for ((_, v) in pointer) {
                    pointerTouchCallback.onTouch(
                        v.down.x,
                        v.down.y,
                        (v.latest.x - v.down.x) * sensitivity,
                        (v.latest.y - v.down.y) * sensitivity
                    )
                }
            }
        }
    }

    private fun cancelTouchData() {
        on = false
    }

    private fun addPointer(event: MotionEvent) {
        val index = event.actionIndex
        pointer[event.getPointerId(index)] =
            TouchPointer(
                PointF(event.getX(index), event.getY(index)),
                PointF(event.getX(index), event.getY(index))
            )
    }

    private fun removePointer(event: MotionEvent) {
        pointer.remove(event.getPointerId(event.actionIndex))
    }

    private data class TouchPointer(
        var down: PointF,
        var latest: PointF
    )
}