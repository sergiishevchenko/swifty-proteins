package com.music42.swiftyprotein.ui.proteinview

import android.media.MediaRecorder
import androidx.compose.ui.viewinterop.AndroidView
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.ClipData
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.PixelCopy
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import androidx.core.content.ContextCompat
import com.music42.swiftyprotein.MainActivity
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Logout
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
    currentUsername: String?,
    onLogout: () -> Unit,
    viewModel: ProteinViewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val accentGreen = Color(0xFF4CAF50)
    val sceneTint = if (MaterialTheme.colorScheme.background.red < 0.4f) {
        Color(0xFF151A20)
    } else {
        Color(0xFFF3F5F7)
    }
    val safeLigandId = ligandId.substringBefore(" -").trim().ifEmpty { ligandId.trim() }
    var zoomFactor by remember(uiState.ligand?.id) { mutableFloatStateOf(1f) }
    var resetTick by remember { mutableIntStateOf(0) }
    var showShareFormatDialog by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var recordErrorMessage by remember { mutableStateOf<String?>(null) }
    var showBallsModeHint by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val recorderHolder = remember { arrayOf<ScreenRecorder?>(null) }
    val sceneViewForScreenshot = remember { arrayOfNulls<android.view.View>(1) }

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
            runCatching { activity.stopService(Intent(activity, MediaProjectionForegroundService::class.java)) }
            return@rememberLauncherForActivityResult
        }
        val mgr = activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val projection = runCatching { mgr.getMediaProjection(result.resultCode, result.data!!) }
            .getOrElse {
                isRecording = false
                recordErrorMessage = it.localizedMessage ?: "MediaProjection failed."
                runCatching { activity.stopService(Intent(activity, MediaProjectionForegroundService::class.java)) }
                return@rememberLauncherForActivityResult
            }
        val file = File(context.cacheDir, "shared_videos/ligand_${safeLigandId}.mp4").apply {
            parentFile?.mkdirs()
            if (exists()) delete()
        }

        val rec = ScreenRecorder(activity, projection, file)
        recorderHolder[0] = rec
        isRecording = true
        val startedOk = runCatching { rec.start() }.isSuccess
        if (!startedOk) {
            runCatching { rec.stop() }
            isRecording = false
            recorderHolder[0] = null
            recordErrorMessage = "Video recording failed to start on this device/emulator."
            runCatching { activity.stopService(Intent(activity, MediaProjectionForegroundService::class.java)) }
            return@rememberLauncherForActivityResult
        }

        coroutineScope.launch {
            delay(5_000)
            val stoppedFile = runCatching { rec.stop() }.getOrNull()
            isRecording = false
            recorderHolder[0] = null
            runCatching { activity.stopService(Intent(activity, MediaProjectionForegroundService::class.java)) }
            if (stoppedFile != null && stoppedFile.exists() && stoppedFile.length() > 0L) {
                shareVideo(context, stoppedFile, safeLigandId, uiState.ligand)
            } else {
                recordErrorMessage = "Video recording failed (empty output). Try a real device."
            }
        }
    }

    val postNotificationsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            recordErrorMessage = "Enable notifications to start screen recording on Android 13+."
            return@rememberLauncherForActivityResult
        }
        val activity = context as? Activity ?: return@rememberLauncherForActivityResult
        val mgr = activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projectionLauncher.launch(mgr.createScreenCaptureIntent())
    }

    fun requestScreenshot(format: ShareFormat) {
        val namePart = uiState.ligand?.name?.takeIf { it.isNotBlank() } ?: "Unknown ligand"
        val atomsPart = uiState.ligand?.atoms?.size?.let { "Atoms: $it" } ?: "Atoms: ?"
        val formulaPart = uiState.ligand?.formula?.takeIf { it.isNotBlank() }?.let { "Formula: $it" } ?: "Formula: ?"
        val shareText = buildString {
            append("Ligand $safeLigandId — $namePart\n")
            append("$atomsPart · $formulaPart\n")
            append("https://www.rcsb.org/ligand/$safeLigandId")
        }
        shareModelScreenshotPixelCopyFallback(
            context = context,
            ligandId = safeLigandId,
            format = format,
            shareText = shareText,
            sceneViewFor3d = sceneViewForScreenshot[0]
        )
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
                    if (!currentUsername.isNullOrBlank()) {
                        Text(
                            text = currentUsername,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = accentGreen,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    navigationIconContentColor = accentGreen,
                    actionIconContentColor = accentGreen,
                    titleContentColor = accentGreen
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
                            resetTick = resetTick,
                            selectedAtom = uiState.selectedAtom,
                            onAtomSelected = viewModel::onAtomSelected,
                            onDismissAtom = viewModel::dismissAtomInfo,
                            selectedBond = uiState.selectedBond,
                            onBondSelected = viewModel::onBondSelected,
                            onDismissBond = viewModel::dismissBondInfo,
                            showAtomLabels = uiState.showAtomLabels,
                            showHydrogens = uiState.showHydrogens,
                            measurementMode = uiState.measurementMode,
                            measurementAtomIds = uiState.measurementAtomIds,
                            onMeasurementAtomTapped = viewModel::onAtomTappedForMeasurement,
                            onClearMeasurement = viewModel::clearMeasurement,
                            autoRotate = isRecording,
                            sceneBackground = sceneTint,
                            onSceneViewForScreenshot = { v -> sceneViewForScreenshot[0] = v },
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
                                            
                                            if (android.os.Build.VERSION.SDK_INT >= 33) {
                                                val ok = ContextCompat.checkSelfPermission(
                                                    activity,
                                                    android.Manifest.permission.POST_NOTIFICATIONS
                                                ) == PackageManager.PERMISSION_GRANTED
                                                if (!ok) {
                                                    postNotificationsLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                                    return@IconButton
                                                }
                                            }
                                            runCatching {
                                                ContextCompat.startForegroundService(
                                                    activity,
                                                    Intent(activity, MediaProjectionForegroundService::class.java)
                                                )
                                            }.onFailure {
                                                recordErrorMessage = it.localizedMessage ?: "Failed to start recording service."
                                                return@IconButton
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
                                        containerColor = if (uiState.showHydrogens)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else
                                            MaterialTheme.colorScheme.surface
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                ) {
                                    IconButton(
                                        onClick = { viewModel.setShowHydrogens(!uiState.showHydrogens) },
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Text(
                                            text = "H",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (uiState.showHydrogens)
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
                                Spacer(modifier = Modifier.height(8.dp))
                                Card(
                                    modifier = Modifier.size(42.dp),
                                    shape = CircleShape,
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    IconButton(
                                        onClick = {
                                            zoomFactor = 1f
                                            resetTick++
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Icon(
                                            Icons.Filled.Refresh,
                                            contentDescription = "Reset view",
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
                                    visible = uiState.showHydrogens,
                                    enter = fadeIn(),
                                    exit = fadeOut()
                                ) {
                                    ModeBanner(
                                        text = "HYDROGENS VISIBLE",
                                        onClick = { viewModel.setShowHydrogens(false) },
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
                    requestScreenshot(ShareFormat.PNG)
                }) { Text("PNG") }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.TextButton(onClick = {
                        showShareFormatDialog = false
                        requestScreenshot(ShareFormat.JPEG)
                    }) { Text("JPEG") }
                    androidx.compose.material3.TextButton(onClick = { showShareFormatDialog = false }) { Text("Cancel") }
                }
            }
        )
    }

    if (recordErrorMessage != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { recordErrorMessage = null },
            title = { Text("Video recording") },
            text = { Text(recordErrorMessage!!) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { recordErrorMessage = null }) {
                    Text("OK")
                }
            }
        )
    }

    if (uiState.largeMoleculeWarning) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { viewModel.dismissLargeMoleculeWarning() },
            title = { Text("Large molecule") },
            text = {
                Text(
                    "This ligand has ${uiState.ligand?.atoms?.size ?: "many"} atoms. " +
                        "Rendering quality has been reduced for better performance."
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { viewModel.dismissLargeMoleculeWarning() }) {
                    Text("OK")
                }
            }
        )
    }

}

private fun shareImageFile(
    context: Context,
    file: File,
    format: ShareFormat,
    chooserTitle: String,
    shareText: String,
    ligandId: String
) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = format.mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, shareText)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        clipData = ClipData.newUri(context.contentResolver, "shared_${ligandId}", uri)
    }
    val resInfoList = context.packageManager.queryIntentActivities(intent, 0)
    for (resolveInfo in resInfoList) {
        runCatching {
            context.grantUriPermission(
                resolveInfo.activityInfo.packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
    }
    context.startActivity(Intent.createChooser(intent, chooserTitle))
}

private fun shareModelScreenshotPixelCopyFallback(
    context: Context,
    ligandId: String,
    format: ShareFormat,
    shareText: String,
    sceneViewFor3d: android.view.View?
) {
    val activity = context as? Activity ?: return
    val window = activity.window
    val decor = window.decorView
    val bitmap = Bitmap.createBitmap(decor.width, decor.height, Bitmap.Config.ARGB_8888)

    PixelCopy.request(window, bitmap, { windowResult ->
        if (windowResult != PixelCopy.SUCCESS) return@request

        val svRoot = sceneViewFor3d
        val surfaceView = svRoot?.let { findSurfaceView(it) }
        if (surfaceView == null || surfaceView.width <= 0 || surfaceView.height <= 0) {
            val file = saveBitmapToCache(context, bitmap, ligandId, format)
            shareImageFile(context, file, format, "Share Ligand", shareText, ligandId)
            return@request
        }

        val modelBitmap = Bitmap.createBitmap(surfaceView.width, surfaceView.height, Bitmap.Config.ARGB_8888)
        PixelCopy.request(
            surfaceView,
            modelBitmap,
            PixelCopy.OnPixelCopyFinishedListener { modelResult ->
                if (modelResult == PixelCopy.SUCCESS) {
                    val loc = IntArray(2)
                    surfaceView.getLocationInWindow(loc)
                    val canvas = android.graphics.Canvas(bitmap)
                    canvas.drawBitmap(modelBitmap, loc[0].toFloat(), loc[1].toFloat(), null)
                }
                val file = saveBitmapToCache(context, bitmap, ligandId, format)
                shareImageFile(context, file, format, "Share Ligand", shareText, ligandId)
            },
            Handler(Looper.getMainLooper())
        )
    }, Handler(Looper.getMainLooper()))
}

private fun findSurfaceView(root: android.view.View): android.view.SurfaceView? {
    if (root is android.view.SurfaceView) return root
    if (root is android.view.ViewGroup) {
        for (i in 0 until root.childCount) {
            val child = root.getChildAt(i) ?: continue
            val found = findSurfaceView(child)
            if (found != null) return found
        }
    }
    return null
}

private fun saveBitmapToCache(
    context: Context,
    bitmap: Bitmap,
    ligandId: String,
    format: ShareFormat
): File {
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
    return file
}

@Composable
private fun MoleculeViewer(
    ligand: Ligand,
    mode: VisualizationMode,
    zoomFactor: Float,
    onZoomFactorChange: (Float) -> Unit,
    resetTick: Int,
    selectedAtom: Atom?,
    onAtomSelected: (Atom?) -> Unit,
    onDismissAtom: () -> Unit,
    selectedBond: Bond?,
    onBondSelected: (Bond?) -> Unit,
    onDismissBond: () -> Unit,
    showAtomLabels: Boolean,
    showHydrogens: Boolean,
    measurementMode: Boolean,
    measurementAtomIds: List<String>,
    onMeasurementAtomTapped: (Atom) -> Unit,
    onClearMeasurement: () -> Unit,
    autoRotate: Boolean,
    sceneBackground: Color,
    onSceneViewForScreenshot: (android.view.View?) -> Unit,
    modifier: Modifier = Modifier
) {
    val engine = rememberEngine()
    val materialLoader = rememberMaterialLoader(engine)

    val lastTap = remember { longArrayOf(0L) }
    val lastTapAtomId = remember { arrayOfNulls<String>(1) }
    var focusTarget by remember(ligand.id) { mutableStateOf<Float3?>(null) }
    var focusOffset by remember(ligand.id) { mutableStateOf(Float3(0f, 0f, 0f)) }
    var panOffset by remember(ligand.id) { mutableStateOf(Float3(0f, 0f, 0f)) }
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

    val (parentNode, atomNodeMap) = remember(ligand, mode, showHydrogens) {
        MoleculeSceneBuilder.build(
            engine = engine,
            materialLoader = materialLoader,
            ligand = ligand,
            mode = mode,
            highlightElement = null,
            centerOffset = Float3(0f, 0f, 0f),
            showHydrogens = showHydrogens
        )
    }
    LaunchedEffect(ligand.id, mode, atomNodeMap.size) {}

    LaunchedEffect(selectedAtom?.id, measurementMode, measurementAtomIds) {
        val measureAccent = Color(0xFFFF6A00) // vivid orange
        val measurementAtomIdSet: Set<String> = if (measurementMode) {
            measurementAtomIds.takeLast(3).toSet()
        } else {
            emptySet()
        }

        val selected = selectedAtom
        val selectedElement = selected?.element?.uppercase()?.trim()

        for (entry in atomNodeMap) {
            val node = entry.key
            val atom = entry.value
            val base = com.music42.swiftyprotein.util.CpkColors.getColor(atom.element)

            val color = if (measurementMode) {
                if (measurementAtomIdSet.contains(atom.id)) {
                    val t = 0.65f
                    Color(
                        base.red + (measureAccent.red - base.red) * t,
                        base.green + (measureAccent.green - base.green) * t,
                        base.blue + (measureAccent.blue - base.blue) * t,
                        1f
                    )
                } else {
                    base
                }
            } else {
                val isSelected = selected != null && atom.id == selected.id
                val isSameElement = selectedElement != null &&
                    atom.element.uppercase().trim() == selectedElement
                when {
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

    val boundingRadius = (atomsForCenter.maxOfOrNull { a ->
        val dx = a.x - cx; val dy = a.y - cy; val dz = a.z - cz
        val dist = kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
        val visualR = if (mode == VisualizationMode.SPACE_FILL) {
            MoleculeSceneBuilder.BALL_RADIUS * MoleculeSceneBuilder.SPACE_FILL_BASE_SCALE *
                (com.music42.swiftyprotein.util.VdwRadii.radiusAngstrom(a.element) / MoleculeSceneBuilder.SPACE_FILL_REF_VDW)
        } else {
            MoleculeSceneBuilder.BALL_RADIUS
        }
        dist + visualR
    } ?: 5f).coerceAtLeast(1f)

    val baseCameraDistance = boundingRadius * 4.5f
    val distance = (baseCameraDistance / zoomFactor)
        .coerceIn(baseCameraDistance * 0.2f, baseCameraDistance * 6f)

    val defaultCamX = baseCameraDistance * 0.43f
    val defaultCamY = baseCameraDistance * 0.32f
    val defaultCamZ = baseCameraDistance * 0.75f
    val defaultCamLen = kotlin.math.sqrt(defaultCamX * defaultCamX + defaultCamY * defaultCamY + defaultCamZ * defaultCamZ)

    val lastCameraVector = remember(ligand.id) {
        floatArrayOf(
            defaultCamX / defaultCamLen * baseCameraDistance,
            defaultCamY / defaultCamLen * baseCameraDistance,
            defaultCamZ / defaultCamLen * baseCameraDistance
        )
    }
    if (resetTick > 0) {
        lastCameraVector[0] = defaultCamX / defaultCamLen * baseCameraDistance
        lastCameraVector[1] = defaultCamY / defaultCamLen * baseCameraDistance
        lastCameraVector[2] = defaultCamZ / defaultCamLen * baseCameraDistance
        panTarget[0] = 0f
        panTarget[1] = 0f
        focusOffset = Float3(0f, 0f, 0f)
        panOffset = Float3(0f, 0f, 0f)
        focusTarget = null
        parentNode.position = io.github.sceneview.math.Position(0f, 0f, 0f)
    }

    val cameraPosition = remember(ligand.id, zoomFactor, resetTick) {
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
            io.github.sceneview.math.Position(
                defaultCamX / defaultCamLen * distance,
                defaultCamY / defaultCamLen * distance,
                defaultCamZ / defaultCamLen * distance
            )
        }
    }
    cameraNode.position = cameraPosition

    val cameraManipulator = remember(ligand.id, resetTick) {
        SceneView.createDefaultCameraManipulator(
            orbitHomePosition = cameraPosition,
            targetPosition = io.github.sceneview.math.Position(0f, 0f, 0f)
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
                            true
                        } else {
                            false
                        }
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

                            val camPos = cameraNode.position
                            val forward = dev.romainguy.kotlin.math.normalize(
                                Float3(-camPos.x, -camPos.y, -camPos.z)
                            )
                            val worldUp = Float3(0f, 1f, 0f)
                            val right = dev.romainguy.kotlin.math.normalize(
                                dev.romainguy.kotlin.math.cross(forward, worldUp)
                            )
                            val up = dev.romainguy.kotlin.math.cross(right, forward)

                            val sensitivity = distance * 0.0006f
                            val panDx = dx * sensitivity
                            val panDy = dy * sensitivity
                            panOffset = Float3(
                                panOffset.x + right.x * panDx + up.x * (-panDy),
                                panOffset.y + right.y * panDx + up.y * (-panDy),
                                panOffset.z + right.z * panDx + up.z * (-panDy)
                            )
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
                                        centerOffset = focusOffset,
                                        showHydrogens = showHydrogens
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
                onSceneViewForScreenshot(this)
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
            },
            onViewUpdated = {
                onSceneViewForScreenshot(this)
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
                } else {
                    val len = kotlin.math.sqrt(p.x * p.x + p.y * p.y + p.z * p.z)
                        .coerceAtLeast(0.0001f)
                    if (kotlin.math.abs(len - distance) > 0.01f) {
                        val k = distance / len
                        cameraNode.position = io.github.sceneview.math.Position(
                            p.x * k, p.y * k, p.z * k
                        )
                    }
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
                    val done =
                        kotlin.math.abs(target.x - next.x) < 0.01f &&
                            kotlin.math.abs(target.y - next.y) < 0.01f &&
                            kotlin.math.abs(target.z - next.z) < 0.01f
                    if (done) {
                        focusOffset = target
                        focusTarget = null
                    }
                }

                parentNode.position = io.github.sceneview.math.Position(
                    -focusOffset.x + panOffset.x,
                    -focusOffset.y + panOffset.y,
                    -focusOffset.z + panOffset.z
                )

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
    val details = when (atoms.size) {
        0 -> "Tap atoms (2 = distance, 3 = angle)"
        1 -> "Selected ${atoms[0].id}"
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
    val headerText = "Measure mode:"
    val detailText = details

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
                contentDescription = "Molecule icon",
                modifier = Modifier.size(28.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = headerText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFFF9800)
                )
                Text(
                    text = detailText,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.inverseOnSurface
                )
            }
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
                contentDescription = "Atom info",
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


private fun shareVideo(
    context: Context,
    file: File,
    ligandId: String,
    ligand: com.music42.swiftyprotein.data.model.Ligand?
) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    val namePart = ligand?.name?.takeIf { it.isNotBlank() } ?: "Unknown ligand"
    val atomsPart = ligand?.atoms?.size?.let { "Atoms: $it" } ?: "Atoms: ?"
    val formulaPart = ligand?.formula?.takeIf { it.isNotBlank() }?.let { "Formula: $it" } ?: "Formula: ?"
    val shareText = buildString {
        append("Ligand $ligandId — $namePart\n")
        append("$atomsPart · $formulaPart\n")
        append("https://www.rcsb.org/ligand/$ligandId")
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "video/mp4"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, shareText)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        clipData = ClipData.newUri(context.contentResolver, "video_${ligandId}", uri)
    }
    val resInfoList = context.packageManager.queryIntentActivities(intent, 0)
    for (resolveInfo in resInfoList) {
        runCatching {
            context.grantUriPermission(
                resolveInfo.activityInfo.packageName,
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
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
    private var projectionCallback: MediaProjection.Callback? = null
    private var started: Boolean = false

    fun start() {
        val metrics = activity.resources.displayMetrics
        val rawWidth = (activity.window.decorView.width.takeIf { it > 0 } ?: metrics.widthPixels)
        val rawHeight = (activity.window.decorView.height.takeIf { it > 0 } ?: metrics.heightPixels)
        val densityDpi = metrics.densityDpi

        val maxWidth = 1280
        val scale = if (rawWidth > maxWidth) maxWidth.toFloat() / rawWidth.toFloat() else 1f
        val width = ((rawWidth * scale).toInt() / 2) * 2
        val height = ((rawHeight * scale).toInt() / 2) * 2

        val mr = MediaRecorder().apply {
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setVideoEncodingBitRate(4_000_000)
            setVideoFrameRate(30)
            setVideoSize(width, height)
            setOutputFile(outputFile.absolutePath)
            prepare()
        }

        recorder = mr
        started = false

        val cb = object : MediaProjection.Callback() {
            override fun onStop() {
            }
        }
        projectionCallback = cb
        projection.registerCallback(cb, Handler(Looper.getMainLooper()))

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
        started = true
    }

    fun stop(): File {
        if (started) {
            runCatching { recorder?.stop() }.onFailure {
            }
        }
        runCatching { recorder?.release() }
        recorder = null
        runCatching { virtualDisplay?.release() }
        virtualDisplay = null
        runCatching {
            projectionCallback?.let { projection.unregisterCallback(it) }
        }
        projectionCallback = null
        runCatching { projection.stop() }
        return outputFile
    }
}

private fun pickBond(
    ligand: Ligand,
    tapNormX: Float,
    tapNormY: Float,
    cameraNode: io.github.sceneview.node.CameraNode,
    centerOffset: Float3,
    showHydrogens: Boolean = false
): Pair<Bond, Float>? {
    val atomsForCenter = ligand.atoms.filterNot {
        val e = it.element.uppercase().trim()
        e == "H" || e == "D"
    }.ifEmpty { ligand.atoms }
    val cx = atomsForCenter.map { it.x }.average().toFloat()
    val cy = atomsForCenter.map { it.y }.average().toFloat()
    val cz = atomsForCenter.map { it.z }.average().toFloat()

    val pickableAtoms = if (showHydrogens) ligand.atoms else atomsForCenter
    val atomById = pickableAtoms.associateBy { it.id }
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
                contentDescription = "Bond info",
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
                contentDescription = "Measurement info",
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
