package me.excq.androidopengles20demo.chapters11

import android.view.MotionEvent
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MyRenderer02: MainActivity.Renderer() {

    override fun onDrawFrame(gl: GL10?) {
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
    }

    override fun onTouch(event: MotionEvent, action: Int, x: Float, y: Float) {
    }

    override fun destroy() {
    }

    external fun surfaceChanged(width: Int, height: Int)

    external fun drawFrame()
}