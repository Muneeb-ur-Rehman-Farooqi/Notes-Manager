package com.muneeb.coursemanager.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.muneeb.coursemanager.navigation.Routes

@Composable
fun AppDrawerContent(
    navController: NavController,
    isDarkMode: Boolean,
    educationLevel: String?,
    onToggleDarkMode: (Boolean) -> Unit,
    onItemClick: () -> Unit
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
                    style = androidx.compose.material3.MaterialTheme.typography.titleLarge
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
            val items = listOf(
                semesterLabel to Routes.SEMESTER_LIST,
                "Notes" to Routes.NOTES_LIST,
                "Timetable" to Routes.TIMETABLE_LIST,
                "Study Schedule" to Routes.STUDY_TASK_LIST
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
        }
    }
}