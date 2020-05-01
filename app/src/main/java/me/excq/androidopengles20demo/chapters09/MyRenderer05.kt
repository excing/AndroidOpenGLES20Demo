package me.excq.androidopengles20demo.chapters09

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
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * 拷贝自 chapters9#MyRenderer04
 *
 * 视角移动
 */
class MyRenderer05(
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

    private val cameraPos = floatArrayOf(0.0f, 0.0f, 0.1f)
    private var cameraFront = floatArrayOf(0.0f, 0.0f, -1.0f)
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

    private var deltaTime = 0L
    private var lastTime = 0L

    private var oldx = 0.0f
    private var oldy = 0.0f

    private val sensitivity = 0.05f
    private var yaw = -90.0
    private var pitch = 0.0

    init {
        vertexBuffer = ByteBuffer.allocateDirect(vertex.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        vertexBuffer.put(vertex)
        vertexBuffer.position(0)

        lastTime = System.currentTimeMillis()
    }

    override fun onTouch(
        event: MotionEvent,
        action: Int,
        x: Float,
        y: Float
    ) {
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                oldx = x
                oldy = y
            }
            MotionEvent.ACTION_MOVE -> {
                /**
                 * 在操作上，当我们向右滑动时，我们希望的是摄像头向左移动（就像转铅笔一样，笔的两端的方向相反），
                 * 反之亦然。
                 * 在 OpenGL 中原点在屏幕中心，上方为 Y 轴正方向，右方为 X 轴正方向，
                 * 摄像头向左移动，则表示偏航角增大（逆时针运动了），但为负值（X 轴左方向为负值）；
                 * 对应到 Android 的 touch 事件中，
                 * 屏幕原点(0, 0)在左上角，下方为 Y 轴正方向，右方为 X 轴正方向，
                 * 往右滑动，则 y 值越大，因此此处要用 oldx 减去 x（oldx 比 x 小），
                 * 因为操作方向与摄像机方向刚好相反。
                 * 由此也能想象到，计算俯仰角时是什么情况了。
                 */
                var xoffset = oldx - x

                /**
                 * 在操作上，当我们向下滑动时，我们希望的是摄像头向上移动（就像跷跷板一样），
                 * 反之亦然。
                 * 前面说了 OpenGL 的坐标系和 Android 的坐标系，X 轴方向一致，但 Y 轴方向相反。
                 * 因此，在计算俯仰角时，
                 * 摄像头向上移动，我们需要一个正值的俯仰角，
                 * 因此此处要用 y 减去 oldy（y 比 oldy 大），
                 * 这与偏航角的计算刚好相反。
                 */
                var yoffset = y - oldy
                oldx = x
                oldy = y

                xoffset *= sensitivity
                yoffset *= sensitivity

                yaw += xoffset
                pitch += yoffset

                /**
                 * 对于俯仰角，要让用户不能看向高于 89 度的地方（在 90 度时视角会发生逆转，所以我们把 89 度作为极限），
                 * 同样也不允许小于 -89 度。
                 * 这样能够保证用户只能看到天空或脚下，但是不能超过这个限制。
                 * 因此代码中我们用了两个 if 来实现这个限制。
                 *
                 * 注意，我们没有给偏航角设置限制，这是因为我们不希望限制用户的水平旋转。
                 * 因为偏航角超过 90 度时不会发生逆转。
                 * 当然，给偏航角设置限制也容易，如果你愿意可以自己实现。
                 */
                if (89f < pitch) pitch = 89.0
                if (pitch < -89f) pitch = -89.0

                val a = (cos(Math.toRadians(pitch)) * cos(Math.toRadians(yaw))).toFloat()
                val b = sin(Math.toRadians(pitch)).toFloat()
                val c = (cos(Math.toRadians(pitch)) * sin(Math.toRadians(yaw))).toFloat()

                cameraFront[0] = a
                cameraFront[1] = b
                cameraFront[2] = c

                cameraFront = normalize(cameraFront)

//                println("onTouch ${cameraFront.contentToString()}")
                println("onTouch $yaw, $pitch")
//                println("onTouch $x, $y")
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
        val currentTime = System.currentTimeMillis()
        deltaTime = currentTime - lastTime
        lastTime = currentTime

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

    /**
     * 矩阵叉乘
     */
    private fun cross(ai: FloatArray, ar: FloatArray): FloatArray {
        val result = FloatArray(3)

        result[0] = ai[1] * ar[2] - ai[2] * ar[1]
        result[1] = ai[2] * ar[0] - ai[0] * ar[2]
        result[2] = ai[0] * ar[1] - ai[2] * ar[0]

        return result
    }

    /**
     * 矩阵乘标量
     */
    private fun multi(arr: FloatArray, b: Float): FloatArray {
        val result = FloatArray(3)

        result[0] = arr[0] * b
        result[1] = arr[1] * b
        result[2] = arr[2] * b

        return result
    }

    /**
     * 归一化
     */
    private fun normalize(arr: FloatArray): FloatArray {
        val len = sqrt(arr[0] * arr[0] + arr[1] * arr[1] + arr[2] * arr[2])

        arr[0] = arr[0] / len
        arr[1] = arr[1] / len
        arr[2] = arr[2] / len

        return arr
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
