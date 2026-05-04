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
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
    currentUsername: String?,
    onLogout: () -> Unit,
    viewModel: CompareViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val accentGreen = Color(0xFF4CAF50)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Compare") },
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
    var resetTick by remember(ligand.id) { mutableIntStateOf(0) }
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
                    resetTick = resetTick,
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
            }
        }
    }
}

@Composable
private fun SimpleMoleculeViewer(
    ligand: Ligand,
    zoomFactor: Float,
    onZoomFactorChange: (Float) -> Unit,
    resetTick: Int,
    modifier: Modifier = Modifier
) {
    val engine = rememberEngine()
    val materialLoader = rememberMaterialLoader(engine)
    val sceneTint = if (MaterialTheme.colorScheme.background.red < 0.4f) {
        Color(0xFF151A20)
    } else {
        Color(0xFFF3F5F7)
    }
    val atoms = ligand.atoms.filterNot {
        val e = it.element.uppercase().trim()
        e == "H" || e == "D"
    }.ifEmpty { ligand.atoms }
    val cx = atoms.map { it.x }.average().toFloat()
    val cy = atoms.map { it.y }.average().toFloat()
    val cz = atoms.map { it.z }.average().toFloat()
    val centerOffset = remember(ligand.id) { dev.romainguy.kotlin.math.Float3(cx, cy, cz) }
    val (parentNode, _, _) = remember(ligand.id) {
        MoleculeSceneBuilder.build(
            engine = engine,
            materialLoader = materialLoader,
            ligand = ligand,
            mode = VisualizationMode.BALL_AND_STICK,
            highlightElement = null,
            centerOffset = centerOffset,
            showHydrogens = false
        )
    }

    val cameraNode = rememberCameraNode(engine).apply {
        near = 0.1f
        far = 1000.0f
    }

    val boundingRadius = (atoms.maxOfOrNull { a ->
        val dx = a.x - cx; val dy = a.y - cy; val dz = a.z - cz
        kotlin.math.sqrt(dx * dx + dy * dy + dz * dz) + MoleculeSceneBuilder.BALL_RADIUS
    } ?: 5f).coerceAtLeast(1f)
    val baseDist = boundingRadius * 4.5f
    val dist = (baseDist / zoomFactor).coerceIn(baseDist * 0.2f, baseDist * 6f)
    val dirX = 0.43f; val dirY = 0.32f; val dirZ = 0.75f
    val dirLen = kotlin.math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ)
    val defaultCameraPos = io.github.sceneview.math.Position(
        dirX / dirLen * dist,
        dirY / dirLen * dist,
        dirZ / dirLen * dist
    )
    LaunchedEffect(ligand.id, resetTick) {
        cameraNode.position = defaultCameraPos
        runCatching { cameraNode.lookAt(io.github.sceneview.math.Position(0f, 0f, 0f)) }
    }

    val cameraManipulator = remember(ligand.id, resetTick) {
        SceneView.createDefaultCameraManipulator(
            orbitHomePosition = defaultCameraPos,
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
        onFrame = {
            runCatching {
                val p = cameraNode.position
                val len = kotlin.math.sqrt(p.x * p.x + p.y * p.y + p.z * p.z).coerceAtLeast(0.0001f)
                val k = dist / len
                cameraNode.position = io.github.sceneview.math.Position(p.x * k, p.y * k, p.z * k)
                cameraNode.lookAt(io.github.sceneview.math.Position(0f, 0f, 0f))
            }
        },
        onViewCreated = {
            renderer.clearOptions = renderer.clearOptions.apply {
                clear = true
                clearColor = floatArrayOf(sceneTint.red, sceneTint.green, sceneTint.blue, 1f)
            }
        },
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

