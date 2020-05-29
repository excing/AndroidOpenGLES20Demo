package me.excq.androidopengles20demo.toy02

import android.opengl.GLES20
import java.nio.ByteBuffer

class Fill02 : MainActivity.IFill {
    private val pixel = ByteBuffer.allocate(3)
    // Color to be filled
    private val color = ByteArray(3)
    private var xScanLeft = 0
    private var xScanRight = 0

    override fun fill(x: Int, y: Int, minX: Int, maxX: Int, minY: Int, maxY: Int, callback: MainActivity.IFillCallback) {
        val surfaceWidth = maxX - minX
        val surfaceHeight = maxY - minY

        GLES20.glReadPixels(x, y, 1, 1, GLES20.GL_RGB, GLES20.GL_UNSIGNED_BYTE, pixel)
        pixel.position(0)

        color[0] = pixel.get(0)
        color[1] = pixel.get(1)
        color[2] = pixel.get(2)

        var xLeft = x
        var xRight = x

        while (isSameColor(xLeft - 1, y) && 0 < xLeft) {
            xLeft--
        }

        while (isSameColor(xRight + 1, y) && xRight < surfaceWidth) {
            xRight++
        }

        callback.fullCallback(xLeft.toFloat(), surfaceHeight - y.toFloat())
        callback.fullCallback(xRight.toFloat(), surfaceHeight - y.toFloat())

        var yUp = y
        xScanLeft = xLeft
        xScanRight = xRight

        while (xScanLeft < xScanRight && yUp < maxY) {
            scanLine(++yUp, minX, maxX, surfaceHeight, callback)
        }

        var yDown = y
        xScanLeft = xLeft
        xScanRight = xRight

        while (xScanLeft < xScanRight && minY < yDown) {
            scanLine(--yDown, minX, maxX, surfaceHeight, callback)
        }
    }

    private fun scanLine(y: Int, minX: Int, maxX: Int, surfaceHeight: Int, callback: MainActivity.IFillCallback) {
        if (isSameColor(xScanLeft, y)) {
            while (minX < xScanLeft && isSameColor(xScanLeft - 1, y)) {
                xScanLeft--
            }
        } else {
            while (xScanLeft < xScanRight && !isSameColor(++xScanLeft, y)) {
            }
        }

        if (isSameColor(xScanRight, y)) {
            while (xScanRight < maxX && isSameColor(xScanRight + 1, y)) {
                xScanRight++
            }
        } else {
            while (xScanLeft < xScanRight && !isSameColor(--xScanRight, y)) {
            }
        }

        callback.fullCallback((xScanLeft).toFloat(), (surfaceHeight - y).toFloat())
        callback.fullCallback((xScanRight).toFloat(), (surfaceHeight - y).toFloat())
    }

    private fun isSameColor(x: Int, y: Int): Boolean {
        GLES20.glReadPixels(x, y, 1, 1, GLES20.GL_RGB, GLES20.GL_UNSIGNED_BYTE, pixel)
        pixel.position(0)
        return (color[0] == pixel.get(0) &&
                color[1] == pixel.get(1) &&
                color[2] == pixel.get(2))
    }
}