package me.excq.androidopengles20demo.toy02

class Fill02: MainActivity.IFill {
    override fun fill(
        x: Int,
        y: Int,
        minX: Int,
        maxX: Int,
        minY: Int,
        maxY: Int,
        callback: MainActivity.IFillCallback
    ) {
        println("IFill click Fill02")
    }
}