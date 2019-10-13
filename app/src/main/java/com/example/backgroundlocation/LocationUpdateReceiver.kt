package com.example.backgroundlocation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.master.locationhelper.LocationHelper

class LocationUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.i("LOCATION_UPDATE", "Lat:${com.master.locationhelper.LocationHelper.getLatitude(intent)},Lng:${com.master.locationhelper.LocationHelper.getLongitude(intent)}")
    }
}