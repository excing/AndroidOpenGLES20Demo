package me.excq.androidopengles20demo.chapters11

import java.nio.ByteBuffer

data class FreeTypeBitmap(
    var width: Int,
    var height: Int,
    var left: Int,
    var top: Int,
    val advance: Int,
    var buffer: ByteBuffer
)