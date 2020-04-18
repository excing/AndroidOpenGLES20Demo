package me.excq.androidopengles20demo

import android.annotation.SuppressLint
import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * 基础 [Activity]
 */
@SuppressLint("Registered")
open class BaseActivity : Activity() {
    private lateinit var root: FrameLayout
    private lateinit var logcatView: TextView
    private lateinit var menu1Button: TextView
    private lateinit var menu2Button: TextView

    override fun setContentView(layoutResID: Int) {
        this.setContentView(LayoutInflater.from(this).inflate(layoutResID, root, false))
    }

    override fun setContentView(view: View) {
        this.setContentView(view, ViewGroup.LayoutParams(-1, -1))
    }

    override fun setContentView(
        view: View,
        params: ViewGroup.LayoutParams
    ) {
        ensureRoot()
        root.addView(view, 0, params)
        super.setContentView(root)
    }

    fun getColorById(colorResId: Int): Int {
        return resources.getColor(colorResId)
    }

    fun getDimenById(dimenResId: Int): Float {
        return resources.getDimension(dimenResId)
    }

    var isLogcatEnable: Boolean
        get() {
            return View.VISIBLE == logcatView.visibility
        }
        set(enable) {
            logcatView.visibility = if (enable) View.VISIBLE else View.GONE
        }

    var isMenu1Enable: Boolean
        get() {
            return View.VISIBLE == menu1Button.visibility
        }
        set(enable) {
            menu1Button.visibility = if (enable) View.VISIBLE else View.GONE
        }

    fun setMenu1Text(text: String?) {
        menu1Button.text = text
    }

    var isMenu2Enable: Boolean
        get() {
            return View.VISIBLE == menu2Button.visibility
        }
        set(enable) {
            menu2Button.visibility = if (enable) View.VISIBLE else View.GONE
        }

    fun setMenu2Text(text: String?) {
        menu2Button.text = text
    }

    protected fun logcat(logcat: String, append: Boolean = false) {
        if (append) {
            logcatView.append(
                """
                    
                    $logcat
                    """.trimIndent()
            )
        } else {
            logcatView.text = logcat
        }
    }

    protected open fun onMenu1Click() {}
    protected open fun onMenu2Click() {}

    private fun ensureRoot() {
        root = FrameLayout(this)
        root.layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        addLogcatView()
        addTitleMenu()
    }

    private fun addLogcatView() {
        logcatView = TextView(this)
        val padding = resources.getDimension(R.dimen.dp_8).toInt()
        val scroll = ScrollView(this)
        logcatView.textSize = 12f
        logcatView.setTextColor(0xddddde)
        logcatView.setPadding(padding, padding, padding, padding)
        logcatView.setBackgroundColor(0x10000000)
        scroll.addView(logcatView)
        root.addView(
            scroll, FrameLayout.LayoutParams(
                getDimenById(R.dimen.dp_180).toInt(), getDimenById(R.dimen.dp_290).toInt()
            )
        )
        isLogcatEnable = false
    }

    private fun addTitleMenu() {
        val title = LinearLayout(this)

        menu1Button = TextView(this)
        menu2Button = TextView(this)
        menu1Button.setText(R.string.string_blog)
        menu2Button.setText(R.string.string_github)

        val padding = resources.getDimension(R.dimen.dp_8).toInt()
        menu1Button.setPadding(padding, padding, padding, padding)
        menu2Button.setPadding(padding, padding, padding, padding)
        menu1Button.textSize = 16f
        menu2Button.textSize = 16f

        // 占位
        val view = View(this)
        view.layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)

        title.orientation = LinearLayout.HORIZONTAL
        title.addView(view)
        title.addView(menu1Button)
        title.addView(menu2Button)

        root.addView(title, FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        menu1Button.setOnClickListener { onMenu1Click() }
        menu2Button.setOnClickListener { onMenu2Click() }
    }
}