package com.example.backgroundlocation

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.master.permissionhelper.PermissionHelper
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    val permissionHelper by lazy {
        PermissionHelper(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ),
            100
        )
    }

    val locationHelper: com.master.locationhelper.LocationHelper by lazy { com.master.locationhelper.LocationHelper(this, this) }

    override fun onResume() {
        super.onResume()
        locationHelper.bindService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        locationHelper.makeLifeCyclerAware(this)

        btnStart.setOnClickListener {
            permissionHelper.requestAll {
                locationHelper.checkLocationSettings {
                    locationHelper.bindAndStartService()
                    btnStart.visibility = View.INVISIBLE
                    btnStop.visibility = View.VISIBLE
                }
            }
        }

        btnStop.setOnClickListener {
            locationHelper.stopService()
            btnStart.visibility = View.VISIBLE
            btnStop.visibility = View.INVISIBLE
        }

        if (serviceIsRunningInForeground(this)) {
            btnStart.visibility = View.INVISIBLE
            btnStop.visibility = View.VISIBLE
        } else {
            btnStart.visibility = View.VISIBLE
            btnStop.visibility = View.INVISIBLE
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        locationHelper.onActivityResult(requestCode, resultCode, data)
    }

    /**
     * Returns true if this is a foreground service.
     *
     * @param context The [Context].
     */
    fun serviceIsRunningInForeground(context: Context): Boolean {
        val manager = context.getSystemService(
            Context.ACTIVITY_SERVICE
        ) as ActivityManager
        for (service in manager.getRunningServices(
            Integer.MAX_VALUE
        )) {
            Log.i("SERVICE", service.service.className)
            if (com.master.locationhelper.BackgroundLocationService::class.java.canonicalName == service.service.className) {
                if (service.foreground) {
                    return true
                }
            }
        }
        return false
    }
}
