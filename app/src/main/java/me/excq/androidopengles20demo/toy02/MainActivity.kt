package me.excq.androidopengles20demo.toy02

import android.content.res.AssetManager
import android.graphics.PointF
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.MotionEvent
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.excq.androidopengles20demo.BaseActivity
import me.excq.androidopengles20demo.WebActivity
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

    private lateinit var iFillList: Array<IFill>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        glView = GLSurfaceView(this)
        iFillList = arrayOf(Fill01(4), Fill02())
        renderer = MyRenderer(assets, iFillList[0])

        glView.setEGLContextClientVersion(2)
        glView.setRenderer(renderer)
        setContentView(glView)

        glView.setOnTouchListener { _, event -> renderer.onTouch(event.action, event.x, event.y) }

        isSpinnerEnable = true
        isMenu1Enable = true
        changedMenu1Status()
    }

    override fun onMenu2Click() {
        WebActivity.open(
            this,
            "https://github.com/excing/AndroidOpenGLES20Demo/tree/master/app/src/main/java/me/excq/androidopengles20demo/toy02"
        )
    }

    override fun getSpinnerData(): Array<String> {
        return arrayOf(
            "01",
            "02"
        )
    }

    override fun onSpinnerSelected(position: Int) {
        renderer.iFill = iFillList[position]
    }

    override fun onResume() {
        super.onResume()
        glView.onResume()
    }

    override fun onPause() {
        super.onPause()
        glView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        renderer.onDestroy()
    }

    override fun onMenu1Click() {
        changedMenu1Status()
    }

    private fun changedMenu1Status() {
        renderer.pointStatusChanged(menu1Status % 3)
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

    interface IFillCallback {
        fun fullCallback(x: Float, y: Float)
    }

    /**
     * 填充算法实现接口，
     * 该接口应在 GLThread 线程内调用。
     */
    interface IFill {
        fun fill(
            x: Int,
            y: Int,
            minX: Int,
            maxX: Int,
            minY: Int,
            maxY: Int,
            callback: IFillCallback
        )
    }

    class MyRenderer(val assets: AssetManager, var iFill: IFill) : GLSurfaceView.Renderer,
        IFillCallback {
        private lateinit var shader: Shader

        private var mPositionHandle: Int = -1
        private var mPointSizeHandle: Int = -1
        private var mColorHandle: Int = -1

        private var vbo: IntBuffer? = null

        private var option = 0

        /**
         * 画笔功能变量
         */
        private var mSurfaceSize = PointF()
        private var action: Int = MotionEvent.ACTION_UP
        private var lastPoint = PointF()
        private var currPoint = PointF()
        private var div: Int = 4  // 插值距离
        private var vertexBuffer =
            ByteBuffer.allocateDirect(1024 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        private var vertexCount = 0

        /**
         * 填充功能变量
         */
        private var fillBuffer =
            ByteBuffer.allocateDirect(1024 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        private var fillCount = 0

        private var on = true
        private var latestFrameCount = 0
        private var lastFrameCount = 0

        init {
            GlobalScope.launch {
                while (on) {
                    delay(1000)
                    println("frameTime: ${latestFrameCount - lastFrameCount}")
                    lastFrameCount = latestFrameCount
                }
            }
        }

        fun onDestroy() {
            on = false
            resetPointerBuffer()
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo!![0])
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo!![1])
        }

        fun onTouch(action: Int, x: Float, y: Float): Boolean {
            this.action = action
            currPoint.x = x
            currPoint.y = y
            return true
        }

        /**
         * 画笔状态改变，
         * 0 表示画笔;
         * 1 表示填充;
         * 2 表示清除;
         */
        fun pointStatusChanged(status: Int) {
            option = status
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

            GLES20.glUniform1f(mPointSizeHandle, div.toFloat())

            if (0 < vertexCount) {

                // 开始画笔绘制 ---------------------------------

                GLES20.glUniform3f(mColorHandle, 0.0f, 0.0f, 0.0f)

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

            if (0 < fillCount) {

                // 开始填充绘制 ----------------------------------

                GLES20.glUniform3f(mColorHandle, 1.0f, 0.5f, 0.3f)

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

            latestFrameCount++
        }

        private fun initRenderer() {
            if (!this::shader.isInitialized) {
                shader = Shader(
                    assets.open("toy02/vertex.glvs"),
                    assets.open("toy02/fragment.glfs")
                )

                mPositionHandle = shader.getAttribLocation("vPosition")
                mPointSizeHandle = shader.getUniformLocation("vPointSize")
                mColorHandle = shader.getUniformLocation("vColor")
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
            action = MotionEvent.ACTION_CANCEL

            val surfaceWidth = mSurfaceSize.x.toInt() * 2
            val surfaceHeight = mSurfaceSize.y.toInt() * 2

            iFill.fill(
                currPoint.x.toInt(),
                surfaceHeight - currPoint.y.toInt(),
                0,
                surfaceWidth,
                0,
                surfaceHeight,
                this
            )
        }

        override fun fullCallback(x: Float, y: Float) {
            fillBuffer = insertPointToBuffer(
                x,
                y,
                fillCount++,
                fillBuffer
            )
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