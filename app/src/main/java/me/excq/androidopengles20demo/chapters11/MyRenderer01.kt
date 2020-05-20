package me.excq.androidopengles20demo.chapters11

import android.content.res.AssetManager
import android.graphics.PointF
import android.opengl.GLES20
import android.view.MotionEvent
import me.excq.androidopengles20demo.chapters06.Shader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.IntBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.random.Random

/**
 * 拷贝自 chapters06/MyRenderer01
 *
 * 贝塞尔曲线
 */
class MyRenderer01(
    private var assets: AssetManager,
    fontPath: String,
    var r: Float = 1f,
    var b: Float = 1f,
    var g: Float = 1f,
    var a: Float = 1f
) : MainActivity.Renderer() {

    private val text = "G"
    private var bitmap: FreeTypeBitmap? = null

    private lateinit var shader: Shader

    private var mPositionHandle: Int = 0
    private var mTextureHandle: Int = 0
    private var mOurTextureHandle: Int = 0
    private var mOurTextColorHandle: Int = 0

    private var boIDs: IntBuffer? = null
    private var textures: IntBuffer? = null

    private var mSurfaceSize = PointF()

    init {
        initTreeType(fontPath)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        mSurfaceSize.x = width.toFloat()
        mSurfaceSize.y = height.toFloat()

        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        initShader()
        initTexture()
        initBuffer()

        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glClearColor(r, b, g, a)

        shader.use()

        GLES20.glEnableVertexAttribArray(mPositionHandle)
        GLES20.glEnableVertexAttribArray(mTextureHandle)

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, boIDs!![0])
        GLES20.glVertexAttribPointer(
            mPositionHandle,
            2,
            GLES20.GL_FLOAT,
            false,
            4 * 4,
            0
        )
        GLES20.glVertexAttribPointer(
            mTextureHandle,
            2,
            GLES20.GL_FLOAT,
            false,
            4 * 4,
            2 * 4
        )

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glUniform1i(mOurTextureHandle, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures!![0])
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_LINEAR
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE
        )

        GLES20.glUniform3f(mOurTextColorHandle, 0.3f, 0.5f, 1.0f)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6)

        GLES20.glDisableVertexAttribArray(mPositionHandle)
        GLES20.glDisableVertexAttribArray(mTextureHandle)
    }

    private fun initShader() {
        if (!this::shader.isInitialized) {
            shader = Shader(
                assets.open("chapters11/vertex.glvs"),
                assets.open("chapters11/fragment.glfs")
            )

            mPositionHandle = shader.getAttribLocation("vPosition")
            mTextureHandle = shader.getAttribLocation("vTexCoord")
            mOurTextureHandle = shader.getUniformLocation("ourTexture")
            mOurTextColorHandle = shader.getUniformLocation("ourTextColor")
        }
    }

    private fun initTexture() {
        if (null == textures) {
            bitmap = getCharBitmap(text)

            val width = bitmap!!.width
            val height = bitmap!!.height

            GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1)

            textures = IntBuffer.allocate(1)
            GLES20.glGenTextures(1, textures)

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures!![0])

            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D,
                0,
                GLES20.GL_LUMINANCE,
                width,
                height,
                0,
                GLES20.GL_LUMINANCE,
                GLES20.GL_UNSIGNED_BYTE,
                bitmap!!.buffer
            )
            GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        }
    }

    private fun initBuffer() {
        if (null == boIDs) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)

            var xpos = 400.0f + bitmap!!.left
            var ypos = 400.0f + (bitmap!!.height + bitmap!!.top)

            val w = bitmap!!.width.toFloat() / mSurfaceSize.x
            val h = bitmap!!.height.toFloat() / mSurfaceSize.y

            xpos = (xpos - mSurfaceSize.x) / mSurfaceSize.x
            ypos = (mSurfaceSize.y - ypos) / mSurfaceSize.y

            val vertices = floatArrayOf(
                // 第一个三角形
                xpos, ypos, 0.0f, 0.0f,
                xpos, ypos - h, 0.0f, 1.0f,
                xpos + w, ypos - h, 1.0f, 1.0f,

                // 第二个三角形
                xpos, ypos, 0.0f, 0.0f,
                xpos + w, ypos - h, 1.0f, 1.0f,
                xpos + w, ypos, 1.0f, 0.0f
            )

            val vertexBuffer = ByteBuffer.allocateDirect(6 * 4 * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
            vertexBuffer.put(vertices)
            vertexBuffer.position(0)

            try {
                boIDs = IntBuffer.allocate(1)
            } catch (e: IllegalArgumentException) {
                println("InitBuffer error ${e.message}")
                return
            }

            GLES20.glGenBuffers(1, boIDs)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, boIDs!![0])
            GLES20.glBufferData(
                GLES20.GL_ARRAY_BUFFER,
                6 * 4 * 4,
                vertexBuffer,
                GLES20.GL_STATIC_DRAW
            )
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
        if (null != boIDs) {
            // 释放缓存
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)

            boIDs = null
            textures = null
        }

        destroyTreeType()
    }

    private external fun initTreeType(fontPath: String): Int

    private external fun getCharBitmap(s: String): FreeTypeBitmap?

    private external fun destroyTreeType()
}
