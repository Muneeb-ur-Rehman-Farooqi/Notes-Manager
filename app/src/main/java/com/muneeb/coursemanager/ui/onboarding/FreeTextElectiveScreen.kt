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
import androidx.compose.material3.OutlinedTextField
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
import com.muneeb.coursemanager.data.SubjectTemplates

@Composable
fun FreeTextElectiveScreen(
    navController: NavController,
    viewModel: OnboardingViewModel
) {
    BackHandler(enabled = true) { }

    val uiState by viewModel.uiState.collectAsState()
    val level = uiState.selectedEducationLevel
    val group = uiState.selectedGroup

    if (level == null || group == null) {
        navController.popBackStack()
        return
    }

    val slotInfo = SubjectTemplates.getFreeTextSlotInfo(level, group)
    if (slotInfo == null) {
        navController.popBackStack()
        return
    }

    val (slotCount, fixedSubjects) = slotInfo
    var userTexts by remember { mutableStateOf(List(slotCount) { "" }) }

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
            text = "Enter elective subjects",
            style = MaterialTheme.typography.headlineSmall,
            color = Color(0xFF222222)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (fixedSubjects.isNotEmpty()) {
            Text(
                text = "Fixed subjects: ${fixedSubjects.joinToString(", ")}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Text(
            text = "Enter $slotCount additional subject name(s)",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(24.dp))

        userTexts.indices.forEach { index ->
            OutlinedTextField(
                value = userTexts[index],
                onValueChange = { newValue ->
                    userTexts = userTexts.toMutableList().apply { set(index, newValue) }
                },
                label = { Text("Subject ${index + 1}") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        val allFilled = userTexts.all { it.isNotBlank() }
        Button(
            onClick = {
                viewModel.setFreeTextElectives(userTexts)
                viewModel.completeOnboarding()
            },
            enabled = allFilled,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFE0E0E0),
                contentColor = Color(0xFF222222),
                disabledContainerColor = Color(0xFFBDBDBD)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue")
        }
    }
}