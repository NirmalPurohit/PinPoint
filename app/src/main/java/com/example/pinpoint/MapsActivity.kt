package com.example.pinpoint

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton

import com.example.pinpoint.R.id.*
import com.example.pinpoint.com.example.pinpoint.LocationInitHelper
import com.example.pinpoint.com.example.pinpoint.LocationListeningCallback

import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.location

import java.lang.ref.WeakReference


class MapsActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var locationEngine: LocationEngine

    private val callback = LocationListeningCallback(this)
    private lateinit var locationPermissionHelper: LocationPermissionHelper
    private lateinit var locationInitHelper: LocationInitHelper

    private lateinit var mainActionFab: FloatingActionButton
    private lateinit var addPinFab: FloatingActionButton

    private lateinit var addPinTextView: TextView
    private lateinit var myLocationText: TextView

    private var isAllFabsVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        mapView = findViewById(R.id.mapView)
        mainActionFab = findViewById(main_action_fab)
        addPinFab = findViewById(add_pin_fab)
        addPinTextView = findViewById(add_pin_text)
        myLocationText = findViewById(user_location_text)

        addPinFab.visibility = View.GONE
        addPinTextView.visibility = View.GONE
        myLocationText.visibility = View.GONE

        locationInitHelper = LocationInitHelper(WeakReference(this))
        locationInitHelper.setMapView(mapView)

        mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS)

        locationEngine = LocationEngineProvider.getBestLocationEngine(this)

        locationPermissionHelper = LocationPermissionHelper(WeakReference(this))

        isAllFabsVisible = false

        onMapReady()
    }

    private fun onMapReady(){

        val DEFAULT_INTERVAL_IN_MILLISECONDS = 1000L
        val DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_MILLISECONDS * 5

        locationEngine = LocationEngineProvider.getBestLocationEngine(this)
        var request = LocationEngineRequest.Builder(DEFAULT_INTERVAL_IN_MILLISECONDS)
            .setPriority(LocationEngineRequest.PRIORITY_NO_POWER)
            .setMaxWaitTime(DEFAULT_MAX_WAIT_TIME)
            .build()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionHelper.checkPermissions { onMapReady() }
        }
        locationEngine.requestLocationUpdates(request, callback, mainLooper)
        locationEngine.getLastLocation(callback)
        mapView.getMapboxMap().setCamera(
            CameraOptions.Builder()
                .zoom(12.0)
                .build()
        )
        locationInitHelper.initLocationComponent()
        locationInitHelper.setupGesturesListener()
        setupFabActions()
    }

    fun setupFabActions(){
        mainActionFab.setOnClickListener(
            View.OnClickListener {
                (if (!isAllFabsVisible!!) {
                    unfoldFAB()

                    /*mainActionFab.setOnClickListener {
                        onMapReady()
                        Toast.makeText(this, "isAllFabsVisible: " + isAllFabsVisible, Toast.LENGTH_LONG).show()
                        foldFAB()
                        isAllFabsVisible = false
                    }*/

                    true
                }else {
                    foldFAB()

                    false
                }).also { isAllFabsVisible = it }
            })

        addPinFab.setOnClickListener{
                foldFAB()
                isAllFabsVisible = false
        }
        }

    private fun unfoldFAB() {
        addPinFab.show()
        addPinTextView.visibility = View.VISIBLE
        myLocationText.visibility = View.VISIBLE

        mapView.foreground = ContextCompat.getDrawable(this, R.drawable.fab_foreground)
        mainActionFab.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_my_location))
    }

    private fun foldFAB() {
        addPinFab.hide()
        addPinTextView.visibility = View.GONE
        myLocationText.visibility = View.GONE

        mapView.foreground = null
        mainActionFab.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_main_action))
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.location
            .removeOnIndicatorBearingChangedListener(locationInitHelper.onIndicatorBearingChangedListener)
        mapView.location
            .removeOnIndicatorPositionChangedListener(locationInitHelper.onIndicatorPositionChangedListener)
        mapView.gestures.removeOnMoveListener(locationInitHelper.onMoveListener)
    }

    override fun onStop() {
        super.onStop()
        locationEngine.removeLocationUpdates(callback)
    }

}