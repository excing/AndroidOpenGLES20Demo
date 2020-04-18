package me.excq.androidopengles20demo

import android.app.ListActivity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.SparseArray
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView

class MainActivity : ListActivity() {
    private val activityClasses = SparseArray<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1)
        listAdapter = adapter

        try {
            val pi = packageManager.getPackageInfo(
                "me.excq.androidopengles20demo",
                PackageManager.GET_ACTIVITIES
            )

            var index = 0
            for (ai in pi.activities) {
                if (isIgnore(ai.name) && 0 == ai.labelRes) continue

                activityClasses.put(index++, ai.name)
                adapter.add(resources.getString(ai.labelRes))
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        }
    }

    // 不需要在首页显示的 Activity
    private fun isIgnore(activityName: String): Boolean {
        return "me.excq.androidopengles20demo.MainActivity" == activityName
                || "me.excq.androidopengles20demo.WebActivity" == activityName
    }

    override fun onListItemClick(
        l: ListView,
        v: View,
        position: Int,
        id: Long
    ) {
        val activityClass = classLoader.loadClass(activityClasses[position])

        startActivity(Intent(this, activityClass))
    }
}
