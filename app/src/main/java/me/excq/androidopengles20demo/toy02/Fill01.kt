package me.excq.androidopengles20demo.toy02

import android.opengl.GLES20
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.min

class Fill01(val div: Int) : MainActivity.IFill {
    private val pixel = ByteBuffer.allocate(3)
    private val color = ByteArray(3)
    private var readPixelCount = 0
    private var xScanLeft = 0
    private var xScanRight = 0

    override fun fill(
        x: Int,
        y: Int,
        minX: Int,
        maxX: Int,
        minY: Int,
        maxY: Int,
        callback: MainActivity.IFillCallback
    ) {
        val timeStart = System.currentTimeMillis()

        // ----------------------- 扫描线种子填充算法 ----------------------------
        // glReadPixels 方法里的 x, y 以左下角为原点，
        // 而 Android 坐标系以左上角为原点，
        // 所以此处需要对 y 轴进行一次反转。
        //
        // 根据用户的点击，获取原始种子和该种子的颜色，
        // 然后向两边探索边界，获取第一对种子。
        //
        // 之后这一对种子的左右两点，分别向各自的两边探索，
        // ---------------------------------------------------------------------

        val surfaceWidth = maxX - minX
        val surfaceHeight = maxY - minY

        GLES20.glReadPixels(x, y, 1, 1, GLES20.GL_RGB, GLES20.GL_UNSIGNED_BYTE, pixel)
        pixel.position(0)

        color[0] = pixel.get(0)
        color[1] = pixel.get(1)
        color[2] = pixel.get(2)

        println("updateFillArea00 ${pixel.get(0).toUByte()}, ${pixel.get(1).toUByte()}, ${pixel.get(2).toUByte()}")

        var xLeft = x
        var xRight = x

        readPixelCount = 0

        // 向左扫描的第一个种子
        while (isSameColor(xLeft - 1, y) && 0 < xLeft) {
            xLeft--
        }

        callback.fullCallback(xLeft.toFloat(), surfaceHeight - y.toFloat())

        // 向右扫描的第一个种子
        while (isSameColor(xRight + 1, y) && xRight < surfaceWidth) {
            xRight++
        }

        callback.fullCallback(xRight.toFloat(), surfaceHeight - y.toFloat())

        println("updateFillArea01 $readPixelCount, $xLeft, $xRight, $y")
        readPixelCount = 0

        // 两个种子分别向上和向下扫描

        var yUp = y
        xScanLeft = xLeft
        xScanRight = xRight

        while (xScanLeft < xScanRight && yUp < surfaceHeight) {
            scanLine(yUp + 1, yUp + div, 0, surfaceWidth, surfaceHeight, callback)
            yUp += div
        }

        println("updateFillArea02 $readPixelCount, $xScanLeft, $xScanRight, $yUp")
        readPixelCount = 0

        var yDown = y
        xScanLeft = xLeft
        xScanRight = xRight

        while (xScanLeft < xScanRight && 0 < yDown) {
            scanLine(yDown - 1, yDown - div, 0, surfaceWidth, surfaceHeight, callback)
            yDown -= div
        }

        println("updateFillArea03 $readPixelCount, $xScanLeft, $xScanRight, $yDown")
        println("updateFillArea04 run time is ${System.currentTimeMillis() - timeStart}")
    }

    private fun scanLine(
        yStart: Int,
        y: Int,
        minX: Int,
        maxX: Int,
        surfaceHeight: Int,
        callback: MainActivity.IFillCallback
    ) {
        if (isSameColor(xScanLeft, y)) {
            while (minX < xScanLeft && isSameColor(xScanLeft - 1, y)) {
                xScanLeft--
            }
        } else { // 表示种子位于区域边缘轮廓线里，需要反方向扫描
            while (xScanLeft < xScanRight && !isSameColor(++xScanLeft, y)) {
            }
        }

        if (isSameColor(xScanRight, y)) {
            while (xScanRight < maxX && isSameColor(xScanRight + 1, y)) {
                xScanRight++
            }
        } else { // 表示种子位于区域边缘轮廓线里，需要反方向扫描
            while (xScanLeft < xScanRight && !isSameColor(--xScanRight, y)) {
            }
        }
        for (i in min(yStart, y)..max(yStart, y)) {
            callback.fullCallback(xScanLeft.toFloat(), surfaceHeight - i.toFloat())
            callback.fullCallback(xScanRight.toFloat(), surfaceHeight - i.toFloat())
        }
    }

    private fun isSameColor(x: Int, y: Int): Boolean {
        GLES20.glReadPixels(x, y, 1, 1, GLES20.GL_RGB, GLES20.GL_UNSIGNED_BYTE, pixel)
        pixel.position(0)
        readPixelCount++
        return (color[0] == pixel.get(0) &&
                color[1] == pixel.get(1) &&
                color[2] == pixel.get(2))
    }
}