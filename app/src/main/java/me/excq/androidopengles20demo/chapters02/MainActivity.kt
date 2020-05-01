package me.excq.androidopengles20demo.chapters02

import android.annotation.SuppressLint
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import me.excq.androidopengles20demo.BaseActivity
import me.excq.androidopengles20demo.WebActivity
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.random.Random

class MainActivity : BaseActivity() {
    private lateinit var glSurfaceView: GLSurfaceView
    private lateinit var myRenderer: MyRenderer

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        super.onCreate(savedInstanceState)

        glSurfaceView = GLSurfaceView(this)
        glSurfaceView.setEGLContextClientVersion(2)

        setContentView(glSurfaceView)

        myRenderer = MyRenderer(1f, 1f, 1f, 1f)
        glSurfaceView.setRenderer(myRenderer)
        glSurfaceView.setOnTouchListener(MyTouch(myRenderer))
    }

    override fun onMenu2Click() {
        WebActivity.open(this, "https://github.com/excing/AndroidOpenGLES20Demo/tree/master/app/src/main/java/me/excq/androidopengles20demo/chapters2");
    }

    override fun onResume() {
        super.onResume()
        glSurfaceView.onResume()
    }

    override fun onPause() {
        super.onPause()
        glSurfaceView.onPause()
    }

    private class MyRenderer(var r: Float, var b: Float, var g: Float, var a: Float) : GLSurfaceView.Renderer {

        private val vertexShaderCode = "" +
                "attribute vec4 vPosition;" +
                "void main() {" +
                "  gl_Position = vPosition;" +
                "}";

        private val fragmentShaderCode = "" +
                "precision mediump float;" +
                "uniform vec4 vColor;" +
                "void main() {" +
                "  gl_FragColor = vColor;" +
                "}";

        private val color = floatArrayOf(
            0.63671875f, 0.76953125f, 0.22265625f, 1.0f
        )

        private val points = floatArrayOf(
            0f, 0.5f, 0.0f,
            -0.5f, -0.5f, 0.0f,
            0.5f, -0.5f, 0.0f
        )

        private var mProgram: Int = 0

        private var vertexBuffer: FloatBuffer

        init {
            /**
             * 创建缓冲存区对象时，必须使用 ByteBuffer.allocateDirect 方法，
             * 此缓冲区对象是发送给 OpenGL 使用的，而 OpenGL 无法读取 JVM 的缓存对象，
             * 因此使用 ByteBuffer.allocate 方法生成的缓存区对象，OpenGL 会直接报错。
             * 错误内容大概如下：
             *
               JNI DETECTED ERROR IN APPLICATION: jarray was NULL
                   in call to GetPrimitiveArrayCritical
                   from void android.opengl.GLES20.glBufferData(int, int, java.nio.Buffer, int)
               "GLThread 1122" prio=5 tid=14 Runnable
                 | group="main" sCount=0 dsCount=0 flags=0 obj=0x9d72e0b8 self=0x95f1ee00
                 | sysTid=804 nice=0 cgrp=default sched=0/0 handle=0x7d793970
                 | state=R schedstat=( 72162360 3964558 40 ) utm=0 stm=7 core=0 HZ=100
                 | stack=0x7d690000-0x7d692000 stackSize=1042KB
                 | held mutexes= "mutator lock"(shared held)
                 native: #00 pc 004151b6  /system/lib/libart.so (art::DumpNativeStack(std::__1::basic_ostream<char, std::__1::char_traits<char>>&, int, BacktraceMap*, char const*, art::ArtMethod*, void*, bool)+198)
                 native: #01 pc 0051034e  /system/lib/libart.so (art::Thread::DumpStack(std::__1::basic_ostream<char, std::__1::char_traits<char>>&, bool, BacktraceMap*, bool) const+382)
                 native: #02 pc 0050b603  /system/lib/libart.so (art::Thread::Dump(std::__1::basic_ostream<char, std::__1::char_traits<char>>&, bool, BacktraceMap*, bool) const+83)
                 native: #03 pc 0031a720  /system/lib/libart.so (art::JavaVMExt::JniAbort(char const*, char const*)+1088)
                 native: #04 pc 0031ab91  /system/lib/libart.so (art::JavaVMExt::JniAbortV(char const*, char const*, char*)+113)
                 native: #05 pc 000d5f77  /system/lib/libart.so (art::(anonymous namespace)::ScopedCheck::AbortF(char const*, ...)+71)
                 native: #06 pc 000d52af  /system/lib/libart.so (art::(anonymous namespace)::ScopedCheck::CheckArray(art::ScopedObjectAccess&, _jarray*)+287)
                 native: #07 pc 000d45c8  /system/lib/libart.so (art::(anonymous namespace)::ScopedCheck::CheckPossibleHeapValue(art::ScopedObjectAccess&, char, art::(anonymous namespace)::JniValueType)+440)
                 native: #08 pc 000d3a5b  /system/lib/libart.so (art::(anonymous namespace)::ScopedCheck::Check(art::ScopedObjectAccess&, bool, char const*, art::(anonymous namespace)::JniValueType*)+811)
                 native: #09 pc 000cfe42  /system/lib/libart.so (art::(anonymous namespace)::CheckJNI::GetPrimitiveArrayCritical(_JNIEnv*, _jarray*, unsigned char*)+930)
                 native: #10 pc 00094335  /system/lib/libandroid_runtime.so (android_glBufferData__IILjava_nio_Buffer_2I(_JNIEnv*, _jobject*, int, int, _jobject*, int)+165)
             *
             * 可以看到，OpenGL 在 调用 glBufferData 时出现了错误，
             * 所以这里一定不能不能忘记了，这可是花了我 1 个多小时才找到的问题，
             * 网上也没有这方面的提示，都是直接写，没有讲原因。
             * 结果有一次（在 MyRenderer10 一节），手写的时候写成了 allocate 方法，结果就报错了，
             * 这还不好找，错误日志里就只说初始化缓存区对象（初始化在 initBuffer 方法里执行的）那里有问题，
             * 然后就是上面那一堆错误，也看不懂啊，网上也没有这方面的案例，
             * 还去着色器脚本那里找 bug。
             * 后面实在没办法，把之前的代码拷贝过来，只改参数，才发现了这个 bug。
             * 泪目~
             */
            vertexBuffer = ByteBuffer.allocateDirect(points.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
            vertexBuffer.put(points)
            /**
             * 一定一定要把位置重置为 0
             */
            vertexBuffer.position(0)
        }

        override fun onDrawFrame(gl: GL10?) {
            initProgram()

            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            GLES20.glClearColor(r, b, g, a)

            GLES20.glUseProgram(mProgram)

            val mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition")
            GLES20.glEnableVertexAttribArray(mPositionHandle)
            GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false, 12, vertexBuffer)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES,  0, 3)

            val mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor")
            GLES20.glUniform4fv(mColorHandle, 1, color, 0)

            GLES20.glDisableVertexAttribArray(mPositionHandle)
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            GLES20.glViewport(0, 0, width, height)
        }

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        }

        fun initProgram() {
            if (0 == mProgram) {
                mProgram = GLES20.glCreateProgram()

                val vertexShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER)
                val fragmentShader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER)

                GLES20.glShaderSource(vertexShader, vertexShaderCode)
                GLES20.glCompileShader(vertexShader)
                GLES20.glAttachShader(mProgram, vertexShader)

                GLES20.glShaderSource(fragmentShader, fragmentShaderCode)
                GLES20.glCompileShader(fragmentShader)
                GLES20.glAttachShader(mProgram, fragmentShader)

                GLES20.glLinkProgram(mProgram)
            }
        }
    }

    private class MyTouch(
        var myRenderer: MyRenderer
    ) : View.OnTouchListener {
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
            myRenderer.r = random.nextFloat()
            myRenderer.g = random.nextFloat()
            myRenderer.b = random.nextFloat()
            myRenderer.a = random.nextFloat()
        }

    }
}