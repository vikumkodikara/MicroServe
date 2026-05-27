package com.example.microserve

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

class PickLocationActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var tvSelectedAddress: TextView
    private lateinit var tvCoordinates: TextView

    private var province: String = ""
    private var district: String = ""
    private var city: String = ""
    private var marker: Marker? = null
    private var selectedLat: Double = 6.9271
    private var selectedLng: Double = 79.8612

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = packageName
        enableEdgeToEdge()
        setContentView(R.layout.activity_pick_location)

        province = intent.getStringExtra(EXTRA_PROVINCE).orEmpty()
        district = intent.getStringExtra(EXTRA_DISTRICT).orEmpty()
        city = intent.getStringExtra(EXTRA_CITY).orEmpty()
        selectedLat = intent.getDoubleExtra(EXTRA_LAT, 6.9271)
        selectedLng = intent.getDoubleExtra(EXTRA_LNG, 79.8612)

        mapView = findViewById(R.id.mapView)
        tvSelectedAddress = findViewById(R.id.tvSelectedAddress)
        tvCoordinates = findViewById(R.id.tvCoordinates)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<MaterialButton>(R.id.btnConfirmLocation).setOnClickListener { confirmSelection() }

        setupMap()
        updateSummary()
    }

    private fun setupMap() {
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(14.0)
        mapView.controller.setCenter(GeoPoint(selectedLat, selectedLng))

        placeMarker(selectedLat, selectedLng)

        val eventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                p ?: return false
                selectedLat = p.latitude
                selectedLng = p.longitude
                placeMarker(selectedLat, selectedLng)
                updateSummary()
                return true
            }

            override fun longPressHelper(p: GeoPoint?): Boolean = false
        })
        mapView.overlays.add(eventsOverlay)
    }

    private fun placeMarker(lat: Double, lng: Double) {
        marker?.let { mapView.overlays.remove(it) }
        marker = Marker(mapView).apply {
            position = GeoPoint(lat, lng)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            isDraggable = true
            setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                override fun onMarkerDrag(marker: Marker) {
                    selectedLat = marker.position.latitude
                    selectedLng = marker.position.longitude
                    updateSummary()
                }

                override fun onMarkerDragEnd(marker: Marker) {
                    selectedLat = marker.position.latitude
                    selectedLng = marker.position.longitude
                    updateSummary()
                }

                override fun onMarkerDragStart(marker: Marker) = Unit
            })
        }
        mapView.overlays.add(marker)
        mapView.invalidate()
    }

    private fun updateSummary() {
        val area = listOf(city, district, province).filter { it.isNotBlank() }.joinToString(", ")
        tvSelectedAddress.text = getString(R.string.pick_location_area_format, area)
        tvCoordinates.text = getString(
            R.string.pick_location_coords_format,
            String.format("%.5f", selectedLat),
            String.format("%.5f", selectedLng)
        )
    }

    private fun confirmSelection() {
        val location = SelectedLocation(
            province = province,
            district = district,
            city = city,
            address = getString(
                R.string.pick_location_pin_format,
                String.format("%.5f", selectedLat),
                String.format("%.5f", selectedLng)
            ),
            latitude = selectedLat,
            longitude = selectedLng
        )
        setResult(
            RESULT_OK,
            Intent().apply { SelectedLocation.putExtras(this, location) }
        )
        finish()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        mapView.onPause()
        super.onPause()
    }

    companion object {
        const val EXTRA_PROVINCE = "pick_province"
        const val EXTRA_DISTRICT = "pick_district"
        const val EXTRA_CITY = "pick_city"
        const val EXTRA_LAT = "pick_lat"
        const val EXTRA_LNG = "pick_lng"
    }
}
