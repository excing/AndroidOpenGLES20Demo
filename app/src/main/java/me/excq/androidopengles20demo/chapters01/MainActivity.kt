package me.excq.androidopengles20demo.chapters01

import android.annotation.SuppressLint
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import me.excq.androidopengles20demo.BaseActivity
import me.excq.androidopengles20demo.WebActivity
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.random.Random

class MainActivity : BaseActivity() {
    private lateinit var glSurfaceView: GLSurfaceView
    private lateinit var myRenderer: MyRenderer

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        glSurfaceView = GLSurfaceView(this)
        // 表明使用 es 2.0 客户端版本
        glSurfaceView.setEGLContextClientVersion(2)

        setContentView(glSurfaceView)

        myRenderer = MyRenderer(1f, 1f, 1f, 1f)
        glSurfaceView.setRenderer(myRenderer)
//        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
        glSurfaceView.setOnTouchListener(MyTouch(myRenderer, glSurfaceView))
    }

    override fun onMenu2Click() {
        WebActivity.open(this, "https://github.com/excing/AndroidOpenGLES20Demo/tree/master/app/src/main/java/me/excq/androidopengles20demo/chapters1");
    }

    override fun onResume() {
        super.onResume()
        glSurfaceView.onResume()
    }

    override fun onPause() {
        super.onPause()
        glSurfaceView.onPause()
    }

    private class MyRenderer(
        var r: Float,
        var g: Float,
        var b: Float,
        var a: Float
    ) : GLSurfaceView.Renderer {
        override fun onDrawFrame(gl: GL10?) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            GLES20.glClearColor(r, g, b, a);
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        }

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        }
    }

    private class MyTouch(var renderer: MyRenderer, var glSurfaceView: GLSurfaceView) : View.OnTouchListener {
        override fun onTouch(v: View?, event: MotionEvent?): Boolean {
            if (event != null) {
                when(event.action) {
                    MotionEvent.ACTION_UP -> touchUp()
                }
            }

            return true
        }

        private fun touchUp() {
            val random = Random(System.currentTimeMillis())
            renderer.r = random.nextFloat()
            renderer.g = random.nextFloat()
            renderer.b = random.nextFloat()
            renderer.a = random.nextFloat()

            glSurfaceView.requestRender()
        }
    }
}
