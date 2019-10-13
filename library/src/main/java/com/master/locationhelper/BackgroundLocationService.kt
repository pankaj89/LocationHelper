package com.master.locationhelper

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import java.text.SimpleDateFormat
import java.util.*

class BackgroundLocationService : Service() {
    private val mBinder = LocalBinder()

    inner class LocalBinder : Binder() {
        internal val service: BackgroundLocationService
            get() = this@BackgroundLocationService
    }

    var mChangingConfiguration = false
    override fun onBind(intent: Intent?): IBinder? {
        stopForeground(true);
        mChangingConfiguration = false;
        return mBinder
    }

    override fun onRebind(intent: Intent?) {
        stopForeground(true);
        mChangingConfiguration = false;
        super.onRebind(intent)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        mChangingConfiguration = true
    }

    override fun onUnbind(intent: Intent?): Boolean {
        stopForeground(true);
        if (!mChangingConfiguration) {
            startForeground(1, getNotification(notificationMessage))
        }
        return true
    }


    companion object {
        const val IS_FOREGROUND = "IS_FOREGROUND"
        const val IS_STOP = "IS_STOP"
        const val NOTIFICATION_MESSAGE = "NOTIFICATION_MESSAGE"

        /*fun bindService(context: Context, mServiceConnection: ServiceConnection) {
            val locationService = Intent(context, BackgroundLocationService::class.java)
            context.bindService(locationService, mServiceConnection, Context.BIND_AUTO_CREATE);
        }

        fun bindAndStartService(context: Context, mServiceConnection: ServiceConnection) {
            val locationService = Intent(context, BackgroundLocationService::class.java)
            context.bindService(locationService, mServiceConnection, Context.BIND_AUTO_CREATE);
            context.startService(locationService)
        }

        fun startForegroundService(context: Context) {
            val locationService = Intent(context, BackgroundLocationService::class.java)
            locationService.putExtra(IS_FOREGROUND, true)
            context.startService(locationService)
            ContextCompat.startForegroundService(context, locationService)
        }

        fun stopService(context: Context) {
            val locationService = Intent(context, BackgroundLocationService::class.java)
            locationService.putExtra(IS_STOP, true)
            context.startService(locationService)
            context.stopService(locationService)
        }*/
    }

    var sdf = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())

    var notificationMessage: String = "Application is running"

    var mNotificationManager: NotificationManager? = null
    var locationHelper2: LocationHelper? = null
    override fun onCreate() {
        super.onCreate()
        locationHelper2 = LocationHelper(applicationContext, null)
        mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (intent?.hasExtra(NOTIFICATION_MESSAGE) == true)
            notificationMessage = intent.getStringExtra(NOTIFICATION_MESSAGE)

        val isForeground = intent?.getBooleanExtra(IS_FOREGROUND, false) ?: false
        val isStop = intent?.getBooleanExtra(IS_STOP, false) ?: false

        if (isStop) {
            locationHelper2?.stopLocationUpdates()
            stopForeground(true)
            stopSelf()
            return START_NOT_STICKY
        } else {
            locationHelper2?.fetchMultipleLocation {
                Log.i("LOCATION", "${it.latitude},${it.longitude} at ${sdf.format(Date())}")

                if (LocationHelper.DEBUG_ENABLED && serviceIsRunningInForeground(this)) {
                    mNotificationManager?.notify(1, getNotification("${it.latitude},${it.longitude} at ${sdf.format(Date())}"))
                }

                val broadCastIntent = Intent("MASTER_ACTION_LOCATION_UPDATED")
                broadCastIntent.putExtra("LAT", it.latitude)
                broadCastIntent.putExtra("LNG", it.longitude)
                broadCastIntent.setPackage(getApplicationContext().getPackageName())
                sendOrderedBroadcast(broadCastIntent, null)
            }
            if (isForeground) {
                startForeground(1, getNotification(notificationMessage))
            } else {
//                mNotificationManager?.notify( 1, getNotification("Starting of location update normal"))
            }
            return START_STICKY
        }
    }

    private fun createNotificationChannel() {
        // Android O requires a Notification Channel.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Location"
            // Create the channel for the notification
            val mChannel =
                NotificationChannel("location", name, NotificationManager.IMPORTANCE_LOW)

            // Set the Notification Channel for the Notification Manager.
            mNotificationManager?.createNotificationChannel(mChannel)
        }
    }

    private fun getNotification(text: String): Notification {
//        val intent = Intent(this, MainActivity::class.java)

        // Extra to help us figure out if we arrived in onStartCommand via the notification or not.
//        intent.putExtra(EXTRA_STARTED_FROM_NOTIFICATION, true)

        // The PendingIntent that leads to a call to onStartCommand() in this service.
//        val servicePendingIntent = PendingIntent.getService(
//            this, 0, intent,
//            PendingIntent.FLAG_UPDATE_CURRENT
//        )

        // The PendingIntent to launch activity.
//        val activityPendingIntent = PendingIntent.getActivity(
////            this, 0,
////            Intent(this, MainActivity::class.java), 0
////        )

        val builder = NotificationCompat.Builder(this, "location")
            /*    .addAction(R.drawable.ic_launch, getString(R.string.launch_activity),
                        activityPendingIntent)
                .addAction(R.drawable.ic_cancel, getString(R.string.remove_location_updates),
                        servicePendingIntent)*/
            .setContentText(text)
            .setContentTitle(getString(R.string.app_name))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSmallIcon(R.drawable.ic_notification_icon)
            .setTicker(text)
            .setWhen(System.currentTimeMillis())

        // Set the Channel ID for Android O.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId("location") // Channel ID
        }

        return builder.build()
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
            if (BackgroundLocationService::class.java.canonicalName == service.service.className) {
                if (service.foreground) {
                    return true
                }
            }
        }
        return false
    }
}