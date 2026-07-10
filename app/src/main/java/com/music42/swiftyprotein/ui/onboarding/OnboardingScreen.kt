package com.music42.swiftyprotein.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.music42.swiftyprotein.ui.settings.SettingsViewModel

@Composable
fun OnboardingScreen(
    onDone: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val pagerState = rememberPagerState(pageCount = { ONBOARDING_PAGE_COUNT })

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

            OnboardingPagerDots(
                pagerState = pagerState,
                pageCount = ONBOARDING_PAGE_COUNT
            )

            OnboardingNavigationBar(
                pagerState = pagerState,
                pageCount = ONBOARDING_PAGE_COUNT,
                onComplete = {
                    viewModel.setOnboardingCompleted(true)
                    onDone()
                }
            )
        }
    }
}
