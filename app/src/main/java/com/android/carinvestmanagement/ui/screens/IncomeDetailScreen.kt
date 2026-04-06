package com.android.carinvestmanagement.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.carinvestmanagement.ui.viewmodels.FleetViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeDetailScreen(
    vehicleId: String,
    rentRecordId: String,
    rateReductionId: String,
    date: String,
    onBack: () -> Unit,
    viewModel: FleetViewModel = viewModel()
) {
    val rate by viewModel.incomeDetailRate.collectAsState()
    val reduction by viewModel.incomeDetailReduction.collectAsState()
    val isLoading by viewModel.isIncomeDetailLoading.collectAsState()
    val isActionLoading by viewModel.isActionLoading.collectAsState()
    val selectedVehicle by viewModel.selectedVehicle.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var rateRedInput by remember { mutableStateOf("") }
    var serviceFeeRedInput by remember { mutableStateOf("") }
    var descInput by remember { mutableStateOf("") }

    val dotSdf = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }
    val isoSdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") } }

    val weekDays = mapOf(
        "1" to "Пн",
        "2" to "Вт",
        "3" to "Ср",
        "4" to "Чт",
        "5" to "Пт",
        "6" to "Сб",
        "7" to "Вс"
    )

    LaunchedEffect(vehicleId) {
        viewModel.getVehicleById(vehicleId)
    }

    LaunchedEffect(rentRecordId, rateReductionId) {
        viewModel.fetchIncomeDetails(rentRecordId, rateReductionId)
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Новая компенсация") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = rateRedInput,
                        onValueChange = { rateRedInput = it },
                        label = { Text("Скидка аренды") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = serviceFeeRedInput,
                        onValueChange = { serviceFeeRedInput = it },
                        label = { Text("Скидка сервиса") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = descInput,
                        onValueChange = { descInput = it },
                        label = { Text("Описание") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val rr = rateRedInput.toIntOrNull() ?: 0
                        val sfr = serviceFeeRedInput.toIntOrNull() ?: 0
                        viewModel.createReductionAndRefresh(vehicleId, date, rr, sfr, descInput) {
                            showAddDialog = false
                            onBack()
                        }
                    },
                    enabled = !isActionLoading && (rateRedInput.isNotEmpty() || serviceFeeRedInput.isNotEmpty())
                ) {
                    Text("СОХРАНИТЬ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("ОТМЕНА")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Детали дохода", fontWeight = FontWeight.Bold)
                        selectedVehicle?.let {
                            Text(it.plateNumber, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !isActionLoading) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Информация о тарифе
                rate?.let {
                    val formattedRateDate = try {
                        val dateParsed = isoSdf.parse(it.startDate)
                        if (dateParsed != null) dotSdf.format(dateParsed) else it.startDate
                    } catch (e: Exception) {
                        it.startDate
                    }
                    val formattedFreeDay = weekDays[it.freeDay] ?: it.freeDay

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Payments, null, tint = Color(0xFF0052CC))
                                Spacer(Modifier.width(8.dp))
                                Text("ТАРИФ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(it.rateDescription, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text("С даты: $formattedRateDate", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Text("Тип: ${it.rateType}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            
                            Spacer(Modifier.height(12.dp))
                            HorizontalDivider()
                            Spacer(Modifier.height(12.dp))
                            
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Аренда", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    Text("${it.rentPrice} ₽", fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text("Сервисный сбор", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    Text("${it.serviceFee} ₽", fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text("Выходной", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    Text(formattedFreeDay, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }

                // Информация о компенсации (если есть)
                reduction?.let { red ->
                    val formattedReductionDate = try {
                        val dateParsed = isoSdf.parse(red.date)
                        if (dateParsed != null) dotSdf.format(dateParsed) else red.date
                    } catch (e: Exception) {
                        red.date
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.AutoMirrored.Filled.TrendingDown, null, tint = MaterialTheme.colorScheme.error)
                                    Spacer(Modifier.width(8.dp))
                                    Text("КОМПЕНСАЦИЯ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                }
                                
                                if (isActionLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                } else {
                                    IconButton(onClick = {
                                        viewModel.deleteReductionAndRefresh(vehicleId, red) {
                                            onBack()
                                        }
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Удалить компенсацию", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Скидка аренды", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    Text("${red.rateReduction} ₽", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                }
                                Column {
                                    Text("Скидка сервиса", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    Text("${red.serviceFeeReduction} ₽", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                }
                                Column {
                                    Text("Дата", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    Text(formattedReductionDate, fontWeight = FontWeight.Medium)
                                }
                            }
                            
                            if (red.description.isNotEmpty()) {
                                Spacer(Modifier.height(12.dp))
                                Text("Комментарий", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text(red.description, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                if (reduction == null && !isLoading) {
                    Button(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        enabled = !isActionLoading
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("ДОБАВИТЬ КОМПЕНСАЦИЮ")
                    }
                }

                if (rate == null && reduction == null && !isLoading) {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Info, null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                            Text("Детали отсутствуют", modifier = Modifier.padding(top = 8.dp), color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}
