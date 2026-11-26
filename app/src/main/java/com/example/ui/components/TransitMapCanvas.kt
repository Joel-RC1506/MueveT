package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.BuildConfig
import com.example.R
import com.example.data.model.GpsPoint
import com.example.data.model.TransitRoute
import com.example.ui.theme.CapacityLow
import com.example.ui.theme.CardBorder
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.MapLandBg
import com.example.ui.theme.SageTint
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.Terracotta
import com.example.ui.theme.UserLocationBlue
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import java.io.File
import org.osmdroid.views.overlay.Marker as OsmMarker
import org.osmdroid.views.overlay.Polygon as OsmPolygon
import org.osmdroid.views.overlay.Polyline as OsmPolyline

private const val DEFAULT_TOLUCA_LAT = 19.2826
private const val DEFAULT_TOLUCA_LNG = -99.6557

@Composable
fun TransitMapCanvas(
    routes: List<TransitRoute>,
    selectedRouteId: String,
    hasLocationPermission: Boolean,
    currentGpsLocation: GpsPoint?,
    currentPlaceName: String?,
    recordedGpsTrail: List<GpsPoint>,
    onSelectRoute: (String) -> Unit,
    onRequestLocationOrRecenter: () -> Unit,
    onReportClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasMapsApiKey = remember {
        BuildConfig.MAPS_API_KEY.isNotBlank() && BuildConfig.MAPS_API_KEY != "MY_MAPS_API_KEY"
    }
    if (hasMapsApiKey) {
        GooglePlatformTransitMap(
            routes = routes,
            selectedRouteId = selectedRouteId,
            hasLocationPermission = hasLocationPermission,
            currentGpsLocation = currentGpsLocation,
            currentPlaceName = currentPlaceName,
            recordedGpsTrail = recordedGpsTrail,
            onSelectRoute = onSelectRoute,
            onRequestLocationOrRecenter = onRequestLocationOrRecenter,
            onReportClick = onReportClick,
            modifier = modifier,
        )
    } else {
        GlobalOsmTransitMap(
            routes = routes,
            selectedRouteId = selectedRouteId,
            hasLocationPermission = hasLocationPermission,
            currentGpsLocation = currentGpsLocation,
            currentPlaceName = currentPlaceName,
            recordedGpsTrail = recordedGpsTrail,
            onSelectRoute = onSelectRoute,
            onRequestLocationOrRecenter = onRequestLocationOrRecenter,
            onReportClick = onReportClick,
            modifier = modifier,
        )
    }
}

@SuppressLint("ClickableViewAccessibility")
@Composable
private fun GlobalOsmTransitMap(
    routes: List<TransitRoute>,
    selectedRouteId: String,
    hasLocationPermission: Boolean,
    currentGpsLocation: GpsPoint?,
    currentPlaceName: String?,
    recordedGpsTrail: List<GpsPoint>,
    onSelectRoute: (String) -> Unit,
    onRequestLocationOrRecenter: () -> Unit,
    onReportClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnSelectRoute by rememberUpdatedState(onSelectRoute)

    var followMyLocation by remember { mutableStateOf(true) }
    val lastRenderedSignature = remember { arrayOf("") }

    remember(context) {
        Configuration.getInstance().apply {
            userAgentValue = "MueveT/${BuildConfig.VERSION_NAME} (${context.packageName})"
            val osmBase = File(context.cacheDir, "osmdroid").apply { mkdirs() }
            val osmTiles = File(osmBase, "tiles").apply { mkdirs() }
            osmdroidBasePath = osmBase
            osmdroidTileCache = osmTiles
        }
    }

    val userDotDrawable = remember(context) { createUserLocationDrawable(context) }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            isHorizontalMapRepetitionEnabled = true
            isVerticalMapRepetitionEnabled = false
            minZoomLevel = 2.5
            maxZoomLevel = 19.5
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            val startPoint = currentGpsLocation?.let { GeoPoint(it.lat, it.lng) }
                ?: GeoPoint(DEFAULT_TOLUCA_LAT, DEFAULT_TOLUCA_LNG)
            controller.setZoom(14.5)
            controller.setCenter(startPoint)

            var touchStartX = 0f
            var touchStartY = 0f
            setOnTouchListener { v, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        touchStartX = event.x
                        touchStartY = event.y
                        v.parent?.requestDisallowInterceptTouchEvent(true)
                    }
                    MotionEvent.ACTION_MOVE -> {
                        v.parent?.requestDisallowInterceptTouchEvent(true)
                        val dx = event.x - touchStartX
                        val dy = event.y - touchStartY
                        if (dx * dx + dy * dy > 144f) {
                            followMyLocation = false
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        v.parent?.requestDisallowInterceptTouchEvent(false)
                    }
                }
                false
            }
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    LaunchedEffect(currentGpsLocation, followMyLocation) {
        if (followMyLocation && currentGpsLocation != null) {
            val target = GeoPoint(currentGpsLocation.lat, currentGpsLocation.lng)
            if (mapView.zoomLevelDouble < 13.0) {
                mapView.controller.animateTo(target, 14.5, 450L)
            } else {
                mapView.controller.animateTo(target)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(296.dp)
            .clipToBounds()
            .background(MapLandBg)
    ) {
        AndroidView(
            factory = { mapView },
            update = { mv ->
                val sig = buildString {
                    append(selectedRouteId)
                    append("|")
                    append(routes.joinToString(",") { "${it.id}:${it.pathPoints.size}:${it.lastUpdatedAt}" })
                    append("|")
                    append(currentGpsLocation?.let { "${it.lat},${it.lng}" } ?: "nogps")
                    append("|")
                    append(recordedGpsTrail.size)
                }
                if (sig != lastRenderedSignature[0]) {
                    lastRenderedSignature[0] = sig
                    mv.overlays.clear()

                    // Non-selected routes first, selected route on top
                    val orderedRoutes = routes.sortedBy { it.id == selectedRouteId }
                    orderedRoutes.forEach { route ->
                        val pts = route.pathPoints.map { GeoPoint(it.lat, it.lng) }
                        if (pts.size >= 2) {
                            val isSelected = route.id == selectedRouteId
                            val routeColor = Color(route.badgeColorHex)
                            if (isSelected) {
                                val casing = OsmPolyline(mv).apply {
                                    setPoints(pts)
                                    outlinePaint.color = android.graphics.Color.WHITE
                                    outlinePaint.strokeWidth = 17f
                                    outlinePaint.strokeCap = Paint.Cap.ROUND
                                    outlinePaint.strokeJoin = Paint.Join.ROUND
                                }
                                mv.overlays.add(casing)
                            }
                            val polyline = OsmPolyline(mv).apply {
                                setPoints(pts)
                                outlinePaint.color = routeColor.copy(alpha = if (isSelected) 0.96f else 0.65f).toArgb()
                                outlinePaint.strokeWidth = if (isSelected) 11f else 6f
                                outlinePaint.strokeCap = Paint.Cap.ROUND
                                outlinePaint.strokeJoin = Paint.Join.ROUND
                                if (route.isDashed) {
                                    outlinePaint.pathEffect = DashPathEffect(floatArrayOf(22f, 14f), 0f)
                                }
                                setOnClickListener { _, _, _ ->
                                    currentOnSelectRoute(route.id)
                                    true
                                }
                            }
                            mv.overlays.add(polyline)
                        }
                    }

                    // Live recorded GPS trail while contributing
                    if (recordedGpsTrail.size >= 2) {
                        val trailLine = OsmPolyline(mv).apply {
                            setPoints(recordedGpsTrail.map { GeoPoint(it.lat, it.lng) })
                            outlinePaint.color = ForestGreen.toArgb()
                            outlinePaint.strokeWidth = 10f
                            outlinePaint.strokeCap = Paint.Cap.ROUND
                            outlinePaint.strokeJoin = Paint.Join.ROUND
                        }
                        mv.overlays.add(trailLine)
                    }

                    // Stops for selected route
                    val selectedRoute = routes.firstOrNull { it.id == selectedRouteId }
                    selectedRoute?.stops?.forEach { stop ->
                        val stopIcon = createStopDotDrawable(
                            context = context,
                            fillColor = Color(selectedRoute.badgeColorHex).toArgb(),
                        )
                        val marker = OsmMarker(mv).apply {
                            position = GeoPoint(stop.lat, stop.lng)
                            title = stop.name
                            snippet = "Ruta ${selectedRoute.code} · ${selectedRoute.formattedPrice}"
                            setAnchor(OsmMarker.ANCHOR_CENTER, OsmMarker.ANCHOR_CENTER)
                            icon = stopIcon
                        }
                        mv.overlays.add(marker)
                    }

                    // Live User Location Halo + Dot
                    if (currentGpsLocation != null) {
                        val userGeo = GeoPoint(currentGpsLocation.lat, currentGpsLocation.lng)
                        val halo = OsmPolygon(mv).apply {
                            points = OsmPolygon.pointsAsCircle(userGeo, 85.0)
                            fillPaint.color = UserLocationBlue.copy(alpha = 0.16f).toArgb()
                            outlinePaint.color = UserLocationBlue.copy(alpha = 0.45f).toArgb()
                            outlinePaint.strokeWidth = 2f
                        }
                        mv.overlays.add(halo)

                        val userMarker = OsmMarker(mv).apply {
                            position = userGeo
                            title = currentPlaceName ?: "Tu ubicación actual"
                            snippet = String.format("%.4f, %.4f", currentGpsLocation.lat, currentGpsLocation.lng)
                            setAnchor(OsmMarker.ANCHOR_CENTER, OsmMarker.ANCHOR_CENTER)
                            icon = userDotDrawable
                        }
                        mv.overlays.add(userMarker)
                    }

                    mv.invalidate()
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        MapOverlayControls(
            zoneLabel = "Explorando ${currentPlaceName ?: "Toluca"}",
            hasLocationPermission = hasLocationPermission,
            currentGpsLocation = currentGpsLocation,
            isFollowingLocation = followMyLocation,
            onZoomIn = { mapView.controller.zoomIn() },
            onZoomOut = { mapView.controller.zoomOut() },
            onRecenterClick = {
                followMyLocation = true
                onRequestLocationOrRecenter()
                val target = currentGpsLocation?.let { GeoPoint(it.lat, it.lng) }
                    ?: GeoPoint(DEFAULT_TOLUCA_LAT, DEFAULT_TOLUCA_LNG)
                mapView.controller.animateTo(target, 15.0, 450L)
            },
            onGpsPillClick = {
                followMyLocation = true
                onRequestLocationOrRecenter()
                currentGpsLocation?.let { loc ->
                    mapView.controller.animateTo(GeoPoint(loc.lat, loc.lng), 15.0, 450L)
                }
            },
            onReportClick = onReportClick,
        )
    }
}

@Composable
private fun GooglePlatformTransitMap(
    routes: List<TransitRoute>,
    selectedRouteId: String,
    hasLocationPermission: Boolean,
    currentGpsLocation: GpsPoint?,
    currentPlaceName: String?,
    recordedGpsTrail: List<GpsPoint>,
    onSelectRoute: (String) -> Unit,
    onRequestLocationOrRecenter: () -> Unit,
    onReportClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val defaultToluca = remember { LatLng(DEFAULT_TOLUCA_LAT, DEFAULT_TOLUCA_LNG) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            currentGpsLocation?.let { LatLng(it.lat, it.lng) } ?: defaultToluca,
            14.2f,
        )
    }
    var followMyLocation by remember { mutableStateOf(true) }

    LaunchedEffect(cameraPositionState.isMoving) {
        if (cameraPositionState.isMoving &&
            cameraPositionState.cameraMoveStartedReason ==
            com.google.maps.android.compose.CameraMoveStartedReason.GESTURE
        ) {
            followMyLocation = false
        }
    }

    LaunchedEffect(currentGpsLocation, followMyLocation) {
        if (followMyLocation && currentGpsLocation != null) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(currentGpsLocation.lat, currentGpsLocation.lng),
                    maxOf(cameraPositionState.position.zoom, 14.5f),
                )
            )
        }
    }

    val selectedRoute = routes.firstOrNull { it.id == selectedRouteId }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(296.dp)
            .clipToBounds()
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false,
                mapToolbarEnabled = false,
            ),
        ) {
            routes.forEach { route ->
                val isSelected = route.id == selectedRouteId
                val points = route.pathPoints.map { LatLng(it.lat, it.lng) }
                if (points.size >= 2) {
                    Polyline(
                        points = points,
                        color = Color(route.badgeColorHex).copy(alpha = if (isSelected) 1f else 0.55f),
                        width = if (isSelected) 12f else 6f,
                        pattern = if (route.isDashed) listOf(Dash(20f), Gap(12f)) else null,
                        clickable = true,
                        onClick = { onSelectRoute(route.id) },
                    )
                }
            }

            if (recordedGpsTrail.size >= 2) {
                Polyline(
                    points = recordedGpsTrail.map { LatLng(it.lat, it.lng) },
                    color = ForestGreen,
                    width = 10f,
                )
            }

            selectedRoute?.stops?.forEach { stop ->
                Marker(
                    state = MarkerState(position = LatLng(stop.lat, stop.lng)),
                    title = stop.name,
                    snippet = "Ruta ${selectedRoute.code} · ${selectedRoute.formattedPrice}",
                )
            }
        }

        MapOverlayControls(
            zoneLabel = "Google Maps · ${currentPlaceName ?: "Toluca"}",
            hasLocationPermission = hasLocationPermission,
            currentGpsLocation = currentGpsLocation,
            isFollowingLocation = followMyLocation,
            onZoomIn = { scope.launch { cameraPositionState.animate(CameraUpdateFactory.zoomIn()) } },
            onZoomOut = { scope.launch { cameraPositionState.animate(CameraUpdateFactory.zoomOut()) } },
            onRecenterClick = {
                followMyLocation = true
                onRequestLocationOrRecenter()
                val target = currentGpsLocation?.let { LatLng(it.lat, it.lng) } ?: defaultToluca
                scope.launch {
                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(target, 15f))
                }
            },
            onGpsPillClick = {
                followMyLocation = true
                onRequestLocationOrRecenter()
                currentGpsLocation?.let { loc ->
                    scope.launch {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(LatLng(loc.lat, loc.lng), 15f)
                        )
                    }
                }
            },
            onReportClick = onReportClick,
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.MapOverlayControls(
    zoneLabel: String,
    hasLocationPermission: Boolean,
    currentGpsLocation: GpsPoint?,
    isFollowingLocation: Boolean,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onRecenterClick: () -> Unit,
    onGpsPillClick: () -> Unit,
    onReportClick: () -> Unit,
) {
    // Top-left zone pill
    Surface(
        shape = RoundedCornerShape(50),
        color = SurfaceWhite.copy(alpha = 0.96f),
        shadowElevation = 3.dp,
        modifier = Modifier
            .align(Alignment.TopStart)
            .padding(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(CapacityLow)
            )
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                text = zoneLabel,
                style = MaterialTheme.typography.labelMedium,
                color = InkPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }

    // Top-right GPS status pill
    Surface(
        shape = RoundedCornerShape(50),
        color = SurfaceWhite.copy(alpha = 0.96f),
        shadowElevation = 3.dp,
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(12.dp)
            .clickable { onGpsPillClick() }
            .testTag("map_gps_status_pill"),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (hasLocationPermission) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(CapacityLow)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = when {
                    hasLocationPermission && currentGpsLocation != null ->
                        "GPS ${String.format("%.3f, %.3f", currentGpsLocation.lat, currentGpsLocation.lng)}"
                    hasLocationPermission -> "Buscando GPS..."
                    else -> "Activar GPS"
                },
                style = MaterialTheme.typography.labelMedium,
                color = if (hasLocationPermission) ForestGreen else InkPrimary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }

    // Right-side Zoom controls
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SurfaceWhite.copy(alpha = 0.97f),
        shadowElevation = 4.dp,
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .padding(end = 12.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(
                onClick = onZoomIn,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("map_zoom_in"),
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.cd_zoom_in),
                    tint = InkPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Box(
                modifier = Modifier
                    .width(28.dp)
                    .height(1.dp)
                    .background(CardBorder)
            )
            IconButton(
                onClick = onZoomOut,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("map_zoom_out"),
            ) {
                Icon(
                    imageVector = Icons.Filled.Remove,
                    contentDescription = stringResource(R.string.cd_zoom_out),
                    tint = InkPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }

    // Bottom-left Recenter / Follow live location button
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (hasLocationPermission && isFollowingLocation) SageTint else SurfaceWhite.copy(alpha = 0.97f),
        shadowElevation = 4.dp,
        modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(12.dp)
            .size(48.dp)
            .testTag("map_recenter_button"),
        onClick = onRecenterClick,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Filled.NearMe,
                contentDescription = stringResource(R.string.cd_recenter_map),
                tint = if (hasLocationPermission && isFollowingLocation) ForestGreen else InkPrimary,
                modifier = Modifier.size(20.dp),
            )
        }
    }

    // Bottom-right Terracotta CTA pill
    Surface(
        shape = RoundedCornerShape(50),
        color = Terracotta,
        shadowElevation = 5.dp,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(12.dp)
            .testTag("map_report_button"),
        onClick = onReportClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                tint = InkPrimary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.report_route_cta),
                style = MaterialTheme.typography.labelLarge,
                color = InkPrimary,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private fun createUserLocationDrawable(context: Context): Drawable {
    val density = context.resources.displayMetrics.density
    val sizePx = (24f * density).toInt().coerceAtLeast(32)
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = sizePx / 2f

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(center, center, center * 0.85f, borderPaint)

    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = UserLocationBlue.toArgb()
        style = Paint.Style.FILL
    }
    canvas.drawCircle(center, center, center * 0.58f, fillPaint)

    return BitmapDrawable(context.resources, bitmap)
}

private fun createStopDotDrawable(context: Context, fillColor: Int): Drawable {
    val density = context.resources.displayMetrics.density
    val sizePx = (14f * density).toInt().coerceAtLeast(20)
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = sizePx / 2f

    val outerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fillColor
        style = Paint.Style.FILL
    }
    canvas.drawCircle(center, center, center * 0.9f, outerPaint)

    val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(center, center, center * 0.5f, innerPaint)

    return BitmapDrawable(context.resources, bitmap)
}
