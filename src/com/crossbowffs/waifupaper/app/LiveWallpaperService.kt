package com.crossbowffs.waifupaper.app

import android.content.Context
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.preference.PreferenceManager
import android.view.SurfaceHolder
import com.crossbowffs.waifupaper.loader.FileLocation
import com.crossbowffs.waifupaper.utils.useNotNull
import net.rbgrn.android.glwallpaperservice.GLWallpaperService

class LiveWallpaperService : GLWallpaperService() {
    private lateinit var preferences: SharedPreferences
    private lateinit var sensorManager: SensorManager
    private lateinit var sensor: Sensor

    override fun onCreate() {
        super.onCreate()
        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    }

    private fun getSelectedModel(): Pair<String, FileLocation>? {
        val data = preferences.getString("selectedModel", null)?.split(':')
        if (data != null) {
            return data[0] to FileLocation.valueOf(data[1])
        }
        return null
    }

    override fun onCreateEngine(): GLEngine = LiveWallpaperEngine()

    private inner class LiveWallpaperEngine : GLWallpaperService.GLEngine(),
            SensorEventListener,
            SharedPreferences.OnSharedPreferenceChangeListener {
        private lateinit var renderer: Live2DRenderer

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            renderer = Live2DRenderer(applicationContext)
            getSelectedModel().useNotNull { renderer.setModel(it.first, it.second) }
            setRenderer(renderer)
            renderMode = GLWallpaperService.GLEngine.RENDERMODE_CONTINUOUSLY
            preferences.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onResume() {
            super.onResume()
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
            renderer.onResume()
        }

        override fun onPause() {
            super.onPause()
            sensorManager.unregisterListener(this)
            renderer.onPause()
        }

        override fun onDestroy() {
            super.onDestroy()
            renderer.release()
        }

        override fun onSensorChanged(event: SensorEvent) {
            // TODO
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {

        }

        override fun onSharedPreferenceChanged(p0: SharedPreferences?, p1: String?) {
            if (p1 == "selectedModel") {
                getSelectedModel().useNotNull { renderer.setModel(it.first, it.second) }
            }
        }
    }
}
