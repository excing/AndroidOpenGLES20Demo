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
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.sqrt


/**
 * 拷贝自 MyRenderer06
 *
 * 使用贝塞尔曲线插值方式的方式实现画板铅笔画线功能，
 *
 * 使用一种近似求插值点数量的方式：
 * 计算两端到控制点的距离，通过该距离计算我们需要的点。
 *
 * 这种计算方式，依然存在固定插值的问题，不过会弱化一些。
 * 我们通过一些手段可以近一步减少这种插值不均匀的现象。
 *
 * 我们发现，二阶贝塞尔（即我们插值使用的）的点（处 t 时间点的点）的距离，
 * 是遵循正态分布的表现形式。
 * 所以我们在计算 t 值时，可以采用加上正态分布因子，
 * 这样就可以进一步减少插值不均匀的情况了。
 *
 * 不过用处不大，建议采用 MyRenderer08 的实现方式。
 */
class MyRenderer07(
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
    private val div = 3.0f
    private val latestPoint = PointF()
    private val oldPoint = PointF(Float.MIN_VALUE, Float.MIN_VALUE)
    private val fromPoint = PointF()
    private val toPoint = PointF()

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

        if (null == vboIDs) return

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glClearColor(r, g, b, a)

        if (0 == mPointerCount) return

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
        val len1X = abs(fromX - controlX)
        val len1Y = abs(fromY - controlY)

        val len2X = abs(toX - controlX)
        val len2Y = abs(toY - controlY)

        val len1 = sqrt(len1X * len1X + len1Y * len1Y)
        val len2 = sqrt(len2X * len2X + len2Y * len2Y)

        val count = ceil((len1 + len2) / pointSize)

        println("pointsWithFrom ---------------------$count")

        for (i in 0..count.toInt()) {
            val t = normalDistribution(i, count, 0.4f)
            println("pointsWithFrom $i -> $t")

            val x = (1 - t) * (1 - t) * fromX + 2 * (1 - t) * t * controlX + t * t * toX
            val y = (1 - t) * (1 - t) * fromY + 2 * (1 - t) * t * controlY + t * t * toY

            addPointerToBuffer(x, y)
        }
    }

    /**
     * 该方法返回一组数据，该数据表现两端密，中间疏的特性。
     *
     * 返回处于 index 位置的正态分布数值，
     * count 是该数据集的长度，
     * f 是分布曲率，f 值越大，则标准差越大。
     */
    private fun normalDistribution(index: Int, count: Float, f: Float): Float {
        val half = count / 2
        val d = 1.0f / count / 2
        val n = index / count

        return if (half <= index) (n - d * f * (count - index)) else (n - d * f * index)
    }

    private fun addPointerToBuffer(x: Float, y: Float) {
        val ndcX = (x - mSurfaceSize.x) / mSurfaceSize.x
        val ndcY = (mSurfaceSize.y - y) / mSurfaceSize.y

        mTouchPointerBuffer.put(ndcX)
        mTouchPointerBuffer.put(ndcY)
        mPointerCount++

        capacityExpansionIfNeeded()
    }

    private fun touchDown(x: Float, y: Float) {
        latestPoint.x = x
        latestPoint.y = y

        oldPoint.x = x
        oldPoint.y = y

        fromPoint.x = x
        fromPoint.y = y
    }

    private fun touchUp() {
        latestPoint.x = Float.MIN_VALUE
        latestPoint.y = Float.MIN_VALUE

        oldPoint.x = Float.MIN_VALUE
        oldPoint.y = Float.MIN_VALUE

        fromPoint.x = Float.MIN_VALUE
        fromPoint.y = Float.MIN_VALUE

        toPoint.x = Float.MIN_VALUE
        toPoint.y = Float.MIN_VALUE
    }

    override fun onTouch(
        event: MotionEvent,
        action: Int,
        x: Float,
        y: Float
    ) {
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                touchDown(x, y)
            }
            MotionEvent.ACTION_MOVE -> {
                latestPoint.x = x
                latestPoint.y = y
            }
            MotionEvent.ACTION_UP -> {
                touchUp()
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
