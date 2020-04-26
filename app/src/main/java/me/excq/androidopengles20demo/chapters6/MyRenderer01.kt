package me.excq.androidopengles20demo.chapters6

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES20
import java.io.InputStream
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

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glClearColor(r, b, g, a)

        shader.use()

        GLES20.glEnableVertexAttribArray(mPositionHandle)
        GLES20.glEnableVertexAttribArray(mTextureHandle)

        /**
         * 当我们使用了多个缓存对象时，操作时就必须指明接下来需要使用的缓存对象，即绑定。
         * 如果不绑定，使用的缓存对象则是前面绑定的，
         * MyRenderer02 的所有操作之前没有指定缓存对象，可作反而教材参考。
         */
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
                assets.open("chapters6/vertex.glvs"),
                assets.open("chapters6/fragment.glfs")
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
            val input: InputStream?
            val bitmap: Bitmap?
            val options = BitmapFactory.Options()
            try {
                options.inJustDecodeBounds = false
                /**
                对图片解码时采用 ARGB_8888 颜色模式，
                如果使用其他颜色模式，则 OpenGL 纹理显示有异常，
                此模式只针对 GLES20.glTexImage2D 方法生成的纹理，
                采用 GLUtils.texImage2D 方法生成的纹理则无需遵守此约定，
                具体代码可参考 MyRenderer02。
                 */
                options.inPreferredConfig = Bitmap.Config.ARGB_8888

                input = assets.open("chapters6/container.jpg")
                bitmap = BitmapFactory.decodeStream(input, null, options)
            } catch (e: Exception) {
                e.printStackTrace()
                throw e
            }

            /**
             * 获取图片的宽高，GLES20.glTexImage2D 方法需要此值
             */
            val width = options.outWidth
            val height = options.outHeight

            /**
             * 通过 `ARGB_8888` 颜色模式获取的 Bitmap 大小，每个像素是 4 个字节
             */
            val image = ByteBuffer.allocateDirect(width * height * 4)
                .order(ByteOrder.nativeOrder())
            /**
             * 将 bitmap 拷贝至 image 字节数组中
             */
            bitmap?.copyPixelsToBuffer(image)
            image.position(0)

            /**
             * 纹理 ID 列表
             */
            textures = IntBuffer.allocate(1)
            /**
             * 生成纹理 ID，第一个参数是需要生成的纹理数量，然后将生成的纹理 ID 列表储存至第二个参数。
             */
            GLES20.glGenTextures(1, textures)

            /**
             * 绑定纹理，第一个参数是将纹理类型，第二个参数是需要绑定的纹理 ID
             */
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures!![0])
            /**
             * 生成纹理，此方法与 glBindTexture 搭配使用，
             * 即生成的纹理与 glBindTexture 方法中的纹理 ID 进行了绑定
             * 具体用法可参考 MyRenderer03。
             *
             * 第一个参数指定了纹理目标（Target）。
             * 设置为 `GL_TEXTURE_2D` 意味着会生成与当前绑定的纹理对象在同一个目标上的纹理（任何绑定到 `GL_TEXTURE_1D` 和 `GL_TEXTURE_3D` 的纹理不会受到影响）
             * 第二个参数为纹理指定多级渐远纹理的级另，如果你希望单独手动设置每个多级渐远纹理的级别的话。这里我们填0，也就是基本级别。
             * 第三个参数告诉 OpenGL 我们希望把纹理储存为何种格式。我们的图像时是采用 `ARGB_8888` 格式获取时，因此我们也把纹理储存为 `RGBA`。
             * 第四个和第五个参数设置最终的纹理宽度和高度。
             * 第六个参数总是被设为 0（历史遗留问题）。
             * 第七第八个参数定义了源图的格式的数据类型，我们的图像时是采用 `ARGB_8888` 格式获取时，因此我们也把纹理储存为 `RGBA`。
             * 最后一个参数是真正的图像数据。
             */
            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D,
                0,
                GLES20.GL_RGBA,
                width,
                height,
                0,
                GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE,
                image
            )
            /**
             * 为当前绑定的纹理自动生成所有需要的多级渐远纹理（必要步骤，否则会全黑）。
             */
            GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)

            /**
             * 生成了纹理和相应的多级渐远纹理后，释放图像的内存并解绑纹理对象。
             */
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
            input.use { i -> i.close() }
            bitmap?.recycle()
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
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)

            boIDs = null
            textures = null
        }
    }
}
