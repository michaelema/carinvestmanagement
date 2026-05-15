package com.android.carinvestmanagement.ui.screens

import android.app.DatePickerDialog
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.android.carinvestmanagement.data.Vehicle
import com.android.carinvestmanagement.ui.components.BottomNavigationBar
import com.android.carinvestmanagement.ui.viewmodels.FleetUiState
import com.android.carinvestmanagement.ui.viewmodels.FleetViewModel
import com.android.carinvestmanagement.utils.VehicleReportData
import com.android.carinvestmanagement.utils.VehicleReportGenerator
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FleetListScreen(
    navController: NavController,
    onVehicleClick: (String) -> Unit,
    viewModel: FleetViewModel = viewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showReportDialog by remember { mutableStateOf(false) }
    var showRefreshDialog by remember { mutableStateOf(false) }
    
    var startDate by remember { mutableStateOf(Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }.time) }
    var endDate by remember { mutableStateOf(Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }.time) }
    
    val displaySdf = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }
    val isoSdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    var isProcessing by remember { mutableStateOf(false) }

    // Unified Dialog for both Report and Refresh
    if (showReportDialog || showRefreshDialog) {
        val isReport = showReportDialog
        AlertDialog(
            onDismissRequest = { if (!isProcessing) { showReportDialog = false; showRefreshDialog = false } },
            title = { Text(if (isReport) "Отчет по всему парку" else "Обновить статистику") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (isProcessing) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CircularProgressIndicator()
                            Text(if (isReport) "Генерация отчета..." else "Обновление данных...", modifier = Modifier.padding(top = 8.dp))
                        }
                    } else {
                        OutlinedCard(
                            onClick = {
                                val cal = Calendar.getInstance().apply { time = startDate }
                                DatePickerDialog(context, { _, year, month, day ->
                                    val newCal = Calendar.getInstance().apply { set(year, month, day) }
                                    startDate = newCal.time
                                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).apply {
                                    show()
                                    window?.decorView?.isHapticFeedbackEnabled = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text("От", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text(displaySdf.format(startDate), style = MaterialTheme.typography.bodyLarge)
                            }
                        }

                        OutlinedCard(
                            onClick = {
                                val cal = Calendar.getInstance().apply { time = endDate }
                                DatePickerDialog(context, { _, year, month, day ->
                                    val newCal = Calendar.getInstance().apply { set(year, month, day) }
                                    endDate = newCal.time
                                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).apply {
                                    show()
                                    window?.decorView?.isHapticFeedbackEnabled = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text("До", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text(displaySdf.format(endDate), style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (!isProcessing) {
                    Button(onClick = {
                        val state = uiState
                        if (state is FleetUiState.Success) {
                            scope.launch {
                                isProcessing = true
                                val startStr = isoSdf.format(startDate)
                                val endStr = isoSdf.format(endDate)
                                
                                if (isReport) {
                                    val reportsData = mutableListOf<VehicleReportData>()
                                    for (vehicle in state.vehicles) {
                                        val expenses = viewModel.repository.getExpensesByCar(vehicle.id, startStr, endStr)
                                        val stats = viewModel.repository.getStats(vehicle.id, startStr, endStr)
                                        val totals = viewModel.repository.getTotalStats(vehicle.id)
                                        val rate = viewModel.repository.getRate(vehicle.id)
                                        reportsData.add(VehicleReportData(vehicle, rate, totals, stats.income, stats.management, expenses))
                                    }

                                    val file = VehicleReportGenerator.generateCommonReport(
                                        context = context,
                                        reportsData = reportsData,
                                        startDate = startDate,
                                        endDate = endDate
                                    )

                                    if (file != null) {
                                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, "application/pdf")
                                            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Открыть отчет"))
                                    }
                                    showReportDialog = false
                                } else {
                                    // Refresh Stats Logic
                                    for (vehicle in state.vehicles) {
                                        viewModel.repository.generateDailyStats(vehicle.id, startStr, endStr)
                                    }
                                    // Refresh the list totals if needed
                                    viewModel.fetchVehicles()
                                    showRefreshDialog = false
                                }
                                isProcessing = false
                            }
                        }
                    }) {
                        Text(if (isReport) "СГЕНЕРИРОВАТЬ" else "ОБНОВИТЬ")
                    }
                }
            },
            dismissButton = {
                if (!isProcessing) {
                    TextButton(onClick = { showReportDialog = false; showRefreshDialog = false }) {
                        Text("ОТМЕНА")
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Мой автопарк", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showReportDialog = true }) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF Отчет по парку")
                    }
                    IconButton(onClick = { showRefreshDialog = true }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Обновить статистику по парку")
                    }
                    IconButton(onClick = { /* Фильтры */ }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Фильтры")
                    }
                }
            )
        },
        bottomBar = { BottomNavigationBar(navController) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Поиск
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                placeholder = { Text("Поиск по госномеру") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                shape = RoundedCornerShape(12.dp)
            )

            when (val state = uiState) {
                is FleetUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is FleetUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = state.message, color = MaterialTheme.colorScheme.error)
                            Button(onClick = { viewModel.fetchVehicles() }) {
                                Text("Retry")
                            }
                        }
                    }
                }
                is FleetUiState.Success -> {
                    val filteredVehicles = state.vehicles.filter {
                        it.name.contains(searchQuery, ignoreCase = true) ||
                                it.plateNumber.contains(searchQuery, ignoreCase = true)
                    }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(filteredVehicles) { vehicle ->
                            VehicleListItem(vehicle, onClick = { onVehicleClick(vehicle.id) })
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleListItem(vehicle: Vehicle, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.LightGray.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = Color.Gray)
            }

            Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
                Text(vehicle.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(vehicle.plateNumber, color = Color.Gray, style = MaterialTheme.typography.bodySmall)

                Spacer(Modifier.height(4.dp))

                StatusBadge(vehicle.status)
            }

            val tariffColor = if (vehicle.rateType == "Рента") Color(0xFF22C55E) else Color(0xFFF59E0B)

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = vehicle.leaserName,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = vehicle.leaserPhone,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = vehicle.rateType,
                    color = tariffColor,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${vehicle.rentPrice} / ${vehicle.serviceFee} ₽",
                    color = tariffColor,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun StatusBadge(isActive: Boolean) {
    val statusLabel = if (isActive) "АКТИВЕН" else "НЕАКТИВЕН"
    val bgColor = if (isActive) Color(0xFFE8F5E9) else Color(0xFFF5F5F5)
    val textColor = if (isActive) Color(0xFF2E7D32) else Color(0xFF757575)

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = statusLabel,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}
