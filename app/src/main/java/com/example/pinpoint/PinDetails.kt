package com.example.pinpoint

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction


class PinDetails : Fragment() {

    private lateinit var closeButton: Button
    private lateinit var submitButton: Button

    private lateinit var fragmentCallback: FragmentCallback
    private lateinit var mapSnapView: ImageView
    private var bitmap: Bitmap? = null

    private var cancelPin = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view =  inflater.inflate(R.layout.fragment_pin_details, container, false)

        mapSnapView = view.findViewById(R.id.map_snap)

        bitmap = arguments?.getParcelable<Bitmap>(BITMAP_KEY)

        return view
    }

    companion object {
        private const val BITMAP_KEY = "bitmap"

        fun newInstance(bitmap: Bitmap): PinDetails {
            val fragment = PinDetails()
            val bundle = Bundle().apply {
                putParcelable(BITMAP_KEY, bitmap)
            }
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        if (bitmap != null) {
            val originalWidth = bitmap!!.width
            val originalHeight = bitmap!!.height

            val cropWidth = Math.min(originalWidth, originalHeight)
            val cropHeight = originalHeight / 4

            val cropX = (originalWidth - cropWidth) / 2
            val cropY = (originalHeight - cropHeight) / 2

            // Crop the snapshot bitmap
            val croppedBitmap =
                bitmap?.let { Bitmap.createBitmap(it, cropX, cropY, originalWidth, cropHeight) }

            mapSnapView.setImageBitmap(croppedBitmap)
        }

        closeButton = view.findViewById(R.id.close_button)
        closeButton.setOnClickListener(View.OnClickListener {
            // mapsActivity
            cancelPin = true
            fragmentCallback.onDataSent(cancelPin)
            val ft: FragmentTransaction? = activity?.supportFragmentManager?.beginTransaction()
            ft?.remove(this)
            ft?.commit()
        })

        submitButton = view.findViewById(R.id.submit_button)
        submitButton.setOnClickListener(View.OnClickListener {
            cancelPin = false
            fragmentCallback.onDataSent(cancelPin)
            val ft: FragmentTransaction? = activity?.supportFragmentManager?.beginTransaction()
            ft?.remove(this)
            ft?.commit()
        })
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        fragmentCallback = context as FragmentCallback
    }
}