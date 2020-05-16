package me.excq.androidopengles20demo.chapters10

import android.annotation.SuppressLint
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import me.excq.androidopengles20demo.BaseActivity
import me.excq.androidopengles20demo.WebActivity
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MainActivity : BaseActivity() {
    private lateinit var glSurfaceView: GLSurfaceView
    private lateinit var rendererProxy: RendererProxy
    private lateinit var myRendererList: Array<Renderer>

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        myRendererList = arrayOf(
            MyRenderer01(assets),
            MyRenderer02(assets),
            MyRenderer03(assets),
            MyRenderer04(assets),
            MyRenderer05(assets),
            MyRenderer06(assets),
            MyRenderer07(assets),
            MyRenderer08(assets)
        )

        glSurfaceView = GLSurfaceView(this)
        glSurfaceView.setEGLContextClientVersion(2)

        setContentView(glSurfaceView)

        rendererProxy = RendererProxy(myRendererList[0])
        glSurfaceView.setOnTouchListener(MyTouch(rendererProxy))
        glSurfaceView.setRenderer(rendererProxy)

        isSpinnerEnable = true
    }

    override fun getSpinnerData(): Array<String> {
        return arrayOf(
            "01",
            "02",
            "03",
            "04",
            "05",
            "06",
            "07",
            "08"
        )
    }

    override fun onSpinnerSelected(position: Int) {
        rendererProxy.updateSurfaceIfChanged(myRendererList[position])
    }

    override fun onMenu2Click() {
        WebActivity.open(
            this,
            "https://github.com/excing/AndroidOpenGLES20Demo/tree/master/app/src/main/java/me/excq/androidopengles20demo/chapters10"
        )
    }

    override fun onResume() {
        super.onResume()
        glSurfaceView.onResume()
    }

    override fun onPause() {
        super.onPause()
        glSurfaceView.onPause()
    }

    override fun onDestroy() {
        rendererProxy.destroy()
        myRendererList.all { renderer -> renderer.destroy(); true }
        super.onDestroy()
    }

    abstract class Renderer : GLSurfaceView.Renderer, ITouch, Destroy

    /**
     * 取消了 RGBA 接口，Touch 事件由 Renderer 内部自己处理
     */
    interface ITouch {
        fun onTouch(
            event: MotionEvent,
            action: Int,
            x: Float,
            y: Float
        )
    }

    interface Destroy {
        fun destroy()
    }

    private class MyTouch(
        var touch: ITouch
    ) : View.OnTouchListener {
        override fun onTouch(v: View?, event: MotionEvent?): Boolean {
            if (event != null) {
                touch.onTouch(event, event.action, event.x, event.y)
            }
            return true
        }
    }

    private class RendererProxy(var renderer: Renderer) : Renderer() {
        private var width = 0
        private var height = 0

        fun updateSurfaceIfChanged(newRenderer: Renderer) {
            this.renderer.destroy()
            this.renderer = newRenderer
            this.renderer.onSurfaceChanged(null, width, height)
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

        override fun onTouch(
            event: MotionEvent,
            action: Int,
            x: Float,
            y: Float
        ) {
            renderer.onTouch(event, action, x, y)
        }

        override fun destroy() {
            renderer.destroy()
        }

    }
}