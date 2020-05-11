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
 * 拷贝自 MyRenderer04
 *
 * 使用直接插值的方式实现画板铅笔画线功能，
 */
class MyRenderer05(
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
    private val div = 4
    private val latestPoint = PointF()
    private val oldPoint = PointF(Float.MIN_VALUE, Float.MIN_VALUE)

    private var on = false
    private var frameTime = 0L
    private var latestFrameCount = 0L
    private var lastFrameCount = 0L

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
        updatePointerBuffer()

        if (0 == mPointerCount) return
        if (null == vboIDs) return

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glClearColor(r, g, b, a)

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
        val ax = latestPoint.x
        val ay = latestPoint.y

        val bx = oldPoint.x
        val by = oldPoint.y

        oldPoint.x = ax
        oldPoint.y = ay

        if (Float.MIN_VALUE == ax && Float.MIN_VALUE == ay) {
            return
        }

        if (ax == bx && ay == by) {
            return
        }

        mTouchPointerBuffer.position(mPointerCount * vertexCountPerPointer)
        interpolationPointer(ax, ay, bx, by)
        mTouchPointerBuffer.position(0)
        println("updatePointerBuffer $mPointerCount")
    }

    private fun interpolationPointer(
        ax: Float,
        ay: Float,
        bx: Float,
        by: Float
    ) {
        if (Float.MIN_VALUE < bx && Float.MIN_VALUE < by) {
            val xLen = abs(ax - bx)
            val yLen = abs(ay - by)
            val len = sqrt(xLen * xLen + yLen * yLen)
            val radians = asin(yLen / len)

            val interpolationPointerCount = len.toInt() / div
            for (i in 1..interpolationPointerCount) {
                val tempX = cos(radians) * div * i * if (ax < bx) 1 else -1
                val tempY = sin(radians) * div * i * if (ay < by) 1 else -1
                addPointerToBuffer(ax + tempX, ay + tempY)
            }
        }

        addPointerToBuffer(ax, ay)
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
        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                latestPoint.x = x
                latestPoint.y = y
            }
            MotionEvent.ACTION_UP -> {
                latestPoint.x = Float.MIN_VALUE
                latestPoint.y = Float.MIN_VALUE
            }
        }
    }

    override fun destroy() {
        if (null != vboIDs) {
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)

            vboIDs = null
        }

        on = false
    }
}
