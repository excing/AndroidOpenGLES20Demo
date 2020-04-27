package me.excq.androidopengles20demo.chapters6

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLUtils
import java.io.InputStream
import java.nio.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * 从 MyRenderer03 拷贝而来，可之 01, 02, 03, 04 对比阅读。
 *
 * 添加内容：纹理的过滤效果
 */
class MyRenderer05(
    private var assets: AssetManager,
    var r: Float = 1f,
    var b: Float = 1f,
    var g: Float = 1f,
    var a: Float = 1f
) : MainActivity.Renderer() {

    private val m = 0.8f
    private val n = 0.2f

    private val vertex = floatArrayOf(
        /**
         * 此处坐标与 MyRenderer03 不同，MyRenderer 坐标绘制顺序是 右上-右下-左上 和 右上-左下-左上，
         * 此处坐标绘制顺序是 左上-右上-右下 和 左上-右下-左下，
         * 因此随之纹理坐标也需要变动，比如第二组矩形坐标未变动，纹理显示方向就会错误。
         */
        // 坐标           // 纹理坐标

        // 左上角矩形
        -m, m, 0.0f, 0.0f, 7.0f,
        -n, m, 0.0f, 7.0f, 7.0f,
        -n, n, 0.0f, 7.0f, 0.0f,
        -m, n, 0.0f, 0.0f, 0.0f,

        // 右上角矩形，纹理坐标与 MyRenderer03 的纹理坐标一致。
        n, m, 0.0f, 1.0f, 1.0f,
        m, m, 0.0f, 1.0f, 0.0f,
        m, n, 0.0f, 0.0f, 0.0f,
        n, n, 0.0f, 0.0f, 1.0f,

        // 右下角矩形
        -m, -n, 0.0f, -3.0f, 3.0f,
        -n, -n, 0.0f, 3.0f, 3.0f,
        -n, -m, 0.0f, 3.0f, -3.0f,
        -m, -m, 0.0f, -3.0f, -3.0f,

        // 右下角矩形
        n, -n, 0.0f, 0.0f, 1.0f,
        m, -n, 0.0f, 1.0f, 1.0f,
        m, -m, 0.0f, 1.0f, 0.0f,
        n, -m, 0.0f, 0.0f, 0.0f
    )

    // 顶点绘制顺序
    private val indices = shortArrayOf(
        // 左上角矩形
        0, 1, 2, 0, 2, 3,

        // 右上角矩形
        4, 5, 6, 4, 6, 7,

        // 左下角矩形
        8, 9, 10, 8, 10, 11,

        // 右下角矩形
        12, 13, 14, 12, 14, 15
    )

    private val borderColor = floatArrayOf(
        0.5f, 0.5f, 0.5f, 1.0f
    )

    private lateinit var shader: Shader

    private var mPositionHandle: Int = 0
    private var mTextureHandle: Int = 0
    private var mOurTextureHandle: Int = 0

    private var vertexBuffer: FloatBuffer
    private var indicesBuffer: ShortBuffer

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
        val params = IntBuffer.allocate(4)
        GLES20.glGetIntegerv(GLES20.GL_MAX_VERTEX_ATTRIBS, params)
        println("GLES20: params: " + params[0])
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        initShader()
        initBuffer()
        initTexture()

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glClearColor(r, b, g, a)

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

        /**
         * 这里与 MyRenderer03 不同，这是新增的代码。
         * 表示当前使用的是是第一个纹理单元。
         * 未指定时，则会使用上次指定的纹理单元，
         * 在 MyRenderer04 中，与 GL_TEXTURE0 绑定的纹理只有一张图片，
         * 因此先进入 MyRenderer04 界面，再进入 MyRenderer03 界面，
         * 就会出现两个矩形都是一张图片的情况。
         */
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glUniform1i(mOurTextureHandle, 0)

        /**
         * 指定绘制顺序的缓存对象
         */
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, boIDs!![1])

        /**
         * 绘制第一个矩形，即左上角矩形，并指定为第一个纹理
         */
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures!![1])
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_MIRRORED_REPEAT
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_MIRRORED_REPEAT
        )
        GLES20.glDrawElements(
            GLES20.GL_TRIANGLES,
            6,
            GLES20.GL_UNSIGNED_SHORT,
            0
        )

        /**
         * 绘制第二个矩形，即右上角矩形，并指定为第二个纹理
         */
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures!![0])
        /**
         * 设置纹理过滤
         * GL_NEAREST OpenGL默认的纹理过滤方式，他表示：
         * OpenGL会选择中心点最接近纹理坐标的那个像素。
         *
         * GL_TEXTURE_MIN_FILTER 表示纹理大于渲染屏幕，将会有一部分像素无法映射到屏幕，即缩小；
         * GL_TEXTURE_MAG_FILTER 表示纹理小于渲染屏幕，没有足够的像素映射到屏幕上，即放大。
         *
         * 本次测试使用的图片分辨率极低，因此以下设置中，仅 GL_TEXTURE_MAG_FILTER 一项发挥了作用。
         */
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_NEAREST
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_NEAREST
        )
        GLES20.glDrawElements(
            GLES20.GL_TRIANGLES,
            6,
            GLES20.GL_UNSIGNED_SHORT,
            12
        )

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures!![0])
        /**
         * GL_CLAMP_TO_EDGE 表示：
         * 纹理坐标会被约束在 0 到 1 之间，超出的部分会重复纹理坐标的边缘，产生一种边缘被拉伸的效果。
         * 超出的部分指纹理坐标不在 [0, 1] 范围内
         */
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
        GLES20.glDrawElements(
            GLES20.GL_TRIANGLES,
            6,
            GLES20.GL_UNSIGNED_SHORT,
            24
        )

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures!![0])
        /**
         * GL_CLAMP_TO_BORDER 表示：
         * 超出的坐标为用户指定的边缘颜色。
         *
         * But Android OpenGL ES 2.0 不支持该属性
         */
//        GLES20.glTexParameterfv(
//            GLES20.GL_TEXTURE_2D,
//            GLES32.GL_TEXTURE_BORDER_COLOR,
//            borderColor,
//            0
//        )
        /**
         * 设置纹理过滤
         * GL_LINEAR 表示：
         * 它会基于纹理坐标附近的纹理像素，计算出一个插值，近似出这些纹理像素之间的颜色。
         * 一个纹理像素的中心距离纹理坐标越近，那么这个纹理像素的颜色对最终的样本颜色的贡献越大。
         */
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
        GLES20.glDrawElements(
            GLES20.GL_TRIANGLES,
            6,
            GLES20.GL_UNSIGNED_SHORT,
            36
        )

        GLES20.glDisableVertexAttribArray(mPositionHandle)
        GLES20.glDisableVertexAttribArray(mTextureHandle)
    }

    private fun initShader() {
        if (!this::shader.isInitialized) {
            shader = Shader(
                assets.open("chapters6/vertex.glvs"),
                assets.open("chapters6/fragment05.glfs")
            )

            mPositionHandle = shader.getAttribLocation("vPosition")
            mTextureHandle = shader.getAttribLocation("vTexCoord")
            mOurTextureHandle = shader.getUniformLocation("ourTexture")
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

            createTexture("chapters6/bigimage.jpg", 0)
            createTexture("chapters6/awesomeface.png", 1)
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
