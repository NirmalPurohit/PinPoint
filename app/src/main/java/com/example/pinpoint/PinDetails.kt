package com.example.pinpoint

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction


class PinDetails : Fragment() {

    private lateinit var closeButton: Button
    private lateinit var submitButton: Button

    private lateinit var fragmentCallback: FragmentCallback

    private var cancelPin = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(R.layout.fragment_pin_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

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