package me.excq.androidopengles20demo.toy

import android.content.res.AssetManager
import android.graphics.PointF
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import me.excq.androidopengles20demo.BaseActivity
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.IntBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MainActivity : BaseActivity() {
    private lateinit var glView: GLSurfaceView
    private lateinit var renderer: MyRenderer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        glView = GLSurfaceView(this)
        renderer = MyRenderer(assets)
        glView.setEGLContextClientVersion(2);
        glView.setRenderer(renderer)
        setContentView(glView)

        glView.setOnTouchListener { v, event -> onTouch(v, event) }
    }

    override fun onResume() {
        super.onResume()
        glView.onResume()
    }

    override fun onPause() {
        super.onPause()
        glView.onPause()
    }

    private fun onTouch(v: View, event: MotionEvent): Boolean {
        renderer.onTouch(event.action, event.x, event.y)
        return true
    }

    class MyRenderer(val assets: AssetManager) : GLSurfaceView.Renderer {

        private val vertex = floatArrayOf(
            -1.0f, 1.0f, 0.0f, 1.0f, 0.0f, 0.0f,
            1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f,
            1.0f, -1.0f, 0.0f, 0.0f, 0.0f, 1.0f,
            -1.0f, 1.0f, 0.0f, 1.0f, 0.0f, 0.0f,
            -1.0f, -1.0f, 0.0f, 0.0f, 1.0f, 0.0f,
            1.0f, -1.0f, 0.0f, 0.0f, 0.0f, 1.0f
        )

        private val matrix = FloatArray(16)

        private var vbo: IntBuffer? = null

        private lateinit var surfaceSize: PointF
        private lateinit var shader: Shader
        private var mPositionHandler: Int = -1
        private var mColorHandler: Int = -1
        private var mMatrixHandler: Int = -1

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            surfaceSize = PointF(width.toFloat(), height.toFloat())
            GLES20.glViewport(0, 0, width, height)
        }

        override fun onDrawFrame(gl: GL10?) {
            initRenderer()

            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            GLES20.glClearColor(1.0f, 0.1f, 1.0f, 1.0f)

            shader.use()

            GLES20.glEnableVertexAttribArray(mPositionHandler)
            GLES20.glEnableVertexAttribArray(mColorHandler)

            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo!![0])
            GLES20.glVertexAttribPointer(mPositionHandler, 3, GLES20.GL_FLOAT, false, 24, 0)
            GLES20.glVertexAttribPointer(mColorHandler, 3, GLES20.GL_FLOAT, false, 24, 12)

            GLES20.glUniformMatrix4fv(mMatrixHandler, 1, false, matrix, 0)

            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6)

            GLES20.glDisableVertexAttribArray(mPositionHandler)
            GLES20.glDisableVertexAttribArray(mColorHandler)
        }

        private fun initRenderer() {
            if (!this::shader.isInitialized) {
                shader = Shader(assets.open("toy/vertex.glvs"), assets.open("toy/fragment.glfs"))
                mPositionHandler = shader.getAttribLocation("vPosition")
                mColorHandler = shader.getAttribLocation("vColor")
                mMatrixHandler = shader.getUniformLocation("matrix")
            }

            if (null == vbo) {
                val size: Int = vertex.size * 4
                val vertexBuffer =
                    ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder()).asFloatBuffer()
                vertexBuffer.put(vertex)
                vertexBuffer.position(0)

                val n = 1
                vbo = IntBuffer.allocate(n)
                GLES20.glGenBuffers(n, vbo)
                GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo!![0])
                GLES20.glBufferData(
                    GLES20.GL_ARRAY_BUFFER,
                    size,
                    vertexBuffer,
                    GLES20.GL_STATIC_DRAW
                )
            }
        }

        fun onTouch(action: Int, x: Float, y: Float) {
            Matrix.setIdentityM(matrix, 0)
            Matrix.translateM(
                matrix,
                0,
                x / surfaceSize.x,
                y / surfaceSize.y,
                (x + y) / (surfaceSize.x + surfaceSize.y)
            )
        }

    }
}