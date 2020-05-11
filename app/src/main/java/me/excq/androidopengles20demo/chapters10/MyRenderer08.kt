package me.excq.androidopengles20demo.chapters10

import android.content.res.AssetManager
import android.graphics.PointF
import android.opengl.GLES20
import android.view.MotionEvent
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.IntBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.*

/**
 * 拷贝自 MyRenderer06
 *
 * 使用贝塞尔曲线插值方式的方式实现画板铅笔画线功能，
 *
 * 此方法容易出现 OOM，建议使用 JNI，在 c 层处理。
 */
class MyRenderer08(
    private var assets: AssetManager,
    var r: Float = 1f,
    var b: Float = 1f,
    var g: Float = 1f,
    var a: Float = 1f
) : MainActivity.Renderer() {
    private val floatTypeSize: Int = 4
    private val vertexCountPerPointer: Int = 2
    private val byteSizePerPointer: Int = vertexCountPerPointer * floatTypeSize
    private val defaultCountPointer: Int = 512
    private val defaultPointerBufferCapacity = defaultCountPointer * byteSizePerPointer

    private lateinit var shader: Shader

    private var aPositionHandle: Int = -1
    private var uColorHandle: Int = -1

    private var vboIDs: IntBuffer? = null
    private var mTouchPointerBuffer = ByteBuffer
        .allocateDirect(defaultPointerBufferCapacity)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
    private var mPointerCount: Int = 0
    private var mSurfaceSize = PointF()

    /**
     * 直线插值的点间距，单位：px
     */
    private val div = 8.0f
    private val latestPoint = PointF()
    private val oldPoint = PointF(Float.MIN_VALUE, Float.MIN_VALUE)
    private val fromPoint = PointF()
    private val toPoint = PointF()

    private var on = false
    private var frameTime = 0L
    private var latestFrameCount = 0L
    private var lastFrameCount = 0L
    private var touchAction = MotionEvent.ACTION_UP
    private var drawAction = touchAction

    private fun startListenFrameInfo() {
        on = true
        GlobalScope.launch {
            while (on) {
                delay(1000)
                println("frameTime: ${latestFrameCount - lastFrameCount}, $frameTime")
                lastFrameCount = latestFrameCount
            }
        }
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        mSurfaceSize.x = width / 2f
        mSurfaceSize.y = height / 2f
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        val latestTime = System.currentTimeMillis()
        latestFrameCount++

        initShader()
        initBuffer()

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glClearColor(r, g, b, a)

        updatePointerBuffer()

        if (0 == mPointerCount) return
        if (null == vboIDs) return

        shader.use()

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboIDs!![0])
        GLES20.glBufferData(
            GLES20.GL_ARRAY_BUFFER,
            mPointerCount * byteSizePerPointer,
            mTouchPointerBuffer,
            GLES20.GL_DYNAMIC_DRAW
        )

        GLES20.glEnableVertexAttribArray(aPositionHandle)
        GLES20.glVertexAttribPointer(
            aPositionHandle,
            vertexCountPerPointer,
            GLES20.GL_FLOAT,
            false,
            byteSizePerPointer,
            0
        )

        GLES20.glUniform4f(uColorHandle, 1.0f, 0.3f, 0.0f, 1.0f)

        GLES20.glDrawArrays(
            GLES20.GL_POINTS,
            0,
            mPointerCount
        )

        GLES20.glDisableVertexAttribArray(aPositionHandle)

        frameTime = System.currentTimeMillis() - latestTime
    }

    private fun initShader() {
        if (!this::shader.isInitialized) {
            shader = Shader(
                assets.open("chapters10/vertex05.glvs"),
                assets.open("chapters10/fragment05.glfs")
            )

            aPositionHandle = shader.getAttribLocation("a_Position")
            shader.printError(GLES20.glGetError())
            uColorHandle = shader.getUniformLocation("u_Color")
            shader.printError(GLES20.glGetError())
        }
    }

    private fun initBuffer() {
        if (null == vboIDs) {
            vboIDs = IntBuffer.allocate(1)
            shader.printError(GLES20.glGetError())
            GLES20.glGenBuffers(1, vboIDs)
            shader.printError(GLES20.glGetError())

            startListenFrameInfo()
        }
    }

    private fun capacityExpansionIfNeeded() {
        val limit = mTouchPointerBuffer.limit()
        val position = mTouchPointerBuffer.position()

        if (limit * 0.67f <= position) {
            val temp = ByteBuffer
                .allocateDirect(limit * floatTypeSize * 2)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
            mTouchPointerBuffer.flip()
            temp.put(mTouchPointerBuffer)
            mTouchPointerBuffer = temp
        }
    }

    private fun updatePointerBuffer() {
        val action = touchAction

        val ax = latestPoint.x
        val ay = latestPoint.y

        val bx = oldPoint.x
        val by = oldPoint.y

        if (drawAction == action && MotionEvent.ACTION_DOWN == drawAction) {
            return
        }

        oldPoint.x = ax
        oldPoint.y = ay
        drawAction = touchAction

        mTouchPointerBuffer.position(mPointerCount * vertexCountPerPointer)
        interpolationPointer(action, ax, ay, bx, by)
        mTouchPointerBuffer.position(0)
        println("updatePointerBuffer $mPointerCount")
    }

    private fun interpolationPointer(
        action: Int,
        ax: Float,
        ay: Float,
        bx: Float,
        by: Float
    ) {
        if (MotionEvent.ACTION_DOWN == action) {
            fromPoint.x = ax - 0.0001f
            fromPoint.y = ay - 0.0001f
        } else if (MotionEvent.ACTION_MOVE == action && ax != bx && ay != by) {
            toPoint.x = (ax + bx) / 2
            toPoint.y = (ay + by) / 2
            pointsWithFrom(fromPoint.x, fromPoint.y, toPoint.x, toPoint.y, bx, by, div)

            fromPoint.x = toPoint.x
            fromPoint.y = toPoint.y
        }
    }

    private fun pointsWithFrom(
        fromX: Float,
        fromY: Float,
        toX: Float,
        toY: Float,
        controlX: Float,
        controlY: Float,
        pointSize: Float
    ) {
        val p0X = fromX
        val p0Y = fromY
        val p1X = controlX
        val p1Y = controlY
        val p2X = toX
        val p2Y = toY

        val ax = p0X - 2 * p1X + p2X
        val ay = p0Y - 2 * p1Y + p2Y
        val bx = 2 * p1X - 2 * p0X
        val by = 2 * p1Y - 2 * p0Y

        val a = 4 * (ax * ax + ay * ay)
        val b = 4 * (ax * bx + ay * by)
        val c = bx * bx + by * by

        val totalLength = lengthWithT(1.0f, a, b, c)
        val pointsPerLength = 5.0f / pointSize

        val count = max(1, ceil(pointsPerLength * totalLength).toInt())

        for (i in 0..count) {
            var t = i.toFloat() / count
            val length = t * totalLength
            t = tWithT(t, length, a, b, c)

            val x = (1 - t) * (1 - t) * p0X + 2 * (1 - t) * t * p1X + t * t * p2X
            val y = (1 - t) * (1 - t) * p0Y + 2 * (1 - t) * t * p1Y + t * t * p2Y

            addPointerToBuffer(x, y)
        }
    }

    private fun tWithT(t: Float, length: Float, a: Float, b: Float, c: Float): Float {
        var t1 = t
        var t2: Float

//        println("tWithT ------------------> $t, $length, $a, $b, $c")
        while (true) {
            val speed = speedWithT(t1, a, b, c)
            if (speed < 0.0001f) {
                t2 = t1
                break
            }
//            println("tWithT $speed, $t1, $t1")
            t2 = t1 - (lengthWithT(t1, a, b, c) - length) / speed
            if (abs(t1 - t2) < 0.0001f) {
                break
            }
            t1 = t2
        }

        return t2
    }

    private fun speedWithT(t: Float, a: Float, b: Float, c: Float): Float {
        val s = a * t * t + b * t + c
        return if (s.isNaN()) 0.0f else max(s, 0.0f)
    }

    private fun lengthWithT(t: Float, a: Float, b: Float, c: Float): Float {
        if (a < 0.00001f) {
            return 0.0f
        }

        val temp1 = sqrt(c + t * (b + a * t))
        val temp2 = 2 * a * t * temp1 + b * (temp1 - sqrt(c))
        val temp3 = log((b + 2 * sqrt(a) * sqrt(c)).toDouble(), Math.E)
        val temp4 = log((b + 2 * a * t + 2 * sqrt(a) * temp1).toDouble(), Math.E)
        val temp5 = 2 * sqrt(a) * temp2
        val temp6 = (b * b - 4 * a * c) * (temp3 - temp4)

        val temp7 = (temp5 + temp6) / (8.0f * a.toDouble().pow(1.5))
        return temp7.toFloat()
    }

    private fun addPointerToBuffer(x: Float, y: Float) {
        val ndcX = (x - mSurfaceSize.x) / mSurfaceSize.x
        val ndcY = (mSurfaceSize.y - y) / mSurfaceSize.y

        mTouchPointerBuffer.put(ndcX)
        mTouchPointerBuffer.put(ndcY)
        mPointerCount++

        capacityExpansionIfNeeded()
    }

    override fun onTouch(
        event: MotionEvent,
        action: Int,
        x: Float,
        y: Float
    ) {
        touchAction = action
        latestPoint.x = x
        latestPoint.y = y
    }

    override fun destroy() {
        if (null != vboIDs) {
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)

            vboIDs = null
        }

        on = false
    }
}
