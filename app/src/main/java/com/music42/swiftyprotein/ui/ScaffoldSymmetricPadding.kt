package com.music42.swiftyprotein.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.LayoutDirection

fun Modifier.scaffoldSymmetricContentPadding(
    inner: PaddingValues,
    layoutDirection: LayoutDirection,
): Modifier {
    val start = inner.calculateStartPadding(layoutDirection)
    val end = inner.calculateEndPadding(layoutDirection)
    val horizontal = maxOf(start, end)
    return padding(
        top = inner.calculateTopPadding(),
        bottom = inner.calculateBottomPadding(),
        start = horizontal,
        end = horizontal,
    )
}
