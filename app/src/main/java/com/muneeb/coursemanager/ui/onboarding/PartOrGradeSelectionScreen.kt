package com.muneeb.coursemanager.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.muneeb.coursemanager.navigation.Routes

@Composable
fun PartOrGradeSelectionScreen(
    navController: NavController,
    viewModel: OnboardingViewModel
) {
    BackHandler(enabled = true) { }

    val uiState by viewModel.uiState.collectAsState()
    val level = uiState.selectedEducationLevel

    LaunchedEffect(level) {
        if (level == null) {
            navController.popBackStack()
        }
    }

    if (level == null) return

    val options = when (level) {
        "INTER" -> listOf("Part 1" to "PART_1", "Part 2" to "PART_2")
        "MATRIC" -> listOf("Grade 9" to "GRADE_9", "Grade 10" to "GRADE_10")
        else -> emptyList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(32.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Select your ${if (level == "INTER") "Part" else "Grade"}",
            style = MaterialTheme.typography.headlineSmall,
            color = Color(0xFF222222)
        )

        Spacer(modifier = Modifier.height(32.dp))

        options.forEach { (display, key) ->
            Button(
                onClick = {
                    viewModel.setPartGrade(key)
                    navController.navigate(Routes.GROUP_SELECTION)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE0E0E0),
                    contentColor = Color(0xFF222222)
                ),
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
            ) {
                Text(display, style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}