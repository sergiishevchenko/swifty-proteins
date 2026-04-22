package com.music42.swiftyprotein.ui.compare

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.music42.swiftyprotein.data.model.Ligand
import com.music42.swiftyprotein.ui.proteinview.VisualizationMode
import com.music42.swiftyprotein.ui.proteinview.MoleculeSceneBuilder
import io.github.sceneview.Scene
import io.github.sceneview.SceneView
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import kotlin.math.hypot

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareScreen(
    onBack: () -> Unit,
    viewModel: CompareViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Compare") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.errorMessage != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Failed to load comparison", color = MaterialTheme.colorScheme.error)
                        Text(uiState.errorMessage ?: "")
                    }
                }
                uiState.ligandA != null && uiState.ligandB != null -> {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ComparePanel(
                            title = uiState.ligandAId,
                            ligand = uiState.ligandA!!,
                            modifier = Modifier.weight(1f)
                        )
                        ComparePanel(
                            title = uiState.ligandBId,
                            ligand = uiState.ligandB!!,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ComparePanel(
    title: String,
    ligand: Ligand,
    modifier: Modifier = Modifier
) {
    var zoomFactor by remember(ligand.id) { mutableFloatStateOf(1f) }
    val background: Color = MaterialTheme.colorScheme.background

    Card(
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = title,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                style = MaterialTheme.typography.titleMedium
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(background)
            ) {
                SimpleMoleculeViewer(
                    ligand = ligand,
                    zoomFactor = zoomFactor,
                    onZoomFactorChange = { zoomFactor = it },
                    modifier = Modifier.fillMaxSize()
                )

                androidx.compose.ui.window.Popup(alignment = Alignment.BottomEnd) {
                    Column(
                        modifier = Modifier.padding(end = 8.dp, bottom = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Card(
                            modifier = Modifier.size(40.dp),
                            shape = RoundedCornerShape(999.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            IconButton(
                                onClick = { zoomFactor = (zoomFactor * 1.2f).coerceIn(0.3f, 5.0f) },
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    Icons.Filled.Add,
                                    contentDescription = "Zoom in",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.size(40.dp),
                            shape = RoundedCornerShape(999.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            IconButton(
                                onClick = { zoomFactor = (zoomFactor / 1.2f).coerceIn(0.3f, 5.0f) },
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    Icons.Filled.Remove,
                                    contentDescription = "Zoom out",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SimpleMoleculeViewer(
    ligand: Ligand,
    zoomFactor: Float,
    onZoomFactorChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val engine = rememberEngine()
    val materialLoader = rememberMaterialLoader(engine)
    val centerOffset = remember(ligand.id) { dev.romainguy.kotlin.math.Float3(0f, 0f, 0f) }
    val (parentNode, _) = remember(ligand.id) {
        MoleculeSceneBuilder.build(
            engine = engine,
            materialLoader = materialLoader,
            ligand = ligand,
            mode = VisualizationMode.BALL_AND_STICK,
            highlightElement = null,
            centerOffset = centerOffset
        )
    }

    val cameraNode = rememberCameraNode(engine).apply {
        near = 0.1f
        far = 1000.0f
    }

    val maxCoord = ligand.atoms.maxOfOrNull {
        maxOf(kotlin.math.abs(it.x), kotlin.math.abs(it.y), kotlin.math.abs(it.z))
    } ?: 5f
    val baseCameraDistance = maxCoord * 2.5f
    val distance = (baseCameraDistance / zoomFactor).coerceIn(baseCameraDistance * 0.3f, baseCameraDistance * 4f)
    cameraNode.position = io.github.sceneview.math.Position(0f, 0f, distance)

    val cameraManipulator = remember(ligand.id, zoomFactor) {
        SceneView.createDefaultCameraManipulator(
            orbitHomePosition = cameraNode.position,
            targetPosition = io.github.sceneview.math.Position(0f, 0f, 0f)
        )
    }

    val twoFingerActive = remember { floatArrayOf(0f) }
    val twoFingerSpan = remember { floatArrayOf(0f) }

    Scene(
        modifier = modifier,
        engine = engine,
        isOpaque = false,
        materialLoader = materialLoader,
        cameraNode = cameraNode,
        cameraManipulator = cameraManipulator,
        childNodes = listOf(parentNode),
        onTouchEvent = { event, _ ->
            when (event.actionMasked) {
                android.view.MotionEvent.ACTION_POINTER_DOWN -> {
                    if (event.pointerCount == 2) {
                        val x0 = event.getX(0)
                        val y0 = event.getY(0)
                        val x1 = event.getX(1)
                        val y1 = event.getY(1)
                        twoFingerActive[0] = 1f
                        twoFingerSpan[0] = hypot(x1 - x0, y1 - y0).coerceAtLeast(1f)
                        true
                    } else false
                }
                android.view.MotionEvent.ACTION_MOVE -> {
                    if (twoFingerActive[0] == 1f && event.pointerCount == 2) {
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
                        true
                    } else false
                }
                android.view.MotionEvent.ACTION_POINTER_UP,
                android.view.MotionEvent.ACTION_UP,
                android.view.MotionEvent.ACTION_CANCEL -> {
                    if (twoFingerActive[0] == 1f) {
                        twoFingerActive[0] = 0f
                        true
                    } else false
                }
                android.view.MotionEvent.ACTION_SCROLL -> {
                    val wheel = event.getAxisValue(android.view.MotionEvent.AXIS_VSCROLL)
                    val scroll = if (wheel != 0f) wheel else event.getAxisValue(android.view.MotionEvent.AXIS_SCROLL)
                    if (scroll != 0f) {
                        val next = if (scroll > 0f) zoomFactor * 1.12f else zoomFactor / 1.12f
                        onZoomFactorChange(next.coerceIn(0.3f, 5.0f))
                        true
                    } else false
                }
                else -> false
            }
        }
    )
}

