package com.piplanner.ui.setup

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.piplanner.R
import com.piplanner.ui.components.PiCard
import com.piplanner.ui.components.PrimaryCta
import com.piplanner.ui.theme.PiPlannerColors
import com.piplanner.ui.theme.PiPlannerDimens
import com.piplanner.ui.theme.PiPlannerTheme
import com.piplanner.ui.theme.PiPlannerTypography

/**
 * Welcome screen — design frame 1; Spec §5.2 J1 / PRD R6.
 * Visual / layout / token / component wiring only (PIP-74).
 * Three "How it works" steps; CTA navigates to Accounts.
 */
@Composable
fun WelcomeScreen(
    viewModel: WelcomeViewModel,
    onNavigateToAccounts: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.shouldNavigateToAccounts) {
        if (uiState.shouldNavigateToAccounts) {
            viewModel.consumeNavigation()
            onNavigateToAccounts()
        }
    }

    WelcomeContent(
        uiState = uiState,
        onSetUpSavings = viewModel::setUpSavings,
    )
}

@Composable
fun WelcomeContent(
    uiState: WelcomeUiState,
    onSetUpSavings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PiPlannerColors.BackgroundApp)
            .verticalScroll(rememberScrollState())
            .padding(PiPlannerDimens.Space20)
            .semantics { contentDescription = "Welcome screen" },
        verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space28),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space12),
            modifier = Modifier.semantics { contentDescription = "welcome.header" },
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = PiPlannerTypography.amountHero,
                color = PiPlannerColors.NavyPrimary,
                modifier = Modifier.semantics { contentDescription = "welcome.brand" },
            )
            Text(
                text = stringResource(R.string.welcome_tagline),
                style = PiPlannerTypography.title,
                color = PiPlannerColors.NavyDeep,
            )
            Text(
                text = stringResource(R.string.welcome_subtitle),
                style = PiPlannerTypography.body,
                color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space16),
            modifier = Modifier.semantics { contentDescription = "welcome.steps" },
        ) {
            Text(
                text = stringResource(R.string.welcome_how_it_works),
                style = PiPlannerTypography.title,
                color = PiPlannerColors.NavyPrimary,
                modifier = Modifier.semantics { contentDescription = "welcome.howItWorks" },
            )
            PiCard(
                contentPadding = PiPlannerDimens.Space20,
                contentDescription = "welcome.steps.card",
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space20),
                ) {
                    uiState.steps.forEach { step ->
                        WelcomeStepRow(step = step)
                    }
                }
            }
        }

        PrimaryCta(
            text = stringResource(R.string.welcome_cta),
            onClick = onSetUpSavings,
            contentDescription = "welcome.cta",
        )

        Spacer(modifier = Modifier.height(PiPlannerDimens.Space8))
    }
}

@Composable
private fun WelcomeStepRow(step: WelcomeStep) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "welcome.step.${step.id}"
            },
        horizontalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space12),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(PiPlannerColors.ChipLightBlue),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = step.id.toString(),
                style = PiPlannerTypography.body,
                fontWeight = FontWeight.Bold,
                color = PiPlannerColors.NavyPrimary,
                textAlign = TextAlign.Center,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(PiPlannerDimens.Space8),
        ) {
            Text(
                text = step.title,
                style = PiPlannerTypography.body,
                fontWeight = FontWeight.SemiBold,
                color = PiPlannerColors.NavyDeep,
            )
            Text(
                text = step.detail,
                style = PiPlannerTypography.caption,
                color = PiPlannerColors.OnSurface.copy(alpha = 0.72f),
            )
        }
    }
}

@Preview(showBackground = true, name = "Welcome · first launch")
@Composable
private fun WelcomeFirstLaunchPreview() {
    PiPlannerTheme {
        WelcomeContent(
            uiState = WelcomeUiState(steps = WelcomeViewModel.DEFAULT_STEPS),
            onSetUpSavings = {},
        )
    }
}

@Preview(showBackground = true, name = "Welcome · after Reset demo")
@Composable
private fun WelcomeAfterResetPreview() {
    // Same visual state as first-run: empty demo → Welcome (1).
    PiPlannerTheme {
        WelcomeContent(
            uiState = WelcomeUiState(steps = WelcomeViewModel.DEFAULT_STEPS),
            onSetUpSavings = {},
        )
    }
}
