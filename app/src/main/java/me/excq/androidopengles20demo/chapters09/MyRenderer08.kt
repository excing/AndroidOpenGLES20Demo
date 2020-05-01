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

/**
 * 拷贝自 chapters9#MyRenderer07
 *
 * 抽象摄像机类
 */
class MyRenderer08(
    private var assets: AssetManager,
    var r: Float = 1f,
    var b: Float = 1f,
    var g: Float = 1f,
    var a: Float = 1f
) : MainActivity.Renderer(),
    PointerTouchCallback,
    Camera.CameraPerspectiveCallback,
    Camera.CameraLookAtCallback {

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

    private var boIDs: IntBuffer? = null
    private var textures: IntBuffer? = null

    private var deltaTime = 0L
    private var lastTime = 0L

    private var aspect = 0.0f

    private val camera = Camera()

    private var frontTouchArea = FloatArray(4)
    private var zoomTouchArea = FloatArray(4)
    private var moveTouchArea = FloatArray(4)
    private var rollTouchArea = FloatArray(4)

    private var iTouch: MainActivity.ITouch

    init {
        vertexBuffer = ByteBuffer.allocateDirect(vertex.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        vertexBuffer.put(vertex)
        vertexBuffer.position(0)

        lastTime = System.currentTimeMillis()

        camera.perspectiveCallback = this
        camera.lookAtCallback = this

//        iTouch = Touch01(pointerTouchCallback = this)
        iTouch = Touch02(pointerTouchCallback = this)
    }

    override fun onTouch(x: Float, y: Float, xOffset: Float, yOffset: Float) {
        if (
            frontTouchArea[0] < x &&
            frontTouchArea[1] < y &&
            x < frontTouchArea[2] &&
            y < frontTouchArea[3]
        ) {
            onFront(xOffset, yOffset)
        } else if (
            zoomTouchArea[0] < x &&
            zoomTouchArea[1] < y &&
            x < zoomTouchArea[2] &&
            y < zoomTouchArea[3]
        ) {
            onZoom(yOffset)
        } else if (
            moveTouchArea[0] < x &&
            moveTouchArea[1] < y &&
            x < moveTouchArea[2] &&
            y < moveTouchArea[3]
        ) {
            onMove(xOffset, yOffset)
        } else if (
            rollTouchArea[0] < x &&
            rollTouchArea[1] < y &&
            x < rollTouchArea[2] &&
            y < rollTouchArea[3]
        ) {
            onRoll(xOffset)
        }
    }

    override fun onTouch(
        event: MotionEvent,
        action: Int,
        x: Float,
        y: Float
    ) {
        iTouch.onTouch(event, action, x, y)
    }

    private fun onRoll(
        xoffset: Float
    ) {
        camera.onRoll(xoffset)
    }

    private fun onMove(
        xoffset: Float,
        yoffset: Float
    ) {
        val cameraSpeedX = deltaTime * xoffset
        val cameraSpeedY = deltaTime * yoffset

        camera.onMove(cameraSpeedX, cameraSpeedY)
    }

    private fun onZoom(
        yoffset: Float
    ) {
        camera.onZoom(yoffset)
    }

    private fun onFront(
        xoffset: Float,
        yoffset: Float
    ) {
        camera.onFront(xoffset, yoffset)
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
        camera.setAndUpdate(this.aspect)
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

    override fun destroy() {
        if (null != boIDs) {
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)

            boIDs = null
            textures = null
        }
    }

    override fun perspective(fovy: Float, aspect: Float, zNear: Float, zFar: Float) {
        Matrix.perspectiveM(projection, 0, fovy, aspect, zNear, zFar)
    }

    override fun lookAt(
        eyeX: Float,
        eyeY: Float,
        eyeZ: Float,
        centerX: Float,
        centerY: Float,
        centerZ: Float,
        upX: Float,
        upY: Float,
        upZ: Float
    ) {
        Matrix.setLookAtM(view, 0, eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ)
    }
}
