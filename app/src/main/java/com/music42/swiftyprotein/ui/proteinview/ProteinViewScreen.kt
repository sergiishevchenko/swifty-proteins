package com.music42.swiftyprotein.ui.proteinview

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.music42.swiftyprotein.data.model.Atom
import com.music42.swiftyprotein.data.model.Ligand
import io.github.sceneview.Scene
import io.github.sceneview.node.MeshNode
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberMaterialLoader
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProteinViewScreen(
    ligandId: String,
    onBack: () -> Unit,
    viewModel: ProteinViewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.ligand?.let { "${it.id} - ${it.name}" } ?: ligandId,
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
                        shareModelScreenshot(context, ligandId)
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
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Loading $ligandId...")
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
                    MoleculeViewer(
                        ligand = uiState.ligand!!,
                        mode = uiState.visualizationMode,
                        selectedAtom = uiState.selectedAtom,
                        onAtomSelected = viewModel::onAtomSelected,
                        onDismissAtom = viewModel::dismissAtomInfo,
                        modifier = Modifier.fillMaxSize()
                    )

                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        VisualizationMode.entries.forEach { mode ->
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
                        }
                    }

                    if (uiState.selectedAtom != null) {
                        Box(modifier = Modifier.align(Alignment.TopCenter)) {
                            AtomTooltip(
                                atom = uiState.selectedAtom!!,
                                onDismiss = viewModel::dismissAtomInfo
                            )
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

    remember(atomNodeMap) {
        atomNodeMap.forEach { (meshNode, atom) ->
            meshNode.isTouchable = true
            meshNode.onSingleTapConfirmed = { _ ->
                onAtomSelected(atom)
                true
            }
        }
        true
    }

    val cameraNode = rememberCameraNode(engine).apply {
        val maxCoord = ligand.atoms.maxOfOrNull {
            maxOf(
                kotlin.math.abs(it.x),
                kotlin.math.abs(it.y),
                kotlin.math.abs(it.z)
            )
        } ?: 5f
        position = io.github.sceneview.math.Position(0f, 0f, maxCoord * 2.5f)
    }

    val cameraManipulator = rememberCameraManipulator(
        orbitHomePosition = cameraNode.position
    )

    Box(modifier = modifier) {
        Scene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            materialLoader = materialLoader,
            cameraNode = cameraNode,
            cameraManipulator = cameraManipulator,
            childNodes = listOf(parentNode),
            onFrame = { }
        )

        if (selectedAtom != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onDismissAtom() }
            )
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
