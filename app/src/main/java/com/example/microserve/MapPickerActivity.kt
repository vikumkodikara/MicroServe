package com.example.microserve

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.util.Locale

/**
 * Full-screen OSMDroid map picker.
 *
 * The user pans/zooms the map — a fixed crosshair pin always sits at the screen centre.
 * Tapping "Confirm Location" reverse-geocodes the centre point and returns the
 * address string (or lat,lon fallback) to the caller via setResult().
 *
 * Result extras:
 *   SELECTED_ADDRESS  – human-readable address string
 *   SELECTED_LAT      – Double latitude
 *   SELECTED_LNG      – Double longitude
 */
class MapPickerActivity : AppCompatActivity() {

    private lateinit var mapView: MapView

    companion object {
        const val EXTRA_ADDRESS = "SELECTED_ADDRESS"
        const val EXTRA_LAT     = "SELECTED_LAT"
        const val EXTRA_LNG     = "SELECTED_LNG"
        const val REQUEST_CODE  = 2001

        fun launch(from: Activity) {
            from.startActivityForResult(
                Intent(from, MapPickerActivity::class.java),
                REQUEST_CODE
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // OSMDroid requires user-agent configuration
        Configuration.getInstance().load(
            this,
            getSharedPreferences("osm_prefs", Context.MODE_PRIVATE)
        )
        Configuration.getInstance().userAgentValue = packageName

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.statusBarColor = Color.TRANSPARENT
        }

        setContentView(R.layout.activity_map_picker)

        mapView = findViewById(R.id.mapView)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)

        // Default centre: Colombo, Sri Lanka (adjust if needed)
        val defaultPoint = GeoPoint(6.9271, 79.8612)
        mapView.controller.setZoom(15.0)
        mapView.controller.setCenter(defaultPoint)

        val btnConfirm: Button = findViewById(R.id.btnConfirmLocation)
        btnConfirm.setOnClickListener {
            val centre = mapView.mapCenter
            val lat    = centre.latitude
            val lng    = centre.longitude

            val address = reverseGeocode(lat, lng)
                ?: "%.5f, %.5f".format(lat, lng)   // fallback to coordinates

            val result = Intent().apply {
                putExtra(EXTRA_ADDRESS, address)
                putExtra(EXTRA_LAT, lat)
                putExtra(EXTRA_LNG, lng)
            }
            setResult(Activity.RESULT_OK, result)
            finish()
        }
    }

    /** Uses Android's built-in Geocoder to resolve lat/lng → readable address. */
    private fun reverseGeocode(lat: Double, lng: Double): String? {
        return try {
            val geocoder   = Geocoder(this, Locale.getDefault())
            val addresses  = geocoder.getFromLocation(lat, lng, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                buildString {
                    addr.subLocality?.let    { append("$it, ") }
                    addr.locality?.let       { append("$it, ") }
                    addr.adminArea?.let      { append(it) }
                }.trimEnd(',', ' ').ifBlank { null }
            } else null
        } catch (e: Exception) {
            null
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDetach()
    }
}
