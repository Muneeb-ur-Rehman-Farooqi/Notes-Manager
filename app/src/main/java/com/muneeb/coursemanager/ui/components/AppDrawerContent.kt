package com.muneeb.coursemanager.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.muneeb.coursemanager.data.formatStageLabel
import com.muneeb.coursemanager.navigation.Routes
import com.muneeb.coursemanager.ui.theme.ThemePalette

@Composable
fun AppDrawerContent(
    navController: NavController,
    isDarkMode: Boolean,
    educationLevel: String?,
    partGrade: String?,
    universityMajor: String?,
    currentSemesterId: Long?,
    userName: String?,
    selectedPalette: ThemePalette,
    onPaletteSelected: (ThemePalette) -> Unit,
    onToggleDarkMode: (Boolean) -> Unit,
    onItemClick: () -> Unit,
    onPromoteClick: () -> Unit
) {
    ModalDrawerSheet(
        modifier = Modifier.fillMaxWidth(0.75f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Course Manager",
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(onClick = onItemClick) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close menu"
                    )
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            val semesterLabel = when (educationLevel) {
                "UNIVERSITY" -> "Semesters"
                "MATRIC" -> "Subjects (Matric)"
                "INTER" -> "Subjects (Inter)"
                else -> "Semesters"
            }
            val semesterRoute = when {
                educationLevel != null && educationLevel != "UNIVERSITY" && currentSemesterId != null ->
                    Routes.courseList(currentSemesterId)
                educationLevel != null && educationLevel != "UNIVERSITY" ->
                    Routes.EDUCATION_LEVEL
                else ->
                    Routes.SEMESTER_LIST
            }
            val items = listOf(
                semesterLabel to semesterRoute,
                "Notes" to Routes.NOTES_LIST,
                "Timetable" to Routes.TIMETABLE_LIST,
                "To-Do List" to Routes.STUDY_TASK_LIST
            )
            items.forEach { (label, route) ->
                NavigationDrawerItem(
                    label = { Text(label) },
                    selected = false,
                    onClick = {
                        navController.navigate(route) {
                            popUpTo(Routes.SEMESTER_LIST) { inclusive = false }
                        }
                        onItemClick()
                    },
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Dark Mode",
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = isDarkMode,
                    onCheckedChange = { onToggleDarkMode(it) }
                )
            }

            Text(
                text = "Theme",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { onPaletteSelected(ThemePalette.MONOCHROME) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        "Mono",
                        fontWeight = if (selectedPalette == ThemePalette.MONOCHROME) FontWeight.Bold else FontWeight.Normal
                    )
                }
                TextButton(
                    onClick = { onPaletteSelected(ThemePalette.PINK) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        "Pink",
                        fontWeight = if (selectedPalette == ThemePalette.PINK) FontWeight.Bold else FontWeight.Normal
                    )
                }
                TextButton(
                    onClick = { onPaletteSelected(ThemePalette.SLATE) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        "Slate",
                        fontWeight = if (selectedPalette == ThemePalette.SLATE) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            if (educationLevel != null && educationLevel != "UNIVERSITY") {
                Button(
                    onClick = onPromoteClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text("Promote to Next Level")
                }
            }

            val educationLabel = if (educationLevel == "UNIVERSITY") {
                universityMajor
            } else if (educationLevel != null) {
                formatStageLabel(educationLevel, partGrade)
            } else null

            educationLabel?.let {
                Text(
                    it,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            userName?.let {
                Text(
                    "Hey $it",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}