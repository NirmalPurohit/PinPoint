package com.example.pinpoint

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat

import com.example.pinpoint.com.example.pinpoint.LocationListeningCallback
import com.example.pinpoint.databinding.ActivityMapsBinding

import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorBearingChangedListener
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.R
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.maps.plugin.gestures.OnMoveListener

import java.lang.ref.WeakReference


class MapsActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var locationEngine: LocationEngine

    private val callback = LocationListeningCallback(this)
    private lateinit var locationPermissionHelper: LocationPermissionHelper

    private lateinit var binding: ActivityMapsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        mapView = MapView(binding.root.context)
        setContentView(mapView)
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
            // checkLocationPermission()
            locationPermissionHelper.checkPermissions { onMapReady() }
        }
        locationEngine.requestLocationUpdates(request, callback, mainLooper)
        locationEngine.getLastLocation(callback)
        mapView.getMapboxMap().setCamera(
            CameraOptions.Builder()
                .zoom(12.0)
                .build()
        )
        initLocationComponent()
        setupGesturesListener()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun initLocationComponent() {
        val locationComponentPlugin = mapView.location
        locationComponentPlugin.updateSettings {
            this.enabled = true
            this.locationPuck = LocationPuck2D(
                topImage = AppCompatResources.getDrawable(
                    this@MapsActivity,
                    R.drawable.mapbox_user_icon
                ),
                bearingImage = AppCompatResources.getDrawable(
                    this@MapsActivity,
                    R.drawable.mapbox_user_bearing_icon
                ),
                shadowImage = AppCompatResources.getDrawable(
                    this@MapsActivity,
                    R.drawable.mapbox_user_stroke_icon
                ),
                scaleExpression = interpolate {
                    linear()
                    zoom()
                    stop {
                        literal(0.0)
                        literal(0.6)
                    }
                    stop {
                        literal(20.0)
                        literal(1.0)
                    }
                }.toJson()
            )
        }
        locationComponentPlugin.addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        locationComponentPlugin.addOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
    }

    private val onMoveListener = object : OnMoveListener {
        override fun onMoveBegin(detector: MoveGestureDetector) {
            onCameraTrackingDismissed()
        }

        override fun onMove(detector: MoveGestureDetector): Boolean {
            return false
        }

        override fun onMoveEnd(detector: MoveGestureDetector) {}
    }

    private fun setupGesturesListener() {
        mapView.gestures.addOnMoveListener(onMoveListener)
    }

    private val onIndicatorBearingChangedListener = OnIndicatorBearingChangedListener {
        mapView.getMapboxMap().setCamera(CameraOptions.Builder().bearing(it).build())
    }

    private val onIndicatorPositionChangedListener = OnIndicatorPositionChangedListener {
        mapView.getMapboxMap().setCamera(CameraOptions.Builder().center(it).build())
        mapView.gestures.focalPoint = mapView.getMapboxMap().pixelForCoordinate(it)
    }

    private fun onCameraTrackingDismissed() {
        // Toast.makeText(this, "onCameraTrackingDismissed", Toast.LENGTH_SHORT).show()
        mapView.location
            .removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        mapView.location
            .removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
        mapView.gestures.removeOnMoveListener(onMoveListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.location
            .removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
        mapView.location
            .removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        mapView.gestures.removeOnMoveListener(onMoveListener)
    }

    override fun onStop() {
        super.onStop()
        locationEngine.removeLocationUpdates(callback)
    }

}