package com.music42.swiftyprotein.ui.proteinview

import android.view.MotionEvent
import com.music42.swiftyprotein.data.model.Atom
import com.music42.swiftyprotein.data.model.Bond
import com.music42.swiftyprotein.data.model.Ligand
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.cross
import dev.romainguy.kotlin.math.normalize
import io.github.sceneview.SceneView
import io.github.sceneview.node.CameraNode
import io.github.sceneview.node.MeshNode
import kotlin.math.hypot

internal class MoleculeTouchState(
    val tapDownPos: FloatArray = floatArrayOf(0f, 0f),
    val tapDownTime: LongArray = longArrayOf(0L),
    val twoFinger: FloatArray = floatArrayOf(0f, 0f, 0f),
    val twoFingerSpan: FloatArray = floatArrayOf(0f)
)

@Suppress("LongParameterList")
internal fun handleMoleculeTouchEvent(
    event: MotionEvent,
    touchState: MoleculeTouchState,
    sceneViewRef: Array<SceneView?>,
    cameraNode: CameraNode,
    atomNodeMap: Map<MeshNode, Atom>,
    ligand: Ligand,
    zoomFactor: Float,
    distance: Float,
    measurementMode: Boolean,
    panOffset: Float3,
    onPanOffsetChange: (Float3) -> Unit,
    onZoomFactorChange: (Float) -> Unit,
    onInvalidateAtomLabels: () -> Unit,
    onApplyScrollZoom: (Float) -> Unit,
    lastTap: LongArray,
    lastTapAtomId: Array<String?>,
    onMeasurementBondTapped: (Bond) -> Unit,
    onMeasurementAtomTapped: (Atom) -> Unit,
    onBondSelected: (Bond) -> Unit,
    onAtomSelected: (Atom) -> Unit,
    onDismissAtom: () -> Unit,
    onDismissBond: () -> Unit,
    onFocusTarget: (Float3) -> Unit
): Boolean {
    return when (event.actionMasked) {
        MotionEvent.ACTION_POINTER_DOWN -> {
            if (event.pointerCount == 2) {
                val x0 = event.getX(0)
                val y0 = event.getY(0)
                val x1 = event.getX(1)
                val y1 = event.getY(1)
                touchState.twoFinger[0] = 1f
                touchState.twoFinger[1] = (x0 + x1) / 2f
                touchState.twoFinger[2] = (y0 + y1) / 2f
                touchState.twoFingerSpan[0] = hypot(x1 - x0, y1 - y0).coerceAtLeast(1f)
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
                onApplyScrollZoom(scroll)
                onInvalidateAtomLabels()
                true
            } else {
                false
            }
        }
        MotionEvent.ACTION_DOWN -> {
            touchState.tapDownPos[0] = event.x
            touchState.tapDownPos[1] = event.y
            touchState.tapDownTime[0] = event.eventTime
            false
        }
        MotionEvent.ACTION_MOVE -> {
            val sceneView = sceneViewRef[0]
            if (touchState.twoFinger[0] == 1f && event.pointerCount == 2 &&
                sceneView != null && sceneView.width > 0 && sceneView.height > 0
            ) {
                val x0 = event.getX(0)
                val y0 = event.getY(0)
                val x1 = event.getX(1)
                val y1 = event.getY(1)

                val span = hypot(x1 - x0, y1 - y0).coerceAtLeast(1f)
                val ratio = (span / touchState.twoFingerSpan[0]).coerceIn(0.85f, 1.15f)
                if (ratio != 1f) {
                    onZoomFactorChange((zoomFactor * ratio).coerceIn(0.3f, 5.0f))
                    onInvalidateAtomLabels()
                }
                touchState.twoFingerSpan[0] = span

                val midX = (x0 + x1) / 2f
                val midY = (y0 + y1) / 2f
                val dx = midX - touchState.twoFinger[1]
                val dy = midY - touchState.twoFinger[2]
                touchState.twoFinger[1] = midX
                touchState.twoFinger[2] = midY

                val camPos = cameraNode.position
                val forward = normalize(Float3(-camPos.x, -camPos.y, -camPos.z))
                val worldUp = Float3(0f, 1f, 0f)
                val right = normalize(cross(forward, worldUp))
                val up = cross(right, forward)

                val sensitivity = distance * 0.0006f
                val panDx = dx * sensitivity
                val panDy = dy * sensitivity
                onPanOffsetChange(
                    Float3(
                        panOffset.x + right.x * panDx + up.x * (-panDy),
                        panOffset.y + right.y * panDx + up.y * (-panDy),
                        panOffset.z + right.z * panDx + up.z * (-panDy)
                    )
                )
                onInvalidateAtomLabels()
                true
            } else {
                false
            }
        }
        MotionEvent.ACTION_UP -> {
            val dx = event.x - touchState.tapDownPos[0]
            val dy = event.y - touchState.tapDownPos[1]
            val moveDist = kotlin.math.sqrt(dx * dx + dy * dy)
            val elapsed = event.eventTime - touchState.tapDownTime[0]
            val sceneView = sceneViewRef[0]
            if (moveDist < 40f && elapsed < 500L && sceneView != null && sceneView.width > 0 && sceneView.height > 0) {
                val tapResult = resolveMoleculeTap(
                    tapX = event.x,
                    tapY = event.y,
                    sceneView = sceneView,
                    cameraNode = cameraNode,
                    atomNodeMap = atomNodeMap,
                    ligand = ligand
                )
                handleMoleculeTapResult(
                    result = tapResult,
                    measurementMode = measurementMode,
                    ligand = ligand,
                    eventTime = event.eventTime,
                    lastTap = lastTap,
                    lastTapAtomId = lastTapAtomId,
                    onMeasurementBondTapped = onMeasurementBondTapped,
                    onMeasurementAtomTapped = onMeasurementAtomTapped,
                    onBondSelected = onBondSelected,
                    onAtomSelected = onAtomSelected,
                    onDismissAtom = onDismissAtom,
                    onDismissBond = onDismissBond,
                    onFocusTarget = onFocusTarget
                )
            }
            false
        }
        MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
            touchState.twoFinger[0] = 0f
            false
        }
        else -> false
    }
}
