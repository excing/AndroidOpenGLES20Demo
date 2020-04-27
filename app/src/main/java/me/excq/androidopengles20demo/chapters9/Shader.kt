package me.excq.androidopengles20demo.chapters9

import android.opengl.GLES20
import java.io.InputStream
import java.nio.IntBuffer
import java.nio.charset.Charset

data class Shader(var vertexStream: InputStream, var fragmentStream: InputStream) {
    var programID: Int = 0

    init {
        val vertexCode = vertexStream.readBytes().toString(Charset.defaultCharset())
        val fragmentCode = fragmentStream.readBytes().toString(Charset.defaultCharset())

        vertexStream.close()
        fragmentStream.close()

        programID = GLES20.glCreateProgram()

        val vertexShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER)
        val fragmentShader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER)

        val success: IntBuffer = IntBuffer.allocate(1)

        GLES20.glShaderSource(vertexShader, vertexCode)
        GLES20.glCompileShader(vertexShader)

        GLES20.glGetShaderiv(vertexShader, GLES20.GL_COMPILE_STATUS, success)
        if (0 == success[0]) {
            throw IllegalArgumentException(
                "Compile vertex Shader has error: \n" +
                        GLES20.glGetShaderInfoLog(
                            vertexShader
                        )
            )
        }

        GLES20.glShaderSource(fragmentShader, fragmentCode)
        GLES20.glCompileShader(fragmentShader)

        GLES20.glGetShaderiv(vertexShader, GLES20.GL_COMPILE_STATUS, success)
        if (0 == success[0]) {
            throw IllegalArgumentException(
                "Compile fragment shader has error: \n" +
                        GLES20.glGetShaderInfoLog(
                            fragmentShader
                        )
            )
        }

        GLES20.glAttachShader(programID, vertexShader)
        GLES20.glAttachShader(programID, fragmentShader)
        GLES20.glLinkProgram(programID)

        GLES20.glGetProgramiv(programID, GLES20.GL_LINK_STATUS, success)
        if (0 == success[0]) {
            throw IllegalArgumentException(
                "Link OpenGL program has error: \n" +
                        GLES20.glGetProgramInfoLog(programID)
            )
        }

        GLES20.glDeleteShader(vertexShader)
        GLES20.glDeleteShader(fragmentShader)
    }

    fun use() {
        GLES20.glUseProgram(programID)
    }

    fun getAttribLocation(name: String): Int {
        return GLES20.glGetAttribLocation(programID, name)
    }

    fun getUniformLocation(name: String): Int {
        return GLES20.glGetUniformLocation(programID, name)
    }
}