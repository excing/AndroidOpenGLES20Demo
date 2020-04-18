package me.excq.androidopengles20demo

import android.net.Uri

/**
 * 前言
 */
class PrefaceActivity : WebActivity() {

    override val uRI: Uri
        get() = Uri.parse("https://github.com/excing/OpenGLES20Simple")

    override fun onMenu2Click() {
        open(this, "https://github.com/excing/OpenGLES20Simple")
    }
}
