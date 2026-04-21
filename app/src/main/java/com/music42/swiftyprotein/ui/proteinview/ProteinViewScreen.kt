package com.music42.swiftyprotein.ui.proteinview

import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.PixelCopy
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.graphics.PixelFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.hilt.navigation.compose.hiltViewModel
import com.music42.swiftyprotein.data.model.Atom
import com.music42.swiftyprotein.data.model.Bond
import com.music42.swiftyprotein.data.model.BondOrder
import com.music42.swiftyprotein.data.model.Ligand
import io.github.sceneview.Scene
import io.github.sceneview.SceneView
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import dev.romainguy.kotlin.math.Float3
import kotlin.math.hypot
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProteinViewScreen(
    ligandId: String,
    onBack: () -> Unit,
    viewModel: ProteinViewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val sceneTint = if (MaterialTheme.colorScheme.background.red < 0.4f) {
        Color(0xFF151A20)
    } else {
        Color(0xFFF3F5F7)
    }
    val safeLigandId = ligandId.substringBefore(" -").trim().ifEmpty { ligandId.trim() }
    var zoomFactor by remember(uiState.ligand?.id) { mutableFloatStateOf(1f) }
    var showShareFormatDialog by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val recorderHolder = remember { arrayOf<ScreenRecorder?>(null) }

    val projectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val activity = context as? Activity ?: return@rememberLauncherForActivityResult
        if (result.resultCode != Activity.RESULT_OK || result.data == null) {
            isRecording = false
            return@rememberLauncherForActivityResult
        }
        val mgr = activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val projection = mgr.getMediaProjection(result.resultCode, result.data!!)
        val file = File(context.cacheDir, "shared_videos/ligand_${safeLigandId}.mp4").apply {
            parentFile?.mkdirs()
            if (exists()) delete()
        }

        val rec = ScreenRecorder(activity, projection, file)
        recorderHolder[0] = rec
        isRecording = true
        rec.start()

        coroutineScope.launch {
            delay(5_000)
            val stoppedFile = runCatching { rec.stop() }.getOrNull()
            isRecording = false
            recorderHolder[0] = null
            if (stoppedFile != null && stoppedFile.exists()) {
                shareVideo(context, stoppedFile, safeLigandId)
            }
        }
    }

    Scaffold(
        containerColor = sceneTint,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.ligand?.id?.substringBefore(" -")?.trim().orEmpty()
                            .ifEmpty { safeLigandId },
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (isRecording) return@IconButton
                            val activity = context as? Activity ?: return@IconButton
                            val mgr = activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                            projectionLauncher.launch(mgr.createScreenCaptureIntent())
                        }
                    ) {
                        Icon(
                            Icons.Default.Videocam,
                            contentDescription = "Record video",
                            tint = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { viewModel.setMeasurementMode(!uiState.measurementMode) }) {
                        Icon(
                            Icons.Default.Straighten,
                            contentDescription = "Toggle measurement mode",
                            tint = if (uiState.measurementMode)
                                MaterialTheme.colorScheme.tertiary
                            else
                                MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { viewModel.setShowAtomLabels(!uiState.showAtomLabels) }) {
                        Icon(
                            Icons.Default.Label,
                            contentDescription = "Toggle atom labels"
                        )
                    }
                    IconButton(onClick = {
                        showShareFormatDialog = true
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary,
                    actionIconContentColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(sceneTint)
        ) {
            when {
                uiState.isLoading -> {
                    Card(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 24.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 28.dp, vertical = 22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(42.dp),
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Loading ligand $safeLigandId",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = uiState.loadingStage,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                modifier = Modifier.width(180.dp),
                                progress = { uiState.loadingProgress.coerceIn(0f, 1f) }
                            )
                        }
                    }
                }

                uiState.errorMessage != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Failed to load ligand",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(uiState.errorMessage!!)
                    }
                }

                uiState.ligand != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        MoleculeViewer(
                            ligand = uiState.ligand!!,
                            mode = uiState.visualizationMode,
                            zoomFactor = zoomFactor,
                            onZoomFactorChange = { zoomFactor = it },
                            selectedAtom = uiState.selectedAtom,
                            onAtomSelected = viewModel::onAtomSelected,
                            onDismissAtom = viewModel::dismissAtomInfo,
                            selectedBond = uiState.selectedBond,
                            onBondSelected = viewModel::onBondSelected,
                            onDismissBond = viewModel::dismissBondInfo,
                            showAtomLabels = uiState.showAtomLabels,
                            measurementMode = uiState.measurementMode,
                            measurementAtomIds = uiState.measurementAtomIds,
                            onMeasurementAtomTapped = viewModel::onAtomTappedForMeasurement,
                            onClearMeasurement = viewModel::clearMeasurement,
                            autoRotate = isRecording,
                            sceneBackground = sceneTint,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 92.dp)
                                .clip(RoundedCornerShape(24.dp))
                        )

                        AnimatedVisibility(
                            visible = uiState.measurementMode,
                            enter = fadeIn(),
                            exit = fadeOut(),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 12.dp)
                        ) {
                            Card(
                                shape = RoundedCornerShape(999.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                            ) {
                                Text(
                                    text = "MEASURE MODE",
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        if (uiState.selectedAtom != null) {
                            androidx.compose.ui.window.Popup(
                                alignment = Alignment.TopStart,
                                onDismissRequest = { viewModel.dismissAtomInfo() }
                            ) {
                                Box(modifier = Modifier.padding(start = 8.dp, top = 8.dp)) {
                                    AtomTooltip(
                                        atom = uiState.selectedAtom!!,
                                        onDismiss = viewModel::dismissAtomInfo
                                    )
                                }
                            }
                        }

                        if (uiState.selectedBond != null) {
                            androidx.compose.ui.window.Popup(
                                alignment = Alignment.TopStart,
                                onDismissRequest = { viewModel.dismissBondInfo() }
                            ) {
                                Box(modifier = Modifier.padding(start = 8.dp, top = 8.dp)) {
                                    BondTooltip(
                                        bond = uiState.selectedBond!!,
                                        ligand = uiState.ligand!!,
                                        onDismiss = viewModel::dismissBondInfo
                                    )
                                }
                            }
                        }

                        androidx.compose.ui.window.Popup(alignment = Alignment.TopEnd) {
                            Column(
                                modifier = Modifier.padding(end = 8.dp, top = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Card(
                                    modifier = Modifier.size(42.dp),
                                    shape = CircleShape,
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    IconButton(
                                        onClick = { zoomFactor = (zoomFactor * 1.2f).coerceIn(0.3f, 5.0f) },
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = "Zoom in",
                                            tint = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Card(
                                    modifier = Modifier.size(42.dp),
                                    shape = CircleShape,
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    IconButton(
                                        onClick = { zoomFactor = (zoomFactor / 1.2f).coerceIn(0.3f, 5.0f) },
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Icon(
                                            Icons.Default.Remove,
                                            contentDescription = "Zoom out",
                                            tint = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                            }
                        }

                        Card(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                VisualizationMode.entries.forEach { mode ->
                                    FilterChip(
                                        modifier = Modifier.weight(1f),
                                        selected = uiState.visualizationMode == mode,
                                        onClick = { viewModel.setVisualizationMode(mode) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        label = {
                                            Text(
                                                modifier = Modifier.fillMaxWidth(),
                                                text = when (mode) {
                                                    VisualizationMode.BALL_AND_STICK -> "Ball & Stick"
                                                    VisualizationMode.SPACE_FILL -> "Space Fill"
                                                    VisualizationMode.STICKS_ONLY -> "Sticks"
                                                    VisualizationMode.WIREFRAME -> "Wireframe"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                textAlign = TextAlign.Center,
                                                maxLines = 1
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showShareFormatDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showShareFormatDialog = false },
            title = { Text("Export format") },
            text = { Text("Choose image format to share.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showShareFormatDialog = false
                    shareModelScreenshot(
                        context = context,
                        ligandId = safeLigandId,
                        ligandName = uiState.ligand?.name.orEmpty(),
                        atomCount = uiState.ligand?.atoms?.size,
                        formula = uiState.ligand?.formula.orEmpty(),
                        format = ShareFormat.PNG
                    )
                }) { Text("PNG") }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.TextButton(onClick = {
                        showShareFormatDialog = false
                        shareModelScreenshot(
                            context = context,
                            ligandId = safeLigandId,
                            ligandName = uiState.ligand?.name.orEmpty(),
                            atomCount = uiState.ligand?.atoms?.size,
                            formula = uiState.ligand?.formula.orEmpty(),
                            format = ShareFormat.JPEG
                        )
                    }) { Text("JPEG") }
                    androidx.compose.material3.TextButton(onClick = { showShareFormatDialog = false }) { Text("Cancel") }
                }
            }
        )
    }
}

@Composable
private fun MoleculeViewer(
    ligand: Ligand,
    mode: VisualizationMode,
    zoomFactor: Float,
    onZoomFactorChange: (Float) -> Unit,
    selectedAtom: Atom?,
    onAtomSelected: (Atom?) -> Unit,
    onDismissAtom: () -> Unit,
    selectedBond: Bond?,
    onBondSelected: (Bond?) -> Unit,
    onDismissBond: () -> Unit,
    showAtomLabels: Boolean,
    measurementMode: Boolean,
    measurementAtomIds: List<String>,
    onMeasurementAtomTapped: (Atom) -> Unit,
    onClearMeasurement: () -> Unit,
    autoRotate: Boolean,
    sceneBackground: Color,
    modifier: Modifier = Modifier
) {
    val engine = rememberEngine()
    val materialLoader = rememberMaterialLoader(engine)

    val lastTap = remember { longArrayOf(0L) }
    val lastTapAtomId = remember { arrayOfNulls<String>(1) }
    var focusTarget by remember(ligand.id) { mutableStateOf<Float3?>(null) }
    var focusOffset by remember(ligand.id) { mutableStateOf(Float3(0f, 0f, 0f)) }
    var labelPositions by remember { mutableStateOf<Map<String, Offset>>(emptyMap()) }
    var labelFrameCounter by remember { mutableIntStateOf(0) }

    val (parentNode, atomNodeMap) = remember(ligand, mode, selectedAtom?.element, focusOffset) {
        MoleculeSceneBuilder.build(
            engine = engine,
            materialLoader = materialLoader,
            ligand = ligand,
            mode = mode,
            highlightElement = selectedAtom?.element,
            centerOffset = focusOffset
        )
    }

    val tapDownPos = remember { floatArrayOf(0f, 0f) }
    val tapDownTime = remember { longArrayOf(0L) }
    val sceneViewRef = remember { arrayOfNulls<SceneView>(1) }
    val panTarget = remember(ligand.id) { floatArrayOf(0f, 0f) } // x,y world units
    val twoFinger = remember { floatArrayOf(0f, 0f, 0f) } // active(0/1), lastMidX, lastMidY
    val twoFingerSpan = remember { floatArrayOf(0f) } // lastSpan

    val cameraNode = rememberCameraNode(engine).apply {
        near = 0.1f
        far = 1000.0f
    }

    val maxCoord = ligand.atoms.maxOfOrNull {
        maxOf(
            kotlin.math.abs(it.x),
            kotlin.math.abs(it.y),
            kotlin.math.abs(it.z)
        )
    } ?: 5f
    val baseCameraDistance = maxCoord * 2.5f
    val distance = (baseCameraDistance / zoomFactor).coerceIn(baseCameraDistance * 0.3f, baseCameraDistance * 4f)
    val lastCameraVector = remember(ligand.id) { floatArrayOf(0f, 0f, baseCameraDistance) }
    val cameraPosition = remember(ligand.id, zoomFactor) {
        val x = lastCameraVector[0]
        val y = lastCameraVector[1]
        val z = lastCameraVector[2]
        val len = kotlin.math.sqrt(x * x + y * y + z * z)
        if (len > 0.0001f) {
            io.github.sceneview.math.Position(
                x / len * distance,
                y / len * distance,
                z / len * distance
            )
        } else {
            io.github.sceneview.math.Position(0f, 0f, distance)
        }
    }
    cameraNode.position = cameraPosition

    val cameraManipulator = remember(ligand.id, zoomFactor) {
        SceneView.createDefaultCameraManipulator(
            orbitHomePosition = cameraPosition,
            targetPosition = io.github.sceneview.math.Position(panTarget[0], panTarget[1], 0f)
        )
    }

    var autoRotateAngle by remember(ligand.id) { mutableFloatStateOf(0f) }
    var sceneReady by remember(ligand.id) { mutableStateOf(false) }

    Box(
        modifier = modifier
            .background(sceneBackground)
    ) {
        Scene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            // Opaque avoids brief black/transparent composition artifacts on SurfaceView.
            isOpaque = true,
            materialLoader = materialLoader,
            cameraNode = cameraNode,
            cameraManipulator = cameraManipulator,
            childNodes = listOf(parentNode),
            onTouchEvent = { event, _ ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_POINTER_DOWN -> {
                        if (event.pointerCount == 2) {
                            val x0 = event.getX(0)
                            val y0 = event.getY(0)
                            val x1 = event.getX(1)
                            val y1 = event.getY(1)
                            twoFinger[0] = 1f
                            twoFinger[1] = (x0 + x1) / 2f
                            twoFinger[2] = (y0 + y1) / 2f
                            twoFingerSpan[0] = hypot(x1 - x0, y1 - y0).coerceAtLeast(1f)
                        }
                        false
                    }
                    MotionEvent.ACTION_SCROLL -> {
                        val wheel = event.getAxisValue(MotionEvent.AXIS_VSCROLL)
                        val genericScroll = event.getAxisValue(MotionEvent.AXIS_SCROLL)
                        val hScroll = event.getAxisValue(MotionEvent.AXIS_HSCROLL)
                        val scroll = if (wheel != 0f) wheel else genericScroll
                        if (scroll != 0f && kotlin.math.abs(scroll) >= kotlin.math.abs(hScroll)) {
                            val next = if (scroll > 0f) {
                                (zoomFactor * 1.12f).coerceIn(0.3f, 5.0f)
                            } else {
                                (zoomFactor / 1.12f).coerceIn(0.3f, 5.0f)
                            }
                            onZoomFactorChange(next)
                            true
                        } else {
                            false
                        }
                    }
                    MotionEvent.ACTION_DOWN -> {
                        tapDownPos[0] = event.x
                        tapDownPos[1] = event.y
                        tapDownTime[0] = event.eventTime
                        false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val sv = sceneViewRef[0]
                        if (twoFinger[0] == 1f && event.pointerCount == 2 && sv != null && sv.width > 0 && sv.height > 0) {
                            val x0 = event.getX(0)
                            val y0 = event.getY(0)
                            val x1 = event.getX(1)
                            val y1 = event.getY(1)

                            // Pinch zoom (explicit)
                            val span = hypot(x1 - x0, y1 - y0).coerceAtLeast(1f)
                            val ratio = (span / twoFingerSpan[0]).coerceIn(0.85f, 1.15f)
                            if (ratio != 1f) {
                                onZoomFactorChange((zoomFactor * ratio).coerceIn(0.3f, 5.0f))
                            }
                            twoFingerSpan[0] = span

                            // Two-finger pan (explicit): move camera target in the XY plane.
                            val midX = (x0 + x1) / 2f
                            val midY = (y0 + y1) / 2f
                            val dx = midX - twoFinger[1]
                            val dy = midY - twoFinger[2]
                            twoFinger[1] = midX
                            twoFinger[2] = midY

                            val worldPerScreenX = distance / sv.width.toFloat()
                            val worldPerScreenY = distance / sv.height.toFloat()
                            panTarget[0] += -dx * worldPerScreenX
                            panTarget[1] += dy * worldPerScreenY
                            true
                        } else {
                            false
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        val dx = event.x - tapDownPos[0]
                        val dy = event.y - tapDownPos[1]
                        val moveDist = kotlin.math.sqrt(dx * dx + dy * dy)
                        val elapsed = event.eventTime - tapDownTime[0]
                        val sv = sceneViewRef[0]
                        if (moveDist < 40f && elapsed < 500L && sv != null && sv.width > 0 && sv.height > 0) {
                            val tapNormX = event.x / sv.width.toFloat()
                            val tapNormY = 1f - event.y / sv.height.toFloat()
                            var closestAtom: Atom? = null
                            var closestDist = Float.MAX_VALUE
                            for ((meshNode, atom) in atomNodeMap) {
                                val viewPos = cameraNode.worldToView(meshNode.worldPosition)
                                val ddx = tapNormX - viewPos.x
                                val ddy = tapNormY - viewPos.y
                                val d = kotlin.math.sqrt(ddx * ddx + ddy * ddy)
                                if (d < closestDist) {
                                    closestDist = d
                                    closestAtom = atom
                                }
                            }
                            if (closestAtom != null && closestDist < 0.12f) {
                                if (measurementMode) {
                                    onMeasurementAtomTapped(closestAtom)
                                } else {
                                    onAtomSelected(closestAtom)
                                }

                                val now = event.eventTime
                                val prev = lastTap[0]
                                val prevId = lastTapAtomId[0]
                                if (prevId == closestAtom.id && now - prev < 320L) {
                                    // Double-tap: center on atom (smooth) by shifting the molecule origin.
                                    val atomsForCenter = ligand.atoms.filterNot {
                                        val e = it.element.uppercase().trim()
                                        e == "H" || e == "D"
                                    }.ifEmpty { ligand.atoms }
                                    val cx = atomsForCenter.map { it.x }.average().toFloat()
                                    val cy = atomsForCenter.map { it.y }.average().toFloat()
                                    val cz = atomsForCenter.map { it.z }.average().toFloat()
                                    focusTarget = Float3(
                                        closestAtom.x - cx,
                                        closestAtom.y - cy,
                                        closestAtom.z - cz
                                    )
                                }
                                lastTap[0] = now
                                lastTapAtomId[0] = closestAtom.id
                            } else {
                                // If no atom selected, try bonds (tap near the middle of bond segment).
                                if (!measurementMode) {
                                    val bondPick = pickBond(
                                        ligand = ligand,
                                        tapNormX = tapNormX,
                                        tapNormY = tapNormY,
                                        cameraNode = cameraNode,
                                        centerOffset = focusOffset
                                    )
                                    if (bondPick != null && bondPick.second < 0.10f) {
                                        onBondSelected(bondPick.first)
                                    } else {
                                        onDismissAtom()
                                        onDismissBond()
                                    }
                                }
                            }
                        }
                        false
                    }
                    MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                        twoFinger[0] = 0f
                        false
                    }
                    else -> false
                }
            },
            onViewCreated = {
                sceneViewRef[0] = this
                // SceneView is backed by a SurfaceView on many devices; without this, it can appear
                // above Compose overlays (making tooltips/measure UI look like it "disappears").
                (this as? android.view.SurfaceView)?.apply {
                    setZOrderOnTop(false)
                    setZOrderMediaOverlay(false)
                    // Avoid brief black flash before first rendered frame.
                    holder.setFormat(PixelFormat.TRANSLUCENT)
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                }
                // Set background immediately to match Compose.
                setBackgroundColor(
                    android.graphics.Color.argb(
                        255,
                        (sceneBackground.red * 255).toInt(),
                        (sceneBackground.green * 255).toInt(),
                        (sceneBackground.blue * 255).toInt()
                    )
                )
                renderer.clearOptions = renderer.clearOptions.apply {
                    clear = true
                    clearColor = floatArrayOf(
                        sceneBackground.red,
                        sceneBackground.green,
                        sceneBackground.blue,
                        1f
                    )
                }
            },
            onFrame = {
                if (!sceneReady) sceneReady = true
                val p = cameraNode.position
                lastCameraVector[0] = p.x
                lastCameraVector[1] = p.y
                lastCameraVector[2] = p.z

                if (autoRotate) {
                    autoRotateAngle += 0.02f
                    val r = distance
                    val x = kotlin.math.sin(autoRotateAngle) * r
                    val z = kotlin.math.cos(autoRotateAngle) * r
                    cameraNode.position = io.github.sceneview.math.Position(x, 0f, z)
                }

                // Smooth center-on-atom animation by shifting molecule origin.
                val target = focusTarget
                if (target != null) {
                    val k = 0.12f
                    val next = Float3(
                        focusOffset.x + (target.x - focusOffset.x) * k,
                        focusOffset.y + (target.y - focusOffset.y) * k,
                        focusOffset.z + (target.z - focusOffset.z) * k
                    )
                    focusOffset = next
                    val done =
                        kotlin.math.abs(target.x - next.x) < 0.01f &&
                            kotlin.math.abs(target.y - next.y) < 0.01f &&
                            kotlin.math.abs(target.z - next.z) < 0.01f
                    if (done) {
                        focusOffset = target
                        focusTarget = null
                    }
                }

                // Atom labels overlay (project to screen every few frames).
                if (showAtomLabels) {
                    labelFrameCounter++
                    if (labelFrameCounter % 3 == 0) {
                        val sv = sceneViewRef[0]
                        if (sv != null && sv.width > 0 && sv.height > 0) {
                            val map = LinkedHashMap<String, Offset>(atomNodeMap.size)
                            for ((node, atom) in atomNodeMap) {
                                val v = cameraNode.worldToView(node.worldPosition)
                                val px = (v.x * sv.width.toFloat()).coerceIn(0f, sv.width.toFloat())
                                val py = ((1f - v.y) * sv.height.toFloat()).coerceIn(0f, sv.height.toFloat())
                                map[atom.id] = Offset(px, py)
                            }
                            labelPositions = map
                        }
                    }
                } else if (labelPositions.isNotEmpty()) {
                    labelPositions = emptyMap()
                }
            }
        )

        // Cover the SceneView's initial black frame until first render.
        AnimatedVisibility(
            visible = !sceneReady,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(sceneBackground)
            )
        }

        if (showAtomLabels && labelPositions.isNotEmpty()) {
            for ((atomId, pos) in labelPositions) {
                val atom = ligand.atoms.firstOrNull { it.id == atomId } ?: continue
                Text(
                    text = atom.element,
                    modifier = Modifier.offset {
                        IntOffset(
                            (pos.x - 8).toInt(),
                            (pos.y - 10).toInt()
                        )
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        if (measurementMode) {
            MeasurementOverlay(
                ligand = ligand,
                selectedAtomIds = measurementAtomIds,
                onClear = onClearMeasurement,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 10.dp, bottom = 10.dp)
            )
        }

    }
}

@Composable
private fun MeasurementOverlay(
    ligand: Ligand,
    selectedAtomIds: List<String>,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val atoms = selectedAtomIds.mapNotNull { id -> ligand.atoms.firstOrNull { it.id == id } }
    val text = when (atoms.size) {
        0 -> "Measure mode: tap atoms (2 = distance, 3 = angle)"
        1 -> "Measure: selected ${atoms[0].id}"
        2 -> {
            val a = atoms[0]
            val b = atoms[1]
            val dx = a.x - b.x
            val dy = a.y - b.y
            val dz = a.z - b.z
            val d = kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
            "Distance (${a.id}–${b.id}): ${String.format("%.2f Å", d)}"
        }
        else -> {
            val a = atoms[0]
            val b = atoms[1]
            val c = atoms[2]
            val ba = Float3(a.x - b.x, a.y - b.y, a.z - b.z)
            val bc = Float3(c.x - b.x, c.y - b.y, c.z - b.z)
            val dot = (ba.x * bc.x + ba.y * bc.y + ba.z * bc.z)
            val len1 = kotlin.math.sqrt(ba.x * ba.x + ba.y * ba.y + ba.z * ba.z)
            val len2 = kotlin.math.sqrt(bc.x * bc.x + bc.y * bc.y + bc.z * bc.z)
            val angle = if (len1 > 1e-6f && len2 > 1e-6f) {
                val cos = (dot / (len1 * len2)).coerceIn(-1f, 1f)
                kotlin.math.acos(cos) * 180f / kotlin.math.PI.toFloat()
            } else 0f
            "Angle (${a.id}–${b.id}–${c.id}): ${String.format("%.1f°", angle)}"
        }
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f)
            )
            androidx.compose.material3.TextButton(onClick = onClear) {
                Text("Reset")
            }
        }
    }
}

@Composable
private fun AtomTooltip(
    atom: Atom,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable { onDismiss() }
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = atom.element,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = atom.elementName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
            Text(
                text = "Atom: ${atom.id}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
            )
        }
    }
}

private enum class ShareFormat(val extension: String, val mimeType: String) {
    PNG("png", "image/png"),
    JPEG("jpg", "image/jpeg")
}

private fun shareModelScreenshot(
    context: Context,
    ligandId: String,
    ligandName: String,
    atomCount: Int?,
    formula: String,
    format: ShareFormat
) {
    val activity = context as? Activity ?: return
    val window = activity.window
    val view = window.decorView
    val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)

    val namePart = ligandName.takeIf { it.isNotBlank() } ?: "Unknown ligand"
    val atomsPart = atomCount?.let { "Atoms: $it" } ?: "Atoms: ?"
    val formulaPart = formula.takeIf { it.isNotBlank() }?.let { "Formula: $it" } ?: "Formula: ?"
    val shareText = buildString {
        append("Ligand $ligandId — $namePart\n")
        append("$atomsPart · $formulaPart\n")
        append("https://www.rcsb.org/ligand/$ligandId")
    }

    PixelCopy.request(window, bitmap, { result ->
        if (result == PixelCopy.SUCCESS) {
            val dir = File(context.cacheDir, "shared_images")
            dir.mkdirs()
            val file = File(dir, "ligand_${ligandId}.${format.extension}")
            file.outputStream().use { out ->
                val compressFormat = when (format) {
                    ShareFormat.PNG -> Bitmap.CompressFormat.PNG
                    ShareFormat.JPEG -> Bitmap.CompressFormat.JPEG
                }
                bitmap.compress(compressFormat, 92, out)
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = format.mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, shareText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Ligand"))
        } else {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            context.startActivity(Intent.createChooser(intent, "Share Ligand"))
        }
    }, Handler(Looper.getMainLooper()))
}

private fun shareVideo(context: Context, file: File, ligandId: String) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "video/mp4"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, "Ligand $ligandId — rotation video\nhttps://www.rcsb.org/ligand/$ligandId")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share Video"))
}

private class ScreenRecorder(
    private val activity: Activity,
    private val projection: MediaProjection,
    private val outputFile: File
) {
    private var recorder: MediaRecorder? = null
    private var virtualDisplay: VirtualDisplay? = null

    fun start() {
        val metrics = activity.resources.displayMetrics
        val width = (activity.window.decorView.width.takeIf { it > 0 } ?: metrics.widthPixels)
        val height = (activity.window.decorView.height.takeIf { it > 0 } ?: metrics.heightPixels)
        val densityDpi = metrics.densityDpi

        val mr = MediaRecorder().apply {
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setVideoEncodingBitRate(6_000_000)
            setVideoFrameRate(30)
            setVideoSize(width, height)
            setOutputFile(outputFile.absolutePath)
            prepare()
        }

        recorder = mr
        virtualDisplay = projection.createVirtualDisplay(
            "SwiftyProteinRecord",
            width,
            height,
            densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mr.surface,
            null,
            null
        )
        mr.start()
    }

    fun stop(): File {
        runCatching { recorder?.stop() }
        runCatching { recorder?.release() }
        recorder = null
        runCatching { virtualDisplay?.release() }
        virtualDisplay = null
        runCatching { projection.stop() }
        return outputFile
    }
}

private fun pickBond(
    ligand: Ligand,
    tapNormX: Float,
    tapNormY: Float,
    cameraNode: io.github.sceneview.node.CameraNode,
    centerOffset: Float3
): Pair<Bond, Float>? {
    val atomsForCenter = ligand.atoms.filterNot {
        val e = it.element.uppercase().trim()
        e == "H" || e == "D"
    }.ifEmpty { ligand.atoms }
    val cx = atomsForCenter.map { it.x }.average().toFloat()
    val cy = atomsForCenter.map { it.y }.average().toFloat()
    val cz = atomsForCenter.map { it.z }.average().toFloat()

    val atomById = atomsForCenter.associateBy { it.id }
    var best: Bond? = null
    var bestDist = Float.MAX_VALUE

    for (bond in ligand.bonds) {
        val a1 = atomById[bond.atomId1] ?: continue
        val a2 = atomById[bond.atomId2] ?: continue
        val p1 = io.github.sceneview.math.Position(
            (a1.x - cx) - centerOffset.x,
            (a1.y - cy) - centerOffset.y,
            (a1.z - cz) - centerOffset.z
        )
        val p2 = io.github.sceneview.math.Position(
            (a2.x - cx) - centerOffset.x,
            (a2.y - cy) - centerOffset.y,
            (a2.z - cz) - centerOffset.z
        )
        val mid = io.github.sceneview.math.Position(
            (p1.x + p2.x) / 2f,
            (p1.y + p2.y) / 2f,
            (p1.z + p2.z) / 2f
        )
        val v = cameraNode.worldToView(mid)
        val dx = tapNormX - v.x
        val dy = tapNormY - v.y
        val d = kotlin.math.sqrt(dx * dx + dy * dy)
        if (d < bestDist) {
            bestDist = d
            best = bond
        }
    }
    return best?.let { it to bestDist }
}

@Composable
private fun BondTooltip(
    bond: Bond,
    ligand: Ligand,
    onDismiss: () -> Unit
) {
    val a1 = ligand.atoms.firstOrNull { it.id == bond.atomId1 }
    val a2 = ligand.atoms.firstOrNull { it.id == bond.atomId2 }
    val length = if (a1 != null && a2 != null) {
        val dx = a1.x - a2.x
        val dy = a1.y - a2.y
        val dz = a1.z - a2.z
        kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
    } else null

    val order = when (bond.order) {
        BondOrder.SINGLE -> "Single"
        BondOrder.DOUBLE -> "Double"
        BondOrder.TRIPLE -> "Triple"
        BondOrder.AROMATIC -> "Aromatic"
    }
    val lengthText = length?.let { String.format("%.2f Å", it) } ?: "?"

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable { onDismiss() }
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Bond",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = "${bond.atomId1} – ${bond.atomId2}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = "Type: $order",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
            )
            Text(
                text = "Length: $lengthText",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
            )
        }
    }
}
