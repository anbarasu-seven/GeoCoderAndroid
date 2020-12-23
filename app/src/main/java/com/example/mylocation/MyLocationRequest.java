package com.example.mylocation;

import com.google.android.gms.location.LocationRequest;

public final class MyLocationRequest {

    public static int UPDATE_INTERVAL = 3000; // 3 sec
    public static int FATEST_INTERVAL = 3000; // 5 sec
    public static int DISPLACEMENT = 10;

    private static LocationRequest mLocationRequest;

    private MyLocationRequest() {
    }

    public static LocationRequest build() {
        if (mLocationRequest == null) {
            mLocationRequest = LocationRequest.create()
                    .setInterval(UPDATE_INTERVAL)
                    .setFastestInterval(FATEST_INTERVAL)
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setSmallestDisplacement(DISPLACEMENT);
        }
        return mLocationRequest;
    }
}
