package com.music42.swiftyprotein.ui.settings

internal fun formatCacheSize(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val kb = 1024.0
    val mb = kb * 1024.0
    return when {
        bytes >= mb -> "%.1f MB".format(bytes / mb)
        bytes >= kb -> "%.1f KB".format(bytes / kb)
        else -> "$bytes B"
    }
}
