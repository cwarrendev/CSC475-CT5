package com.example.locationtracker

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import com.example.locationtracker.ui.theme.LocationTrackerTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startLocationUpdates()
        } else {
            // Handle permission denial
        }
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.map)
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = WebViewClient()

        val htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Leaflet Map</title>
                <meta charset="utf-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <link rel="stylesheet" href="https://unpkg.com/leaflet@1.7.1/dist/leaflet.css" />
                <script src="https://unpkg.com/leaflet@1.7.1/dist/leaflet.js"></script>
                <style>
                    #map {
                        height: 100%;
                    }
                </style>
            </head>
            <body>
                <div id="map" style="height: 100vh;"></div>
                <script>
                    var map;

                    function updateLocation(lat, lon) {
                        map.setView([lat, lon], 13);
                        L.marker([lat, lon]).addTo(map)
                            .bindPopup('Updated Location')
                            .openPopup();
                    }

                    document.addEventListener("DOMContentLoaded", function() {
                        map = L.map('map').setView([51.505, -0.09], 13);

                        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                            attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                        }).addTo(map);

                        L.marker([51.5, -0.09]).addTo(map)
                            .bindPopup('A pretty CSS3 popup.<br> Easily customizable.')
                            .openPopup();
                    });
                </script>
            </body>
            </html>
        """.trimIndent()

        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    updateLocationOnMap(location)
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            startLocationUpdates()
        }

        findViewById<ComposeView>(R.id.compose_view).setContent {
            LocationTrackerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) {
                    MapView()
                }
            }
        }
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setMinUpdateIntervalMillis(5000)
            .build()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            mainLooper
        )
    }

    private fun updateLocationOnMap(location: Location) {
        val js = "javascript:updateLocation(${location.latitude}, ${location.longitude})"
        webView.evaluateJavascript(js, null)
    }
}

@Composable
fun MapView() {
    AndroidView(factory = { context ->
        WebView(context).apply {
            settings.javaScriptEnabled = true
            webViewClient = WebViewClient()
            loadDataWithBaseURL(null, """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Leaflet Map</title>
                    <meta charset="utf-8" />
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.7.1/dist/leaflet.css" />
                    <script src="https://unpkg.com/leaflet@1.7.1/dist/leaflet.js"></script>
                    <style>
                        #map {
                            height: 100%;
                        }
                    </style>
                </head>
                <body>
                    <div id="map" style="height: 100vh;"></div>
                    <script>
                        var map;

                        function updateLocation(lat, lon) {
                            map.setView([lat, lon], 13);
                            L.marker([lat, lon]).addTo(map)
                                .bindPopup('Updated Location')
                                .openPopup();
                        }

                        document.addEventListener("DOMContentLoaded", function() {
                            map = L.map('map').setView([51.505, -0.09], 13);

                            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                                attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                            }).addTo(map);

                            L.marker([51.5, -0.09]).addTo(map)
                                .bindPopup('A pretty CSS3 popup.<br> Easily customizable.')
                                .openPopup();
                        });
                    </script>
                </body>
                </html>
            """.trimIndent(), "text/html", "UTF-8", null)
        }
    })
}