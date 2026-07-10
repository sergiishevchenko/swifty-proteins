package com.music42.swiftyprotein.ui.proteinview

import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import com.music42.swiftyprotein.data.model.Atom
import com.music42.swiftyprotein.data.model.Bond
import com.music42.swiftyprotein.data.model.Ligand
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.Scene
import io.github.sceneview.SceneView
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.CameraNode
import io.github.sceneview.node.MeshNode

@Composable
internal fun MoleculeViewer(
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
    measurementBonds: List<Bond>,
    onMeasurementBondTapped: (Bond) -> Unit,
    onClearMeasurement: () -> Unit,
    onExitMeasurementMode: () -> Unit,
    autoRotate: Boolean,
    modelAutoRotate: Boolean,
    sceneBackground: Color,
    onSceneViewForScreenshot: (android.view.View?) -> Unit,
    overlaysEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val engine = rememberEngine()
    val materialLoader = rememberMaterialLoader(engine)

    val lastTap = remember { longArrayOf(0L) }
    val lastTapAtomId = remember { arrayOfNulls<String>(1) }
    var focusTarget by remember(ligand.id) { mutableStateOf<Float3?>(null) }
    var focusOffset by remember(ligand.id) { mutableStateOf(Float3(0f, 0f, 0f)) }
    var panOffset by remember(ligand.id) { mutableStateOf(Float3(0f, 0f, 0f)) }
    val labelOverlayViewRef = remember { arrayOfNulls<android.view.View>(1) }
    val labelCameraRef = remember { arrayOfNulls<CameraNode>(1) }
    val labelAtomNodeMapRef = remember { arrayOfNulls<Map<MeshNode, Atom>>(1) }
    var sceneViewSizePx by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(showAtomLabels) {
        if (!showAtomLabels) {
            labelOverlayViewRef[0] = null
        }
    }

    val (parentNode, atomNodeMap, bondNodeMap) = remember(ligand, mode, showHydrogens) {
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

    MoleculeAtomHighlightEffect(
        atomNodeMap = atomNodeMap,
        materialLoader = materialLoader,
        selectedAtom = selectedAtom,
        measurementMode = measurementMode,
        measurementAtomIds = measurementAtomIds,
        measurementBonds = measurementBonds
    )

    MoleculeBondHighlightEffect(
        bondNodeMap = bondNodeMap,
        materialLoader = materialLoader,
        selectedBond = selectedBond,
        measurementMode = measurementMode,
        measurementBonds = measurementBonds,
        mode = mode,
        showHydrogens = showHydrogens
    )

    val touchState = remember { MoleculeTouchState() }
    val sceneViewRef = remember { arrayOfNulls<SceneView>(1) }

    fun invalidateAtomLabels() {
        if (!showAtomLabels || !overlaysEnabled) return
        val overlay = labelOverlayViewRef[0] ?: return
        val sceneView = sceneViewRef[0]
        if (sceneView != null) {
            sceneView.postOnAnimation { overlay.invalidate() }
        } else {
            overlay.postInvalidateOnAnimation()
        }
    }

    val panTarget = remember(ligand.id) { floatArrayOf(0f, 0f) }
    val firstFrameLogged = remember(ligand.id) { booleanArrayOf(false) }

    val cameraNode = rememberCameraNode(engine).apply {
        near = 0.1f
        far = 1000.0f
    }
    labelCameraRef[0] = cameraNode
    labelAtomNodeMapRef[0] = atomNodeMap

    val centerAtoms = ligandCenterAtoms(ligand)
    val cx = centerAtoms.map { it.x }.average().toFloat()
    val cy = centerAtoms.map { it.y }.average().toFloat()
    val cz = centerAtoms.map { it.z }.average().toFloat()

    val boundingRadius = computeBoundingRadius(ligand, mode, centerAtoms, cx, cy, cz)
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
    var autoRotateAngle by remember(ligand.id) { mutableFloatStateOf(0f) }
    var modelRotationAngle by remember(ligand.id) { mutableFloatStateOf(0f) }
    var lastFrameTimeNanos by remember(ligand.id) { mutableStateOf<Long?>(null) }
    if (resetTick > 0) {
        resetDefaultCameraVector(
            lastCameraVector = lastCameraVector,
            defaultCamX = defaultCamX,
            defaultCamY = defaultCamY,
            defaultCamZ = defaultCamZ,
            defaultCamLen = defaultCamLen,
            baseCameraDistance = baseCameraDistance
        )
        panTarget[0] = 0f
        panTarget[1] = 0f
        focusOffset = Float3(0f, 0f, 0f)
        panOffset = Float3(0f, 0f, 0f)
        focusTarget = null
        parentNode.position = io.github.sceneview.math.Position(0f, 0f, 0f)
        modelRotationAngle = 0f
        parentNode.rotation = Rotation(0f, 0f, 0f)
    }

    val cameraPosition = remember(ligand.id, zoomFactor, resetTick) {
        computeCameraPosition(
            lastCameraVector = lastCameraVector,
            distance = distance,
            defaultCamX = defaultCamX,
            defaultCamY = defaultCamY,
            defaultCamZ = defaultCamZ,
            defaultCamLen = defaultCamLen
        )
    }
    cameraNode.position = cameraPosition

    val cameraManipulator = remember(ligand.id, resetTick) {
        SceneView.createDefaultCameraManipulator(
            orbitHomePosition = cameraPosition,
            targetPosition = io.github.sceneview.math.Position(0f, 0f, 0f)
        )
    }

    val zoomFactorRef = remember { floatArrayOf(zoomFactor) }
    val onZoomCallbackRef = remember { arrayOf(onZoomFactorChange) }
    zoomFactorRef[0] = zoomFactor
    onZoomCallbackRef[0] = onZoomFactorChange

    fun applyScrollZoom(scroll: Float) {
        if (scroll == 0f) return
        val currentZoom = zoomFactorRef[0]
        val next = if (scroll > 0f) {
            (currentZoom * 1.12f).coerceIn(0.3f, 5.0f)
        } else {
            (currentZoom / 1.12f).coerceIn(0.3f, 5.0f)
        }
        onZoomCallbackRef[0](next)
    }

    Box(modifier = modifier) {
        Scene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            isOpaque = false,
            materialLoader = materialLoader,
            cameraNode = cameraNode,
            cameraManipulator = cameraManipulator,
            childNodes = listOf(parentNode),
            onTouchEvent = { event, _ ->
                handleMoleculeTouchEvent(
                    event = event,
                    touchState = touchState,
                    sceneViewRef = sceneViewRef,
                    cameraNode = cameraNode,
                    atomNodeMap = atomNodeMap,
                    ligand = ligand,
                    zoomFactor = zoomFactor,
                    distance = distance,
                    measurementMode = measurementMode,
                    panOffset = panOffset,
                    onPanOffsetChange = { panOffset = it },
                    onZoomFactorChange = onZoomFactorChange,
                    onInvalidateAtomLabels = ::invalidateAtomLabels,
                    onApplyScrollZoom = ::applyScrollZoom,
                    lastTap = lastTap,
                    lastTapAtomId = lastTapAtomId,
                    onMeasurementBondTapped = onMeasurementBondTapped,
                    onMeasurementAtomTapped = onMeasurementAtomTapped,
                    onBondSelected = onBondSelected,
                    onAtomSelected = onAtomSelected,
                    onDismissAtom = onDismissAtom,
                    onDismissBond = onDismissBond,
                    onFocusTarget = { focusTarget = it }
                )
            },
            onViewCreated = {
                sceneViewRef[0] = this
                onSceneViewForScreenshot(this)
                isFocusable = true
                isFocusableInTouchMode = true
                setOnGenericMotionListener { _, motionEvent ->
                    if (motionEvent.actionMasked != MotionEvent.ACTION_SCROLL) return@setOnGenericMotionListener false
                    val wheel = motionEvent.getAxisValue(MotionEvent.AXIS_VSCROLL)
                    val genericScroll = motionEvent.getAxisValue(MotionEvent.AXIS_SCROLL)
                    val hScroll = motionEvent.getAxisValue(MotionEvent.AXIS_HSCROLL)
                    val scroll = when {
                        wheel != 0f -> wheel
                        genericScroll != 0f -> genericScroll
                        else -> 0f
                    }
                    if (scroll != 0f && kotlin.math.abs(scroll) >= kotlin.math.abs(hScroll)) {
                        applyScrollZoom(scroll)
                        invalidateAtomLabels()
                        true
                    } else {
                        false
                    }
                }
                requestFocus()
                setZOrderOnTop(true)
                runCatching { holder.setFormat(PixelFormat.TRANSLUCENT) }
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
                sceneViewSizePx = IntSize(width.coerceAtLeast(0), height.coerceAtLeast(0))
            },
            onFrame = { frameTimeNanos ->
                if (!firstFrameLogged[0]) {
                    firstFrameLogged[0] = true
                }
                val position = cameraNode.position
                lastCameraVector[0] = position.x
                lastCameraVector[1] = position.y
                lastCameraVector[2] = position.z

                if (autoRotate) {
                    autoRotateAngle += 0.02f
                    val radius = distance
                    val x = kotlin.math.sin(autoRotateAngle) * radius
                    val z = kotlin.math.cos(autoRotateAngle) * radius
                    cameraNode.position = io.github.sceneview.math.Position(x, 0f, z)
                } else {
                    val len = kotlin.math.sqrt(position.x * position.x + position.y * position.y + position.z * position.z)
                        .coerceAtLeast(0.0001f)
                    if (kotlin.math.abs(len - distance) > 0.01f) {
                        val scale = distance / len
                        cameraNode.position = io.github.sceneview.math.Position(
                            position.x * scale,
                            position.y * scale,
                            position.z * scale
                        )
                    }
                }

                if (modelAutoRotate) {
                    val lastFrame = lastFrameTimeNanos
                    if (lastFrame != null) {
                        val deltaSeconds = (frameTimeNanos - lastFrame) / 1_000_000_000f
                        modelRotationAngle += MODEL_ROTATION_DEGREES_PER_SECOND * deltaSeconds
                    }
                    lastFrameTimeNanos = frameTimeNanos
                } else {
                    lastFrameTimeNanos = null
                }

                runCatching {
                    cameraNode.lookAt(io.github.sceneview.math.Position(0f, 0f, 0f))
                }

                val target = focusTarget
                if (target != null) {
                    val blend = 0.12f
                    val next = Float3(
                        focusOffset.x + (target.x - focusOffset.x) * blend,
                        focusOffset.y + (target.y - focusOffset.y) * blend,
                        focusOffset.z + (target.z - focusOffset.z) * blend
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
                parentNode.rotation = Rotation(0f, modelRotationAngle, 0f)

                invalidateAtomLabels()
            }
        )

        MoleculeLabelsOverlay(
            showAtomLabels = showAtomLabels,
            overlaysEnabled = overlaysEnabled,
            sceneViewSizePx = sceneViewSizePx,
            sceneViewRef = sceneViewRef,
            labelCameraRef = labelCameraRef,
            labelAtomNodeMapRef = labelAtomNodeMapRef,
            labelOverlayViewRef = labelOverlayViewRef
        )

        MoleculeMeasurementOverlay(
            measurementMode = measurementMode,
            overlaysEnabled = overlaysEnabled,
            ligand = ligand,
            measurementAtomIds = measurementAtomIds,
            measurementBonds = measurementBonds,
            onClearMeasurement = onClearMeasurement,
            onExitMeasurementMode = onExitMeasurementMode
        )
    }
}

private const val MODEL_ROTATION_DEGREES_PER_SECOND = 30f
