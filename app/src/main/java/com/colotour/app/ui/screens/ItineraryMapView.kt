package com.colotour.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.colotour.app.data.model.ItineraryStop
import com.colotour.app.data.model.StopType
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@Composable
fun ItineraryMapView(
    stops: List<ItineraryStop>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Configurar User-Agent para OSMDroid
    remember {
        Configuration.getInstance().userAgentValue = context.packageName
        true
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            isTilesScaledToDpi = true
        }
    }

    // Administrar el ciclo de vida de MapView
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_DESTROY -> mapView.onDetach()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { map ->
            map.overlays.clear()

            val geoPoints = mutableListOf<GeoPoint>()

            stops.forEach { stop ->
                val lat = stop.latitud
                val lon = stop.longitud
                if (lat != null && lon != null) {
                    val point = GeoPoint(lat, lon)
                    geoPoints.add(point)

                    val marker = Marker(map).apply {
                        position = point
                        val markerIcon = when (stop.type) {
                            StopType.START -> "Punto de Partida"
                            StopType.FOOD -> "Parada Gastronómica"
                            StopType.PLACE -> "Atracción Turística"
                        }
                        title = "[$markerIcon] ${stop.order}. ${stop.titulo}"
                        subDescription = "${stop.horaInicio} - ${stop.descripcion}"
                        snippet = stop.reason
                    }
                    map.overlays.add(marker)
                }
            }

            // Dibujar recorrido simple (Polyline)
            if (geoPoints.size > 1) {
                val polyline = Polyline(map).apply {
                    setPoints(geoPoints)
                    outlinePaint.color = 0xFF2196F3.toInt() // Azul principal
                    outlinePaint.strokeWidth = 5f
                }
                map.overlays.add(polyline)
            }

            // Centrado y zoom óptimo
            if (geoPoints.isNotEmpty()) {
                val lats = geoPoints.map { it.latitude }
                val lons = geoPoints.map { it.longitude }
                val minLat = lats.minOrNull() ?: 0.0
                val maxLat = lats.maxOrNull() ?: 0.0
                val minLon = lons.minOrNull() ?: 0.0
                val maxLon = lons.maxOrNull() ?: 0.0

                val centerLat = (minLat + maxLat) / 2
                val centerLon = (minLon + maxLon) / 2
                val center = GeoPoint(centerLat, centerLon)

                map.controller.setCenter(center)
                val zoom = if (geoPoints.size <= 1) 15.0 else 13.5
                map.controller.setZoom(zoom)
            }
            
            map.invalidate()
        }
    )
}
