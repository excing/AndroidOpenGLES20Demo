package me.excq.androidopengles20demo.chapters10

import android.content.res.AssetManager
import android.opengl.GLES20
import android.view.MotionEvent
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.IntBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.random.Random

/**
 * 拷贝自 MyRenderer02
 *
 * 绘制基于贝塞尔曲线去绘制曲边扇形
 */
class MyRenderer03(
    private var assets: AssetManager,
    var r: Float = 1f,
    var b: Float = 1f,
    var g: Float = 1f,
    var a: Float = 1f
) : MainActivity.Renderer() {

    /**
     * 取 256 个点
     */
    private val pointsNum: Int = 256
    private val pointPerTriangles: Int = 3

    private lateinit var shader: Shader

    private var vboIDs: IntBuffer? = null

    private var aTDataHandle: Int = -1
    private var uStartEndDataHandle: Int = -1
    private var uControlDataHandle: Int = -1
    private var uOffsetHandle: Int = -1
    private var uColorHandle: Int = -1

    private var mFrameIndex: Int = 0

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        initShader()
        initBuffer()

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glClearColor(r, g, b, a)

        shader.use()

        GLES20.glEnableVertexAttribArray(aTDataHandle)
        GLES20.glVertexAttribPointer(
            aTDataHandle,
            1,
            GLES20.GL_FLOAT,
            false,
            4,
            0
        )

        GLES20.glUniform4f(uStartEndDataHandle, -1.0f, 0.0f, 1.0f, 0.0f)
        GLES20.glUniform4f(uControlDataHandle, -0.04f, 0.99f, 0.0f, 0.99f)
        GLES20.glUniform4f(uColorHandle, 1.0f, 0.3f, 0.0f, 1.0f)

        var offset = (mFrameIndex % 100).toFloat() / 100
        offset = if (1 == (mFrameIndex / 100) % 2) 1 - offset else offset

        GLES20.glUniform1f(uOffsetHandle, offset)

        GLES20.glDrawArrays(
            GLES20.GL_TRIANGLES,
            0,
            pointsNum * pointPerTriangles
        )

        GLES20.glDisableVertexAttribArray(aTDataHandle)

        mFrameIndex = if (mFrameIndex == Int.MAX_VALUE) 0 else mFrameIndex + 1
    }

    private fun initShader() {
        if (!this::shader.isInitialized) {
            shader = Shader(
                assets.open("chapters10/vertex03.glvs"),
                assets.open("chapters10/fragment03.glfs")
            )

            aTDataHandle = shader.getAttribLocation("a_tData")
            shader.printError(GLES20.glGetError())
            uStartEndDataHandle = shader.getUniformLocation("u_StartEndData")
            shader.printError(GLES20.glGetError())
            uControlDataHandle = shader.getUniformLocation("u_ControlData")
            shader.printError(GLES20.glGetError())
            uOffsetHandle = shader.getUniformLocation("u_Offset")
            shader.printError(GLES20.glGetError())
            uColorHandle = shader.getUniformLocation("u_Color")
            shader.printError(GLES20.glGetError())
        }
    }

    private fun initBuffer() {
        if (null == vboIDs) {
            val tDataSize = pointsNum * pointPerTriangles
            val pointTData = FloatArray(tDataSize)

            for (i in 0 until tDataSize step pointPerTriangles) {
                pointTData[i] = i.toFloat() / tDataSize
                pointTData[i + 1] = (i + 3).toFloat() / tDataSize
                /**
                 * 这里也可以这样写：
                 *
                 * pointTData[i + 2] = 0.0f
                 *
                 * 当 t 为 0 时，不管几阶贝塞尔公式，返回的都是 0，
                 * 而在 vertex03.glvs 顶点着色器脚本中，当 t 小于 0 时，
                 * t 所处的顶点位置被置为 (0.0, 0.0) 了。
                 * 因此此处两种写法都 OK。
                 * 如果使用注释中的写法，那么顶点着色器脚本中，if-else 代码段可以不要了。
                 */
                pointTData[i + 2] = -1.0f
            }

            val pointTBuffer = ByteBuffer.allocateDirect(tDataSize * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
            pointTBuffer.put(pointTData)
            pointTBuffer.position(0)

//            println("BufferSize2 ${pointTBuffer.position()}, ${pointTBuffer.limit()}, ${pointTBuffer.remaining()}")

            vboIDs = IntBuffer.allocate(1)
            shader.printError(GLES20.glGetError())
            GLES20.glGenBuffers(1, vboIDs)
            shader.printError(GLES20.glGetError())
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboIDs!![0])
            shader.printError(GLES20.glGetError())
            GLES20.glBufferData(
                GLES20.GL_ARRAY_BUFFER,
                pointTBuffer.limit() * 4,
                pointTBuffer,
                GLES20.GL_STATIC_DRAW
            )
            shader.printError(GLES20.glGetError())
        }
    }

    override fun onTouch(
        event: MotionEvent,
        action: Int,
        x: Float,
        y: Float
    ) {
        if (action == MotionEvent.ACTION_UP) {
            val random = Random(System.currentTimeMillis())
            this.r = random.nextFloat()
            this.g = random.nextFloat()
            this.b = random.nextFloat()
            this.a = random.nextFloat()
        }
    }

    override fun destroy() {
        if (null != vboIDs) {
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)

            vboIDs = null
        }
    }
}
