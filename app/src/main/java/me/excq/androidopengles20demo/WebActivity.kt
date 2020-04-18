package me.excq.androidopengles20demo

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.View.OnLongClickListener
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebView.HitTestResult
import android.webkit.WebViewClient

/**
 * Web 界面浏览
 */
open class WebActivity : BaseActivity() {
    private lateinit var webView: WebView
    private lateinit var fromUri: Uri

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webView = WebView(this)
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()
        webView.isLongClickable = true
        webView.setOnLongClickListener(SimpleLoneClick())
        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true

        setContentView(webView)

        isMenu1Enable = false
        isMenu2Enable = false

        val uri = if (null != uRI) uRI else intent.data

        if (null != uri) webView.loadUrl(uri.toString())

        if (uri != null) {
            fromUri = uri
        } else {
            finish()
        }
    }

    protected open val uRI: Uri?
        get() = null

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onPause() {
        webView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }

    override fun onBackPressed() {
        val webOriginalUrl = webView.originalUrl
        if (TextUtils.isEmpty(webOriginalUrl) || fromUri.toString() == webOriginalUrl) {
            super.onBackPressed()
        } else {
            webView.goBack()
        }
    }

    private class SimpleLoneClick : OnLongClickListener {
        override fun onLongClick(v: View): Boolean {
            val webview = v as WebView
            val result = webview.hitTestResult
            if (result.type == HitTestResult.SRC_ANCHOR_TYPE) {
                openDialog(v.getContext(), result.extra)
                return false
            }
            return false
        }
    }

    companion object {
        fun open(from: Context, uri: String?) {
            val intent = Intent(from, WebActivity::class.java)
            intent.data = Uri.parse(uri)
            from.startActivity(intent)
        }

        private fun openDialog(context: Context, url: String?) {
            val builder: AlertDialog.Builder
            builder = if (Build.VERSION_CODES.LOLLIPOP <= Build.VERSION.SDK_INT) {
                AlertDialog.Builder(
                    context,
                    android.R.style.Theme_Material_Light_Dialog
                )
            } else {
                AlertDialog.Builder(context)
            }
            builder.setTitle(context.getString(R.string.string_open_browser))
                .setPositiveButton(
                    context.getString(R.string.string_sure)
                ) { dialog, which ->
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse(url)
                    context.startActivity(intent)
                }.show()
        }
    }
}