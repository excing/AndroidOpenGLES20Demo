package me.excq.androidopengles20demo.chapters11

import android.content.res.AssetManager
import android.view.MotionEvent
import me.excq.androidopengles20demo.chapters06.Shader
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MyRenderer02(val assets: AssetManager, private val fontPath: String) :
    MainActivity.Renderer() {

    private lateinit var shader: Shader

    override fun onDrawFrame(gl: GL10?) {
        initShader()
        drawFrame()
    }

    private fun initShader() {
        if (!this::shader.isInitialized) {
            shader = Shader(
                assets.open("chapters11/vertex02.glvs"),
                assets.open("chapters11/fragment02.glfs")
            )
            init(fontPath, shader.programID, "G")
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        surfaceChanged(width, height)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
    }

    override fun onTouch(event: MotionEvent, action: Int, x: Float, y: Float) {
    }

    override fun destroy() {
    }

    private external fun init(fontPath: String, programID: Int, text: String)

    private external fun surfaceChanged(width: Int, height: Int)

    private external fun drawFrame()
}