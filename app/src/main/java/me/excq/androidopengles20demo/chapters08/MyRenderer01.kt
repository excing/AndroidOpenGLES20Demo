package me.excq.androidopengles20demo.chapters08

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
 * 拷贝自 chapters7#MyRenderer01
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

    private val indices = shortArrayOf(
        0, 1, 2, 0, 2, 3
    )

    private lateinit var shader: Shader

    private var mPositionHandle: Int = -1
    private var mTextureHandle: Int = -1
    private var mOurTextureHandle: Int = -1

    private var mModelHandle: Int = -1
    private var mViewHandle: Int = -1
    private var mProjectionHandle: Int = -1

    private var vertexBuffer: FloatBuffer
    private var indicesBuffer: ShortBuffer

    /**
     * 模型矩阵，
     * 通过将顶点坐标乘以这个模型矩阵，我们将该顶点坐标变换到世界坐标。
     */
    private val model = FloatArray(16)
    /**
     * 视图矩阵，
     * 由一系列的位移和旋转的组合来完成，平移/旋转场景从而使得特定的对象被变换到摄像机的前方，
     * 他被用来将世界坐标变换到观察空间。
     */
    private val view = FloatArray(16)
    /**
     * 投影矩阵，
     * 将在一个指定的范围内的坐标变换为标准化设备坐标的范围(-1.0, 1.0)。
     * 所有在范围外的坐标不会被映射到在-1.0到1.0的范围之间，所以会被裁剪掉。
     */
    private val projection = FloatArray(16)

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
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)

        /**
         * 初始化一个模型矩阵，并使围绕 X 轴逆时针旋转 55 度
         */
        Matrix.setIdentityM(model, 0)
        Matrix.rotateM(model, 0, -55f, 1.0f, 0.0f, 0.0f)

        /**
         * 初始化一个视图矩阵，并使之在 Z 轴上，住后移动三个 OpenGL 单位
         */
        Matrix.setIdentityM(view, 0)
        Matrix.translateM(view, 0, 0f, 0f, -3f)

        /**
         * 初始化一个投影矩阵，并对这个矩阵进行了赋值。
         *
         * 其中观察视角为 45 度，这个值通常被称为 field of view，简单 fov。
         * 设置了近平面的宽高比，即我们的屏幕显示区域。
         * 第三个和第四个参数分别表示近平面和远平面与相机的距离。
         *
         * 这里出现的近平面、远平面和相机，分别表示：
         * 近平面：我们的屏幕
         * 远平面：物体实际应该如处的位置
         * 相机：我们的眼睛
         *
         * 简单的说，就是我们通过眼睛看到远处的画像时，在手机屏幕上应该是什么样子的。
         *
         * 这里设置的近平面距离是 0.1，差不多是眼睛贴在屏幕上远处画像所显示的效果。
         * 远平面距离设置的是 100，差不多是离我们眼睛很远的意思。
         */
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

        GLES20.glUniformMatrix4fv(mModelHandle, 1, false, model, 0)
        GLES20.glUniformMatrix4fv(mViewHandle, 1, false, view, 0)
        GLES20.glUniformMatrix4fv(mProjectionHandle, 1, false, projection, 0)

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
