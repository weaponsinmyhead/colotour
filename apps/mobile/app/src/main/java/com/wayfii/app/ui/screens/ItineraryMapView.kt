package com.wayfii.app.ui.screens

import android.graphics.DashPathEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.wayfii.app.data.model.ItineraryStop
import com.wayfii.app.data.model.SideQuestItem
import com.wayfii.app.data.model.StopType
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@Composable
fun ItineraryMapView(
    stops: List<ItineraryStop>,
    selectedStopOrder: Int?,
    onMarkerClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    completedStopOrders: Set<Int> = emptySet(),
    sideQuests: List<SideQuestItem> = emptyList(),
    onSideQuestClick: ((SideQuestItem) -> Unit)? = null
) {
    val context = LocalContext.current

    remember {
        Configuration.getInstance().userAgentValue = context.packageName
        true
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            isTilesScaledToDpi = true
            setBuiltInZoomControls(false)
        }
    }

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

            val mainPoints = mutableListOf<GeoPoint>()
            val completedPoints = mutableListOf<GeoPoint>()
            var targetMarker: Marker? = null
            var targetPoint: GeoPoint? = null

            val totalStops = stops.size

            stops.forEachIndexed { index, stop ->
                val lat = stop.latitud
                val lon = stop.longitud
                if (lat != null && lon != null) {
                    val point = GeoPoint(lat, lon)
                    mainPoints.add(point)

                    val isCompleted = completedStopOrders.contains(stop.order)
                    if (isCompleted) {
                        completedPoints.add(point)
                    }

                    val isStart = index == 0 || stop.type == StopType.START
                    val isEnd = index == totalStops - 1

                    val iconLabel = when {
                        isStart -> "🏠 Inicio"
                        isEnd -> "🏁 Misión Cumplida"
                        stop.type == StopType.FOOD -> "🍴 Comida"
                        else -> "⭐ Misión Principal"
                    }

                    val marker = Marker(map).apply {
                        position = point
                        title = "$iconLabel: ${stop.titulo}"
                        subDescription = "${stop.horaInicio} · ${stop.duracionEstimada}"
                        snippet = if (isCompleted) "✅ ¡Completado!" else stop.reason

                        setOnMarkerClickListener { _, _ ->
                            onMarkerClick(stop.order)
                            showInfoWindow()
                            true
                        }
                    }
                    map.overlays.add(marker)

                    if (stop.order == selectedStopOrder) {
                        targetMarker = marker
                        targetPoint = point
                    }
                }
            }

            // Side Quests Markers (Secondary, non-competing)
            sideQuests.forEach { sideQuest ->
                val lat = sideQuest.latitud
                val lon = sideQuest.longitud
                if (lat != null && lon != null) {
                    val point = GeoPoint(lat, lon)
                    val sqMarker = Marker(map).apply {
                        position = point
                        title = "${sideQuest.iconEmoji} ${sideQuest.title}"
                        subDescription = "${sideQuest.category} · ${sideQuest.distanceDetourText}"
                        snippet = sideQuest.description

                        setOnMarkerClickListener { _, _ ->
                            onSideQuestClick?.invoke(sideQuest)
                            showInfoWindow()
                            true
                        }
                    }
                    map.overlays.add(sqMarker)
                }
            }

            // Draw Route Polylines: Solid line for completed parts, Dashed line for remaining parts
            if (completedPoints.size > 1) {
                val solidPolyline = Polyline(map).apply {
                    setPoints(completedPoints)
                    outlinePaint.color = 0xFF00897B.toInt() // Teal Active
                    outlinePaint.strokeWidth = 10f
                    outlinePaint.isAntiAlias = true
                }
                map.overlays.add(solidPolyline)
            }

            if (mainPoints.size > 1) {
                val dashedPolyline = Polyline(map).apply {
                    setPoints(mainPoints)
                    outlinePaint.color = 0xFF80DEEA.toInt() // Soft Teal Dashed
                    outlinePaint.strokeWidth = 8f
                    outlinePaint.isAntiAlias = true
                    outlinePaint.pathEffect = DashPathEffect(floatArrayOf(24f, 16f), 0f)
                }
                map.overlays.add(dashedPolyline)
            }

            // Centering and animation
            if (targetPoint != null && targetMarker != null) {
                map.controller.animateTo(targetPoint)
                if (map.zoomLevelDouble < 15.0) {
                    map.controller.setZoom(16.0)
                }
                targetMarker?.showInfoWindow()
            } else if (mainPoints.isNotEmpty()) {
                val lats = mainPoints.map { it.latitude }
                val lons = mainPoints.map { it.longitude }
                val minLat = lats.minOrNull() ?: 0.0
                val maxLat = lats.maxOrNull() ?: 0.0
                val minLon = lons.minOrNull() ?: 0.0
                val maxLon = lons.maxOrNull() ?: 0.0

                val centerLat = (minLat + maxLat) / 2
                val centerLon = (minLon + maxLon) / 2
                val center = GeoPoint(centerLat, centerLon)

                map.controller.setCenter(center)
                val zoom = if (mainPoints.size <= 1) 15.0 else 13.5
                map.controller.setZoom(zoom)
            }

            map.invalidate()
        }
    )
}
