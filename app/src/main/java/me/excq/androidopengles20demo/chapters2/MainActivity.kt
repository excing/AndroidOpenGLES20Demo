package me.excq.androidopengles20demo.chapters2

import android.annotation.SuppressLint
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import me.excq.androidopengles20demo.BaseActivity
import me.excq.androidopengles20demo.WebActivity
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.random.Random

class MainActivity : BaseActivity() {
    private lateinit var glSurfaceView: GLSurfaceView
    private lateinit var myRenderer: MyRenderer

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        super.onCreate(savedInstanceState)

        glSurfaceView = GLSurfaceView(this)
        glSurfaceView.setEGLContextClientVersion(2)

        setContentView(glSurfaceView)

        myRenderer = MyRenderer(1f, 1f, 1f, 1f)
        glSurfaceView.setRenderer(myRenderer)
        glSurfaceView.setOnTouchListener(MyTouch(myRenderer))
    }

    override fun onMenu2Click() {
        WebActivity.open(this, "https://github.com/excing/AndroidOpenGLES20Demo/tree/master/app/src/main/java/me/excq/androidopengles20demo/chapters2");
    }

    override fun onResume() {
        super.onResume()
        glSurfaceView.onResume()
    }

    override fun onPause() {
        super.onPause()
        glSurfaceView.onPause()
    }

    private class MyRenderer(var r: Float, var b: Float, var g: Float, var a: Float) : GLSurfaceView.Renderer {

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

        private val color = floatArrayOf(
            0.63671875f, 0.76953125f, 0.22265625f, 1.0f
        )

        private val points = floatArrayOf(
            0f, 0.5f, 0.0f,
            -0.5f, -0.5f, 0.0f,
            0.5f, -0.5f, 0.0f
        )

        private var mProgram: Int = 0

        private var vertexBuffer: FloatBuffer

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

            val mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition")
            GLES20.glEnableVertexAttribArray(mPositionHandle)
            GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false, 12, vertexBuffer)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES,  0, 3)

            val mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor")
            GLES20.glUniform4fv(mColorHandle, 1, color, 0)

            GLES20.glDisableVertexAttribArray(mPositionHandle)
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            GLES20.glViewport(0, 0, width, height)
        }

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
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
            }
        }
    }

    private class MyTouch(
        var myRenderer: MyRenderer
    ) : View.OnTouchListener {
        override fun onTouch(v: View?, event: MotionEvent?): Boolean {
            if (event != null) {
                when(event.action) {
                    MotionEvent.ACTION_UP -> touchUp()
                }
            }
            return true
        }

        private fun touchUp() {
            val random = Random(System.currentTimeMillis())
            myRenderer.r = random.nextFloat()
            myRenderer.g = random.nextFloat()
            myRenderer.b = random.nextFloat()
            myRenderer.a = random.nextFloat()
        }

    }
}