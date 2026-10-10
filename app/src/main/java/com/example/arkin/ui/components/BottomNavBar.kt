package com.example.arkin.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arkin.model.BottomNavTab
import com.example.ui.theme.ArkinPrimary

@Composable
fun ArkinBottomNavBar(
    currentTab: BottomNavTab,
    onTabSelected: (BottomNavTab) -> Unit,
    onFabClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(88.dp)
            .testTag("arkin_bottom_nav_container")
    ) {
        // Docked navigation bar container
        Surface(
            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 14.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .align(Alignment.BottomCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavTabItem(
                    title = "Home",
                    isSelected = currentTab == BottomNavTab.HOME,
                    selectedIcon = Icons.Filled.Home,
                    unselectedIcon = Icons.Outlined.Home,
                    onClick = { onTabSelected(BottomNavTab.HOME) }
                )

                NavTabItem(
                    title = "Notes",
                    isSelected = currentTab == BottomNavTab.NOTES,
                    selectedIcon = Icons.Filled.Description,
                    unselectedIcon = Icons.Outlined.Description,
                    onClick = { onTabSelected(BottomNavTab.NOTES) }
                )

                NavTabItem(
                    title = "Planner",
                    isSelected = currentTab == BottomNavTab.PLANNER,
                    selectedIcon = Icons.Filled.CalendarMonth,
                    unselectedIcon = Icons.Outlined.CalendarMonth,
                    onClick = { onTabSelected(BottomNavTab.PLANNER) }
                )

                NavTabItem(
                    title = "AI Assistant",
                    isSelected = currentTab == BottomNavTab.AI_ASSISTANT,
                    selectedIcon = Icons.Rounded.Psychology,
                    unselectedIcon = Icons.Rounded.Psychology,
                    onClick = { onTabSelected(BottomNavTab.AI_ASSISTANT) }
                )

                NavTabItem(
                    title = "Profile",
                    isSelected = currentTab == BottomNavTab.PROFILE,
                    selectedIcon = Icons.Filled.Person,
                    unselectedIcon = Icons.Outlined.Person,
                    onClick = { onTabSelected(BottomNavTab.PROFILE) }
                )
            }
        }
    }
}

@Composable
private fun NavTabItem(
    title: String,
    isSelected: Boolean,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .testTag("nav_item_${title.lowercase().replace(" ", "_")}")
    ) {
        Icon(
            imageVector = if (isSelected) selectedIcon else unselectedIcon,
            contentDescription = title,
            tint = if (isSelected) ArkinPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = title,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) ArkinPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
