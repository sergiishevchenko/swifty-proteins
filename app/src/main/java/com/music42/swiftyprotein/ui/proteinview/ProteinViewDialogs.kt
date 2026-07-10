package com.music42.swiftyprotein.ui.proteinview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.music42.swiftyprotein.R

@Composable
internal fun ShareFormatDialog(
    onDismiss: () -> Unit,
    onPngSelected: () -> Unit,
    onJpegSelected: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export format") },
        text = { Text("Choose image format to share.") },
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                onPngSelected()
            }) { Text("PNG") }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = {
                    onDismiss()
                    onJpegSelected()
                }) { Text("JPEG") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

@Composable
internal fun RecordErrorDialog(
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Video recording") },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ok))
            }
        }
    )
}

@Composable
internal fun LargeMoleculeWarningDialog(
    atomCount: Int?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.large_molecule_title)) },
        text = {
            Text(
                if (atomCount != null) {
                    stringResource(R.string.large_molecule_message, atomCount)
                } else {
                    stringResource(R.string.load_error_message)
                }
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ok))
            }
        }
    )
}
