package com.music42.swiftyprotein.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.music42.swiftyprotein.ui.settings.SettingsViewModel
import kotlinx.coroutines.launch

private const val PAGE_COUNT = 3

@Composable
fun OnboardingScreen(
    onDone: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    val scope = rememberCoroutineScope()

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    when (page) {
                        0 -> OnboardingPageGestures(defaultMode = settings.defaultVisualizationMode.name)
                        1 -> OnboardingPageFeatures()
                        2 -> OnboardingPageExtras()
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(PAGE_COUNT) { index ->
                    val color by animateColorAsState(
                        targetValue = if (pagerState.currentPage == index)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.outlineVariant,
                        label = "dot"
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (pagerState.currentPage > 0) {
                    OutlinedButton(
                        onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Back")
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                Button(
                    onClick = {
                        if (pagerState.currentPage < PAGE_COUNT - 1) {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        } else {
                            viewModel.setOnboardingCompleted(true)
                            onDone()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (pagerState.currentPage < PAGE_COUNT - 1) "Next" else "Get started")
                }
            }
        }
    }
}

@Composable
private fun OnboardingPageGestures(defaultMode: String) {
    Text(
        text = "Welcome to Swifty Proteins",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold
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
private fun OnboardingPageFeatures() {
    Text(
        text = "Visualization & Sharing",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold
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
private fun OnboardingPageExtras() {
    Text(
        text = "More Features",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold
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

@Composable
private fun OnboardingCard(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}
