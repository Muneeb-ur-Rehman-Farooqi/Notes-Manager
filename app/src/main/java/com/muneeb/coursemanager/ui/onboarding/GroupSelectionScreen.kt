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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.muneeb.coursemanager.data.SubjectTemplates
import com.muneeb.coursemanager.navigation.Routes

@Composable
fun GroupSelectionScreen(
    navController: NavController,
    viewModel: OnboardingViewModel
) {
    BackHandler(enabled = true) { }

    val uiState by viewModel.uiState.collectAsState()
    val selectedLevel = uiState.selectedEducationLevel
    val selectedPart = uiState.selectedPartGrade

    if (uiState.isLoading) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
            Text(
                text = "Loading...",
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
            Text(text = "Error: ${uiState.error}", color = Color.Red)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { navController.popBackStack() }) {
                Text("Go Back")
            }
        }
        return
    }

    if (selectedLevel == null || selectedPart == null) {
        navController.popBackStack()
        return
    }

    val groupOptions = SubjectTemplates.getGroupsForLevel(selectedLevel)

    if (groupOptions.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("No groups available for this level.")
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { navController.popBackStack() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE0E0E0),
                    contentColor = Color(0xFF222222)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
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
            text = "Select your group",
            style = MaterialTheme.typography.headlineSmall,
            color = Color(0xFF222222)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "For $selectedLevel - $selectedPart",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(32.dp))

        groupOptions.forEach { groupName ->
            Button(
                onClick = {
                    viewModel.setGroup(groupName)
                    when (groupName) {
                        "PRE_MEDICAL", "PRE_ENGINEERING" -> {
                            viewModel.completeOnboarding()
                        }
                        "ICS" -> {
                            navController.navigate(Routes.ELECTIVE_CHOICE)
                        }
                        "SCIENCE" -> {
                            navController.navigate(Routes.ELECTIVE_CHOICE)
                        }
                        "ARTS" -> {
                            navController.navigate(Routes.FREE_TEXT_ELECTIVE)
                        }
                        else -> {
                            viewModel.completeOnboarding()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE0E0E0),
                    contentColor = Color(0xFF222222)
                ),
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
            ) {
                Text(groupName, style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}