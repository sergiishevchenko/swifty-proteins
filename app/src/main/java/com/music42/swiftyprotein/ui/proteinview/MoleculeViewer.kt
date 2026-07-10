package com.music42.swiftyprotein.ui.proteinview

import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.music42.swiftyprotein.data.model.Atom
import com.music42.swiftyprotein.data.model.Bond
import com.music42.swiftyprotein.data.model.Ligand
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.Scene
import io.github.sceneview.SceneView
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.node.CameraNode
import io.github.sceneview.node.MeshNode
import kotlin.math.hypot

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
    val sceneViewWindowXY = remember { intArrayOf(0, 0) }
    var sceneViewSizePx by remember { mutableStateOf(IntSize.Zero) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

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
    LaunchedEffect(ligand.id, mode, atomNodeMap.size) {}

    LaunchedEffect(selectedAtom?.id, measurementMode, measurementAtomIds, measurementBonds) {
        val measureAccent = Color(0xFFFF6A00)
            val measurementAtomIdSet: Set<String> = if (measurementMode) {
                if (measurementBonds.isNotEmpty()) {
                    emptySet()
                } else {
                    measurementAtomIds.takeLast(2).toSet()
                }
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

    fun sameBond(a: Bond, b: Bond): Boolean {
        return (a.atomId1 == b.atomId1 && a.atomId2 == b.atomId2) ||
            (a.atomId1 == b.atomId2 && a.atomId2 == b.atomId1)
    }

    LaunchedEffect(selectedBond, measurementMode, measurementBonds, mode, showHydrogens) {
        // Highlight bonds when selected (info) and when used for angle measurement.
        val selected = selectedBond
        val measured = if (measurementMode) measurementBonds else emptyList()

        for ((node, info) in bondNodeMap) {
            val base = info.baseColor
            val isMeasured = measured.any { sameBond(it, info.bond) }
            val isSelected = selected != null && sameBond(selected, info.bond)

            val color = when {
                // Use vivid accent colors so the highlight is obvious.
                isMeasured -> Color(0.90f, 0.35f, 0.05f, 1f) // vivid orange (measure)
                isSelected -> Color(0.00f, 0.82f, 0.98f, 1f) // bright cyan (info)
                else -> base
            }

            node.materialInstance = materialLoader.createColorInstance(
                color = color,
                metallic = 0.0f,
                roughness = 0.4f,
                reflectance = 0.5f
            )
        }
    }

    val tapDownPos = remember { floatArrayOf(0f, 0f) }
    val tapDownTime = remember { longArrayOf(0L) }
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
    val twoFinger = remember { floatArrayOf(0f, 0f, 0f) }
    val twoFingerSpan = remember { floatArrayOf(0f) }
    val firstFrameLogged = remember(ligand.id) { booleanArrayOf(false) }

    val cameraNode = rememberCameraNode(engine).apply {
        near = 0.1f
        far = 1000.0f
    }
    labelCameraRef[0] = cameraNode
    labelAtomNodeMapRef[0] = atomNodeMap

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

    val zoomFactorRef = remember { floatArrayOf(zoomFactor) }
    val onZoomCallbackRef = remember { arrayOf(onZoomFactorChange) }
    zoomFactorRef[0] = zoomFactor
    onZoomCallbackRef[0] = onZoomFactorChange

    fun applyScrollZoom(scroll: Float) {
        if (scroll == 0f) return
        val zf = zoomFactorRef[0]
        val next = if (scroll > 0f) {
            (zf * 1.12f).coerceIn(0.3f, 5.0f)
        } else {
            (zf / 1.12f).coerceIn(0.3f, 5.0f)
        }
        onZoomCallbackRef[0](next)
    }

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
                            applyScrollZoom(scroll)
                            invalidateAtomLabels()
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
                                invalidateAtomLabels()
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
                            invalidateAtomLabels()
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
                            val bondPick = pickBond(
                                tapPxX = event.x,
                                tapPxY = event.y,
                                viewWidthPx = sv.width,
                                viewHeightPx = sv.height,
                                cameraNode = cameraNode,
                                atomNodeMap = atomNodeMap,
                                ligand = ligand
                            )
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
                            val atomDistPx = closestDist * sv.width.toFloat()
                            val atomHit = closestAtom != null && closestDist < ATOM_PICK_RADIUS_NORM
                            val bondDistPx = bondPick?.distancePx ?: Float.MAX_VALUE
                            val bondScreenHit = bondPick != null && bondDistPx <= bondPick.thresholdPx
                            val bondHit = bondScreenHit && (!atomHit || bondDistPx + 18f < atomDistPx)
                            val atomWins = atomHit && (!bondScreenHit || atomDistPx + 18f <= bondDistPx)
                            if (measurementMode) {
                                when {
                                    bondHit -> onMeasurementBondTapped(bondPick!!.bond)
                                    atomWins -> onMeasurementAtomTapped(closestAtom!!)
                                }
                            } else when {
                                bondHit -> onBondSelected(bondPick!!.bond)
                                atomWins -> {
                                    onAtomSelected(closestAtom!!)
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
                                }
                                else -> {
                                    onDismissAtom()
                                    onDismissBond()
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

                invalidateAtomLabels()
            }
        )

        if (showAtomLabels && overlaysEnabled) {
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
                    sceneViewRef = sceneViewRef,
                    cameraRef = labelCameraRef,
                    atomNodeMapRef = labelAtomNodeMapRef,
                    paint = paint,
                    textHalfH = textHalfH,
                    onOverlayView = { labelOverlayViewRef[0] = it }
                )
            }
        }

        if (measurementMode && overlaysEnabled) {
            androidx.compose.ui.window.Popup(
                alignment = Alignment.BottomCenter,
                properties = androidx.compose.ui.window.PopupProperties(
                    focusable = false,
                    usePlatformDefaultWidth = true,
                ),
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

    }
}
