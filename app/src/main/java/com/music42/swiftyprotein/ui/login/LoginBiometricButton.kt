package com.music42.swiftyprotein.ui.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.music42.swiftyprotein.R
import com.music42.swiftyprotein.util.BiometricHelper

@Composable
internal fun LoginBiometricButton(
    visible: Boolean,
    onSuccess: () -> Unit,
    onFailure: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    AnimatedVisibility(visible = visible, modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    val activity = context as? FragmentActivity ?: return@OutlinedButton
                    BiometricHelper.authenticate(
                        activity = activity,
                        title = context.getString(R.string.biometric_title),
                        subtitle = context.getString(R.string.biometric_subtitle),
                        negativeButtonText = context.getString(R.string.biometric_cancel),
                        onSuccess = onSuccess,
                        onFailure = onFailure
                    )
                },
                modifier = Modifier.height(50.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Fingerprint login",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.biometric_login))
            }
        }
    }
}
