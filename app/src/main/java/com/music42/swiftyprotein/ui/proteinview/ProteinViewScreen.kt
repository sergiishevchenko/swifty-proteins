package com.music42.swiftyprotein.ui.proteinview

import android.media.MediaRecorder
import androidx.compose.ui.viewinterop.AndroidView
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
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
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
    var showBallsModeHint by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val recorderHolder = remember { arrayOf<ScreenRecorder?>(null) }

    LaunchedEffect(showBallsModeHint) {
        if (showBallsModeHint) {
            kotlinx.coroutines.delay(2000L)
            showBallsModeHint = false
        }
    }

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
        containerColor = Color.Transparent,
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
                    LaunchedEffect(uiState.ligandId, uiState.ligand?.atoms?.size, uiState.ligand?.bonds?.size) {
                        val a = uiState.ligand?.atoms?.size ?: -1
                        val b = uiState.ligand?.bonds?.size ?: -1
                        Log.i("SwiftyProtein", "ProteinView ready id=${uiState.ligandId} atoms=$a bonds=$b")
                    }
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

                        androidx.compose.ui.window.Popup(alignment = Alignment.TopEnd) {
                            Column(
                                modifier = Modifier.padding(end = 8.dp, top = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Card(
                                    modifier = Modifier.size(42.dp),
                                    shape = CircleShape,
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isRecording)
                                            MaterialTheme.colorScheme.errorContainer
                                        else
                                            MaterialTheme.colorScheme.surface
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (isRecording) return@IconButton
                                            val activity = context as? Activity ?: return@IconButton
                                            (activity as? com.music42.swiftyprotein.MainActivity)?.let {
                                                it.suppressLoginOnResume = true
                                            }
                                            val mgr = activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                                            projectionLauncher.launch(mgr.createScreenCaptureIntent())
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Icon(
                                            Icons.Default.Videocam,
                                            contentDescription = "Record video",
                                            tint = if (isRecording)
                                                MaterialTheme.colorScheme.onErrorContainer
                                            else
                                                MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Card(
                                    modifier = Modifier.size(42.dp),
                                    shape = CircleShape,
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (uiState.measurementMode)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else
                                            MaterialTheme.colorScheme.surface
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (uiState.visualizationMode != VisualizationMode.BALL_AND_STICK) {
                                                showBallsModeHint = true
                                                return@IconButton
                                            }
                                            viewModel.setMeasurementMode(!uiState.measurementMode)
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Icon(
                                            Icons.Default.Straighten,
                                            contentDescription = "Toggle measurement mode",
                                            tint = if (uiState.measurementMode)
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            else
                                                MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Card(
                                    modifier = Modifier.size(42.dp),
                                    shape = CircleShape,
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (uiState.showAtomLabels)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else
                                            MaterialTheme.colorScheme.surface
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (uiState.visualizationMode != VisualizationMode.BALL_AND_STICK) {
                                                showBallsModeHint = true
                                                return@IconButton
                                            }
                                            viewModel.setShowAtomLabels(!uiState.showAtomLabels)
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Icon(
                                            Icons.Default.Label,
                                            contentDescription = "Toggle atom labels",
                                            tint = if (uiState.showAtomLabels)
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            else
                                                MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Card(
                                    modifier = Modifier.size(42.dp),
                                    shape = CircleShape,
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                ) {
                                    IconButton(
                                        onClick = { showShareFormatDialog = true },
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Icon(
                                            Icons.Default.Share,
                                            contentDescription = "Share",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        androidx.compose.ui.window.Popup(alignment = Alignment.TopStart) {
                            Column(
                                modifier = Modifier.padding(start = 8.dp, top = 8.dp),
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

                        androidx.compose.ui.window.Popup(alignment = Alignment.TopCenter) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(top = 12.dp)
                            ) {
                                AnimatedVisibility(
                                    visible = uiState.measurementMode,
                                    enter = fadeIn(),
                                    exit = fadeOut()
                                ) {
                                    ModeBanner(
                                        text = "MEASURE MODE",
                                        onClick = { viewModel.setMeasurementMode(false) }
                                    )
                                }
                                AnimatedVisibility(
                                    visible = uiState.showAtomLabels,
                                    enter = fadeIn(),
                                    exit = fadeOut()
                                ) {
                                    ModeBanner(
                                        text = "LABELS MODE",
                                        onClick = { viewModel.setShowAtomLabels(false) },
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                                AnimatedVisibility(
                                    visible = showBallsModeHint,
                                    enter = fadeIn(),
                                    exit = fadeOut()
                                ) {
                                    ModeBanner(
                                        text = "Switch to Balls mode",
                                        onClick = { showBallsModeHint = false },
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }
                        }

                        if (uiState.selectedAtom != null) {
                            androidx.compose.ui.window.Popup(
                                alignment = Alignment.BottomStart,
                                onDismissRequest = { viewModel.dismissAtomInfo() }
                            ) {
                                AtomTooltip(
                                    atom = uiState.selectedAtom!!,
                                    onDismiss = viewModel::dismissAtomInfo,
                                    modifier = Modifier.padding(start = 10.dp, bottom = 100.dp)
                                )
                            }
                        }

                        if (uiState.selectedBond != null) {
                            androidx.compose.ui.window.Popup(
                                alignment = Alignment.BottomStart,
                                onDismissRequest = { viewModel.dismissBondInfo() }
                            ) {
                                BondTooltip(
                                    bond = uiState.selectedBond!!,
                                    ligand = uiState.ligand!!,
                                    onDismiss = viewModel::dismissBondInfo,
                                    modifier = Modifier.padding(start = 10.dp, bottom = 100.dp)
                                )
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
                                                    VisualizationMode.BALL_AND_STICK -> "Balls"
                                                    VisualizationMode.SPACE_FILL -> "Fill"
                                                    VisualizationMode.STICKS_ONLY -> "Sticks"
                                                    VisualizationMode.WIREFRAME -> "Wire"
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
    val sceneViewWindowXY = remember { intArrayOf(0, 0) }
    var sceneViewSizePx by remember { mutableStateOf(IntSize.Zero) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    LaunchedEffect(showAtomLabels) {
        labelFrameCounter = 0
        if (!showAtomLabels && labelPositions.isNotEmpty()) {
            mainHandler.post { labelPositions = emptyMap() }
        }
    }

    val (parentNode, atomNodeMap) = remember(ligand, mode) {
        MoleculeSceneBuilder.build(
            engine = engine,
            materialLoader = materialLoader,
            ligand = ligand,
            mode = mode,
            highlightElement = null,
            centerOffset = Float3(0f, 0f, 0f)
        )
    }
    LaunchedEffect(ligand.id, mode, atomNodeMap.size) {
        Log.i("SwiftyProtein", "Scene built id=${ligand.id} mode=$mode nodes=${atomNodeMap.size}")
    }

    LaunchedEffect(selectedAtom?.id) {
        val selected = selectedAtom
        val selectedElement = selected?.element?.uppercase()?.trim()
        for (entry in atomNodeMap) {
            val node = entry.key
            val atom = entry.value
            val isSelected = selected != null && atom.id == selected.id
            val isSameElement = selectedElement != null &&
                atom.element.uppercase().trim() == selectedElement
            val base = com.music42.swiftyprotein.util.CpkColors.getColor(atom.element)
            val color = when {
                isSelected -> {
                    val t = 0.45f
                    Color(
                        base.red + (1f - base.red) * t,
                        base.green + (1f - base.green) * t,
                        base.blue + (1f - base.blue) * t,
                        1f
                    )
                }
                isSameElement -> {
                    val f = 0.55f
                    Color(base.red * f, base.green * f, base.blue * f, 1f)
                }
                else -> base
            }
            runCatching {
                node.materialInstance = materialLoader.createColorInstance(
                    color = color,
                    metallic = 0.0f,
                    roughness = 0.6f,
                    reflectance = 0.3f
                )
            }
        }
    }

    val tapDownPos = remember { floatArrayOf(0f, 0f) }
    val tapDownTime = remember { longArrayOf(0L) }
    val sceneViewRef = remember { arrayOfNulls<SceneView>(1) }
    val panTarget = remember(ligand.id) { floatArrayOf(0f, 0f) }
    val twoFinger = remember { floatArrayOf(0f, 0f, 0f) }
    val twoFingerSpan = remember { floatArrayOf(0f) }
    val firstFrameLogged = remember(ligand.id) { booleanArrayOf(false) }

    val cameraNode = rememberCameraNode(engine).apply {
        near = 0.1f
        far = 1000.0f
    }

    val atomsForCenter = ligand.atoms.filterNot {
        val e = it.element.uppercase().trim()
        e == "H" || e == "D"
    }.ifEmpty { ligand.atoms }
    val cx = atomsForCenter.map { it.x }.average().toFloat()
    val cy = atomsForCenter.map { it.y }.average().toFloat()
    val cz = atomsForCenter.map { it.z }.average().toFloat()
    val maxCoord = atomsForCenter.maxOfOrNull {
        maxOf(
            kotlin.math.abs(it.x - cx),
            kotlin.math.abs(it.y - cy),
            kotlin.math.abs(it.z - cz)
        )
    } ?: 5f
    val baseCameraDistance = (maxCoord.coerceAtLeast(2f)) * 2.8f
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

    Box(
        modifier = modifier
    ) {
        Scene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            isOpaque = false,
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

                            
                            val span = hypot(x1 - x0, y1 - y0).coerceAtLeast(1f)
                            val ratio = (span / twoFingerSpan[0]).coerceIn(0.85f, 1.15f)
                            if (ratio != 1f) {
                                onZoomFactorChange((zoomFactor * ratio).coerceIn(0.3f, 5.0f))
                            }
                            twoFingerSpan[0] = span

                            
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
                                if (prevId == closestAtom.id && now - prev < 550L) {
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
                runCatching {
                    val loc = IntArray(2)
                    getLocationInWindow(loc)
                    sceneViewWindowXY[0] = loc[0]
                    sceneViewWindowXY[1] = loc[1]
                }
                sceneViewSizePx = IntSize(width.coerceAtLeast(0), height.coerceAtLeast(0))
                renderer.clearOptions = renderer.clearOptions.apply {
                    clear = true
                    clearColor = floatArrayOf(
                        sceneBackground.red,
                        sceneBackground.green,
                        sceneBackground.blue,
                        1f
                    )
                }
                Log.i("SwiftyProtein", "SceneView created w=$width h=$height")
            },
            onViewUpdated = {
                runCatching {
                    val loc = IntArray(2)
                    getLocationInWindow(loc)
                    sceneViewWindowXY[0] = loc[0]
                    sceneViewWindowXY[1] = loc[1]
                }
                sceneViewSizePx = IntSize(width.coerceAtLeast(0), height.coerceAtLeast(0))
            },
            onFrame = {
                if (!firstFrameLogged[0]) {
                    firstFrameLogged[0] = true
                    Log.i("SwiftyProtein", "First frame")
                }
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

                runCatching {
                    cameraNode.lookAt(io.github.sceneview.math.Position(0f, 0f, 0f))
                }

                val target = focusTarget
                if (target != null) {
                    val k = 0.12f
                    val next = Float3(
                        focusOffset.x + (target.x - focusOffset.x) * k,
                        focusOffset.y + (target.y - focusOffset.y) * k,
                        focusOffset.z + (target.z - focusOffset.z) * k
                    )
                    focusOffset = next
                    parentNode.position = io.github.sceneview.math.Position(-next.x, -next.y, -next.z)
                    val done =
                        kotlin.math.abs(target.x - next.x) < 0.01f &&
                            kotlin.math.abs(target.y - next.y) < 0.01f &&
                            kotlin.math.abs(target.z - next.z) < 0.01f
                    if (done) {
                        focusOffset = target
                        parentNode.position = io.github.sceneview.math.Position(-target.x, -target.y, -target.z)
                        focusTarget = null
                    }
                }

                if (showAtomLabels) {
                    labelFrameCounter++
                    if (labelFrameCounter == 1 || labelFrameCounter % 2 == 0) {
                        val sv = sceneViewRef[0]
                        if (sv != null && sv.width > 0 && sv.height > 0) {
                            val entries = atomNodeMap.entries.toList()
                            val map = LinkedHashMap<String, Offset>(entries.size)
                            for ((node, atom) in entries) {
                                val v = cameraNode.worldToView(node.worldPosition)
                                val px = (v.x * sv.width.toFloat()).coerceIn(0f, sv.width.toFloat())
                                val py = ((1f - v.y) * sv.height.toFloat()).coerceIn(0f, sv.height.toFloat())
                                map[atom.id] = Offset(px, py)
                            }
                            mainHandler.post { labelPositions = map }
                        }
                    }
                } else if (labelPositions.isNotEmpty()) {
                    mainHandler.post { labelPositions = emptyMap() }
                }
            }
        )

        if (showAtomLabels && labelPositions.isNotEmpty()) {
            val atomById = remember(ligand.id) { ligand.atoms.associateBy { it.id } }
            val onSurface = MaterialTheme.colorScheme.onSurface
            val density = androidx.compose.ui.platform.LocalDensity.current
            val popupW = sceneViewSizePx.width
            val popupH = sceneViewSizePx.height
            val paint = remember(onSurface, density) {
                android.graphics.Paint().apply {
                    isAntiAlias = true
                    color = android.graphics.Color.argb(
                        230,
                        (onSurface.red * 255).toInt(),
                        (onSurface.green * 255).toInt(),
                        (onSurface.blue * 255).toInt()
                    )
                    textSize = with(density) { 11.dp.toPx() }
                    typeface = android.graphics.Typeface.create(
                        android.graphics.Typeface.DEFAULT,
                        android.graphics.Typeface.BOLD
                    )
                }
            }
            val fm = paint.fontMetrics
            val textHalfH = -(fm.ascent + fm.descent) / 2f
            if (popupW > 0 && popupH > 0) {
                LabelOverlayPopup(
                    widthPx = popupW,
                    heightPx = popupH,
                    density = density,
                    positions = labelPositions,
                    atomById = atomById,
                    paint = paint,
                    textHalfH = textHalfH
                )
            }
        }

        if (measurementMode) {
            androidx.compose.ui.window.Popup(alignment = Alignment.BottomStart) {
                MeasurementOverlay(
                    ligand = ligand,
                    selectedAtomIds = measurementAtomIds,
                    onClear = onClearMeasurement,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 10.dp, end = 10.dp, bottom = 10.dp)
                )
            }
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
        shape = RoundedCornerShape(999.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.88f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Image(
                painter = androidx.compose.ui.res.painterResource(com.music42.swiftyprotein.R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.inverseOnSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Reset",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.inverseOnSurface,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable(onClick = onClear)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun AtomTooltip(
    atom: Atom,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.88f)
    val fg = MaterialTheme.colorScheme.inverseOnSurface
    Card(
        shape = RoundedCornerShape(999.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = modifier.clickable(onClick = onDismiss)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
        ) {
            Image(
                painter = androidx.compose.ui.res.painterResource(com.music42.swiftyprotein.R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${atom.element}  ·  ${atom.elementName}  ·  ${atom.id}",
                style = MaterialTheme.typography.labelLarge,
                color = fg,
                fontWeight = FontWeight.SemiBold
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
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
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

    val bg = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.88f)
    val fg = MaterialTheme.colorScheme.inverseOnSurface
    Card(
        shape = RoundedCornerShape(999.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = modifier.clickable(onClick = onDismiss)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
        ) {
            Image(
                painter = androidx.compose.ui.res.painterResource(com.music42.swiftyprotein.R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$order  ·  ${bond.atomId1}–${bond.atomId2}  ·  $lengthText",
                style = MaterialTheme.typography.labelLarge,
                color = fg,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun LabelOverlayPopup(
    widthPx: Int,
    heightPx: Int,
    density: androidx.compose.ui.unit.Density,
    positions: Map<String, Offset>,
    atomById: Map<String, com.music42.swiftyprotein.data.model.Atom>,
    paint: android.graphics.Paint,
    textHalfH: Float
) {
    val overlayRef = remember { arrayOfNulls<android.view.View>(1) }
    val posRef = remember { arrayOfNulls<Map<String, Offset>>(1) }
    val atomRef = remember { arrayOfNulls<Map<String, com.music42.swiftyprotein.data.model.Atom>>(1) }
    posRef[0] = positions
    atomRef[0] = atomById

    LaunchedEffect(positions) {
        overlayRef[0]?.invalidate()
    }

    androidx.compose.ui.window.Popup(
        alignment = Alignment.TopStart,
        properties = androidx.compose.ui.window.PopupProperties(
            focusable = false,
            clippingEnabled = false
        )
    ) {
        val sizeModifier = with(density) {
            Modifier.size(widthPx.toDp(), heightPx.toDp())
        }
        AndroidView(
            factory = { ctx ->
                object : android.view.View(ctx) {
                    init {
                        setWillNotDraw(false)
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    }
                    override fun onTouchEvent(event: android.view.MotionEvent?) = false
                    override fun onDraw(c: android.graphics.Canvas) {
                        super.onDraw(c)
                        val curPositions = posRef[0] ?: return
                        val curAtoms = atomRef[0] ?: return
                        for ((atomId, pos) in curPositions) {
                            val atom = curAtoms[atomId] ?: continue
                            val text = atom.element
                            val w = paint.measureText(text)
                            c.drawText(text, pos.x - w / 2f, pos.y + textHalfH, paint)
                        }
                    }
                    override fun onAttachedToWindow() {
                        super.onAttachedToWindow()
                        var r: android.view.ViewParent? = parent
                        while (r != null) {
                            if (r is android.view.View) {
                                val root = r as android.view.View
                                val lp = root.layoutParams
                                if (lp is android.view.WindowManager.LayoutParams) {
                                    lp.flags = lp.flags or
                                        android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                                    runCatching {
                                        (root.context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager)
                                            .updateViewLayout(root, lp)
                                    }
                                    break
                                }
                            }
                            r = r.parent
                        }
                    }
                }.also { overlayRef[0] = it }
            },
            update = { it.invalidate() },
            modifier = sizeModifier
        )
    }
}

@Composable
private fun ModeBanner(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.88f)
    val fg = MaterialTheme.colorScheme.inverseOnSurface
    Card(
        shape = RoundedCornerShape(999.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
        ) {
            Image(
                painter = androidx.compose.ui.res.painterResource(com.music42.swiftyprotein.R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = fg,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
