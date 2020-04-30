package me.excq.androidopengles20demo

import android.app.ListActivity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Half.EPSILON
import android.util.SparseArray
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class MainActivity : ListActivity(), SensorEventListener {
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
                if (isIgnore(ai.name) || 0 == ai.labelRes) continue

                activityClasses.put(index++, ai.name)
                adapter.add(resources.getString(ai.labelRes))
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        }

        getSoundBySensorTest()
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

    /**
     * 测试个好玩的东西，这里有个项目：
     *
     * https://github.com/yueyuzhao/gyrophone
     *
     * 项目中表示，灵敏的陀螺仪可以感受到附近声音的振动，
     * 通过这个振动，可以反向推导到声音内容。
     *
     * 所以这里就试试，我们的陀螺仪有没有那么灵敏（好玩）。
     *
     * 实验结果，数据时刻都在发生变化
     */
    private fun getSoundBySensorTest() {
//        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
//        val sensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
//
//        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    private val NS2S = 1.0f / 1000000000.0f
    private val deltaRotationVector = FloatArray(4) { 0f }
    private var timestamp: Float = 0f

    override fun onSensorChanged(event: SensorEvent?) {
        if (timestamp != 0f && event != null) {
            val dT = (event.timestamp - timestamp) * NS2S
            var axisX: Float = event.values[0]
            var axisY: Float = event.values[1]
            var axisZ: Float = event.values[2]

            val omegaMagnitude: Float = sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ)

            println("onSensorChanged, $axisX, $axisY, $axisZ")

            if (omegaMagnitude > EPSILON) {
                axisX /= omegaMagnitude
                axisY /= omegaMagnitude
                axisZ /= omegaMagnitude
            }

            val thetaOverTwo: Float = omegaMagnitude * dT / 2.0f
            val sinThetaOverTwo: Float = sin(thetaOverTwo)
            val cosThetaOverTwo: Float = cos(thetaOverTwo)
            deltaRotationVector[0] = sinThetaOverTwo * axisX
            deltaRotationVector[1] = sinThetaOverTwo * axisY
            deltaRotationVector[2] = sinThetaOverTwo * axisZ
            deltaRotationVector[3] = cosThetaOverTwo
        }

        timestamp = event?.timestamp?.toFloat() ?: 0f
        val deltaRotationMatrix = FloatArray(9) { 0f }
        SensorManager.getRotationMatrixFromVector(deltaRotationMatrix, deltaRotationVector);
    }
}
