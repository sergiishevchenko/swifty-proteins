package com.music42.swiftyprotein.ui.proteinview

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.music42.swiftyprotein.MainActivity
import com.music42.swiftyprotein.R
import com.music42.swiftyprotein.ui.scaffoldSymmetricContentPadding
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
    val layoutDirection = LocalLayoutDirection.current
    val accentColor = MaterialTheme.colorScheme.primary
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
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
    var overlayVisible by remember { mutableStateOf(true) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val leaveScreen: () -> Unit = {
        overlayVisible = false
        mainHandler.post { onBack() }
    }

    BackHandler(enabled = overlayVisible, onBack = leaveScreen)

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopAnimation()
        }
    }

    LaunchedEffect(showBallsModeHint) {
        if (showBallsModeHint) {
            delay(2000L)
            showBallsModeHint = false
        }
    }

    val shareLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        val activity = context as? MainActivity ?: return@rememberLauncherForActivityResult
        activity.suppressLoginFor(SHARE_LOGIN_SUPPRESS_MS)
        activity.bringToForeground()
    }

    fun openShareChooser(intent: Intent, title: String, loginSuppressMs: Long) {
        (context as? MainActivity)?.suppressLoginFor(loginSuppressMs)
        shareLauncher.launch(Intent.createChooser(intent, title))
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
                shareVideo(
                    context = context,
                    file = stoppedFile,
                    ligandId = safeLigandId,
                    ligand = uiState.ligand,
                    openChooser = { intent, title ->
                        openShareChooser(intent, title, VIDEO_SHARE_LOGIN_SUPPRESS_MS)
                    }
                )
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
        (activity as? MainActivity)?.suppressLoginFor(VIDEO_SHARE_LOGIN_SUPPRESS_MS)
        val mgr = activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projectionLauncher.launch(mgr.createScreenCaptureIntent())
    }

    fun startVideoRecording() {
        val activity = context as? Activity ?: return
        (activity as? MainActivity)?.suppressLoginFor(VIDEO_SHARE_LOGIN_SUPPRESS_MS)

        if (android.os.Build.VERSION.SDK_INT >= 33) {
            val ok = ContextCompat.checkSelfPermission(
                activity,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!ok) {
                postNotificationsLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        runCatching {
            ContextCompat.startForegroundService(
                activity,
                Intent(activity, MediaProjectionForegroundService::class.java)
            )
        }.onFailure {
            recordErrorMessage = it.localizedMessage ?: "Failed to start recording service."
            return
        }
        val mgr = activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projectionLauncher.launch(mgr.createScreenCaptureIntent())
    }

    fun requestScreenshot(format: ShareFormat) {
        val shareText = buildLigandShareText(safeLigandId, uiState.ligand)
        shareModelScreenshotPixelCopyFallback(
            context = context,
            ligandId = safeLigandId,
            format = format,
            shareText = shareText,
            sceneViewFor3d = sceneViewForScreenshot[0],
            openChooser = { intent, title ->
                openShareChooser(intent, title, SHARE_LOGIN_SUPPRESS_MS)
            }
        )
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
                    IconButton(onClick = leaveScreen) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    if (!currentUsername.isNullOrBlank()) {
                        Text(
                            text = currentUsername,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                    }
                    IconButton(onClick = onLogout) {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            contentDescription = stringResource(R.string.cd_logout)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    navigationIconContentColor = accentColor,
                    actionIconContentColor = accentColor,
                    titleContentColor = accentColor
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scaffoldSymmetricContentPadding(innerPadding, layoutDirection)
        ) {
            when {
                uiState.isLoading -> {
                    ProteinViewLoadingCard(
                        ligandId = safeLigandId,
                        loadingStage = uiState.loadingStage,
                        loadingProgress = uiState.loadingProgress,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                uiState.errorMessage != null -> {
                    ProteinViewErrorContent(
                        errorMessage = uiState.errorMessage!!,
                        onRetry = viewModel::retryLoad,
                        onBack = leaveScreen,
                        modifier = Modifier.align(Alignment.Center)
                    )
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
                        val bottomBarHeight = 86.dp
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
                            measurementBonds = uiState.measurementBonds,
                            onMeasurementBondTapped = viewModel::onBondTappedForMeasurement,
                            onClearMeasurement = viewModel::clearMeasurement,
                            onExitMeasurementMode = { viewModel.setMeasurementMode(false) },
                            autoRotate = isRecording,
                            modelAutoRotate = uiState.isAnimationEnabled,
                            sceneBackground = sceneTint,
                            onSceneViewForScreenshot = { v -> sceneViewForScreenshot[0] = v },
                            overlaysEnabled = overlayVisible,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = bottomBarHeight)
                                .clip(RoundedCornerShape(24.dp))
                        )

                        if (overlayVisible) {
                            Popup(
                                alignment = Alignment.TopEnd,
                                properties = PopupProperties(focusable = false)
                            ) {
                                ProteinViewActionButtons(
                                    isLandscape = isLandscape,
                                    bottomBarHeight = bottomBarHeight,
                                    isRecording = isRecording,
                                    isAnimationEnabled = uiState.isAnimationEnabled,
                                    measurementMode = uiState.measurementMode,
                                    showAtomLabels = uiState.showAtomLabels,
                                    showHydrogens = uiState.showHydrogens,
                                    visualizationMode = uiState.visualizationMode,
                                    onStartRecording = ::startVideoRecording,
                                    onToggleAnimation = viewModel::toggleAnimation,
                                    onToggleMeasurement = {
                                        viewModel.setMeasurementMode(!uiState.measurementMode)
                                    },
                                    onToggleLabels = {
                                        viewModel.setShowAtomLabels(!uiState.showAtomLabels)
                                    },
                                    onToggleHydrogens = {
                                        viewModel.setShowHydrogens(!uiState.showHydrogens)
                                    },
                                    onShare = { showShareFormatDialog = true },
                                    onBallsModeRequired = { showBallsModeHint = true }
                                )
                            }

                            Popup(
                                alignment = Alignment.TopStart,
                                properties = PopupProperties(focusable = false)
                            ) {
                                ProteinViewZoomControls(
                                    isLandscape = isLandscape,
                                    bottomBarHeight = bottomBarHeight,
                                    onZoomIn = {
                                        zoomFactor = (zoomFactor * 1.2f).coerceIn(0.3f, 5.0f)
                                    },
                                    onZoomOut = {
                                        zoomFactor = (zoomFactor / 1.2f).coerceIn(0.3f, 5.0f)
                                    },
                                    onReset = {
                                        zoomFactor = 1f
                                        resetTick++
                                    }
                                )
                            }

                            Popup(alignment = Alignment.TopCenter) {
                                ProteinViewModeBanners(
                                    measurementMode = uiState.measurementMode,
                                    showAtomLabels = uiState.showAtomLabels,
                                    showHydrogens = uiState.showHydrogens,
                                    showBallsModeHint = showBallsModeHint,
                                    onExitMeasurement = { viewModel.setMeasurementMode(false) },
                                    onExitLabels = { viewModel.setShowAtomLabels(false) },
                                    onExitHydrogens = { viewModel.setShowHydrogens(false) },
                                    onDismissBallsHint = { showBallsModeHint = false }
                                )
                            }

                            if (uiState.selectedAtom != null) {
                                Popup(
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
                                Popup(
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

                            ProteinViewVisualizationBar(
                                selectedMode = uiState.visualizationMode,
                                onModeSelected = viewModel::setVisualizationMode,
                                modifier = Modifier.align(Alignment.BottomCenter)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showShareFormatDialog) {
        ShareFormatDialog(
            onDismiss = { showShareFormatDialog = false },
            onPngSelected = { requestScreenshot(ShareFormat.PNG) },
            onJpegSelected = { requestScreenshot(ShareFormat.JPEG) }
        )
    }

    recordErrorMessage?.let { message ->
        RecordErrorDialog(
            message = message,
            onDismiss = { recordErrorMessage = null }
        )
    }

    if (uiState.largeMoleculeWarning) {
        LargeMoleculeWarningDialog(
            atomCount = uiState.ligand?.atoms?.size,
            onDismiss = { viewModel.dismissLargeMoleculeWarning() }
        )
    }
}
