package com.lifepad.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.automirrored.outlined.Note
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

data class BottomNavItem(
    val label: String,
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(
        label = "Dashboard",
        route = Screen.Dashboard.route,
        selectedIcon = Icons.Filled.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard
    ),
    BottomNavItem(
        label = "Notes",
        route = Screen.Notepad.route,
        selectedIcon = Icons.AutoMirrored.Filled.Note,
        unselectedIcon = Icons.AutoMirrored.Outlined.Note
    ),
    BottomNavItem(
        label = "Journal",
        route = Screen.Journal.route,
        selectedIcon = Icons.Filled.Book,
        unselectedIcon = Icons.Outlined.Book
    ),
    BottomNavItem(
        label = "Finance",
        route = Screen.Finance.route,
        selectedIcon = Icons.Filled.AccountBalance,
        unselectedIcon = Icons.Outlined.AccountBalance
    ),
    BottomNavItem(
        label = "Search",
        route = Screen.Search.createRoute(),
        selectedIcon = Icons.Filled.Search,
        unselectedIcon = Icons.Outlined.Search
    )
)

@Composable
fun BottomNavBar(navController: NavController) {
    val backStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry.value?.destination?.route

    NavigationBar(
        modifier = Modifier.testTag("bottom_nav")
    ) {
        bottomNavItems.forEach { item ->
            val isSelected = if (item.label == "Search") {
                currentRoute?.startsWith("search") == true
            } else {
                currentRoute == item.route
            }
            val tag = when (item.route) {
                Screen.Dashboard.route -> "nav_dashboard"
                Screen.Notepad.route -> "nav_notes"
                Screen.Journal.route -> "nav_journal"
                Screen.Finance.route -> "nav_finance"
                Screen.Search.createRoute() -> "nav_search"
                else -> "nav_${item.label.lowercase()}"
            }
            NavigationBarItem(
                modifier = Modifier.testTag(tag),
                selected = isSelected,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            // Pop up to the start destination to avoid building up a large stack
                            popUpTo(Screen.Dashboard.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label
                    )
                },
                label = { Text(item.label) }
            )
        }
    }
}
