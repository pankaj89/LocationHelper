package com.master.locationhelper

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.app.AlertDialog
import android.location.Location
import android.os.Build
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.util.Log

import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.google.android.gms.location.SettingsClient

import android.app.Activity.RESULT_OK
import android.content.*
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent

/**
 * @author Pankaj Sharma
 * This helper class is used to
 * 1. Fetch single location
 * 2. Fetch multiple location
 * 3. In service (Foreground service-Notification Always Show)
 * 4. In service (Activity aware, if actvitiy is foreground then notification will not shown else it will show notification).
 *
 * NOTE : You must check for ACCESS_FINE_LOCATION OR ACCESS_BACKGROUND_LOCATION.
 * Example
 * val locationHelper: LocationHelper by lazy { LocationHelper(this, this) }
 *
 * 1. Fetch single location
 *
 *      locationhelper.fetchSingleLocation{ it->
 *      }
 *
 * 2. Fetch multiple location
 *
 *      locationhelper.fetchMultipleLocation{ it->
 *      }
 *
 * 3. In service (Foreground service-Notification Always Show)
 *
 *      locationHelper.startForegroundService()
 *
 * 4. In service (Activity aware, if actvitiy is foreground then notification will not shown else it will show notification).
 *
 *      locationHelper.bindAndStartService()
 *      and in
 *      override fun onResume() {
 *          super.onResume()
 *          locationHelper.bindService()
 *      }
 */

class LocationHelper(private val context: Context, private val mActivity: Activity? = null, private val notificationMessage: String = "Application is running") : LifecycleObserver {

//     ===============STARTING FOR HELPER METHODS===============
//     Helper methods for starting foreground service and stoping it

    /**
     * Method to make this as life cycle aware so you no need to call stop location update, and helpfull when you are calling bindable service
     */
    fun makeLifeCyclerAware(activity: AppCompatActivity) {
        activity.lifecycle.addObserver(this)
    }

    private var isServiceConnected = false
    private val mServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            isServiceConnected = false
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            isServiceConnected = true
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public fun onDestroy() {
        Log.d(TAG, "onDestroy() called");
        if (isServiceConnected) {
            (mActivity ?: context).unbindService(mServiceConnection)
            isServiceConnected = false
        }
        stopLocationUpdates()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public fun onPause() {
        Log.d(TAG, "onPause() called")
        if (isServiceConnected) {
            (mActivity ?: context).unbindService(mServiceConnection)
            isServiceConnected = false
        }
    }

    /**
     * Call this method if you require background location, call this method on resume so notification goes away
     */
    fun bindService() {
        val locationService = Intent(context, BackgroundLocationService::class.java)
        (mActivity ?: context).bindService(locationService, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * Call thid method so it will start the sevice and immediatly bind it so notification will not show when your activity is foreground else it will show notfication
     * You MUST HAVE ACCESS_BACKGROUND_LCOATION PERMISSION
     */
    fun bindAndStartService() {
        val locationService = Intent(context, BackgroundLocationService::class.java)
        context.bindService(locationService, mServiceConnection, Context.BIND_AUTO_CREATE);
        (mActivity ?: context).startService(locationService)
    }

    /**
     * This method is used to immediately start service in foreground mode, NOTE: Notification will be display on top regardless of your activity
     */
    fun startForegroundService() {
        val locationService = Intent(context, BackgroundLocationService::class.java)
        locationService.putExtra(BackgroundLocationService.IS_FOREGROUND, true)
        locationService.putExtra(BackgroundLocationService.NOTIFICATION_MESSAGE, notificationMessage)
        (mActivity ?: context).startService(locationService)
        ContextCompat.startForegroundService((mActivity ?: context), locationService)
    }

    /**
     * Call this method when you not required of this service
     */
    fun stopService() {
        val locationService = Intent(context, BackgroundLocationService::class.java)
        locationService.putExtra(BackgroundLocationService.IS_STOP, true)
        locationService.putExtra(BackgroundLocationService.NOTIFICATION_MESSAGE, notificationMessage)
        context.startService(locationService)
    }


//    ===============ENDING FOR HELPER METHODS===============


    //================
    private var mLocationCallback: ((location: Location) -> Unit)? = null
    private var isLocationUpdates: Boolean = false

    //Location Request
    internal var mLocationRequest: LocationRequest? = null

    init {
        setLocationRequest()
    }

    private fun setLocationRequest() {
        if (mLocationRequest == null) {
            mLocationRequest = LocationRequest()
            mLocationRequest!!.interval = 1000
            mLocationRequest!!.fastestInterval = 5000
            mLocationRequest!!.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }

    //Settings client
    private val settingsClient: SettingsClient by lazy { LocationServices.getSettingsClient(context) }

    //Fused Location client
    private val mFusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(
            context
        )
    }

    internal var locationListener: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            super.onLocationResult(locationResult)
            val location = locationResult!!.lastLocation
            if (location != null) {
                LATITUDE = location.latitude
                LONGITUDE = location.longitude
            }
            Log.d("LOG", "onLocationChanged() called with: location = [$location]")
            if (mLocationCallback != null) {
                mLocationCallback?.invoke(location)
            }
            if (isLocationUpdates == false) {
                stopLocationUpdates()
            }
        }
    }

    fun fetchSingleLocation(mLocationCallback: (location: Location) -> Unit) {
        isLocationUpdates = false
        this.mLocationCallback = mLocationCallback
        if (mActivity != null) {
            checkLocationSettings()
        } else {
            startLocationUpdates()
        }
    }

    fun fetchMultipleLocation(mLocationCallback: (location: Location) -> Unit) {
        isLocationUpdates = true
        this.mLocationCallback = mLocationCallback
        if (mActivity != null) {
            checkLocationSettings()
        } else {
            startLocationUpdates()
        }
    }

    var locationSettingsCallback: (() -> Unit)? = null
    fun checkLocationSettings(callback: (() -> Unit)? = null) {

        if (mActivity == null) {
            Log.i("LOG", "Activity is null")
            return
        }

        if (mLocationRequest == null) {
            Log.i("LOG", "Location Request is null")
            return
        }

        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(mLocationRequest!!)

        locationSettingsCallback = callback
        val result = settingsClient.checkLocationSettings(builder.build())
        result.addOnSuccessListener(mActivity) {
            if (callback != null) {
                locationSettingsCallback?.invoke()
            } else {
                startLocationUpdates()
            }
        }
        result.addOnFailureListener(mActivity) { e ->
            val statusCode = (e as ApiException).statusCode
            when (statusCode) {
                LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
// Location settings are not satisfied, but this can be fixed
// by showing the user a dialog.
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        try {
// Show the dialog by calling startResolutionForResult(),
// and check the result in onActivityResult().
                            val rae = e as ResolvableApiException
                            rae.startResolutionForResult(mActivity, REQUEST_CHECK_SETTINGS)
                        } catch (sendIntentException: IntentSender.SendIntentException) {
// Ignore the error.
                        }

                    } else {
                        buildAlertMessageNoGps()
                    }
                    Log.d(
                        "LOG",
                        "onResult() called with: locationSettingsResult = RESOLUTION_REQUIRED"
                    )
                }
                LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE ->
// Location settings are not satisfied. However, we have no way
// to fix the settings so we won't show the dialog.
                    Log.d(
                        "LOG",
                        "onResult() called with: locationSettingsResult = SETTINGS_CHANGE_UNAVAILABLE"
                    )
            }
        }
    }

    private fun buildAlertMessageNoGps() {
        if (mActivity == null) {
            Log.i("LOG", "Activity is null")
            return
        }
        val builder = AlertDialog.Builder(mActivity)
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
            .setCancelable(false)
            .setPositiveButton("Yes") { dialog, id ->
                mActivity.startActivityForResult(
                    Intent(
                        Settings.ACTION_LOCATION_SOURCE_SETTINGS
                    ), REQUEST_GPS_SETTINGS
                )
            }
            .setNegativeButton("No") { dialog, id -> dialog.cancel() }
        val alert = builder.create()
        alert.show()
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK && requestCode == REQUEST_CHECK_SETTINGS) {
            if (mActivity != null && !mActivity.isFinishing)
                checkLocationSettings(locationSettingsCallback)
        } else if (requestCode == REQUEST_GPS_SETTINGS) {
            if (mActivity != null && !mActivity.isFinishing)
                checkLocationSettings(locationSettingsCallback)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        mFusedLocationClient.requestLocationUpdates(
            mLocationRequest,
            locationListener,
            Looper.myLooper()
        )
    }

    fun stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(locationListener)
    }

    companion object {
        var LATITUDE = 0.0
        var LONGITUDE = 0.0
        var DEBUG_ENABLED=false
        private val TAG = "LocationHelper"

        fun getLatitude(intent: Intent?): Double {
            return intent?.getDoubleExtra("LAT", 0.0) ?: 0.0
        }

        fun getLongitude(intent: Intent?): Double {
            return intent?.getDoubleExtra("LNG", 0.0) ?: 0.0
        }

        //===========
//Linked with Settings- START
//===========
        val REQUEST_CHECK_SETTINGS = 101
        private val REQUEST_GPS_SETTINGS = 102

        @TargetApi(Build.VERSION_CODES.KITKAT)
        fun isLocationEnabled(context: Context): Boolean {
            var locationMode = 0
            val locationProviders: String
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                try {
                    locationMode = Settings.Secure.getInt(
                        context.contentResolver,
                        Settings.Secure.LOCATION_MODE
                    )
                } catch (e: Settings.SettingNotFoundException) {
                    e.printStackTrace()
                }

                return locationMode != Settings.Secure.LOCATION_MODE_OFF
            } else {
                locationProviders = Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.LOCATION_PROVIDERS_ALLOWED
                )
                return !TextUtils.isEmpty(locationProviders)
            }
        }
    }

    interface LocationUpdateListener {
        fun onLocationUpdate(log: Location)
    }
}
