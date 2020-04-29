package me.excq.androidopengles20demo.chapters9

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
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
 * 拷贝自 chapters9#MyRenderer06
 *
 * 支持多点操作
 */
class MyRenderer07(
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

    private var pointer = mutableMapOf<Int, PointF>()

    private val sensitivity = 0.05f
    private var yaw = -90.0
    private var pitch = 0.0

    private var fov = 45f
    private var aspect = 0.0f

    private var roll = 0.0f

    private var frontTouchArea = FloatArray(4)
    private var zoomTouchArea = FloatArray(4)
    private var moveTouchArea = FloatArray(4)
    private var rollTouchArea = FloatArray(4)

    init {
        vertexBuffer = ByteBuffer.allocateDirect(vertex.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        vertexBuffer.put(vertex)
        vertexBuffer.position(0)

        lastTime = System.currentTimeMillis()
    }

    private var mActivePointerId: Int = 0
    private fun testTouch(event: MotionEvent) {
        /**
         * 我们可以通过每个指针的索引和 ID 跟踪 `MotionEvent` 中的各个指针：
         *
         * 索引：`MotionEvent` 会有效地将关于每个的信息存储在数组中。
         * 指针的索引就是指针在此数组中的位置。
         * 用于与指针进行交互的大多数 `MotionEvent` 方法都使用指针索引（而非指针 ID）作为参数
         *
         * ID：每个的指针还具有一个 ID 映射，该映射在轻触事件之间保持不变，让我们能够在整个手势中跟踪单个指针。
         *
         * 单个指针在动作事件中的显示顺序是未定义的。
         * 因此，不同事件中的指针索引可能不一样，但只要指针保持活动状态（即 MotionEvent.ACTION_MOVE），
         * 其指针 ID 就会保持不变。
         * 我们可以使用 `getPointerId()` 方法获取指针的 ID，并在手势中的所有后续动作中跟踪该指针。
         * 然后，对于连续动作事件，可以使用 `findPointerIndex()` 方法获取给定指针 ID 在相应动作事件中的指针索引。
         */
        when (event.actionMasked) {
            /**
             * 针对轻触屏幕的第一个指针。
             * 这是手势的起点。
             * 此指针的指针数据始终位于 MotionEvent 中的索引 0 处。
             */
            MotionEvent.ACTION_DOWN -> {
                println("onTouch down ${event.actionIndex} and id is ${event.getPointerId(event.actionIndex)}")
            }
            /**
             * 当最后一个指针离开屏幕时发送。
             */
            MotionEvent.ACTION_UP -> {
                println("onTouch up ${event.actionIndex} and id is ${event.getPointerId(event.actionIndex)}")
            }
            /**
             * 针对除第一个指针以外进入屏幕的其他指针。
             * 此指针的指针数据位于 getActionIndex() 返回的索引处。
             */
            MotionEvent.ACTION_POINTER_DOWN -> {
                println(
                    "onTouch pointer down ${event.actionIndex} and id is ${event.getPointerId(
                        event.actionIndex
                    )}"
                )
            }
            /**
             * 当非主要指针抬起时发送。
             */
            MotionEvent.ACTION_POINTER_UP -> {
                println(
                    "onTouch pointer up ${event.actionIndex} and id is ${event.getPointerId(
                        event.actionIndex
                    )}"
                )
            }
            /**
             * 在执行按下手势的过程中发生了更改。
             */
            MotionEvent.ACTION_MOVE -> {
                /**
                 * actionIndex 表示当前活动指针的索引，
                 * 活动指针一般情况下，是第一个指针，当第一个指针离开后，就是第二个指针，
                 * 如果第一个指针最后一个离开，那么活动指针始终是第一个指针，actionIndex 也始终是第一个指针的索引
                 * pointerIndex 表示（所有的指针含隐蔽的，隐蔽的指针意思是说非活动指针）指针的索引。
                 *
                 * 具体情况就是，在 ACTION_MOVE 事件下，只能获得 actionIndex 索引，
                 * 且始终只会返回一个当前活动指针的索引，而没有其他的指针索引。
                 */
                for (i in 0 until event.pointerCount) {
                    val id = event.getPointerId(i)
                    val pointerIndex = event.findPointerIndex(id)
                    val x = event.getX(pointerIndex)
                    val y = event.getY(pointerIndex)

                    println("onTouch move $i, $pointerIndex, $id and xy is $x, $y")
                }
            }
        }


        // ------------------------------------------------------

//        mActivePointerId = event.getPointerId(0)
//
//        val (x: Float, y: Float) = event.findPointerIndex(mActivePointerId).let { index ->
//            event.getX(index) to event.getY(index)
//        }
//        println("onTouch0 ${event.pointerCount}, $mActivePointerId, $x, $y")
//
//        /**
//         * 获取当前 touch 指针的动作及坐标。
//         * `event.actionMasked` 能够与多个指针一块使用。
//         * 他可以返回正在执行的指针操作，但不包含指针索引位。
//         * 然而我们可以使用 `event.actionIndex` 返回与此操作相关联的指针索引。
//         * 再通过该索引，我们可以通过 `event.getX(index)` 方法获得当前操作的指针坐标值。
//         *
//         * 因此，在多点操作中，`event.action` 就不需要了
//         */
//        val (xPos: Float, yPos: Float) = event.actionMasked.let { action ->
//            val actionString = when (action) {
//                MotionEvent.ACTION_DOWN -> "Down"
//                MotionEvent.ACTION_MOVE -> "Move"
//                MotionEvent.ACTION_POINTER_DOWN -> "Pointer Down"
//                MotionEvent.ACTION_UP -> "Up"
//                MotionEvent.ACTION_POINTER_UP -> "Pointer Up"
//                MotionEvent.ACTION_OUTSIDE -> "Outside"
//                MotionEvent.ACTION_CANCEL -> "Cancel"
//                else -> ""
//            }
//            println("onTouch1 $actionString")
//            event.actionIndex.let { index ->
//                event.getX(index) to event.getY(index)
//            }
//        }
//
//        if (1 < event.pointerCount) {
//            println("onTouch2 Multitouch event, $xPos, $yPos")
//        } else {
//            println("ouTouch2 Single touch event, $xPos, $yPos")
//        }

        // -------------------------------------------------------

//        event.actionMasked.let { at ->
//            event.actionIndex.let { index ->
//                val x1 = event.getX(index)
//                val y1 = event.getY(index)
//                val id = event.getPointerId(index)
//
//                when (at) {
//                    MotionEvent.ACTION_DOWN -> {
//                        println("onTouch down $id, $x1, $y1")
//                    }
//                    MotionEvent.ACTION_MOVE -> {
//                        println("onTouch move $id, $x1, $y1")
//                    }
//                    MotionEvent.ACTION_POINTER_DOWN -> {
//                        println("onTouch down $id, $x1, $y1 -----------")
//                    }
//                    MotionEvent.ACTION_UP -> {
//                        println("onTouch up $id, $x1, $y1")
//                    }
//                    MotionEvent.ACTION_POINTER_UP -> {
//                        println("onTouch up $id, $x1, $y1 ------------")
//                    }
//                    MotionEvent.ACTION_OUTSIDE -> {
//                        println("onTouch outside")
//                    }
//                    MotionEvent.ACTION_CANCEL -> {
//                        println("onTouch")
//                    }
//                }
//            }
//        }
    }

    override fun onTouch(
        event: MotionEvent,
        action: Int,
        x: Float,
        y: Float
    ) {
//        testTouch(event)
        onCameraEvent(event)
    }

    private fun onCameraEvent(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val index = event.actionIndex
                pointer[event.getPointerId(index)] = PointF(event.getX(index), event.getY(index))
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                pointer.remove(event.getPointerId(event.actionIndex))
            }
            MotionEvent.ACTION_MOVE -> {
                for ((k, v) in pointer) {
                    val index = event.findPointerIndex(k)
                    val x = event.getX(index)
                    val y = event.getY(index)

                    val xoffset = v.x - x
                    val yoffset = y - v.y

                    v.x = x
                    v.y = y

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
                }
            }
        }
    }

    private fun onRoll(
        xoffset: Float
    ) {
        roll += xoffset * sensitivity * 0.1f

        val rx = sin(roll)
        val ry = cos(roll)

        cameraUp[0] = rx
        cameraUp[1] = ry
    }

    private fun onMove(
        xoffset: Float,
        yoffset: Float
    ) {
        val cameraSpeedX = 0.0005f * deltaTime * xoffset
        val cameraSpeedY = 0.0005f * deltaTime * yoffset

        val moveZ = multi(normalize(cross(cameraFront, cameraUp)), cameraSpeedX)
        val moveX = multi(cameraFront, cameraSpeedY)

        cameraPos[0] += moveZ[0]
        cameraPos[1] += moveZ[1]
        cameraPos[2] += moveZ[2]

        cameraPos[0] -= moveX[0]
        cameraPos[1] -= moveX[1]
        cameraPos[2] -= moveX[2]
    }

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
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        val startY1 = height * 0.4f
        val startY2 = height * 0.7f
        val divX = width * 0.5f

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
            cameraPos[0],
            cameraPos[1],
            cameraPos[2],

            cameraFront[0] + cameraPos[0],
            cameraFront[1] + cameraPos[1],
            cameraFront[2] + cameraPos[2],

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
