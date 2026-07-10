package com.music42.swiftyprotein.ui.proteinview

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import androidx.core.content.FileProvider
import com.music42.swiftyprotein.data.model.Ligand
import java.io.File

internal enum class ShareFormat(val extension: String, val mimeType: String) {
    PNG("png", "image/png"),
    JPEG("jpg", "image/jpeg")
}

internal fun buildLigandShareText(ligandId: String, ligand: Ligand?): String {
    val namePart = ligand?.name?.takeIf { it.isNotBlank() } ?: "Unknown ligand"
    val atomsPart = ligand?.atoms?.size?.let { "Atoms: $it" } ?: "Atoms: ?"
    val formulaPart = ligand?.formula?.takeIf { it.isNotBlank() }?.let { "Formula: $it" } ?: "Formula: ?"
    return buildString {
        append("Ligand $ligandId — $namePart\n")
        append("$atomsPart · $formulaPart\n")
        append("https://www.rcsb.org/ligand/$ligandId")
    }
}

internal fun shareImageFile(
    context: Context,
    file: File,
    format: ShareFormat,
    chooserTitle: String,
    shareText: String,
    ligandId: String,
    openChooser: (Intent, String) -> Unit
) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = format.mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, shareText)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val clip = ClipData.newUri(
            context.contentResolver,
            "ligand_${ligandId}",
            uri
        )
        clip.addItem(ClipData.Item(shareText))
        clipData = clip
    }
    val resInfoList = context.packageManager.queryIntentActivities(intent, 0)
    for (resolveInfo in resInfoList) {
        runCatching {
            context.grantUriPermission(
                resolveInfo.activityInfo.packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
    }
    openChooser(intent, chooserTitle)
}

internal fun shareModelScreenshotPixelCopyFallback(
    context: Context,
    ligandId: String,
    format: ShareFormat,
    shareText: String,
    sceneViewFor3d: android.view.View?,
    openChooser: (Intent, String) -> Unit
) {
    val activity = context as? Activity ?: return
    val window = activity.window
    val decor = window.decorView
    val bitmap = Bitmap.createBitmap(decor.width, decor.height, Bitmap.Config.ARGB_8888)

    PixelCopy.request(window, bitmap, { windowResult ->
        if (windowResult != PixelCopy.SUCCESS) return@request

        val svRoot = sceneViewFor3d
        val surfaceView = svRoot?.let { findSurfaceView(it) }
        if (surfaceView == null || surfaceView.width <= 0 || surfaceView.height <= 0) {
            val file = saveBitmapToCache(context, bitmap, ligandId, format)
            shareImageFile(context, file, format, "Share Ligand", shareText, ligandId, openChooser)
            return@request
        }

        val modelBitmap = Bitmap.createBitmap(surfaceView.width, surfaceView.height, Bitmap.Config.ARGB_8888)
        PixelCopy.request(
            surfaceView,
            modelBitmap,
            PixelCopy.OnPixelCopyFinishedListener { modelResult ->
                if (modelResult == PixelCopy.SUCCESS) {
                    val loc = IntArray(2)
                    surfaceView.getLocationInWindow(loc)
                    val canvas = android.graphics.Canvas(bitmap)
                    canvas.drawBitmap(modelBitmap, loc[0].toFloat(), loc[1].toFloat(), null)
                }
                val file = saveBitmapToCache(context, bitmap, ligandId, format)
                shareImageFile(context, file, format, "Share Ligand", shareText, ligandId, openChooser)
            },
            Handler(Looper.getMainLooper())
        )
    }, Handler(Looper.getMainLooper()))
}

private fun findSurfaceView(root: android.view.View): android.view.SurfaceView? {
    if (root is android.view.SurfaceView) return root
    if (root is android.view.ViewGroup) {
        for (i in 0 until root.childCount) {
            val child = root.getChildAt(i) ?: continue
            val found = findSurfaceView(child)
            if (found != null) return found
        }
    }
    return null
}

private fun saveBitmapToCache(
    context: Context,
    bitmap: Bitmap,
    ligandId: String,
    format: ShareFormat
): File {
    val dir = File(context.cacheDir, "shared_images")
    dir.mkdirs()
    val file = File(dir, "ligand_${ligandId}.${format.extension}")
    file.outputStream().use { out ->
        val compressFormat = when (format) {
            ShareFormat.PNG -> Bitmap.CompressFormat.PNG
            ShareFormat.JPEG -> Bitmap.CompressFormat.JPEG
        }
        bitmap.compress(compressFormat, 92, out)
    }
    return file
}

internal fun shareVideo(
    context: Context,
    file: File,
    ligandId: String,
    ligand: Ligand?,
    openChooser: (Intent, String) -> Unit
) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    val shareText = buildLigandShareText(ligandId, ligand)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "video/mp4"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, shareText)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val clip = ClipData.newUri(
            context.contentResolver,
            "ligand_${ligandId}",
            uri
        )
        clip.addItem(ClipData.Item(shareText))
        clipData = clip
    }
    val resInfoList = context.packageManager.queryIntentActivities(intent, 0)
    for (resolveInfo in resInfoList) {
        runCatching {
            context.grantUriPermission(
                resolveInfo.activityInfo.packageName,
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
    }
    openChooser(intent, "Share Ligand")
}

internal class ScreenRecorder(
    private val activity: Activity,
    private val projection: MediaProjection,
    private val outputFile: File
) {
    private var recorder: MediaRecorder? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var projectionCallback: MediaProjection.Callback? = null
    private var started: Boolean = false

    fun start() {
        val metrics = activity.resources.displayMetrics
        val rawWidth = (activity.window.decorView.width.takeIf { it > 0 } ?: metrics.widthPixels)
        val rawHeight = (activity.window.decorView.height.takeIf { it > 0 } ?: metrics.heightPixels)
        val densityDpi = metrics.densityDpi

        val maxWidth = 1280
        val scale = if (rawWidth > maxWidth) maxWidth.toFloat() / rawWidth.toFloat() else 1f
        val width = ((rawWidth * scale).toInt() / 2) * 2
        val height = ((rawHeight * scale).toInt() / 2) * 2

        val mr = MediaRecorder().apply {
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setVideoEncodingBitRate(4_000_000)
            setVideoFrameRate(30)
            setVideoSize(width, height)
            setOutputFile(outputFile.absolutePath)
            prepare()
        }

        recorder = mr
        started = false

        val cb = object : MediaProjection.Callback() {
            override fun onStop() {
            }
        }
        projectionCallback = cb
        projection.registerCallback(cb, Handler(Looper.getMainLooper()))

        virtualDisplay = projection.createVirtualDisplay(
            "SwiftyProteinRecord",
            width,
            height,
            densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mr.surface,
            null,
            null
        )
        mr.start()
        started = true
    }

    fun stop(): File {
        if (started) {
            runCatching { recorder?.stop() }.onFailure {
            }
        }
        runCatching { recorder?.release() }
        recorder = null
        runCatching { virtualDisplay?.release() }
        virtualDisplay = null
        runCatching {
            projectionCallback?.let { projection.unregisterCallback(it) }
        }
        projectionCallback = null
        runCatching { projection.stop() }
        return outputFile
    }
}
