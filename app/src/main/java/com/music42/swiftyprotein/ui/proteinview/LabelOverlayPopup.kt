package com.music42.swiftyprotein.ui.proteinview

import android.content.Context
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.music42.swiftyprotein.data.model.Atom
import io.github.sceneview.SceneView
import io.github.sceneview.node.CameraNode
import io.github.sceneview.node.MeshNode

@Composable
internal fun LabelOverlayPopup(
    widthPx: Int,
    heightPx: Int,
    density: Density,
    sceneViewRef: Array<SceneView?>,
    cameraRef: Array<CameraNode?>,
    atomNodeMapRef: Array<Map<MeshNode, Atom>?>,
    paint: android.graphics.Paint,
    textHalfH: Float,
    onOverlayView: (android.view.View) -> Unit
) {
    Popup(
        alignment = Alignment.TopStart,
        properties = PopupProperties(
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
                        val sv = sceneViewRef[0] ?: return
                        val camera = cameraRef[0] ?: return
                        val nodes = atomNodeMapRef[0] ?: return
                        if (sv.width <= 0 || sv.height <= 0) return
                        val w = sv.width.toFloat()
                        val h = sv.height.toFloat()
                        for ((node, atom) in nodes) {
                            val v = camera.worldToView(node.worldPosition)
                            val px = (v.x * w).coerceIn(0f, w)
                            val py = ((1f - v.y) * h).coerceIn(0f, h)
                            val text = atom.element
                            val tw = paint.measureText(text)
                            c.drawText(text, px - tw / 2f, py + textHalfH, paint)
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
                }.also { onOverlayView(it) }
            },
            modifier = sizeModifier
        )
    }
}
