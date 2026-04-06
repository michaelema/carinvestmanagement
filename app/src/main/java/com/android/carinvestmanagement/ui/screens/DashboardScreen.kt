package com.android.carinvestmanagement.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.android.carinvestmanagement.ui.components.BottomNavigationBar
import com.android.carinvestmanagement.ui.components.ChartSection
import com.android.carinvestmanagement.ui.components.ProfitCard
import com.android.carinvestmanagement.ui.components.TopVehicleList
import com.android.carinvestmanagement.ui.theme.VelocityFleetTheme


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavController, onNavigateToFleet: () -> Unit) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Executive Fleet", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = {}) { Icon(Icons.Default.Notifications, null) }
                }
            )
        },
        bottomBar = { BottomNavigationBar(navController) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 16.dp,
                bottom = padding.calculateBottomPadding() + 100.dp, // Отступ для BottomBar
                start = 16.dp,
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                MetricCard("ОБЩАЯ ВЫРУЧКА", "$142.8k", "+12.4% с прошлого месяца")
            }
            item {
                MetricCard("РАСХОДЫ", "$42.3k", "32% от целевой мощности")
            }
            item {
                // ТЕПЕРЬ ОШИБКИ НЕ БУДЕТ
                ProfitCard("$100.5k")
            }
            item {
                ChartSection("Тренды показателей")
            }
            item {
                TopVehicleList()
            }
        }
    }
}

@Composable
fun MetricCard(title: String, value: String, subtitle: String) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.labelMedium)
            Text(text = value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardScreenPreview() {
    VelocityFleetTheme {
        DashboardScreen(navController = rememberNavController(), onNavigateToFleet = {})
    }
}
