package me.excq.androidopengles20demo.chapters4

import android.annotation.SuppressLint
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import me.excq.androidopengles20demo.BaseActivity
import me.excq.androidopengles20demo.WebActivity
import kotlin.random.Random

class MainActivity : BaseActivity() {
    private lateinit var glSurfaceView: GLSurfaceView
    private lateinit var myRenderer: Renderer

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        super.onCreate(savedInstanceState)

        glSurfaceView = GLSurfaceView(this)
        glSurfaceView.setEGLContextClientVersion(2)

        setContentView(glSurfaceView)

        myRenderer = MyRenderer04(1f, 1f, 1f, 1f)
        glSurfaceView.setRenderer(myRenderer)
        glSurfaceView.setOnTouchListener(MyTouch(myRenderer as RGBA))
    }

    override fun onMenu2Click() {
        WebActivity.open(
            this,
            "https://github.com/excing/AndroidOpenGLES20Demo/tree/master/app/src/main/java/me/excq/androidopengles20demo/chapters4"
        );
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
        myRenderer.destroy()
        super.onDestroy()
    }

    abstract class Renderer : GLSurfaceView.Renderer, RGBA, Destroy

    interface RGBA {
        fun updateBackground(r: Float, b: Float, g: Float, a: Float)
    }

    interface Destroy {
        fun destroy()
    }

    private class MyTouch(
        var rgba: RGBA
    ) : View.OnTouchListener {
        override fun onTouch(v: View?, event: MotionEvent?): Boolean {
            if (event != null) {
                when (event.action) {
                    MotionEvent.ACTION_UP -> touchUp()
                }
            }
            return true
        }

        private fun touchUp() {
            val random = Random(System.currentTimeMillis())

            rgba.updateBackground(
                random.nextFloat(),
                random.nextFloat(),
                random.nextFloat(),
                random.nextFloat()
            )
        }

    }
}