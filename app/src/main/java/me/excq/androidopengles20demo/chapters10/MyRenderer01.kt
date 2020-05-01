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
 * 贝塞尔曲线
 */
class MyRenderer01(
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

    private var a_tData: Int = -1
    private var u_StartEndData: Int = -1
    private var u_ControlData: Int = -1
    private var u_Color: Int = -1

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

        GLES20.glEnableVertexAttribArray(a_tData)
        GLES20.glVertexAttribPointer(
            a_tData,
            1,
            GLES20.GL_FLOAT,
            false,
            4,
            0
        )

        GLES20.glUniform4f(u_StartEndData, -1.0f, 0.0f, 1.0f, 0.0f)
        GLES20.glUniform4f(u_ControlData, -0.04f, 0.99f, 0.0f, 0.99f)
        GLES20.glUniform4f(u_Color, 1.0f, 0.3f, 0.0f, 1.0f)

        GLES20.glDrawArrays(
            GLES20.GL_POINTS,
            0,
            pointsNum * pointPerTriangles
        )

        GLES20.glDisableVertexAttribArray(a_tData)
    }

    private fun initShader() {
        if (!this::shader.isInitialized) {
            shader = Shader(
                assets.open("chapters10/vertex.glvs"),
                assets.open("chapters10/fragment.glfs")
            )

            a_tData = shader.getAttribLocation("a_tData")
            shader.printError(GLES20.glGetError())
            u_StartEndData = shader.getUniformLocation("u_StartEndData")
            shader.printError(GLES20.glGetError())
            u_ControlData = shader.getUniformLocation("u_ControlData")
            shader.printError(GLES20.glGetError())
            u_Color = shader.getUniformLocation("u_Color")
            shader.printError(GLES20.glGetError())
        }
    }

    private fun initBuffer() {
        if (null == vboIDs) {
            val tDataSize = pointsNum * pointPerTriangles
            val pointTData = FloatArray(tDataSize)

            for (i in 0 until tDataSize step pointPerTriangles) {
                pointTData[i] = i.toFloat() / tDataSize
                pointTData[i + 1] = (i + 1).toFloat() / tDataSize
                pointTData[i + 2] = (i + 2).toFloat() / tDataSize
            }

            val pointTBuffer = ByteBuffer.allocateDirect(tDataSize * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
            pointTBuffer.put(pointTData)
            /**
             * 一定要把位置重置为0
             */
            pointTBuffer.position(0)

            vboIDs = IntBuffer.allocate(1)
            shader.printError(GLES20.glGetError())
            GLES20.glGenBuffers(1, vboIDs)
            shader.printError(GLES20.glGetError())
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboIDs!![0])
            shader.printError(GLES20.glGetError())
            GLES20.glBufferData(
                GLES20.GL_ARRAY_BUFFER,
                tDataSize * 4,
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
