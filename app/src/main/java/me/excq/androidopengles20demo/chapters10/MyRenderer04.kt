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

/**
 * 拷贝自 MyRenderer01
 *
 * 绘制画板铅笔功能，
 * 由于 Android Touch 的事件有时间间隔，因此在快速移动时，点与点之间是不连续的，
 * 要想解决这个问题，需要用到贝塞尔曲线，
 * 这将在 MyRenderer05 中应用。
 */
class MyRenderer04(
    private var assets: AssetManager,
    var r: Float = 1f,
    var b: Float = 1f,
    var g: Float = 1f,
    var a: Float = 1f
) : MainActivity.Renderer() {
    /**
     * float 类型占 4 个字节长度
     */
    private val floatTypeSize: Int = 4

    /**
     * 每个点的顶点属性数量，
     * 这里的每个点只有 x, y 两个轴的属性，
     * 由此数量为 2。
     */
    private val vertexCountPerPointer: Int = 2

    /**
     * 每个点的字节大小，
     * 计算方式：顶点属性数量 * 顶点属性字段类型长度，
     * 这里的顶点属性数量为 2，
     * 顶点属性字段类型为 float, float 类型长度为 4，
     * 因此此字节大小是 2 * 4
     */
    private val byteSizePerPointer: Int = vertexCountPerPointer * floatTypeSize

    /**
     * 默认点的缓存数量，当超过这个数量的 2/3 后，自动扩容。
     *
     * 扩容策略：再加一次 defaultCountPointer
     */
    private val defaultCountPointer: Int = 512

    /**
     * 默认的顶点缓存容量
     */
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

    private val latestPoint = PointF()
    private val oldPoint = PointF()

    private var on = true
    private var frameTime = 0L

    init {
        GlobalScope.launch {
            while (on) {
                delay(30)
                println("frameTime: $frameTime")
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

        initShader()
        initBuffer()
        updatePointerBuffer()

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

        if (0 == mPointerCount) return

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
                assets.open("chapters10/vertex04.glvs"),
                assets.open("chapters10/fragment04.glfs")
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
        }
    }

    private fun capacityExpansionIfNeeded() {
        val limit = mTouchPointerBuffer.limit()
        val position = mTouchPointerBuffer.position()
        val remaining = mTouchPointerBuffer.remaining()

        if (limit * 0.67f <= position) {
            println("capacityExpansionIfNeeded1 $limit, $position,  $remaining")
            val temp = ByteBuffer
                .allocateDirect(limit * floatTypeSize + defaultPointerBufferCapacity)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
            println("capacityExpansionIfNeeded2 ${temp.limit()}, ${temp.position()},  ${temp.remaining()}")
            mTouchPointerBuffer.flip()
            println("capacityExpansionIfNeeded3 ${mTouchPointerBuffer.limit()}, ${mTouchPointerBuffer.position()},  ${mTouchPointerBuffer.remaining()}")
            temp.put(mTouchPointerBuffer)
            mTouchPointerBuffer = temp
            println("capacityExpansionIfNeeded4 ${mTouchPointerBuffer.limit()}, ${mTouchPointerBuffer.position()},  ${mTouchPointerBuffer.remaining()}")
        }
    }

    /**
     * 开始监听触屏事件
     */
    private fun updatePointerBuffer() {
        /**
         * latestPoint 在 touch 事件（UI线程）中不断地被更新，
         * 此处只读取一次，以防在后续使用过程中，被 UI 线程更新了，
         * 从而导致上下文使用的数值不是同一个。
         */
        val x = latestPoint.x
        val y = latestPoint.y

        if (x == oldPoint.x && y == oldPoint.y) {
            return
        }

        /**
         * 换算示意图：document/08：屏幕坐标换算到标准化设备坐标.png
         */
        val ndcX = (x - mSurfaceSize.x) / mSurfaceSize.x
        val ndcY = (mSurfaceSize.y - y) / mSurfaceSize.y

        mTouchPointerBuffer.position(mPointerCount * vertexCountPerPointer)
        mTouchPointerBuffer.put(ndcX)
        mTouchPointerBuffer.put(ndcY)
        mPointerCount++
        capacityExpansionIfNeeded()
        mTouchPointerBuffer.position(0)

        oldPoint.x = x
        oldPoint.y = y
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
