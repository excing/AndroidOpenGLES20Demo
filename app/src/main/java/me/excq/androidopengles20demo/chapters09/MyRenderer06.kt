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

/**
 * 拷贝自 chapters9#MyRenderer05
 *
 * 添加：缩放、摄像机翻滚。
 * 同时将视角移动和摄像机平移操作也加进来了，
 * 其中摄像机平移操作采用了另一种实现方法。
 * 注意：只支持单点操作，多点操作可以见 MyRenderer07
 */
class MyRenderer06(
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

    /**
     * 视野
     */
    private var fov = 45f
    private var aspect = 0.0f

    /**
     * 摄像机翻滚角度
     */
    private var roll = 0.0f

    /**
     * 视角移动操作区域
     *
     * 两个对角点
     */
    private var frontTouchArea = FloatArray(4)

    /**
     * 缩放操作区域
     *
     * 两个对角点
     */
    private var zoomTouchArea = FloatArray(4)

    /**
     * 位移操作区域
     */
    private var moveTouchArea = FloatArray(4)

    /**
     * 摄像机翻滚操作区域
     */
    private var rollTouchArea = FloatArray(4)

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
                val xoffset = oldx - x
                val yoffset = y - oldy

                oldx = x
                oldy = y

                if (
                    frontTouchArea[0] < x &&
                    frontTouchArea[1] < y &&
                    x < frontTouchArea[2] &&
                    y < frontTouchArea[3]
                ) {
                    onFront(xoffset, yoffset)
                } else if (
                    zoomTouchArea[0] < x &&
                    zoomTouchArea[1] < y &&
                    x < zoomTouchArea[2] &&
                    y < zoomTouchArea[3]
                ) {
                    onZoom(yoffset)
                } else if (
                    moveTouchArea[0] < x &&
                    moveTouchArea[1] < y &&
                    x < moveTouchArea[2] &&
                    y < moveTouchArea[3]
                ) {
                    onMove(xoffset, yoffset)
                } else if (
                    rollTouchArea[0] < x &&
                    rollTouchArea[1] < y &&
                    x < rollTouchArea[2] &&
                    y < rollTouchArea[3]
                ) {
                    onRoll(xoffset)
                }
//                println("onTouch $x, $y, ${frontTouchArea.contentToString()}, ${scrollTouchArea.contentToString()}")
            }
        }
    }

    /**
     * 摄像机翻滚
     */
    private fun onRoll(
        xoffset: Float
    ) {
        roll += xoffset * sensitivity * 0.1f

        val rx = sin(roll)
        val ry = cos(roll)

        /**
         * 此函数执行线程与 GLThread 线程不同，
         * 因此在操作与 GLThread 同用的变量时，尽量减少运算时间，以免出现不同步的情况，
         * 当然也可以采用同步块或锁的方式，不过那不在这个教程的范围内了。
         */
        cameraUp[0] = rx
        cameraUp[1] = ry

//        println("onRoll $roll, ${cameraUp.contentToString()}")
    }

    /**
     * 摄像机位移
     */
    private fun onMove(
        xoffset: Float,
        yoffset: Float
    ) {
        val moveX = xoffset * sensitivity * deltaTime / 100f
        val moveZ = yoffset * sensitivity * deltaTime / 100f

        cameraPos[0] += moveX
        cameraPos[2] += moveZ
    }

    /**
     * 缩放
     */
    private fun onZoom(
        yoffset: Float
    ) {
        if (fov in 1.0f..90.0f) {
            fov -= yoffset * sensitivity
        }
        if (fov <= 1.0f) {
            fov = 1.0f
        }
        if (90.0f <= fov) {
            fov = 90.0f
        }

        Matrix.perspectiveM(
            projection, 0,
            fov,
            aspect, 0.1f, 100f
        )
    }

    /**
     * 摄像头焦点(视角)移动
     */
    private fun onFront(
        xoffset: Float,
        yoffset: Float
    ) {
        yaw += xoffset * sensitivity
        pitch += yoffset * sensitivity

        if (89f < pitch) pitch = 89.0
        if (pitch < -89f) pitch = -89.0

        val a = (cos(Math.toRadians(pitch)) * cos(Math.toRadians(yaw))).toFloat()
        val b = sin(Math.toRadians(pitch)).toFloat()
        val c = (cos(Math.toRadians(pitch)) * sin(Math.toRadians(yaw))).toFloat()

        val temp = floatArrayOf(a, b, c)

        cameraFront = normalize(temp)

//        println("onTouch ${cameraFront.contentToString()}")
//        println("onTouch $yaw, $pitch")
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        val startY1 = height * 0.4f // 屏幕高度的十分之四的位置开始监听
        val startY2 = height * 0.7f // 屏幕高度的十分之七的位置开始监听
        val divX = width * 0.5f    // 屏幕宽度的二分之一处区分视角移动与缩放的区域

        aspect = width.toFloat() / height

        rollTouchArea[0] = 0f
        rollTouchArea[1] = startY1
        rollTouchArea[2] = divX
        rollTouchArea[3] = startY2

        zoomTouchArea[0] = divX
        zoomTouchArea[1] = startY1
        zoomTouchArea[2] = width.toFloat()
        zoomTouchArea[3] = startY2

        frontTouchArea[0] = 0f
        frontTouchArea[1] = startY2
        frontTouchArea[2] = divX
        frontTouchArea[3] = height.toFloat()

        moveTouchArea[0] = divX
        moveTouchArea[1] = startY2
        moveTouchArea[2] = width.toFloat()
        moveTouchArea[3] = height.toFloat()

        GLES20.glViewport(0, 0, width, height)

        Matrix.setIdentityM(view, 0)

        Matrix.setIdentityM(projection, 0)
        Matrix.perspectiveM(projection, 0, fov, aspect, 0.1f, 100f)
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
