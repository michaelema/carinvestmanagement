package com.android.carinvestmanagement.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Fleet : Screen("fleet_list", "ПАРК", Icons.Default.DirectionsCar)
    object Dashboard : Screen("dashboard", "АНАЛИТИКА", Icons.Default.InsertChart)
    object AddVehicle : Screen("add_vehicle", "ДОБАВИТЬ", Icons.Default.AddCircle)
    object Persons : Screen("person_list", "КЛИЕНТЫ", Icons.Default.People)
    object History : Screen("dashboard"/*"history"*/, "ИСТОРИЯ", Icons.Default.History)
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(
        Screen.Fleet,
        Screen.Dashboard,
//        Screen.AddVehicle,
        Screen.Persons,
//        Screen.History,
    )
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        items.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.label) },
                label = { Text(screen.label) },
                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
