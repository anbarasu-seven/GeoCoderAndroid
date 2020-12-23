package com.example.mylocation

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import java.lang.StringBuilder
import java.util.*

class MainActivity : AppCompatActivity(), GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private var adressText: TextView? = null
    private var labelText: TextView? = null
    private var loader: ProgressBar? = null
    private var  getLocation: Button? = null

    private var mGoogleApiClient: GoogleApiClient? = null
    private val TAG = MainActivity::class.java.simpleName
    private val locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            Logger.D(TAG, "locationCallback")
            super.onLocationResult(locationResult)
            if (locationResult != null) {
                val locations =
                    locationResult.locations
                for (location in locations) {
                    Logger.D(TAG, location.latitude.toString() + "-" + location.longitude)
                    labelText?.text = location.latitude.toString() + "-" + location.longitude
                    getAddress(location.latitude,location.longitude)
                }
            }
        }
    }

    fun getAddress(latitude: Double, longitude: Double) {
        val gcd = Geocoder(
            baseContext,
            Locale.getDefault()
        )
        val addresses: List<Address>
        loader?.visibility = View.GONE
        try {
            addresses = gcd.getFromLocation(
                latitude,
                longitude, 1
            )
            if (addresses.size > 0) {
                val locality: String = addresses[0].getLocality()
                val subLocality: String = addresses[0].getSubLocality()
                val state: String = addresses[0].getAdminArea()
                val country: String = addresses[0].getCountryName()
                val postalCode: String = addresses[0].getPostalCode()
                val knownName: String = addresses[0].getFeatureName()
                adressText?.text = StringBuilder()
                    .append(" ").append(knownName)
                    .append(" ").append(subLocality)
                    .append(" ").append(locality)
                    .append(" ").append(state)
                    .append(" ").append(country)
                    .append(" ").append(postalCode)
                    .toString()

            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                this@MainActivity,
                "Error getting address for the location",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onConnected(@Nullable bundle: Bundle?) {
        Logger.D(TAG, "onConnected")
        // Once connected with google api, get the location
        // Check If Google Services Is Available
        if (getServicesAvailable()) {
            getMyLocationUpdates()
        }
    }

    override fun onConnectionSuspended(i: Int) {
        Logger.D(TAG, "onConnectionSuspended")
        //reconnect google client by calling this method
        onLocationSettingSuccess()
    }

    override fun onConnectionFailed(result: ConnectionResult) {
        Logger.D(TAG, "onConnectionFailed")
        Logger.D(TAG, "Connection failed: ConnectionResult.getErrorCode() = "
                + result.getErrorCode())
    }

    //Starting the location updates
    private fun getMyLocationUpdates() {
        Logger.D(TAG, "getMyLocationUpdates")
        if (ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Check Permissions Now
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                REQUEST_LOCATION
            )
        } else {
            Handler().postDelayed(Runnable {
                afterAllPermissionsOk()
            }, 2000)
        }
    }

    private fun afterAllPermissionsOk() {
        Logger.D(TAG, "afterAllPermissionsOk")
        if (ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            loader?.visibility = View.VISIBLE
            LocationServices.getFusedLocationProviderClient(this@MainActivity)
                .requestLocationUpdates(MyLocationRequest.build(), locationCallback, null)
        }else{
            Logger.E(TAG, "Permission error");
        }
    }

    private fun onLocationSettingSuccess() {
        Logger.D(TAG, "onLocationSettingSuccess")
        if (mGoogleApiClient == null) {
            mGoogleApiClient = GoogleApiClient.Builder(this@MainActivity)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API).build()
            mGoogleApiClient?.connect()
        }else{
            getMyLocationUpdates()
        }
    }

    // call this method to enable gps setting and start requesting runtime permission
    private fun checkDeviceLocationSetting() {
        Logger.D(TAG, "checkDeviceLocationSetting")
        val builder: LocationSettingsRequest.Builder = LocationSettingsRequest.Builder()
                .addLocationRequest(MyLocationRequest.build())
        val task: Task<LocationSettingsResponse> = LocationServices.getSettingsClient(this@MainActivity)
                .checkLocationSettings(builder.build())
        task.addOnCompleteListener { task ->
            try {
                Logger.D(TAG, "onComplete gps setting")
                val response: LocationSettingsResponse? = task.getResult(ApiException::class.java)
                // All location settings are satisfied. The client can initialize location
                // requests here
                onLocationSettingSuccess()
            } catch (exception: ApiException) {
                Logger.D(TAG, "ApiException")
                when (exception.getStatusCode()) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                        Logger.D(TAG, "RESOLUTION_REQUIRED")
                        // Location settings are not satisfied. But could be fixed by showing the
                        // deliverer a dialog.
                        try {
                            Logger.D(TAG, "show gps setting dialog")
                            // Cast to a resolvable exception.
                            val resolvable: ResolvableApiException = exception as ResolvableApiException
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            resolvable.startResolutionForResult(
                                this@MainActivity,
                                REQUEST_CHECK_SETTINGS
                            )
                        } catch (e: SendIntentException) {
                            e.printStackTrace()
                            // Ignore the error.
                        } catch (e: ClassCastException) {
                            // Ignore, should be an impossible error.
                            e.printStackTrace()
                        }
                    }
                    LocationSettingsStatusCodes.SUCCESS -> {
                        Logger.D(TAG, "SUCCESS")
                        // All location settings are satisfied. The client can initialize location
                        // requests here.
                        onLocationSettingSuccess()
                    }
                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> Logger.D(TAG, "SETTINGS_CHANGE_UNAVAILABLE")
                }
            }
        }
        builder.setAlwaysShow(true)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        Logger.D(TAG, "onRequestPermissionsResult")
        if (requestCode == REQUEST_LOCATION) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.isNotEmpty()
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission was granted, yay! Do the
                // location-related task you need to do.
                if (ActivityCompat.checkSelfPermission(this@MainActivity,
                                Manifest.permission.ACCESS_FINE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(this@MainActivity,
                                Manifest.permission.ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    Logger.D(TAG, "ACCESS_FINE_LOCATION & ACCESS_COARSE_LOCATION is PERMISSION_GRANTED")
                    //Check If Google Services Is Available
                    if (getServicesAvailable()) {
                        //displayLastLocation();
                        getMyLocationUpdates()
                    }
                }
            } else {
                //this@MainActivity.finish()
            }
        }
        //super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Logger.D(TAG, "onActivityResult")
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CHECK_SETTINGS -> when (resultCode) {
                Activity.RESULT_OK -> {
                    Logger.D(TAG, "RESULT_OK")
                    // The deliverer was asked to change settings, switched on location
                    onLocationSettingSuccess()
                }
                Activity.RESULT_CANCELED -> {
                    Logger.D(TAG, "RESULT_CANCELED")
                    // The deliverer was asked to change settings, but chose not to
                    //this@MainActivity.finish()
                }
                else -> {
                }
            }
        }
    }

    private fun getServicesAvailable(): Boolean {
        Logger.D(TAG, "getServicesAvailable")
        val api = GoogleApiAvailability.getInstance()
        val isAvailable = api.isGooglePlayServicesAvailable(this@MainActivity)
        if (isAvailable == ConnectionResult.SUCCESS) {
            return true
        } else if (api.isUserResolvableError(isAvailable)) {
            val dialog = api.getErrorDialog(this@MainActivity, isAvailable, 0)
            dialog.show()
        } else {
            Toast.makeText(this@MainActivity, "Cannot Connect To Play Services", Toast.LENGTH_SHORT).show();
        }
        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        adressText = findViewById<TextView>(R.id.adressText)
        getLocation = findViewById<Button>(R.id.getLocation)
        labelText = findViewById<TextView>(R.id.labelText)
        loader = findViewById<ProgressBar>(R.id.loader)
        loader?.visibility = View.GONE

        getLocation?.setOnClickListener {
            labelText?.text = ""
            adressText?.text = ""
            checkDeviceLocationSetting()
        }
    }

    companion object {
        private const val REQUEST_CHECK_SETTINGS = 1
        private const val REQUEST_LOCATION = 0
    }
}