package com.example.reto9

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.config.Configuration
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import okhttp3.MediaType.Companion.toMediaType
import kotlin.math.*

class MainActivity : ComponentActivity(), LocationListener {

    private lateinit var locationManager: LocationManager
    private lateinit var mapView: MapView
    private var userLocation: GeoPoint? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(this, getSharedPreferences("osm", MODE_PRIVATE))

        setContent {
            MaterialTheme {
                GPSMapScreen()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun GPSMapScreen() {
        val context = LocalContext.current
        var radioKm by remember { mutableStateOf(2.0) } // Radio en km
        var showDialog by remember { mutableStateOf(false) }

        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) getLocation(context)
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Mapa GPS - Puntos de Interés") },
                    actions = {
                        IconButton(onClick = { showDialog = true }) {
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(
                                    android.R.drawable.ic_menu_manage
                                ),
                                contentDescription = "Configurar radio"
                            )
                        }
                        IconButton(onClick = {
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                getLocation(context)
                            } else {
                                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            }
                        }) {
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(
                                    android.R.drawable.ic_menu_mylocation
                                ),
                                contentDescription = "Actualizar ubicación"
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                AndroidView(
                    factory = {
                        mapView = MapView(it)
                        mapView.setMultiTouchControls(true)
                        mapView.controller.setZoom(15.0)
                        mapView
                    },
                    update = {
                        userLocation?.let { loc -> mapView.controller.animateTo(loc) }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Diálogo para cambiar el radio
            if (showDialog) {
                var newRadio by remember { mutableStateOf(radioKm.toString()) }
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    confirmButton = {
                        TextButton(onClick = {
                            radioKm = newRadio.toDoubleOrNull() ?: 2.0
                            showDialog = false
                            // Guardar preferencia (opcional)
                            val prefs = context.getSharedPreferences("config", Context.MODE_PRIVATE)
                            prefs.edit().putFloat("radio_km", radioKm.toFloat()).apply()
                        }) {
                            Text("Guardar")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDialog = false }) { Text("Cancelar") }
                    },
                    title = { Text("Configurar radio de búsqueda") },
                    text = {
                        OutlinedTextField(
                            value = newRadio,
                            onValueChange = { newRadio = it },
                            label = { Text("Radio en kilómetros") },
                            singleLine = true
                        )
                    }
                )
            }
        }
    }


    private fun getLocation(context: Context) {
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        try {
            val gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val netEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (!gpsEnabled && !netEnabled) {
                android.util.Log.e("GPS", "Ningún proveedor disponible")
                return
            }

            // Última ubicación conocida
            val lastGps = if (gpsEnabled) locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) else null
            val lastNet = if (netEnabled) locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) else null

            val bestLast = when {
                lastGps != null && lastNet != null -> if (lastGps.time > lastNet.time) lastGps else lastNet
                lastGps != null -> lastGps
                lastNet != null -> lastNet
                else -> null
            }

            if (bestLast != null) {
                onLocationChanged(bestLast)
            }

            // Pide actualizaciones SOLO si el proveedor existe
            if (gpsEnabled) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    2000L,
                    5f,
                    this
                )
            }

            if (netEnabled) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    2000L,
                    5f,
                    this
                )
            }

        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: Exception) {
            // Evita que la tablet cierre la app
            e.printStackTrace()
        }
    }


    override fun onLocationChanged(location: Location) {
        userLocation = GeoPoint(location.latitude, location.longitude)
        mapView.overlays.clear()

        val userMarker = Marker(mapView).apply {
            position = userLocation
            title = "Tu ubicación"
            icon = androidx.core.content.ContextCompat.getDrawable(
                this@MainActivity,
                R.drawable.ic_user_location   // 👈 icono diferente
            )
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }

        mapView.overlays.add(userMarker)
        mapView.controller.setZoom(16.0)
        mapView.controller.animateTo(userLocation)

        val prefs = getSharedPreferences("config", Context.MODE_PRIVATE)
        val radioKm = prefs.getFloat("radio_km", 2.0f).toDouble()
        getRealPOIs(userLocation!!, radioKm)
    }


    private fun getRealPOIs(center: GeoPoint, radioKm: Double) {
        Thread {
            try {
                val overpassUrl = "https://overpass-api.de/api/interpreter"
                val query = """
                [out:json];
                (
                  node["amenity"~"hospital|school|restaurant|park|museum"](
                    around:${(radioKm * 1000).toInt()},${center.latitude},${center.longitude}
                  );
                );
                out;
            """.trimIndent()

                val client = okhttp3.OkHttpClient()
                val requestBody = okhttp3.RequestBody.create(
                    "application/x-www-form-urlencoded".toMediaType(),
                    "data=$query"
                )
                val request = okhttp3.Request.Builder()
                    .url(overpassUrl)
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: return@Thread

                val json = org.json.JSONObject(body)
                val elements = json.getJSONArray("elements")

                runOnUiThread {
                    for (i in 0 until elements.length()) {
                        val obj = elements.getJSONObject(i)
                        val lat = obj.getDouble("lat")
                        val lon = obj.getDouble("lon")

                        val tags = obj.optJSONObject("tags")
                        val name = tags?.optString("name") ?: "Punto sin nombre"
                        val type = tags?.optString("amenity") ?: "POI"

                        val poiPoint = GeoPoint(lat, lon)
                        val marker = Marker(mapView)
                        marker.position = poiPoint
                        marker.title = "$name\nTipo: $type"
                        mapView.overlays.add(marker)
                    }
                    mapView.invalidate()
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }



    private fun distanceInKm(a: GeoPoint, b: GeoPoint): Double {
        val earthRadius = 6371.0 // km
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val haversine = sin(dLat / 2).pow(2.0) + sin(dLon / 2).pow(2.0) * cos(lat1) * cos(lat2)
        return 2 * earthRadius * asin(sqrt(haversine))
    }

    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
}
