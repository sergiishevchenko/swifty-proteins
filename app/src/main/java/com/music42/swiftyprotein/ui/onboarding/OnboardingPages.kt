package com.music42.swiftyprotein.ui.onboarding

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun OnboardingPageGestures(
    defaultMode: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = "Welcome to Swifty Proteins",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        modifier = modifier
    )
    Spacer(modifier = Modifier.height(12.dp))
    OnboardingCard(title = "3D Navigation") {
        Text("• Drag with one finger to rotate the molecule.")
        Text("• Pinch with two fingers to zoom in/out.")
        Text("• Move two fingers together to pan the view.")
        Text("• Tap an atom or bond to see details.")
        Text("• Double-tap an atom to center the camera on it.")
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Default mode: $defaultMode",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
internal fun OnboardingPageFeatures(modifier: Modifier = Modifier) {
    Text(
        text = "Visualization & Sharing",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        modifier = modifier
    )
    Spacer(modifier = Modifier.height(12.dp))
    OnboardingCard(title = "Visualization modes") {
        Text("• Balls — classic ball-and-stick model.")
        Text("• Fill — space-filling (van der Waals radii).")
        Text("• Sticks — bonds only, no atom spheres.")
        Text("• Wire — thin wireframe representation.")
        Spacer(modifier = Modifier.height(6.dp))
        Text("Switch modes instantly from the toolbar chips.")
    }
    Spacer(modifier = Modifier.height(12.dp))
    OnboardingCard(title = "Sharing") {
        Text("• Tap the share icon to export a screenshot (PNG or JPEG).")
        Text("• Tap the camera icon to record a rotating video.")
    }
}

@Composable
internal fun OnboardingPageExtras(modifier: Modifier = Modifier) {
    Text(
        text = "More Features",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        modifier = modifier
    )
    Spacer(modifier = Modifier.height(12.dp))
    OnboardingCard(title = "Favorites & Compare") {
        Text("• Tap the star on any ligand to add it to favorites.")
        Text("• Open the favorites list from the toolbar.")
        Text("• Select two favorites to compare side-by-side in 3D.")
    }
    Spacer(modifier = Modifier.height(12.dp))
    OnboardingCard(title = "Measurement & Labels") {
        Text("• Balls mode only — use the ruler and labels icons in the toolbar.")
        Text("• Measurement: tap 2 atoms for distance (Å), or 2 bonds sharing an atom for angle (°).")
        Text("• Labels: show element symbols on atoms; they track as you rotate and zoom.")
    }
    Spacer(modifier = Modifier.height(12.dp))
    OnboardingCard(title = "Settings") {
        Text("• Choose theme: System, Light, or Dark.")
        Text("• Set your default visualization mode.")
    }
}
