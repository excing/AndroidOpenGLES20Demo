package me.excq.androidopengles20demo

import android.annotation.SuppressLint
import android.app.Activity
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*

/**
 * 基础 [Activity]
 */
@SuppressLint("Registered")
abstract class BaseActivity : Activity() {
    private lateinit var root: FrameLayout
    private lateinit var logcatView: TextView
    private lateinit var logcatScroll: ScrollView
    private lateinit var menu1Button: TextView
    private lateinit var menu2Button: TextView
    private lateinit var spinner: Spinner

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
            return View.VISIBLE == logcatScroll.visibility
        }
        set(enable) {
            logcatScroll.visibility = if (enable) View.VISIBLE else View.GONE
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

    var isSpinnerEnable: Boolean
        get() {
            return View.VISIBLE == spinner.visibility
        }
        set(enable) {
            spinner.visibility = if (enable) View.VISIBLE else View.GONE
        }

    protected open fun getSpinnerData(): Array<String> {
        return arrayOf()
    }

    protected fun logcat(logcat: String, append: Boolean = false) {
        if (append) {
            logcatView.append(
                "\n$logcat".trimIndent()
            )
        } else {
            logcatView.text = logcat
        }
    }

    protected open fun onMenu1Click() {}
    protected open fun onMenu2Click() {}
    protected open fun onSpinnerSelected(position: Int) {}

    private fun ensureRoot() {
        root = FrameLayout(this)
        root.layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        addLogcatView()
        addTitleMenu()
    }

    private fun addLogcatView() {
        val padding = resources.getDimension(R.dimen.dp_8).toInt()

        logcatView = TextView(this)
        logcatView.textSize = 10f
        logcatView.setTextColor(0xddddde)
        logcatView.setPadding(padding, padding, padding, padding)

        logcatScroll = ScrollView(this)
        logcatScroll.setBackgroundColor(0x10000000)
        logcatScroll.addView(logcatView)
        root.addView(
            logcatScroll,
            FrameLayout.LayoutParams(
                getDimenById(R.dimen.dp_160).toInt(),
                getDimenById(R.dimen.dp_200).toInt(),
                Gravity.BOTTOM
            )
        )

        isLogcatEnable = false
    }

    private fun addTitleMenu() {
        val title = LinearLayout(this)
        title.orientation = LinearLayout.HORIZONTAL

        spinner = Spinner(this)

        ArrayAdapter(
            this, android.R.layout.simple_spinner_item, 0, getSpinnerData()
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }

        val l = object : OnSpinnerSelectListener {
            override fun onSpinnerSelect(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                (::onSpinnerSelected)(position)
            }
        }

        spinner.onItemSelectedListener = SpinnerSelect(l)

        // 占位
        val view = View(this)
        view.layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)

        menu1Button = TextView(this)
        menu2Button = TextView(this)
        menu1Button.setText(R.string.string_blog)
        menu2Button.setText(R.string.string_github)

        val padding = resources.getDimension(R.dimen.dp_8).toInt()
        menu1Button.setPadding(padding, padding, padding, padding)
        menu2Button.setPadding(padding, padding, padding, padding)
        menu1Button.textSize = 16f
        menu2Button.textSize = 16f

        title.addView(spinner)
        title.addView(view)
        title.addView(menu1Button)
        title.addView(menu2Button)

        isSpinnerEnable = false
        isMenu1Enable = false
        isMenu2Enable = true

        root.addView(title, FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        menu1Button.setOnClickListener { onMenu1Click() }
        menu2Button.setOnClickListener { onMenu2Click() }
    }

    interface OnSpinnerSelectListener {
        fun onSpinnerSelect(
            parent: AdapterView<*>?,
            view: View?,
            position: Int,
            id: Long
        )
    }

    class SpinnerSelect(var l: OnSpinnerSelectListener) : AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(parent: AdapterView<*>?) {}

        override fun onItemSelected(
            parent: AdapterView<*>?,
            view: View?,
            position: Int,
            id: Long
        ) {
            l.onSpinnerSelect(parent, view, position, id)
        }

    }
}