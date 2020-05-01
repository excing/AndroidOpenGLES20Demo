package me.excq.androidopengles20demo.chapters04

import android.opengl.GLES20
import java.nio.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MyRenderer02(var r: Float, var b: Float, var g: Float, var a: Float) :
    MainActivity.Renderer() {

    private val vertexShaderCode = "" +
            "attribute vec4 vPosition;" +
            "attribute vec4 vColor;" +
            "varying vec4 outColor;" +
            "void main() {" +
            "  gl_Position = vPosition;" +
            "  outColor = vColor;" +
            "}";

    private val fragmentShaderCode = "" +
            "precision mediump float;" +
            "varying vec4 outColor;" +
            "void main() {" +
            "  gl_FragColor = outColor;" +
            "}";

    private val points = floatArrayOf(
        // 坐标            // 颜色
        0.0f, 0.5f, 0.0f, 1.0f, 0.0f, 0.0f,
        -0.5f, -0.5f, 0.0f, 0.0f, 1.0f, 0.0f,
        0.5f, -0.5f, 0.0f, 0.0f, 0.0f, 1.0f
    )

    // 顶点绘制顺序
    private val indices = shortArrayOf(
        0, 1, 2
    )

    private var mProgram: Int = 0
    private var mPositionHandle: Int = 0
    private var mColorHandle: Int = 0

    private var vertexBuffer: FloatBuffer
    private var indicesBuffer: ShortBuffer

    private var boIDs: IntBuffer? = null

    init {
        vertexBuffer = ByteBuffer.allocateDirect(points.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        vertexBuffer.put(points)
        vertexBuffer.position(0)

        indicesBuffer = ByteBuffer.allocateDirect(indices.size * 2)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
        indicesBuffer.put(indices)
        indicesBuffer.position(0)
    }

    override fun onDrawFrame(gl: GL10?) {
        initProgram()
        initBuffer()

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glClearColor(r, b, g, a)

        GLES20.glUseProgram(mProgram)

        GLES20.glEnableVertexAttribArray(mPositionHandle)
        GLES20.glVertexAttribPointer(
            mPositionHandle,
            3,
            GLES20.GL_FLOAT,
            false,
            24,
            0
        )

        GLES20.glEnableVertexAttribArray(mColorHandle)
        GLES20.glVertexAttribPointer(
            mColorHandle, 3,
            GLES20.GL_FLOAT,
            false,
            24,
            12
        )

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 3, GLES20.GL_UNSIGNED_SHORT, 0)

        GLES20.glDisableVertexAttribArray(mPositionHandle)
        GLES20.glDisableVertexAttribArray(mColorHandle)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        val params = IntBuffer.allocate(4)
        GLES20.glGetIntegerv(GLES20.GL_MAX_VERTEX_ATTRIBS, params)
        println("GLES20: params: " + params[0])
    }

    private fun initProgram() {
        if (0 == mProgram) {
            mProgram = GLES20.glCreateProgram()

            val vertexShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER)
            val fragmentShader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER)

            GLES20.glShaderSource(vertexShader, vertexShaderCode)
            GLES20.glCompileShader(vertexShader)
            GLES20.glAttachShader(mProgram, vertexShader)

            GLES20.glShaderSource(fragmentShader, fragmentShaderCode)
            GLES20.glCompileShader(fragmentShader)
            GLES20.glAttachShader(mProgram, fragmentShader)

            GLES20.glLinkProgram(mProgram)

            GLES20.glDeleteShader(vertexShader)
            GLES20.glDeleteShader(fragmentShader)

            mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition")
            mColorHandle = GLES20.glGetAttribLocation(mProgram, "vColor")
        }
    }

    private fun initBuffer() {
        if (null == boIDs) {
            boIDs = IntBuffer.allocate(2)
            GLES20.glGenBuffers(2, boIDs)

            // 绑定第一个缓冲区对象，当缓存类型为 `GL_ARRAY_BUFFER` 时，该对象称之为 VBO
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, boIDs!![0])
            GLES20.glBufferData(
                GLES20.GL_ARRAY_BUFFER,
                points.size * 4,
                vertexBuffer,
                GLES20.GL_STATIC_DRAW
            )

            // 绑定第二个缓冲区对象，当缓存类型为 `GL_ELEMENT_ARRAY_BUFFER` 时，该对象称之为 EBO
            // EBO 是图元索引缓存区对象，用于按该索引顺序绘制图元，
            // 因此不能使用 `glDrawArrays` 绘制图元，应使用 `glDrawElements` 方法
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, boIDs!![1])
            GLES20.glBufferData(
                GLES20.GL_ELEMENT_ARRAY_BUFFER,
                indices.size * 2,
                indicesBuffer,
                GLES20.GL_STATIC_DRAW
            )
        }
    }

    override fun updateBackground(r: Float, b: Float, g: Float, a: Float) {
        this.r = r
        this.g = g
        this.b = b
        this.a = a
    }

    override fun destroy() {
        if (null != boIDs) {
            // 释放缓存
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)

            boIDs = null
        }
    }
}
