package com.music42.swiftyprotein.ui.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.music42.swiftyprotein.R

@Composable
internal fun SettingsCacheSection(
    cacheSizeBytes: Long,
    isClearing: Boolean,
    onClearCache: () -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = stringResource(R.string.settings_cache_title),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = stringResource(R.string.settings_cache_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = stringResource(
            R.string.settings_cache_size,
            formatCacheSize(cacheSizeBytes)
        ),
        style = MaterialTheme.typography.bodyMedium
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedButton(
        onClick = onClearCache,
        enabled = !isClearing && cacheSizeBytes > 0L,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            if (isClearing) {
                stringResource(R.string.settings_cache_clearing)
            } else {
                stringResource(R.string.settings_cache_clear)
            }
        )
    }
}
