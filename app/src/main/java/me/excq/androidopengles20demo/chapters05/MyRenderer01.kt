package me.excq.androidopengles20demo.chapters05

import android.content.res.AssetManager
import android.opengl.GLES20
import java.nio.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MyRenderer01(
    private var assets: AssetManager,
    var r: Float = 1f,
    var b: Float = 1f,
    var g: Float = 1f,
    var a: Float = 1f
) : MainActivity.Renderer() {

    private val points = floatArrayOf(
        // 坐标            // 颜色
        0.5f, 0.5f, 0.0f, 1.0f, 0.0f, 0.0f,
        0.5f, -0.5f, 0.0f, 0.0f, 0.0f, 1.0f,
        -0.5f, -0.5f, 0.0f, 0.0f, 1.0f, 0.0f,
        -0.5f, 0.5f, 0.0f, 0.5f, 0.5f, 1.0f,

        0.2f, 0.2f, 0.0f, 1.0f, 0.0f, 0.0f,
        0.2f, -0.2f, 0.0f, 0.0f, 0.0f, 1.0f,
        -0.2f, -0.2f, 0.0f, 0.0f, 1.0f, 0.0f,
        -0.2f, 0.2f, 0.0f, 0.5f, 0.5f, 1.0f
    )

    // 顶点绘制顺序
    private val indices = shortArrayOf(
        0, 1, 2, 0, 2, 3,
        4, 5, 6, 4, 6, 7
    )

    private lateinit var shader: Shader

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
        initShader()
        initBuffer()

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glClearColor(r, b, g, a)

        shader.use()

        GLES20.glEnableVertexAttribArray(mPositionHandle)
        GLES20.glEnableVertexAttribArray(mColorHandle)

        GLES20.glVertexAttribPointer(
            mPositionHandle,
            3,
            GLES20.GL_FLOAT,
            false,
            24,
            0
        )
        GLES20.glVertexAttribPointer(
            mColorHandle,
            3,
            GLES20.GL_FLOAT,
            false,
            24,
            12
        )
//        GLES20.glDrawElements(
//            GLES20.GL_LINES,
//            12,
//            GLES20.GL_UNSIGNED_SHORT,
//            0
//        )
        GLES20.glDrawElements(
            GLES20.GL_LINES,
            6,
            GLES20.GL_UNSIGNED_SHORT,
            0
        )
        GLES20.glDrawElements(
            GLES20.GL_TRIANGLES,
            6,
            GLES20.GL_UNSIGNED_SHORT,
            12
        )

        GLES20.glDisableVertexAttribArray(mPositionHandle)
        GLES20.glDisableVertexAttribArray(mColorHandle)
    }

    private fun initShader() {
        if (!this::shader.isInitialized) {
            shader = Shader(
                assets.open("chapters5/vertex.glvs"),
                assets.open("chapters5/fragment.glfs")
            )

            mPositionHandle = shader.getAttribLocation("vPosition")
            mColorHandle = shader.getAttribLocation("vColor")
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        val params = IntBuffer.allocate(4)
        GLES20.glGetIntegerv(GLES20.GL_MAX_VERTEX_ATTRIBS, params)
        println("GLES20: params: " + params[0])
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

            // OpenGL ES 2.0 中不支持 VAO
//            GLES30.glGenVertexArrays(1, vaoIDS)
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
