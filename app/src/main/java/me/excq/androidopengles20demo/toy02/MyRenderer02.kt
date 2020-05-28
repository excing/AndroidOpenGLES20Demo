package me.excq.androidopengles20demo.toy02

import android.content.res.AssetManager
import android.view.MotionEvent
import android.view.View
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * 优化 MyRenderer01 实现类的算法，对画笔轮廓边缘进行填充。
 */
class MyRenderer02(val assets: AssetManager): MainActivity.IRenderer {
    override fun pointStatusChanged(status: Int) {
        TODO("Not yet implemented")
    }

    override fun onDestroy() {
        TODO("Not yet implemented")
    }

    override fun onDrawFrame(gl: GL10?) {
        TODO("Not yet implemented")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        TODO("Not yet implemented")
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        TODO("Not yet implemented")
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        TODO("Not yet implemented")
    }
}