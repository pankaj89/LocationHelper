![alt text](https://github.com/pankaj89/LocationHelper/blob/master/location_helper_banner1.svg)

# LocationHelper

<!--[![N|Solid](https://img.shields.io/badge/Android%20Arsenal-Simpler%20Recycler%20View%20Adapter-brightgreen.svg)](https://android-arsenal.com/details/1/5354)-->

#### Location Helper used to fetch location(Single/Multiple) in Background.

# Features
- ##### Easy to use
- ##### Fetch Single Location
- ##### Fetch multiple location
- ##### Get location Callback even when your application not exist (Usefull for location tracking)
- ##### Show notification bar when your application is not foreground.

### Setup
Include the following dependency in your build.gradle files.
```
// Project level build.gradle
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}

// App level build.gradle
dependencies {
    implementation 'com.github.pankaj89:MasterExoPlayer:1.0'
}
	
```
# How to use

#### Attach to RecyclerView

### 1. Checking for location settings (GPS)
```
val locationHelper = LocationHelper(this, this)
locationHelper.makeLifeCyclerAware(this)

//Check location setting whether gps enable to get location
locationHelper.checkLocationSettings {

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
```

### Bindable service
```
fun onCreate(){

    //Service
    locationHelper.bindAndStartService()
    
    //Fetch Single Location
    locationHelper.fetchSingleLocation { it->Location }
    
    //Fetch Multiple Location
    locationHelper.fetchMultipleLocation { it->Location }
    
}
fun onResume(){
    super.onResume()
    locationHelper.bindService()
}
```

### Background Service
```
locationHelper.startForegroundService()
```

### LocationUpdate callback when application is in background
BroadcastReceiver
```
class LocationUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val lat=LocationHelper.getLatitude(intent)
        val lng=LocationHelper.getLongitude(intent)
        Log.i("LOCATION_UPDATE", "Lat:${lat},Lng:${lng}")
    }
}
```
Register BroadcastReceiver in manifest
```
<receiver
    android:name=".LocationUpdateReceiver"
    android:exported="true">
    <intent-filter>
        <action android:name="MASTER_ACTION_LOCATION_UPDATED" />
    </intent-filter>
</receiver>
```


### Stop service
```
locationHelper.startForegroundService()
```


### Special Thanks to
###### PermissionHelper [(<u><i>link</i></u>)](https://github.com/pankaj89/PermissionHelper)

### My Other Libraries
###### Runtime Permission Helper [(<u><i>link</i></u>)](https://github.com/google/ExoPlayer)
###### Simple Adapter for RecyclerView [(<u><i>link</i></u>)](https://github.com/pankaj89/PermissionHelper)
###### ExoPlayer inside RecyclerView [(<u><i>link</i></u>)](https://github.com/pankaj89/MasterExoPlayer)
### License
```
Copyright 2017 Pankaj Sharma

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
