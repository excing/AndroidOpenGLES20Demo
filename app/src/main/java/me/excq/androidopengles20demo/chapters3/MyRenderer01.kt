package me.excq.androidopengles20demo.chapters3

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.sin
import kotlin.random.Random

class MyRenderer01(var r: Float, var b: Float, var g: Float, var a: Float) :
    GLSurfaceView.Renderer, MainActivity.RGBA {

    private val vertexShaderCode = "" +
            "attribute vec4 vPosition;" +
            "void main() {" +
            "  gl_Position = vPosition;" +
            "}";

    private val fragmentShaderCode = "" +
            "precision mediump float;" +
            "uniform vec4 vColor;" +
            "void main() {" +
            "  gl_FragColor = vColor;" +
            "}";

    private val points = floatArrayOf(
        0f, 0.5f, 0.0f,
        -0.5f, -0.5f, 0.0f,
        0.5f, -0.5f, 0.0f
    )

    private val colors = floatArrayOf(
        0.5f, 0.0f, 0.0f, 1.0f
    )

    private var mProgram: Int = 0
    private var mPositionHandle: Int = 0
    private var mColorHandle: Int = 0

    private var vertexBuffer: FloatBuffer

    private val random = Random(System.currentTimeMillis())

    init {
        vertexBuffer = ByteBuffer.allocateDirect(points.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer();
        vertexBuffer.put(points)
        vertexBuffer.position(0)
    }

    override fun onDrawFrame(gl: GL10?) {
        initProgram()

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glClearColor(r, b, g, a)

        GLES20.glUseProgram(mProgram)

        GLES20.glEnableVertexAttribArray(mPositionHandle)
        GLES20.glVertexAttribPointer(
            mPositionHandle, 3,
            GLES20.GL_FLOAT,
            false,
            12,
            vertexBuffer
        )
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3)

        colors[0] = random.nextFloat()
        colors[1] = random.nextFloat()
        colors[2] = ((sin(System.currentTimeMillis().toDouble()) + 1) / 2).toFloat()
        val b = ((sin(System.currentTimeMillis().toDouble()) + 1) / 2).toFloat()

//            GLES20.glUniform4f(mColorHandle, 0f, 0f, b, 1f)
        GLES20.glUniform4f(mColorHandle, random.nextFloat(), random.nextFloat(), b, 1f)

        GLES20.glDisableVertexAttribArray(mPositionHandle)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        val params = IntBuffer.allocate(4)
        GLES20.glGetIntegerv(GLES20.GL_MAX_VERTEX_ATTRIBS, params)
        println("GLES20: params: " + params[0])
    }

    fun initProgram() {
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

            /*
             链接结束后，获取着色器变量
             */

            mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition")
            mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor")
        }
    }

    override fun updateBackground(r: Float, b: Float, g: Float, a: Float) {
        this.r = r
        this.g = g
        this.b = b
        this.a = a
    }
}
