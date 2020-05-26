package me.excq.androidopengles20demo.toy02

import android.content.res.AssetManager
import android.graphics.PointF
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.MotionEvent
import me.excq.androidopengles20demo.BaseActivity
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.*

/**
 * 填充
 *
 * 填充采用扫描线种子填充法。
 *
 * 实现步骤如下：
 * 先获取一个种子（坐标点，可以是用户点击的坐标点或随机的一个坐标点或某封闭区域内的一个坐标点等等），
 * 然后向该种子的 -x 和 x 方向延伸，直到遇到与种子颜色不同的坐标点停止，两个方向分别记录为 xLeft 和 xRight。
 * 记录这两点坐标以备填充使用，并将该两点作为新的种子，
 * 重复以上动作，直到所有不同颜色的区域都遍历结束。
 * 以直线绘制方式绘制所有两两相对的点即可。
 */
class MainActivity : BaseActivity() {

    private lateinit var glView: GLSurfaceView
    private lateinit var renderer: MyRenderer

    private var menu1Status = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        glView = GLSurfaceView(this)
        renderer = MyRenderer(assets)

        glView.setEGLContextClientVersion(2)
        glView.setRenderer(renderer)
        setContentView(glView)

        glView.setOnTouchListener { _, event -> renderer.onTouch(event.action, event.x, event.y) }

        isMenu1Enable = true
        changedMenu1Status()
    }

    override fun onMenu1Click() {
        changedMenu1Status()
    }

    private fun changedMenu1Status() {
        when (menu1Status % 3) {
            0 -> {
                renderer.brush()
            }
            1 -> {
                renderer.fill()
            }
            else -> {
                renderer.clean()
            }
        }
        menu1Status++
        when (menu1Status % 3) {
            0 -> {
                setMenu1Text("画笔")
            }
            1 -> {
                setMenu1Text("填充")
            }
            else -> {
                setMenu1Text("清空")
            }
        }
    }

    class MyRenderer(val assets: AssetManager) : GLSurfaceView.Renderer {
        private lateinit var shader: Shader

        private var mPositionHandle: Int = 0

        private var vbo: IntBuffer? = null

        private var option = 0

        /**
         * 画笔功能变量
         */
        private var mSurfaceSize = PointF()
        private var action: Int = MotionEvent.ACTION_UP
        private var lastPoint = PointF()
        private var currPoint = PointF()
        private var div: Int = 1  // 插值距离
        private var vertexBuffer =
            ByteBuffer.allocateDirect(1024 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        private var vertexCount = 0

        /**
         * 填充功能变量
         */
        private val pixel =
            ByteBuffer.allocateDirect(3 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        private val color = FloatArray(3)
        private var xScanLeft = 0
        private var xScanRight = 0
        private var fillBuffer =
            ByteBuffer.allocateDirect(1024 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        private var fillCount = 0
        private var readPixelCount = 0

        fun onTouch(action: Int, x: Float, y: Float): Boolean {
            this.action = action
            currPoint.x = x
            currPoint.y = y
            return true
        }

        // 画笔
        fun brush() {
            option = 0
        }

        // 填充
        fun fill() {
            option = 1
        }

        // 清空
        fun clean() {
            option = 2
        }

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            mSurfaceSize.set(width / 2.0f, height / 2.0f)

            GLES20.glViewport(0, 0, width, height)
        }

        override fun onDrawFrame(gl: GL10?) {
            initRenderer()

            when (option) {
                0 -> updatePointerBuffer()
                1 -> updateFillArea()
                else -> resetPointerBuffer()
            }

            lastPoint.set(currPoint)

            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f)

            shader.use()

            if (0 <= vertexCount) {

                // 开始画笔绘制 ---------------------------------

                vertexBuffer.position(0)
                GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo!![0])
                GLES20.glBufferData(
                    GLES20.GL_ARRAY_BUFFER,
                    vertexCount * 2 * 4,
                    vertexBuffer,
                    GLES20.GL_DYNAMIC_DRAW
                )

                GLES20.glEnableVertexAttribArray(mPositionHandle)
                GLES20.glVertexAttribPointer(
                    mPositionHandle,
                    2,
                    GLES20.GL_FLOAT,
                    false,
                    2 * 4, // 一个顶点(x, y)
                    0
                )
                GLES20.glDrawArrays(
                    GLES20.GL_POINTS,
                    0,
                    vertexCount
                )
                GLES20.glDisableVertexAttribArray(mPositionHandle)
            }

            if (0 <= fillCount) {

                // 开始填充绘制 ----------------------------------

                fillBuffer.position(0)
                GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo!![1])
                GLES20.glBufferData(
                    GLES20.GL_ARRAY_BUFFER,
                    fillCount * 2 * 4,
                    fillBuffer,
                    GLES20.GL_DYNAMIC_DRAW
                )

                GLES20.glEnableVertexAttribArray(mPositionHandle)
                GLES20.glVertexAttribPointer(
                    mPositionHandle,
                    2,
                    GLES20.GL_FLOAT,
                    false,
                    2 * 4, // 一个顶点(x, y)
                    0
                )
                GLES20.glDrawArrays(
                    GLES20.GL_LINES,
                    0,
                    fillCount
                )
                GLES20.glDisableVertexAttribArray(mPositionHandle)
            }
        }

        private fun initRenderer() {
            if (!this::shader.isInitialized) {
                shader = Shader(
                    assets.open("toy02/vertex.glvs"),
                    assets.open("toy02/fragment.glfs")
                )

                mPositionHandle = shader.getAttribLocation("vPosition")
            }

            if (null == vbo) {
                vbo = IntBuffer.allocate(2)
                GLES20.glGenBuffers(2, vbo)
                GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo!![0])
                GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo!![1])
            }
        }

        private fun updateFillArea() {
            if (MotionEvent.ACTION_DOWN != action) return

            // ----------------------- 扫描线种子填充算法 ----------------------------
            // glReadPixels 方法里的 x, y 以左下角为原点，
            // 而 Android 坐标系以左上角为原点，
            // 所以此处需要对 y 轴进行一次反转。
            //
            // 根据用户的点击，获取原始种子和该种子的颜色，
            // 然后向两边探索边界，获取第一对种子。
            //
            // 之后这一对种子的左右两点，分别向各自的两边探索，
            // ---------------------------------------------------------------------

            val surfaceWidth = mSurfaceSize.x * 2
            val surfaceHeight = mSurfaceSize.y * 2
            val x = currPoint.x.toInt()
            val y = (surfaceHeight - currPoint.y).toInt()

            GLES20.glReadPixels(x, y, 1, 1, GLES20.GL_RGB, GLES20.GL_FLOAT, pixel)
            pixel.position(0)

            color[0] = pixel.get(0)
            color[1] = pixel.get(1)
            color[2] = pixel.get(2)

            var xLeft = x
            var xRight = x

            readPixelCount = 0

            // 向左扫描的第一个种子
            while (isSameColor(xLeft - 1, y) && 0 < xLeft) {
                xLeft--
            }
            fillBuffer = insertPointToBuffer(
                xLeft.toFloat(),
                surfaceHeight - y.toFloat(),
                fillCount++,
                fillBuffer
            )

            // 向右扫描的第一个种子
            while (isSameColor(xRight + 1, y) && xRight < surfaceWidth) {
                xRight++
            }
            fillBuffer = insertPointToBuffer(
                xRight.toFloat(),
                surfaceHeight - y.toFloat(),
                fillCount++,
                fillBuffer
            )

            println("updateFillArea01 $readPixelCount, $xLeft, $xRight")
            readPixelCount = 0

            // 两个种子分别向上和向下扫描

            var yUp = y
            xScanLeft = xLeft
            xScanRight = xRight

            while (xScanLeft <= xScanRight && yUp < surfaceHeight) {
                yUp++
                scanLine(yUp, 0, surfaceWidth.toInt(), surfaceHeight.toInt())
            }

            println("updateFillArea02 $readPixelCount, $xLeft, $xRight, $xScanLeft, $xScanRight")
            readPixelCount = 0

            var yDown = y
            xScanLeft = xLeft
            xScanRight = xRight

            while (xScanLeft <= xScanRight && 0 < yDown) {
                yDown--
                scanLine(yDown, 0, surfaceWidth.toInt(), surfaceHeight.toInt())
            }

            println("updateFillArea03 $readPixelCount, $xLeft, $xRight, $xScanLeft, $xScanRight")
        }

        private fun scanLine(
            y: Int,
            minX: Int,
            maxX: Int,
            surfaceHeight: Int
        ) {

            if (isSameColor(xScanLeft, y)) {
                while (isSameColor(xScanLeft - 1, y) && minX < xScanLeft) {
                    xScanLeft--
                }
            } else { // 表示种子位于区域边缘轮廓线里，需要反方向扫描
                while (!isSameColor(++xScanLeft, y) && xScanLeft < xScanRight) {
                }
            }
            fillBuffer = insertPointToBuffer(
                xScanLeft.toFloat(),
                surfaceHeight - y.toFloat(),
                fillCount++,
                fillBuffer
            )

            if (isSameColor(xScanRight, y)) {
                while (isSameColor(xScanRight + 1, y) && xScanRight < maxX) {
                    xScanRight++
                }
            } else { // 表示种子位于区域边缘轮廓线里，需要反方向扫描
                while (!isSameColor(--xScanRight, y) && xScanLeft < xScanRight) {
                }
            }
            fillBuffer = insertPointToBuffer(
                xScanRight.toFloat(),
                surfaceHeight - y.toFloat(),
                fillCount++,
                fillBuffer
            )
        }

        private fun isSameColor(x: Int, y: Int): Boolean {
            GLES20.glReadPixels(x, y, 1, 1, GLES20.GL_RGB, GLES20.GL_FLOAT, pixel)
            pixel.position(0)
            readPixelCount++
            return (color[0] == pixel.get(0) &&
                    color[1] == pixel.get(1) &&
                    color[2] == pixel.get(2))
        }

        private fun updatePointerBuffer() {
            if (currPoint.equals(lastPoint.x, lastPoint.y)) return
            when (action) {
                MotionEvent.ACTION_DOWN -> {
                }
                MotionEvent.ACTION_UP -> {
                    linearInterpolation()
                }
                MotionEvent.ACTION_MOVE -> {
                    linearInterpolation()
                }
            }

            vertexBuffer =
                insertPointToBuffer(currPoint.x, currPoint.y, vertexCount++, vertexBuffer)
        }

        private fun linearInterpolation() {
            val xLen = abs(currPoint.x - lastPoint.x)
            val yLen = abs(currPoint.y - lastPoint.y)
            val len = sqrt(xLen * xLen + yLen * yLen)
            val radians = asin(yLen / len)

            for (i in 1..(len / div).toInt()) {
                val tempX = cos(radians) * div * i * if (currPoint.x < lastPoint.x) 1 else -1
                val tempY = sin(radians) * div * i * if (currPoint.y < lastPoint.y) 1 else -1
                vertexBuffer = insertPointToBuffer(
                    currPoint.x + tempX,
                    currPoint.y + tempY,
                    vertexCount++,
                    vertexBuffer
                )
            }
        }

        private fun insertPointToBuffer(
            x: Float,
            y: Float,
            index: Int,
            buffer: FloatBuffer
        ): FloatBuffer {
            buffer.position(index * 2)
            buffer.put((x - mSurfaceSize.x) / mSurfaceSize.x)
            buffer.put((mSurfaceSize.y - y) / mSurfaceSize.y)

            return capacityExpansionIfNeeded(buffer)
        }

        private fun capacityExpansionIfNeeded(buffer: FloatBuffer): FloatBuffer {
            val limit = buffer.limit()
            val position = buffer.position()

            if (limit * 0.75 < position) {
                val temp = ByteBuffer.allocateDirect(limit * 2 * 4).order(ByteOrder.nativeOrder())
                    .asFloatBuffer()
                buffer.flip() // filp 方法将 limit 置为 position, 然后把 position 置为0, 这样下一行 put 时, temp 才会从 vertex 的位置 0 处拷贝
                temp.put(buffer)
                buffer.clear()

                return temp
            } else {
                return buffer
            }
        }

        private fun resetPointerBuffer() {
            vertexBuffer.clear()
            vertexCount = 0
            fillBuffer.clear()
            fillCount = 0
        }

    }
}