package com.music42.swiftyprotein.ui.proteinview

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.PixelCopy
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.music42.swiftyprotein.data.model.Atom
import com.music42.swiftyprotein.data.model.Ligand
import io.github.sceneview.Scene
import io.github.sceneview.SceneView
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import java.io.File
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProteinViewScreen(
    ligandId: String,
    onBack: () -> Unit,
    viewModel: ProteinViewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val safeLigandId = ligandId.substringBefore(" -").trim().ifEmpty { ligandId.trim() }
    var zoomFactor by remember(uiState.ligand?.id) { mutableFloatStateOf(1f) }

    Scaffold(
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
                    IconButton(onClick = {
                        shareModelScreenshot(context, safeLigandId)
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
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
                    val loadingMessages = listOf(
                        "Fetching ligand data",
                        "Building 3D geometry",
                        "Preparing scene"
                    )
                    var loadingMessageIndex by remember { mutableIntStateOf(0) }
                    androidx.compose.runtime.LaunchedEffect(uiState.isLoading) {
                        while (uiState.isLoading) {
                            delay(550)
                            loadingMessageIndex = (loadingMessageIndex + 1) % loadingMessages.size
                        }
                    }
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
                                text = loadingMessages[loadingMessageIndex],
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                modifier = Modifier.width(180.dp)
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
                    Box(modifier = Modifier.fillMaxSize()) {
                        MoleculeViewer(
                            ligand = uiState.ligand!!,
                            mode = uiState.visualizationMode,
                            zoomFactor = zoomFactor,
                            onZoomFactorChange = { zoomFactor = it },
                            selectedAtom = uiState.selectedAtom,
                            onAtomSelected = viewModel::onAtomSelected,
                            onDismissAtom = viewModel::dismissAtomInfo,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 84.dp)
                        )
                        if (uiState.selectedAtom != null) {
                            androidx.compose.ui.window.Popup(
                                alignment = Alignment.TopCenter,
                                onDismissRequest = { viewModel.dismissAtomInfo() }
                            ) {
                                AtomTooltip(
                                    atom = uiState.selectedAtom!!,
                                    onDismiss = viewModel::dismissAtomInfo
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            VisualizationMode.entries.forEachIndexed { index, mode ->
                                FilterChip(
                                    selected = uiState.visualizationMode == mode,
                                    onClick = { viewModel.setVisualizationMode(mode) },
                                    label = {
                                        Text(
                                            when (mode) {
                                                VisualizationMode.BALL_AND_STICK -> "Ball & Stick"
                                                VisualizationMode.SPACE_FILL -> "Space Fill"
                                                VisualizationMode.STICKS_ONLY -> "Sticks"
                                            },
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                )
                                if (index != VisualizationMode.entries.lastIndex) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            FilledTonalIconButton(
                                onClick = { zoomFactor = (zoomFactor / 1.2f).coerceIn(0.3f, 5.0f) },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Zoom out")
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            FilledTonalIconButton(
                                onClick = { zoomFactor = (zoomFactor * 1.2f).coerceIn(0.3f, 5.0f) },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Zoom in")
                            }
                        }
                    }
                }
            }
        }
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
    modifier: Modifier = Modifier
) {
    val engine = rememberEngine()
    val materialLoader = rememberMaterialLoader(engine)

    val (parentNode, atomNodeMap) = remember(ligand, mode) {
        MoleculeSceneBuilder.build(engine, materialLoader, ligand, mode)
    }

    val tapDownPos = remember { floatArrayOf(0f, 0f) }
    val tapDownTime = remember { longArrayOf(0L) }
    val sceneViewRef = remember { arrayOfNulls<SceneView>(1) }

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
            targetPosition = io.github.sceneview.math.Position(0f, 0f, 0f)
        )
    }

    Box(
        modifier = modifier
            .background(Color.White)
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
                                onAtomSelected(closestAtom)
                            } else {
                                onDismissAtom()
                            }
                        }
                        false
                    }
                    else -> false
                }
            },
            onViewCreated = {
                sceneViewRef[0] = this
                renderer.clearOptions = renderer.clearOptions.apply {
                    clear = true
                    clearColor = floatArrayOf(1f, 1f, 1f, 1f)
                }
            },
            onFrame = {
                val p = cameraNode.position
                lastCameraVector[0] = p.x
                lastCameraVector[1] = p.y
                lastCameraVector[2] = p.z
            }
        )

    }
}

@Composable
private fun AtomTooltip(
    atom: Atom,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(top = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.inverseSurface)
            .clickable { onDismiss() }
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = atom.element,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.inverseOnSurface
            )
            Text(
                text = atom.elementName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.8f)
            )
            Text(
                text = "Atom: ${atom.id}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.6f)
            )
        }
    }
}

private fun shareModelScreenshot(context: Context, ligandId: String) {
    val activity = context as? Activity ?: return
    val window = activity.window
    val view = window.decorView
    val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)

    PixelCopy.request(window, bitmap, { result ->
        if (result == PixelCopy.SUCCESS) {
            val dir = File(context.cacheDir, "shared_images")
            dir.mkdirs()
            val file = File(dir, "ligand_${ligandId}.png")
            file.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Ligand $ligandId — visualized with Swifty Protein\nhttps://www.rcsb.org/ligand/$ligandId"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Ligand"))
        } else {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Ligand $ligandId — visualized with Swifty Protein\nhttps://www.rcsb.org/ligand/$ligandId"
                )
            }
            context.startActivity(Intent.createChooser(intent, "Share Ligand"))
        }
    }, Handler(Looper.getMainLooper()))
}
