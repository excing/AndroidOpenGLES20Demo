package me.excq.androidopengles20demo.chapters7

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLUtils
import android.opengl.Matrix
import java.io.InputStream
import java.nio.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * 拷贝自 chapters6#MyRenderer01
 */
class MyRenderer01(
    private var assets: AssetManager,
    var r: Float = 1f,
    var b: Float = 1f,
    var g: Float = 1f,
    var a: Float = 1f
) : MainActivity.Renderer() {

    private val vertex = floatArrayOf(
        // 坐标           // 纹理坐标
        0.5f, 0.5f, 0.0f, 1.0f, 1.0f,
        0.5f, -0.5f, 0.0f, 1.0f, 0.0f,
        -0.5f, -0.5f, 0.0f, 0.0f, 0.0f,
        -0.5f, 0.5f, 0.0f, 0.0f, 1.0f
    )

    // 顶点绘制顺序
    private val indices = shortArrayOf(
        0, 1, 2, 0, 2, 3
    )

    private lateinit var shader: Shader

    private var mPositionHandle: Int = 0
    private var mTextureHandle: Int = 0
    private var mOurTextureHandle: Int = 0
    private var mTransformHandle: Int = 0

    private var vertexBuffer: FloatBuffer
    private var indicesBuffer: ShortBuffer

    private val trans = FloatArray(16)
    private var angle = 0f

    private var boIDs: IntBuffer? = null
    private var textures: IntBuffer? = null

    init {
        vertexBuffer = ByteBuffer.allocateDirect(vertex.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        vertexBuffer.put(vertex)
        vertexBuffer.position(0)

        indicesBuffer = ByteBuffer.allocateDirect(indices.size * 2)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
        indicesBuffer.put(indices)
        indicesBuffer.position(0)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
//        val params = IntBuffer.allocate(4)
//        GLES20.glGetIntegerv(GLES20.GL_MAX_VERTEX_ATTRIBS, params)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        initShader()
        initBuffer()
        initTexture()

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glClearColor(r, g, b, a)

        shader.use()

        GLES20.glEnableVertexAttribArray(mPositionHandle)
        GLES20.glEnableVertexAttribArray(mTextureHandle)

        /**
         * 代码解释：
         *
         * 首先使用 setIdentityM 初始化一个矩阵数组，通用的矩阵数组长度为 16，是 4 * 4 结构。
         * 初始化后，数组的 1*1, 2*2, 3*3, 4*4 的位置，即对角位置均被设置为 1。
         *
         * 接着调用了 translateM 方法将矩阵按右上角的方向向量 [0.5, 0.5] 进行了位移。
         *
         * 然后调用 rotateM 设置矩阵旋转方向，其中 angle 为旋转角度，其后三个参数，分别表示在哪个方向旋转，
         * 其中 Z 轴设置为 1f，表示方向垂直于 Z 轴，进行旋转。
         *
         * 最后调用 scaleM 设置矩阵的缩放比例，最后三个参数分别表示在 x, y, z 轴上的缩放比例。
         */
        Matrix.setIdentityM(trans, 0)
        Matrix.translateM(trans, 0, 0.5f, 0.5f, 0f)
        Matrix.rotateM(trans, 0, angle++, 0f, 0f, 1f)
        Matrix.scaleM(trans, 0, 0.5f, 0.5f, 0.5f)
        GLES20.glUniformMatrix4fv(mTransformHandle, 1, false, trans, 0)

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, boIDs!![0])
        GLES20.glVertexAttribPointer(
            mPositionHandle,
            3,
            GLES20.GL_FLOAT,
            false,
            20,
            0
        )
        GLES20.glVertexAttribPointer(
            mTextureHandle,
            2,
            GLES20.GL_FLOAT,
            false,
            20,
            12
        )

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glUniform1i(mOurTextureHandle, 0)

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures!![0])
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, boIDs!![1])
        GLES20.glDrawElements(
            GLES20.GL_TRIANGLES,
            6,
            GLES20.GL_UNSIGNED_SHORT,
            0
        )

        GLES20.glDisableVertexAttribArray(mPositionHandle)
        GLES20.glDisableVertexAttribArray(mTextureHandle)
    }

    private fun initShader() {
        if (!this::shader.isInitialized) {
            shader = Shader(
                assets.open("chapters7/vertex.glvs"),
                assets.open("chapters7/fragment.glfs")
            )

            mPositionHandle = shader.getAttribLocation("vPosition")
            mTextureHandle = shader.getAttribLocation("vTexCoord")
            mOurTextureHandle = shader.getUniformLocation("ourTexture")
            mTransformHandle = shader.getUniformLocation("transform")
        }
    }

    private fun initBuffer() {
        if (null == boIDs) {
            boIDs = IntBuffer.allocate(2)
            GLES20.glGenBuffers(2, boIDs)

            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, boIDs!![0])
            GLES20.glBufferData(
                GLES20.GL_ARRAY_BUFFER,
                vertex.size * 4,
                vertexBuffer,
                GLES20.GL_STATIC_DRAW
            )

            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, boIDs!![1])
            GLES20.glBufferData(
                GLES20.GL_ELEMENT_ARRAY_BUFFER,
                indices.size * 2,
                indicesBuffer,
                GLES20.GL_STATIC_DRAW
            )
        }
    }


    private fun initTexture() {
        if (null == textures) {
            textures = IntBuffer.allocate(2)
            GLES20.glGenTextures(2, textures)

            createTexture("chapters7/container.jpg", 0)
        }
    }

    private fun createTexture(path: String, index: Int) {
        val input: InputStream?
        val bitmap: Bitmap?
        try {
            input = assets.open(path)
            bitmap = BitmapFactory.decodeStream(input)
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures!![index])
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        input.use { i -> i.close() }
        bitmap?.recycle()
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
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)

            boIDs = null
            textures = null
        }
    }
}
