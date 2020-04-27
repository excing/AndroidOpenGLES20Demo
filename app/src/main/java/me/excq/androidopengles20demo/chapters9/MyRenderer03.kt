package me.excq.androidopengles20demo.chapters9

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLUtils
import android.opengl.Matrix
import android.view.MotionEvent
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs
import kotlin.random.Random

/**
 * 拷贝自 chapters9#MyRenderer02
 *
 * 摄像机围绕世界坐标旋转
 */
class MyRenderer03(
    private var assets: AssetManager,
    var r: Float = 1f,
    var b: Float = 1f,
    var g: Float = 1f,
    var a: Float = 1f
) : MainActivity.Renderer() {

    private val vertex = floatArrayOf(
        // 坐标            // 纹理坐标
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

    private lateinit var shader: Shader

    private var mPositionHandle: Int = -1
    private var mTextureHandle: Int = -1
    private var mOurTextureHandle: Int = -1

    private var mModelHandle: Int = -1
    private var mViewHandle: Int = -1
    private var mProjectionHandle: Int = -1

    private var vertexBuffer: FloatBuffer

    private val model = FloatArray(16)
    private val view = FloatArray(16)
    private val projection = FloatArray(16)

    private val cameraPos = floatArrayOf(0.0f, 0.0f, 3.0f)
    private val cameraFront = floatArrayOf(0.0f, 0.0f, -1.0f)
    private val cameraUp = floatArrayOf(0.0f, 1.0f, 0.0f)

    private val cubePositions = floatArrayOf(
        0.0f, 0.0f, 0.0f,
        2.0f, 5.0f, -15.0f,
        -1.5f, -2.2f, -2.5f,
        -3.8f, -2.0f, -12.3f,
        2.4f, -0.4f, -3.5f,
        -1.7f, 3.0f, -7.5f,
        1.3f, -2.0f, -2.5f,
        1.5f, 2.0f, -2.5f,
        1.5f, 0.2f, -1.5f,
        -1.3f, 1.0f, -1.5f
    )

    private var boIDs: IntBuffer? = null
    private var textures: IntBuffer? = null

    init {
        vertexBuffer = ByteBuffer.allocateDirect(vertex.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        vertexBuffer.put(vertex)
        vertexBuffer.position(0)
    }

    private val cameraSpeed = 0.05f
    private var oldx = 0.0f
    private var oldy = 0.0f

    /**
     * 坐标轴, 1 表示横向, 2 表示纵向
     */
    private var dir = 0

    override fun onTouch(action: Int, x: Float, y: Float) {
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                oldx = x
                oldy = y
                dir = 0
            }
            MotionEvent.ACTION_UP -> {
                if (0 == dir && abs(oldx - x) < 30 && abs(oldy - y) < 30) {
                    updateBackground()
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (0 == dir && 30 < abs(oldx - x)) {
                    dir = 1
                } else if (0 == dir && 30 < abs(oldy - y)) {
                    dir = 2
                }

                when (dir) {
                    1 -> { // 横向滑动
                        val temp = multi(cross(cameraFront, cameraUp), cameraSpeed)
                        if (oldx < x) {
                            cameraPos[0] -= temp[0]
                            cameraPos[1] -= temp[1]
                            cameraPos[2] -= temp[2]
                        } else {
                            cameraPos[0] += temp[0]
                            cameraPos[1] += temp[1]
                            cameraPos[2] += temp[2]
                        }
                    }
                    2 -> { // 纵向滑动
                        val temp = multi(cameraFront, cameraSpeed)
                        if (oldy < y) {
                            cameraPos[0] += temp[0]
                            cameraPos[1] += temp[1]
                            cameraPos[2] += temp[2]
                        } else {
                            cameraPos[0] -= temp[0]
                            cameraPos[1] -= temp[1]
                            cameraPos[2] -= temp[2]
                        }
                    }
                }

                if (0 != dir) {
                    oldx = x
                    oldy = y
                }
            }
        }
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)

        Matrix.setIdentityM(view, 0)

        Matrix.setIdentityM(projection, 0)
        Matrix.perspectiveM(projection, 0, 45f, width.toFloat() / height, 0.1f, 100f)
    }

    override fun onDrawFrame(gl: GL10?) {
        initShader()
        initBuffer()
        initTexture()

        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
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

        Matrix.setLookAtM(
            view,
            0,
            // 摄像机位置
            cameraPos[0],
            cameraPos[1],
            cameraPos[2],

            // 摄像机指向位置
            cameraFront[0] + cameraPos[0],
            cameraFront[1] + cameraPos[1],
            cameraFront[2] + cameraPos[2],

            // 摄像机的上向量，用于确定 X 轴的
            cameraUp[0],
            cameraUp[1],
            cameraUp[2]
        )
        GLES20.glUniformMatrix4fv(mViewHandle, 1, false, view, 0)

        GLES20.glUniformMatrix4fv(mProjectionHandle, 1, false, projection, 0)

        for (i in 0..9) {
            Matrix.setIdentityM(model, 0)
            Matrix.translateM(
                model,
                0,
                cubePositions[i * 3 + 0],
                cubePositions[i * 3 + 1],
                cubePositions[i * 3 + 2]
            )
            Matrix.rotateM(
                model,
                0,
                20f * i,
                1.0f,
                0.3f,
                0.5f
            )

            GLES20.glUniformMatrix4fv(mModelHandle, 1, false, model, 0)

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures!![0])
            GLES20.glDrawArrays(
                GLES20.GL_TRIANGLES,
                0,
                36
            )
        }

        GLES20.glDisableVertexAttribArray(mPositionHandle)
        GLES20.glDisableVertexAttribArray(mTextureHandle)
    }

    private fun initShader() {
        if (!this::shader.isInitialized) {
            shader = Shader(
                assets.open("chapters9/vertex.glvs"),
                assets.open("chapters9/fragment.glfs")
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
            boIDs = IntBuffer.allocate(1)
            GLES20.glGenBuffers(1, boIDs)

            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, boIDs!![0])
            GLES20.glBufferData(
                GLES20.GL_ARRAY_BUFFER,
                vertex.size * 4,
                vertexBuffer,
                GLES20.GL_STATIC_DRAW
            )
        }
    }


    private fun initTexture() {
        if (null == textures) {
            textures = IntBuffer.allocate(1)
            GLES20.glGenTextures(1, textures)

            createTexture("chapters9/container.jpg", 0)
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

    private fun updateBackground() {
        val random = Random(System.currentTimeMillis())
        this.r = random.nextFloat()
        this.g = random.nextFloat()
        this.b = random.nextFloat()
        this.a = random.nextFloat()
    }

    private fun cross(ai: FloatArray, ar: FloatArray): FloatArray {
        val result = FloatArray(3)

        result[0] = ai[1] * ar[2] - ai[2] * ar[1]
        result[1] = ai[2] * ar[0] - ai[0] * ar[2]
        result[2] = ai[0] * ar[1] - ai[2] * ar[0]

        return result
    }

    private fun multi(arr: FloatArray, b: Float): FloatArray {
        val result = FloatArray(3)

        result[0] = arr[0] * b
        result[1] = arr[1] * b
        result[2] = arr[2] * b

        return result
    }

    override fun destroy() {
        if (null != boIDs) {
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)

            boIDs = null
            textures = null
        }
    }
}
