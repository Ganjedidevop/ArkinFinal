package com.example.arkin.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
            .height(84.dp)
            .testTag("arkin_bottom_nav_container")
    ) {
        // Docked navigation bar container
        Surface(
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 14.dp,
            tonalElevation = 3.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .align(Alignment.BottomCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
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
private fun RowScope.NavTabItem(
    title: String,
    isSelected: Boolean,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.12f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "tab_scale"
    )
    val tintColor by animateColorAsState(
        targetValue = if (isSelected) ArkinPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        label = "tab_color"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 4.dp)
            .testTag("nav_item_${title.lowercase().replace(" ", "_")}")
    ) {
        // Active indicator pill
        AnimatedVisibility(
            visible = isSelected,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Box(
                modifier = Modifier
                    .width(18.dp)
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(ArkinPrimary)
            )
        }
        if (!isSelected) {
            Spacer(modifier = Modifier.height(3.dp))
        }

        Spacer(modifier = Modifier.height(4.dp))

        Icon(
            imageVector = if (isSelected) selectedIcon else unselectedIcon,
            contentDescription = title,
            tint = tintColor,
            modifier = Modifier
                .size(23.dp)
                .scale(scale)
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = title,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = tintColor,
            maxLines = 1
        )
    }
}

