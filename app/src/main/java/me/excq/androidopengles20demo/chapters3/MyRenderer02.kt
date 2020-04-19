package me.excq.androidopengles20demo.chapters3

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * 使用 VBO 绘制
 * VBO（Vertex Buffer Object）是指顶点缓冲区对象，而EBO（Element Buffer Object）是指图元索引缓冲区对象。
 * VAO 和 EBO 实际上是对同一类 Buffer 按照用途的不同称呼。
 *
 * OpenGL ES 编程中，用于绘制的顶点数组数据首先保存在 CPU 内存中，
 * 在调用 `glDrawArrays` 或者 `glDrawElements` 等进行绘制时，
 * 需要将顶点数组数据从 CPU 内存拷贝到 GPU 内存（显存）
 *
 * 我们不是每次都要去进行内存拷贝，比如一些固定的数组数据，
 * 如果可以在显存中缓存这些数据，就可以在很大程度上降低内存拷贝带来的开销。
 *
 * VBO 和 EBO 的出现就是为了解决这个问题。
 * （有一说：VBO 和 EBO 是 OpenGL ES 3.0 的特性，但在 Android 上，OpenGL ES 2.0 也可以用。）
 * VBO 和 EBO 的作用是在显存中提前开辟好一块内存，用于缓存顶点数据或者图元索引数据，
 * 从而避免每次䌊时的 CPU 与 GPU 之间的内存拷贝，可以改进渲染性能，降低内存带宽和功耗。
 */
class MyRenderer02(var r: Float, var b: Float, var g: Float, var a: Float) :
    GLSurfaceView.Renderer, MainActivity.RGBA {

    private val vertexShaderCode = "" +
            "attribute vec4 vPosition;" +
            "attribute vec4 vColor;" +
            "varying vec4 outColor;" +
            "void main() {" +
            "  gl_Position = vPosition;" +
            "  outColor = vColor;" + // 输入 color 作为输出
            "}";

    private val fragmentShaderCode = "" +
            "precision mediump float;" +
            "varying vec4 outColor;" +
            "void main() {" +
            "  gl_FragColor = outColor;" + // 片段着色器的输入需与顶点着色器的输入定义保持一致
            "}";

    // 每个 float 大小 4，一个顶点 3 个坐标值和 3 个颜色值，共 6 个 float，大小 24。
    // 每个顶点之间有 6 个 float，故偏移量为 24，
    // 第一个颜色的位置在第一个顶点之后，即 3 个 float 之后，偏移为 4 * 3 = 12
    private val points = floatArrayOf(
        // 坐标            // 颜色
        0.0f, 0.5f, 0.0f, 1.0f, 0.0f, 0.0f,
        -0.5f, -0.5f, 0.0f, 0.0f, 1.0f, 0.0f,
        0.5f, -0.5f, 0.0f, 0.0f, 0.0f, 1.0f
    )

    private var mProgram: Int = 0
    private var mPositionHandle: Int = 0
    private var mColorHandle: Int = 0

    private var vertexBuffer: FloatBuffer

    init {
        vertexBuffer = ByteBuffer.allocateDirect(points.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer();
        vertexBuffer.put(points)
        vertexBuffer.position(0)
    }

    override fun onDrawFrame(gl: GL10?) {
        initProgram()

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glClearColor(r, b, g, a)

        GLES20.glUseProgram(mProgram)

        GLES20.glEnableVertexAttribArray(mPositionHandle)
        GLES20.glVertexAttribPointer(
            mPositionHandle,
            3,                      // 顶点总数
            GLES20.GL_FLOAT,
            false,
            24,                     // 每个顶点之间的偏移量
            0                       // 该属性的第一个值所在缓存位置
        )

        GLES20.glEnableVertexAttribArray(mColorHandle)
        GLES20.glVertexAttribPointer(
            mColorHandle, 3,
            GLES20.GL_FLOAT,
            false,
            24,
            12
        )

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3)

        GLES20.glDisableVertexAttribArray(mPositionHandle)
        GLES20.glDisableVertexAttribArray(mColorHandle)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        val params = IntBuffer.allocate(4)
        GLES20.glGetIntegerv(GLES20.GL_MAX_VERTEX_ATTRIBS, params)
        println("GLES20: params: " + params[0])
    }

    fun initProgram() {
        if (0 == mProgram) {
            mProgram = GLES20.glCreateProgram()

            val vertexShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER)
            val fragmentShader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER)

            GLES20.glShaderSource(vertexShader, vertexShaderCode)
            GLES20.glCompileShader(vertexShader)

            // 着色器对象链接到程序对象
            GLES20.glAttachShader(mProgram, vertexShader)

            GLES20.glShaderSource(fragmentShader, fragmentShaderCode)
            GLES20.glCompileShader(fragmentShader)
            GLES20.glAttachShader(mProgram, fragmentShader)

            GLES20.glLinkProgram(mProgram)

            // 在把着色器对象链接到程序对象以后，记得删除着色器对象，我们不再需要它们了

            GLES20.glDeleteShader(vertexShader)
            GLES20.glDeleteShader(fragmentShader)

            /*
             链接结束后，获取着色器变量
             */

            mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition")
            mColorHandle = GLES20.glGetAttribLocation(mProgram, "vColor")

            // 创建和更新 VBO

            val vbo = IntBuffer.allocate(1)
            GLES20.glGenBuffers(1, vbo)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo[0])

            // @see https://www.khronos.org/registry/OpenGL-Refpages/gl2.1/xhtml/glBufferData.xml
            GLES20.glBufferData(
                GLES20.GL_ARRAY_BUFFER, // 缓存类型，`GL_ARRAY_BUFFER` 标志表示用于保存顶点数组
                points.size * 4,        // 缓存大小
                vertexBuffer,           // 缓存
                GLES20.GL_STATIC_DRAW   // 指定数据的预期使用方式。`GL_STATIC_DRAW` 表示缓存由应用程序修改，并用作GL绘图和图像规范命令的源。
            )
        }
    }

    override fun updateBackground(r: Float, b: Float, g: Float, a: Float) {
        this.r = r
        this.g = g
        this.b = b
        this.a = a
    }
}
