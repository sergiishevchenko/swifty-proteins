package com.music42.swiftyprotein.ui.proteinview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.music42.swiftyprotein.data.model.Atom
import com.music42.swiftyprotein.data.model.Bond
import com.music42.swiftyprotein.data.model.Ligand
import io.github.sceneview.SceneView
import io.github.sceneview.node.CameraNode
import io.github.sceneview.node.MeshNode

@Composable
internal fun MoleculeLabelsOverlay(
    showAtomLabels: Boolean,
    overlaysEnabled: Boolean,
    sceneViewSizePx: IntSize,
    sceneViewRef: Array<SceneView?>,
    labelCameraRef: Array<CameraNode?>,
    labelAtomNodeMapRef: Array<Map<MeshNode, Atom>?>,
    labelOverlayViewRef: Array<android.view.View?>
) {
    if (!showAtomLabels || !overlaysEnabled) return

    val onSurface = MaterialTheme.colorScheme.onSurface
    val density = LocalDensity.current
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
    val fontMetrics = paint.fontMetrics
    val textHalfH = -(fontMetrics.ascent + fontMetrics.descent) / 2f
    if (popupW <= 0 || popupH <= 0) return

    LabelOverlayPopup(
        widthPx = popupW,
        heightPx = popupH,
        density = density,
        sceneViewRef = sceneViewRef,
        cameraRef = labelCameraRef,
        atomNodeMapRef = labelAtomNodeMapRef,
        paint = paint,
        textHalfH = textHalfH,
        onOverlayView = { labelOverlayViewRef[0] = it }
    )
}

@Composable
internal fun MoleculeMeasurementOverlay(
    measurementMode: Boolean,
    overlaysEnabled: Boolean,
    ligand: Ligand,
    measurementAtomIds: List<String>,
    measurementBonds: List<Bond>,
    onClearMeasurement: () -> Unit,
    onExitMeasurementMode: () -> Unit
) {
    if (!measurementMode || !overlaysEnabled) return

    Popup(
        alignment = Alignment.BottomCenter,
        properties = PopupProperties(
            focusable = false,
            usePlatformDefaultWidth = true,
        )
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            MeasurementOverlay(
                ligand = ligand,
                selectedAtomIds = measurementAtomIds,
                selectedBonds = measurementBonds,
                onClear = onClearMeasurement,
                onExitMeasurementMode = onExitMeasurementMode,
            )
        }
    }
}
