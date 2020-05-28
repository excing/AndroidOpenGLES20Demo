package me.excq.androidopengles20demo.toy02

import android.content.res.AssetManager
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import me.excq.androidopengles20demo.BaseActivity
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

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
    private lateinit var rendererProxy: MyRenderer
    private lateinit var myRendererList: Array<IRenderer>

    private var menu1Status = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        glView = GLSurfaceView(this)
        myRendererList = arrayOf(
            MyRenderer01(assets),
            MyRenderer02(assets)
        )

        rendererProxy = MyRenderer(assets, myRendererList[0])

        glView.setEGLContextClientVersion(2)
        glView.setRenderer(rendererProxy)
        setContentView(glView)

        glView.setOnTouchListener { v, event -> rendererProxy.onTouch(v, event) }

        isSpinnerEnable = true
        isMenu1Enable = true
        changedMenu1Status()
    }

    override fun getSpinnerData(): Array<String> {
        return arrayOf(
            "01",
            "02"
//            "03",
//            "04",
//            "05",
//            "06",
//            "07",
//            "08"
        )
    }

    override fun onSpinnerSelected(position: Int) {
        rendererProxy.updateSurfaceIfChanged(myRendererList[position])
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
        rendererProxy.onDestroy()
    }

    override fun onMenu1Click() {
        changedMenu1Status()
    }

    private fun changedMenu1Status() {
        rendererProxy.pointStatusChanged(menu1Status % 3)
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

    interface IRenderer: GLSurfaceView.Renderer, View.OnTouchListener {
        fun pointStatusChanged(status: Int)
        fun onDestroy()
    }

    class MyRenderer(val assets: AssetManager, var renderer: IRenderer) : IRenderer {
        private var width: Int = 0
        private var height: Int = 0

        override fun pointStatusChanged(status: Int) {
            renderer.pointStatusChanged(status)
        }

        override fun onDestroy() {
            renderer.onDestroy()
        }

        override fun onDrawFrame(gl: GL10?) {
            renderer.onDrawFrame(gl)
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            this.width = width
            this.height = height
            renderer.onSurfaceChanged(gl, width, height)
        }

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            renderer.onSurfaceCreated(gl, config)
        }

        override fun onTouch(v: View?, event: MotionEvent?): Boolean {
            return renderer.onTouch(v, event)
        }

        fun updateSurfaceIfChanged(iRenderer: IRenderer) {
            renderer.onDestroy()
            renderer = iRenderer
            renderer.onSurfaceChanged(null, width, height)
        }

    }
}