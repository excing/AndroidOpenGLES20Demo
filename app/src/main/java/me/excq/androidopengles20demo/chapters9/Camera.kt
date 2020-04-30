package me.excq.androidopengles20demo.chapters9

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 摄像机
 */
class Camera(
    eyeZ: Float = 3.0f,
    centerZ: Float = -1.0f
) {

    /**
     * 偏航角
     */
    private var yaw = -90.0

    /**
     * 俯仰角
     */
    private var pitch = 0.0

    /**
     * 翻滚角
     */
    private var roll = 0.0f

    var fovy: Float = 45.0f
    var aspect: Float = 0.0f
    var zNear: Float = 0.1f
    var zFar: Float = 100.0f

    private var cameraPos: FloatArray = floatArrayOf(0.0f, 0.0f, eyeZ)
    private var cameraFront = floatArrayOf(0.0f, 0.0f, centerZ)
    private val cameraUp = floatArrayOf(0.0f, 1.0f, 0.0f)

    var perspectiveCallback: CameraPerspectiveCallback? = null
    var lookAtCallback: CameraLookAtCallback? = null

    var moveSensit = 0.0005f
    var frontSensit = 0.05f
    var rollSensit = 0.005f
    var zoomSensit = 0.05f

    fun setSensitivity(move: Float, front: Float, roll: Float, zoom: Float) {
        moveSensit = move
        frontSensit = front
        rollSensit = roll
        zoomSensit = zoom
    }

    fun setPerspective(
        fovy: Float = this.fovy,
        aspect: Float = this.aspect,
        zNear: Float = this.zNear,
        zFar: Float = this.zFar
    ) {
        this.fovy = fovy
        this.aspect = aspect
        this.zNear = zNear
        this.zFar = zFar
    }

    fun setAndUpdate(aspect: Float = this.aspect) {
        this.aspect = aspect

        lookAt()
        perspectiveCallback?.perspective(fovy, this.aspect, zNear, zFar)
    }

    fun onRoll(rollOffset: Float) {
        roll += rollOffset * rollSensit

        val rx = sin(roll)
        val ry = cos(roll)

        cameraUp[0] = rx
        cameraUp[1] = ry

        lookAt()
    }

    fun onMove(xOffset: Float, yOffset: Float) {
        val cameraSpeedX = moveSensit * xOffset
        val cameraSpeedY = moveSensit * yOffset

        val moveZ = multi(normalize(cross(cameraFront, cameraUp)), cameraSpeedX)
        val moveX = multi(cameraFront, cameraSpeedY)

        cameraPos[0] += moveZ[0]
        cameraPos[1] += moveZ[1]
        cameraPos[2] += moveZ[2]

        cameraPos[0] -= moveX[0]
        cameraPos[1] -= moveX[1]
        cameraPos[2] -= moveX[2]

        lookAt()
    }

    fun onFront(yawOffset: Float, pitchOffset: Float) {
        yaw += yawOffset * frontSensit
        pitch += pitchOffset * frontSensit

        if (89f < pitch) pitch = 89.0
        if (pitch < -89f) pitch = -89.0

        val a = (cos(Math.toRadians(pitch)) * cos(Math.toRadians(yaw))).toFloat()
        val b = sin(Math.toRadians(pitch)).toFloat()
        val c = (cos(Math.toRadians(pitch)) * sin(Math.toRadians(yaw))).toFloat()

        val temp = floatArrayOf(a, b, c)

        cameraFront = normalize(temp)

        lookAt()
    }

    fun onZoom(zoomOffset: Float) {
        if (fovy in 1.0f..100.0f) {
            fovy -= zoomOffset * zoomSensit
        }
        if (fovy <= 1.0f) {
            fovy = 1.0f
        }
        if (100.0f <= fovy) {
            fovy = 100.0f
        }

        perspectiveCallback?.perspective(fovy, aspect, zNear, zFar)
    }

    fun cross(ai: FloatArray, ar: FloatArray): FloatArray {
        val result = FloatArray(3)

        result[0] = ai[1] * ar[2] - ai[2] * ar[1]
        result[1] = ai[2] * ar[0] - ai[0] * ar[2]
        result[2] = ai[0] * ar[1] - ai[2] * ar[0]

        return result
    }

    fun multi(arr: FloatArray, b: Float): FloatArray {
        val result = FloatArray(3)

        result[0] = arr[0] * b
        result[1] = arr[1] * b
        result[2] = arr[2] * b

        return result
    }

    /**
     * 归一化
     */
    fun normalize(arr: FloatArray): FloatArray {
        val len = sqrt(arr[0] * arr[0] + arr[1] * arr[1] + arr[2] * arr[2])

        arr[0] = arr[0] / len
        arr[1] = arr[1] / len
        arr[2] = arr[2] / len

        return arr
    }

    private fun lookAt() {
        lookAtCallback?.lookAt(
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
    }

    interface CameraPerspectiveCallback {
        fun perspective(fovy: Float, aspect: Float, zNear: Float, zFar: Float)
    }

    interface CameraLookAtCallback {
        fun lookAt(
            eyeX: Float,
            eyeY: Float,
            eyeZ: Float,
            centerX: Float,
            centerY: Float,
            centerZ: Float,
            upX: Float,
            upY: Float,
            upZ: Float
        )
    }
}