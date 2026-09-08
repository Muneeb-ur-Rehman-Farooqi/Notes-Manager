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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.muneeb.coursemanager.navigation.Routes

@Composable
fun EducationLevelScreen(
    navController: NavController,
    viewModel: OnboardingViewModel
) {
    BackHandler(enabled = true) { }

    val uiState by viewModel.uiState.collectAsState()
    var isSubmitting by remember { mutableStateOf(false) }
    val showLoading = uiState.isLoading || isSubmitting

    if (showLoading) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
            Text(
                text = "Setting up your courses...",
                modifier = Modifier.padding(top = 16.dp),
                color = Color.Gray
            )
        }
        return
    }

    if (uiState.error != null) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Error: ${uiState.error}",
                color = Color.Red
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { /* Retry not implemented */ }) {
                Text("Go Back")
            }
        }
        return
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
            text = "What's your education level?",
            style = MaterialTheme.typography.headlineSmall,
            color = Color(0xFF222222)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                isSubmitting = true
                viewModel.setEducationLevel("UNIVERSITY")
                viewModel.completeOnboarding()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFE0E0E0),
                contentColor = Color(0xFF222222)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("University", style = MaterialTheme.typography.bodyLarge)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                viewModel.setEducationLevel("INTER")
                navController.navigate(Routes.PART_GRADE_SELECTION)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFE0E0E0),
                contentColor = Color(0xFF222222)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Inter", style = MaterialTheme.typography.bodyLarge)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                viewModel.setEducationLevel("MATRIC")
                navController.navigate(Routes.PART_GRADE_SELECTION)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFE0E0E0),
                contentColor = Color(0xFF222222)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Matric", style = MaterialTheme.typography.bodyLarge)
        }
    }
}