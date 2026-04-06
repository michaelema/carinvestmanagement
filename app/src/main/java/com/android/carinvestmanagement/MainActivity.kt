package com.android.carinvestmanagement

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.android.carinvestmanagement.ui.screens.*
import com.android.carinvestmanagement.ui.theme.VelocityFleetTheme
import com.android.carinvestmanagement.ui.viewmodels.FleetViewModel
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {
    private var sharedImageUri by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        handleIntent(intent)

        setContent {
            VelocityFleetTheme {
                AppNavigation(sharedImageUri) {
                    sharedImageUri = null
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("image/") == true) {
            val uri = intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri
            if (uri != null) {
                try {
                    // Create a local copy of the shared image to avoid permission issues
                    val inputStream = contentResolver.openInputStream(uri)
                    val tempFile = File(cacheDir, "shared_image_${System.currentTimeMillis()}.jpg")
                    val outputStream = FileOutputStream(tempFile)
                    inputStream?.use { input ->
                        outputStream.use { output ->
                            input.copyTo(output)
                        }
                    }
                    // Use FileProvider to avoid file:// URI exposure and potential crashes on Android 13
                    sharedImageUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", tempFile)
                } catch (e: Exception) {
                    sharedImageUri = uri // Fallback to original URI
                }
            }
        }
    }
}

@Composable
fun AppNavigation(sharedUri: Uri?, onConsumed: () -> Unit) {
    val navController = rememberNavController()
    val fleetViewModel: FleetViewModel = viewModel()

    LaunchedEffect(sharedUri) {
        if (sharedUri != null) {
            navController.navigate("select_vehicle_for_expense")
        }
    }

    NavHost(
        navController = navController,
        startDestination = "fleet_list"
    ) {
        composable("login") {
            LoginScreen(onLoginSuccess = {
                navController.navigate("dashboard") {
                    popUpTo("login") { inclusive = true }
                }
            })
        }

        composable("dashboard") {
            DashboardScreen(navController,
                onNavigateToFleet = { navController.navigate("fleet_list") }
            )
        }

        composable("fleet_list") {
            FleetListScreen(navController,
                onVehicleClick = { vehicleId ->
                    navController.navigate("vehicle_details/$vehicleId")
                },
                viewModel = fleetViewModel
            )
        }

        composable("add_vehicle") {
            AddVehicleScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable("vehicle_details/{vehicleId}") { backStackEntry ->
            val vehicleId = backStackEntry.arguments?.getString("vehicleId") ?: ""
            VehicleDetailsScreen(navController,
                vehicleId = vehicleId,
                onBack = { navController.popBackStack() },
                viewModel = fleetViewModel
            )
        }

        composable(
            route = "income_detail/{vehicleId}/{rentRecordId}/{rateReductionId}/{date}",
            arguments = listOf(
                navArgument("vehicleId") { type = NavType.StringType },
                navArgument("rentRecordId") { type = NavType.StringType },
                navArgument("rateReductionId") { type = NavType.StringType },
                navArgument("date") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val vehicleId = backStackEntry.arguments?.getString("vehicleId") ?: ""
            val rentRecordId = backStackEntry.arguments?.getString("rentRecordId") ?: ""
            val rateReductionId = backStackEntry.arguments?.getString("rateReductionId") ?: ""
            val date = backStackEntry.arguments?.getString("date") ?: ""
            IncomeDetailScreen(
                vehicleId = vehicleId,
                rentRecordId = rentRecordId,
                rateReductionId = rateReductionId,
                date = date,
                onBack = { navController.popBackStack() },
                viewModel = fleetViewModel
            )
        }

        composable(
            route = "add_expense/{vehicleId}?imageUri={imageUri}",
            arguments = listOf(
                navArgument("vehicleId") { type = NavType.StringType },
                navArgument("imageUri") { 
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val vehicleId = backStackEntry.arguments?.getString("vehicleId") ?: ""
            val imageUriString = backStackEntry.arguments?.getString("imageUri")
            val imageUri = imageUriString?.let { Uri.parse(Uri.decode(it)) }

            AddExpenseScreen(
                vehicleId = vehicleId,
                initialImageUri = imageUri,
                onBack = { navController.popBackStack() },
                viewModel = fleetViewModel
            )
        }

        composable(
            route = "expense_details/{vehicleId}/{expenseId}",
            arguments = listOf(
                navArgument("vehicleId") { type = NavType.StringType },
                navArgument("expenseId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val vehicleId = backStackEntry.arguments?.getString("vehicleId") ?: ""
            val expenseId = backStackEntry.arguments?.getString("expenseId") ?: ""
            ExpenseDetailsScreen(
                vehicleId = vehicleId,
                expenseId = expenseId,
                onBack = { navController.popBackStack() },
                viewModel = fleetViewModel
            )
        }

        composable("change_tariff/{vehicleId}") { backStackEntry ->
            val vehicleId = backStackEntry.arguments?.getString("vehicleId") ?: ""
            ChangeTariffScreen(
                vehicleId = vehicleId,
                onBack = { navController.popBackStack() },
                viewModel = fleetViewModel
            )
        }

        composable("select_vehicle_for_expense") {
            FleetListScreen(
                navController = navController,
                onVehicleClick = { vehicleId ->
                    if (sharedUri != null) {
                        val encodedUri = Uri.encode(sharedUri.toString())
                        navController.navigate("add_expense/$vehicleId?imageUri=$encodedUri") {
                            popUpTo("select_vehicle_for_expense") { inclusive = true }
                        }
                        onConsumed()
                    }
                },
                viewModel = fleetViewModel
            )
        }
    }
}
