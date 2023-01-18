package com.example.pinpoint

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import com.example.pinpoint.com.example.pinpoint.LocationInitHelper

import com.example.pinpoint.com.example.pinpoint.LocationListeningCallback
import com.example.pinpoint.databinding.ActivityMapsBinding

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

    private lateinit var binding: ActivityMapsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        mapView = MapView(binding.root.context)
        setContentView(mapView)
        locationInitHelper = LocationInitHelper(WeakReference(this))
        locationInitHelper.setMapView(mapView)
        mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS)
        locationEngine = LocationEngineProvider.getBestLocationEngine(this)
        locationPermissionHelper = LocationPermissionHelper(WeakReference(this))
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