package com.example.pinpoint


import android.Manifest
import android.content.ContentValues.TAG
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.FragmentTransaction
import com.example.pinpoint.R.id.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.Annotation
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.OnPointAnnotationDragListener
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.location
import java.lang.ref.WeakReference


class MapsActivity : AppCompatActivity(), FragmentCallback {

    private lateinit var mapView: MapView
    private lateinit var locationEngine: LocationEngine

    private val callback = LocationListeningCallback(this)
    private lateinit var locationPermissionHelper: LocationPermissionHelper
    private lateinit var locationInitHelper: LocationInitHelper

    private lateinit var mainActionFab: FloatingActionButton
    private lateinit var addPinFab: FloatingActionButton
    private lateinit var confirmPinFab: FloatingActionButton
    private lateinit var cancelPinFab: FloatingActionButton


    private lateinit var addPinTextView: TextView
    private lateinit var myLocationText: TextView

    private var isAllFabsVisible = false
    private var isPinCancelled = false

    private lateinit var currentLoc: Point
    private var pointAnnotationManager: PointAnnotationManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        mapView = findViewById(R.id.mapView)
        mainActionFab = findViewById(main_action_fab)
        addPinFab = findViewById(add_pin_fab)
        addPinTextView = findViewById(add_pin_text)
        myLocationText = findViewById(user_location_text)
        confirmPinFab = findViewById(confirm_pin_fab)
        cancelPinFab = findViewById(cancel_pin_fab)


        addPinFab.visibility = View.GONE
        addPinTextView.visibility = View.GONE
        myLocationText.visibility = View.GONE
        confirmPinFab.visibility = View.GONE
        cancelPinFab.visibility = View.GONE


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
        val request = LocationEngineRequest.Builder(DEFAULT_INTERVAL_IN_MILLISECONDS)
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
                .zoom(15.0)
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

                    mainActionFab.setOnClickListener (
                        View.OnClickListener {
                            (if (isAllFabsVisible!!) {
                                onMapReady()
                                foldFAB()

                                false
                            } else {
                                unfoldFAB()

                                true
                            }).also { isAllFabsVisible = it }
                        })

                    true
                }else {
                    foldFAB()

                    false
                }).also { isAllFabsVisible = it }
            })

        addPinFab.setOnClickListener{
            this.currentLoc = mapView.getMapboxMap().cameraState.center
            setAnnotationToMap()
            foldFAB()
            isAllFabsVisible = false
        }
        }

    private fun unfoldFAB() {
        addPinFab.show()
        addPinTextView.visibility = View.VISIBLE
        myLocationText.visibility = View.VISIBLE

        ViewCompat.animate(mainActionFab).rotation(360.0F).withLayer().setDuration(700L).setInterpolator(
            OvershootInterpolator(5.0F)
        ).start()

        mapView.foreground = ContextCompat.getDrawable(this, R.drawable.fab_foreground)
        mainActionFab.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_my_location))

    }

    private fun foldFAB() {
        addPinFab.hide()
        addPinTextView.visibility = View.GONE
        myLocationText.visibility = View.GONE

        ViewCompat.animate(mainActionFab).rotation(0.0F).withLayer().setDuration(700L).setInterpolator(
            OvershootInterpolator(5.0F)
        ).start()

        mapView.foreground = null
        mainActionFab.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_main_action))
    }

    private fun setAnnotationToMap() {
        bitmapFromDrawableRes(
            this@MapsActivity,
            R.drawable.red_marker
        )?.let { it ->
            val annotationApi = mapView?.annotations
            pointAnnotationManager = annotationApi?.createPointAnnotationManager()
            val pointAnnotationOptions: PointAnnotationOptions = PointAnnotationOptions()
                .withPoint(Point.fromLngLat(currentLoc.longitude(), currentLoc.latitude()))
                .withIconImage(it)
                .withDraggable(true)
            pointAnnotationManager?.create(pointAnnotationOptions)
            if (pointAnnotationManager != null) {
                setPinActions(pointAnnotationManager!!)
            }
            pointAnnotationManager?.addDragListener(object: OnPointAnnotationDragListener {

                override fun onAnnotationDrag(annotation: Annotation<*>) {
                    Log.d(TAG, "The marker is about to move")
                }

                override fun onAnnotationDragFinished(annotation: Annotation<*>) {
                    setPinActions(pointAnnotationManager!!)
                }

                override fun onAnnotationDragStarted(annotation: Annotation<*>) {
                    Log.d(TAG, "The marker is moving")
                }
            })
        }
    }
    
    private fun bitmapFromDrawableRes(context: Context, @DrawableRes resourceId: Int) =
        convertDrawableToBitmap(AppCompatResources.getDrawable(context, resourceId))

    private fun convertDrawableToBitmap(sourceDrawable: Drawable?): Bitmap? {
        if (sourceDrawable == null) {
            return null
        }
        return if (sourceDrawable is BitmapDrawable) {
            sourceDrawable.bitmap
        } else {
            val constantState = sourceDrawable.constantState ?: return null
            val drawable = constantState.newDrawable().mutate()
            val bitmap: Bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth, drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        }
    }

    private fun setPinActions(pointAnnotationManager: PointAnnotationManager) {
        confirmPinFab.visibility = View.VISIBLE
        cancelPinFab.visibility = View.VISIBLE
        confirmPinFab.setOnClickListener(
            View.OnClickListener {
                pointAnnotationManager.annotations.forEach {
                    it.isDraggable = !it.isDraggable
                    savePinLocation()
                    confirmPinFab.visibility = View.GONE
                    cancelPinFab.visibility = View.GONE
                }
            })
        cancelPinFab.setOnClickListener(View.OnClickListener {
            pointAnnotationManager.annotations.forEach{
                pointAnnotationManager.delete(it)
            }
            confirmPinFab.visibility = View.GONE
            cancelPinFab.visibility = View.GONE
        })
    }

    private fun savePinLocation() {
        mainActionFab.visibility = View.GONE
        val ft: FragmentTransaction = supportFragmentManager.beginTransaction()
        ft.replace(fragementHolder, PinDetails())
        ft.commit()
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

    override fun onDataSent(fragmentClosed: Boolean) {
        isPinCancelled = fragmentClosed
        if (isPinCancelled) {
            pointAnnotationManager?.annotations?.forEach {
                pointAnnotationManager!!.delete(it)
            }
        }
        mainActionFab.visibility = View.VISIBLE
    }

}