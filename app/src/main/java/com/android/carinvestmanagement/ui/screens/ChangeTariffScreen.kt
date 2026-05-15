package com.android.carinvestmanagement.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.carinvestmanagement.ui.components.AddPersonDialog
import com.android.carinvestmanagement.ui.viewmodels.FleetViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeTariffScreen(
    vehicleId: String,
    onBack: () -> Unit,
    viewModel: FleetViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val isActionLoading by viewModel.isActionLoading.collectAsState()
    val rate by viewModel.selectedVehicleRate.collectAsState()
    val selectedVehicle by viewModel.selectedVehicle.collectAsState()
    val allPersons by viewModel.allPersons.collectAsState()

    val calendar = remember { Calendar.getInstance() }
    var selectedDate by remember { mutableStateOf(calendar.time) }
    val displaySdf = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }
    val isoSdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    var freeDay by remember { mutableStateOf(rate?.freeDay ?: "") }
    var rateDescription by remember { mutableStateOf(rate?.rateDescription ?: "") }
    var rateType by remember { mutableStateOf(rate?.rateType ?: "Рента") }
    var rentPrice by remember { mutableStateOf(rate?.rentPrice?.toString() ?: "") }
    var serviceFee by remember { mutableStateOf(rate?.serviceFee?.toString() ?: "400") }
    var selectedLeaserId by remember { mutableStateOf(rate?.leaser ?: "") }

    val weekDays = listOf(
        "0" to "Без выходных",
        "1" to "Понедельник",
        "2" to "Вторник",
        "3" to "Среда",
        "4" to "Четверг",
        "5" to "Пятница",
        "6" to "Суббота",
        "7" to "Воскресенье"
    )
    val rentTypes = listOf("Рента", "Пересдача", "Ремонт")
    var expandedDay by remember { mutableStateOf(false) }
    var expandedType by remember { mutableStateOf(false) }
    var expandedLeaser by remember { mutableStateOf(false) }
    var showAddPersonDialog by remember { mutableStateOf(false) }

    LaunchedEffect(vehicleId) {
        viewModel.getVehicleById(vehicleId)
        viewModel.fetchRate(vehicleId)
        viewModel.fetchPersons()
    }

    LaunchedEffect(rate) {
        rate?.let {
            freeDay = it.freeDay
            rateDescription = it.rateDescription
            rateType = it.rateType
            rentPrice = it.rentPrice.toString()
            serviceFee = it.serviceFee.toString()
            selectedLeaserId = it.leaser
        }
    }

    LaunchedEffect(rateType) {
        if (rateType == "Пересдача") {
            freeDay = "0"
            rateDescription = "Пересдача"
            rentPrice = "0"
            serviceFee = "0"
        } else if (rateType == "Ремонт") {
            freeDay = "0"
            rateDescription = "Сложный ремонт"
            rentPrice = "0"
            serviceFee = "0"
        }
    }

    val isEditable = !isActionLoading && rateType != "Пересдача" && rateType != "Ремонт"

    if (showAddPersonDialog) {
        AddPersonDialog(
            onDismiss = { showAddPersonDialog = false },
            onConfirm = { person ->
                viewModel.createPersonAndGetId(person) { id ->
                    if (id != null) {
                        selectedLeaserId = id
                    }
                    showAddPersonDialog = false
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Изменить тариф", fontWeight = FontWeight.Bold)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Дата начала
            OutlinedTextField(
                value = displaySdf.format(selectedDate),
                onValueChange = {},
                readOnly = true,
                label = { Text("ДАТА НАЧАЛА") },
                trailingIcon = {
                    IconButton(onClick = {
                        val cal = Calendar.getInstance().apply { time = selectedDate }
                        DatePickerDialog(context, { _, year, month, day ->
                            val newCal = Calendar.getInstance().apply { set(year, month, day) }
                            selectedDate = newCal.time
                        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).apply {
                            show()
                            window?.decorView?.isHapticFeedbackEnabled = false
                        }
                    }, enabled = !isActionLoading) {
                        Icon(Icons.Default.CalendarToday, null)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isActionLoading
            )

            // Арендатор (Leaser)
            Row(verticalAlignment = Alignment.CenterVertically) {
                ExposedDropdownMenuBox(
                    expanded = expandedLeaser,
                    onExpandedChange = { if (!isActionLoading) expandedLeaser = !expandedLeaser },
                    modifier = Modifier.weight(1f)
                ) {
                    val currentLeaser = allPersons.find { it.id == selectedLeaserId }
                    val leaserLabel = if (currentLeaser != null) {
                        "${currentLeaser.lastName} ${currentLeaser.firstName}"
                    } else {
                        "Выберите арендатора"
                    }

                    OutlinedTextField(
                        value = leaserLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("АРЕНДАТОР") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedLeaser) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true),
                        enabled = !isActionLoading
                    )
                    ExposedDropdownMenu(
                        expanded = expandedLeaser,
                        onDismissRequest = { expandedLeaser = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Без арендатора") },
                            onClick = {
                                selectedLeaserId = ""
                                expandedLeaser = false
                            }
                        )
                        allPersons.forEach { person ->
                            DropdownMenuItem(
                                text = { Text("${person.lastName} ${person.firstName}") },
                                onClick = {
                                    selectedLeaserId = person.id
                                    expandedLeaser = false
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = { showAddPersonDialog = true },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Новый клиент")
                }
            }

            // Тип тарифа
            ExposedDropdownMenuBox(
                expanded = expandedType,
                onExpandedChange = { if (!isActionLoading) expandedType = !expandedType }
            ) {
                OutlinedTextField(
                    value = rateType,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("ТИП ТАРИФА") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedType) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true),
                    enabled = !isActionLoading
                )
                ExposedDropdownMenu(
                    expanded = expandedType,
                    onDismissRequest = { expandedType = false }
                ) {
                    rentTypes.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type) },
                            onClick = {
                                rateType = type
                                expandedType = false
                            }
                        )
                    }
                }
            }

            // Выходной день
            ExposedDropdownMenuBox(
                expanded = expandedDay,
                onExpandedChange = { if (isEditable) expandedDay = !expandedDay }
            ) {
                OutlinedTextField(
                    value = weekDays.find { it.first == freeDay }?.second ?: "Выберите день",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("ВЫХОДНОЙ") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDay) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true),
                    enabled = isEditable
                )
                ExposedDropdownMenu(
                    expanded = expandedDay,
                    onDismissRequest = { expandedDay = false }
                ) {
                    weekDays.forEach { (id, name) ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                freeDay = id
                                expandedDay = false
                            }
                        )
                    }
                }
            }

            // Описание
            OutlinedTextField(
                value = rateDescription,
                onValueChange = { rateDescription = it },
                label = { Text("ОПИСАНИЕ") },
                modifier = Modifier.fillMaxWidth(),
                enabled = true
            )

            // Цена аренды
            OutlinedTextField(
                value = rentPrice,
                onValueChange = { rentPrice = it },
                label = { Text("ЦЕНА АРЕНДЫ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                enabled = isEditable
            )

            // Сервисный сбор
            OutlinedTextField(
                value = serviceFee,
                onValueChange = { serviceFee = it },
                label = { Text("СЕРВИСНЫЙ СБОР") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                enabled = isEditable
            )

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    val priceInt = rentPrice.toIntOrNull() ?: 0
                    val feeInt = serviceFee.toIntOrNull() ?: 0
                    
                    if (rate == null) {
                        scope.launch { snackbarHostState.showSnackbar("Ошибка: Данные о текущем тарифе не загружены") }
                        return@Button
                    }

                    viewModel.changeTariff(
                        rentRecordId = rate!!.rentRecordId,
                        rateId = rate!!.rateId,
                        rateDate = isoSdf.format(selectedDate),
                        freeDay = freeDay,
                        rateDescription = rateDescription,
                        rateType = rateType,
                        rentPrice = priceInt,
                        serviceFee = feeInt,
                        leaserId = if (selectedLeaserId.isNotEmpty()) selectedLeaserId else null,
                        onSuccess = {
                            onBack()
                        },
                        onError = { error ->
                            scope.launch { snackbarHostState.showSnackbar(error) }
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !isActionLoading && rentPrice.isNotEmpty()
            ) {
                if (isActionLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Default.Save, null)
                    Spacer(Modifier.width(8.dp))
                    Text("СОХРАНИТЬ ТАРИФ", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
