package me.excq.androidopengles20demo.chapters8

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLUtils
import android.opengl.Matrix
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * 拷贝自 chapters8#MyRenderer01
 *
 * 立方体旋转
 */
class MyRenderer02(
    private var assets: AssetManager,
    var r: Float = 1f,
    var b: Float = 1f,
    var g: Float = 1f,
    var a: Float = 1f
) : MainActivity.Renderer() {

    private val vertex = floatArrayOf(
        // 坐标            // 纹理坐标
        0.5f, 0.5f, 0.0f, 1.0f, 1.0f,
        0.5f, -0.5f, 0.0f, 1.0f, 0.0f,
        -0.5f, -0.5f, 0.0f, 0.0f, 0.0f,
        -0.5f, 0.5f, 0.0f, 0.0f, 1.0f,

        -0.5f, -0.5f, -0.5f, 0.0f, 0.0f,
        0.5f, -0.5f, -0.5f, 1.0f, 0.0f,
        0.5f, 0.5f, -0.5f, 1.0f, 1.0f,
        0.5f, 0.5f, -0.5f, 1.0f, 1.0f,
        -0.5f, 0.5f, -0.5f, 0.0f, 1.0f,
        -0.5f, -0.5f, -0.5f, 0.0f, 0.0f,

        -0.5f, -0.5f, 0.5f, 0.0f, 0.0f,
        0.5f, -0.5f, 0.5f, 1.0f, 0.0f,
        0.5f, 0.5f, 0.5f, 1.0f, 1.0f,
        0.5f, 0.5f, 0.5f, 1.0f, 1.0f,
        -0.5f, 0.5f, 0.5f, 0.0f, 1.0f,
        -0.5f, -0.5f, 0.5f, 0.0f, 0.0f,

        -0.5f, 0.5f, 0.5f, 1.0f, 0.0f,
        -0.5f, 0.5f, -0.5f, 1.0f, 1.0f,
        -0.5f, -0.5f, -0.5f, 0.0f, 1.0f,
        -0.5f, -0.5f, -0.5f, 0.0f, 1.0f,
        -0.5f, -0.5f, 0.5f, 0.0f, 0.0f,
        -0.5f, 0.5f, 0.5f, 1.0f, 0.0f,

        0.5f, 0.5f, 0.5f, 1.0f, 0.0f,
        0.5f, 0.5f, -0.5f, 1.0f, 1.0f,
        0.5f, -0.5f, -0.5f, 0.0f, 1.0f,
        0.5f, -0.5f, -0.5f, 0.0f, 1.0f,
        0.5f, -0.5f, 0.5f, 0.0f, 0.0f,
        0.5f, 0.5f, 0.5f, 1.0f, 0.0f,

        -0.5f, -0.5f, -0.5f, 0.0f, 1.0f,
        0.5f, -0.5f, -0.5f, 1.0f, 1.0f,
        0.5f, -0.5f, 0.5f, 1.0f, 0.0f,
        0.5f, -0.5f, 0.5f, 1.0f, 0.0f,
        -0.5f, -0.5f, 0.5f, 0.0f, 0.0f,
        -0.5f, -0.5f, -0.5f, 0.0f, 1.0f,

        -0.5f, 0.5f, -0.5f, 0.0f, 1.0f,
        0.5f, 0.5f, -0.5f, 1.0f, 1.0f,
        0.5f, 0.5f, 0.5f, 1.0f, 0.0f,
        0.5f, 0.5f, 0.5f, 1.0f, 0.0f,
        -0.5f, 0.5f, 0.5f, 0.0f, 0.0f,
        -0.5f, 0.5f, -0.5f, 0.0f, 1.0f
    )

//    private val indices = shortArrayOf(
//        0, 1, 2, 0, 2, 3
//    )

    private lateinit var shader: Shader

    private var mPositionHandle: Int = -1
    private var mTextureHandle: Int = -1
    private var mOurTextureHandle: Int = -1

    private var mModelHandle: Int = -1
    private var mViewHandle: Int = -1
    private var mProjectionHandle: Int = -1

    private var vertexBuffer: FloatBuffer
//    private var indicesBuffer: ShortBuffer

    /**
     * 旋转角度
     */
    private var angle = 0f

    private val model = FloatArray(16)
    private val view = FloatArray(16)
    private val projection = FloatArray(16)

    private var boIDs: IntBuffer? = null
    private var textures: IntBuffer? = null

    init {
        vertexBuffer = ByteBuffer.allocateDirect(vertex.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        vertexBuffer.put(vertex)
        vertexBuffer.position(0)

//        indicesBuffer = ByteBuffer.allocateDirect(indices.size * 2)
//            .order(ByteOrder.nativeOrder())
//            .asShortBuffer()
//        indicesBuffer.put(indices)
//        indicesBuffer.position(0)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)

        Matrix.setIdentityM(view, 0)
        Matrix.translateM(view, 0, 0f, 0f, -3f)

        Matrix.setIdentityM(projection, 0)
        Matrix.perspectiveM(projection, 0, 45f, width.toFloat() / height, 0.1f, 100f)
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

        Matrix.setIdentityM(model, 0)
        Matrix.rotateM(model, 0, angle++, 0.5f, 1.0f, 0.1f)

        GLES20.glUniformMatrix4fv(mModelHandle, 1, false, model, 0)
        GLES20.glUniformMatrix4fv(mViewHandle, 1, false, view, 0)
        GLES20.glUniformMatrix4fv(mProjectionHandle, 1, false, projection, 0)

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures!![0])
        GLES20.glDrawArrays(
            GLES20.GL_TRIANGLES,
            4, // 从第四个顶点数据开始，因为前面 4 个顶点数据是 MyRenderer01 的顶点数据，此处未删除，如果删除了，应填 0
            36
        )
//        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, boIDs!![1])
//        GLES20.glDrawElements(
//            GLES20.GL_TRIANGLES,
//            6,
//            GLES20.GL_UNSIGNED_SHORT,
//            0
//        )

        GLES20.glDisableVertexAttribArray(mPositionHandle)
        GLES20.glDisableVertexAttribArray(mTextureHandle)
    }

    private fun initShader() {
        if (!this::shader.isInitialized) {
            shader = Shader(
                assets.open("chapters8/vertex.glvs"),
                assets.open("chapters8/fragment.glfs")
            )

            mPositionHandle = shader.getAttribLocation("vPosition")
            mTextureHandle = shader.getAttribLocation("vTexCoord")
            mOurTextureHandle = shader.getUniformLocation("ourTexture")

            mModelHandle = shader.getUniformLocation("model")
            mViewHandle = shader.getUniformLocation("view")
            mProjectionHandle = shader.getUniformLocation("projection")
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

//            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, boIDs!![1])
//            GLES20.glBufferData(
//                GLES20.GL_ELEMENT_ARRAY_BUFFER,
//                indices.size * 2,
//                indicesBuffer,
//                GLES20.GL_STATIC_DRAW
//            )
        }
    }


    private fun initTexture() {
        if (null == textures) {
            textures = IntBuffer.allocate(1)
            GLES20.glGenTextures(1, textures)

            createTexture("chapters8/container.jpg", 0)
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
