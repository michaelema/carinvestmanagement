package com.android.carinvestmanagement.ui.screens

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.android.carinvestmanagement.ui.viewmodels.FleetUiState
import com.android.carinvestmanagement.ui.viewmodels.FleetViewModel
import com.android.carinvestmanagement.utils.VehicleReportGenerator
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleDetailsScreen(
    navController: NavController,
    vehicleId: String,
    onBack: () -> Unit,
    viewModel: FleetViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedVehicle by viewModel.selectedVehicle.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val isExpensesLoading by viewModel.isExpensesLoading.collectAsState()
    val rate by viewModel.selectedVehicleRate.collectAsState()
    val isRateLoading by viewModel.isRateLoading.collectAsState()
    val incomeStats by viewModel.incomeStats.collectAsState()
    val managementStats by viewModel.managementStats.collectAsState()
    val isStatsLoading by viewModel.isStatsLoading.collectAsState()
    val totalStats by viewModel.totalStats.collectAsState()

    val context = LocalContext.current
    val displaySdf = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) }
    val dotSdf = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }
    val isoSdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    
    val calendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    var endDate by remember { mutableStateOf(calendar.time) }
    
    val startCalendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }
    var startDate by remember { mutableStateOf(startCalendar.time) }

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("РАСХОДЫ", "ДОХОДЫ", "УПРАВЛЕНИЕ")

    LaunchedEffect(uiState, vehicleId) {
        if (uiState is FleetUiState.Success) {
            viewModel.getVehicleById(vehicleId)
        }
    }

    LaunchedEffect(vehicleId, startDate, endDate) {
        viewModel.fetchExpenses(vehicleId, isoSdf.format(startDate), isoSdf.format(endDate))
        viewModel.fetchStats(vehicleId, isoSdf.format(startDate), isoSdf.format(endDate))
    }

    LaunchedEffect(vehicleId) {
        viewModel.fetchRate(vehicleId)
        viewModel.fetchTotalStats(vehicleId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Детали автомобиля") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        selectedVehicle?.let { vehicle ->
                            val file = VehicleReportGenerator.generateReport(
                                context = context,
                                vehicle = vehicle,
                                rate = rate,
                                totalStats = totalStats,
                                incomeStats = incomeStats,
                                managementStats = managementStats,
                                expenses = expenses,
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
                        }
                    }) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF Отчет")
                    }
                    IconButton(onClick = { /* Редактировать */ }) {
                        Icon(Icons.Default.Edit, contentDescription = "Редактировать")
                    }
                }
            )
        }
    ) { padding ->
        val vehicle = selectedVehicle

        if (vehicle == null) {
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(padding), contentAlignment = Alignment.Center) {
                if (uiState is FleetUiState.Loading) {
                    CircularProgressIndicator()
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Автомобиль не найден")
                        Button(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) {
                            Text("Назад к списку")
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                // Шапка с фото и статусом
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Color.LightGray.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.DirectionsCar, modifier = Modifier.size(64.dp), contentDescription = null, tint = Color.Gray)
                    
                    val isActive = vehicle.status
                    val statusLabel = if (isActive) "АКТИВЕН" else "НЕАКТИВЕН"
                    val statusColor = if (isActive) Color(0xFF22C55E) else Color.Gray

                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp),
                        color = statusColor,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(statusLabel, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                Column(modifier = Modifier.padding(16.dp)) {
                    Text(vehicle.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Госномер: ${vehicle.plateNumber}", color = Color.Gray)

                    Spacer(Modifier.height(24.dp))

                    // Финансовые показатели
                    val revenueStr = totalStats?.let { "${it.revenue} ₽" } ?: "..."
                    val expensesStr = totalStats?.let { "${it.expenses} ₽" } ?: "..."
                    val profitStr = totalStats?.let { "${it.profit} ₽" } ?: "..."

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DetailMetricCard("ВЫРУЧКА", revenueStr, Modifier.weight(1f))
                        DetailMetricCard("РАСХОДЫ", expensesStr, Modifier.weight(1f))
                    }

                    Spacer(Modifier.height(12.dp))

                    ProfitCardDetail(profitStr)

                    Spacer(Modifier.height(24.dp))

                    // Тариф
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("ТЕКУЩИЙ ТАРИФ", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        TextButton(onClick = { navController.navigate("change_tariff/$vehicleId") }) {
                            Text("ИЗМЕНИТЬ")
                        }
                    }
                    
                    if (isRateLoading) {
                        CircularProgressIndicator(modifier = Modifier.padding(vertical = 16.dp).align(Alignment.CenterHorizontally))
                    } else if (rate != null) {
                        val formattedRateDate = try {
                            val date = isoSdf.parse(rate!!.startDate)
                            if (date != null) dotSdf.format(date) else rate!!.startDate
                        } catch (e: Exception) {
                            rate!!.startDate
                        }

                        val weekDays = mapOf(
                            "1" to "Пн",
                            "2" to "Вт",
                            "3" to "Ср",
                            "4" to "Чт",
                            "5" to "Пт",
                            "6" to "Сб",
                            "7" to "Вс"
                        )
                        val formattedFreeDay = weekDays[rate!!.freeDay] ?: rate!!.freeDay

                        Card(modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)) {
                            Column(Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Payments, null, tint = Color(0xFF0052CC))
                                    Spacer(Modifier.width(16.dp))
                                    Column {
                                        Text(rate!!.rateDescription, fontWeight = FontWeight.Bold)
                                        Text("С даты: $formattedRateDate", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                                HorizontalDivider()
                                Spacer(Modifier.height(12.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text("Аренда", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                        Text("${rate!!.rentPrice} ₽/день", fontWeight = FontWeight.Medium)
                                    }
                                    Column {
                                        Text("Сервисный сбор", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                        Text("${rate!!.serviceFee} ₽/день", fontWeight = FontWeight.Medium)
                                    }
                                    Column {
                                        Text("Выходной", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                        Text(formattedFreeDay, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    } else {
                        Text("Данные о тарифе отсутствуют", modifier = Modifier.padding(vertical = 8.dp), style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    }

                    Spacer(Modifier.height(24.dp))

                    // Поля выбора даты
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(Modifier.padding(8.dp)) {
                                Text("От", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.DateRange, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                                    Spacer(Modifier.width(4.dp))
                                    Text(displaySdf.format(startDate), style = MaterialTheme.typography.bodyMedium)
                                }
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
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(Modifier.padding(8.dp)) {
                                Text("До", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.DateRange, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                                    Spacer(Modifier.width(4.dp))
                                    Text(displaySdf.format(endDate), style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Заголовок раздела (перемещен под интервалы дат)
                    Text("ЖУРНАЛ ЗАПИСЕЙ", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    
                    // Выбор: РАСХОДЫ / ДОХОДЫ / УПРАВЛЕНИЕ
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TabRow(
                            selectedTabIndex = selectedTabIndex,
                            modifier = Modifier.weight(1f),
                            containerColor = Color.Transparent,
                            divider = {},
                            indicator = { tabPositions ->
                                if (selectedTabIndex < tabPositions.size) {
                                    TabRowDefaults.SecondaryIndicator(
                                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex])
                                    )
                                }
                            }
                        ) {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedTabIndex == index,
                                    onClick = { selectedTabIndex = index },
                                    text = { Text(title, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
                                )
                            }
                        }
                        
                        if (selectedTabIndex == 0) {
                            IconButton(onClick = { navController.navigate("add_expense/$vehicleId") }) {
                                Icon(Icons.Default.Add, contentDescription = "Добавить расход", tint = MaterialTheme.colorScheme.primary)
                            }
                        } else {
                            IconButton(onClick = {
                                viewModel.generateAndRefreshStats(vehicleId, isoSdf.format(startDate), isoSdf.format(endDate))
                            }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Обновить статистику", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    when (selectedTabIndex) {
                        0 -> { // РАСХОДЫ
                            if (isExpensesLoading) {
                                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            } else if (expenses.isEmpty()) {
                                Text("Расходов за этот период нет", modifier = Modifier.padding(vertical = 16.dp), color = Color.Gray)
                            } else {
                                val sortedExpenses = expenses.sortedBy { it.date }
                                val totalExpenses = sortedExpenses.sumOf { it.amount }
                                GenericListItem(title = "ИТОГО", subtitle = "За выбранный период", amount = totalExpenses, color = Color.Red, fontWeight = FontWeight.ExtraBold)
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                
                                sortedExpenses.forEach { expense ->
                                    val formattedExpenseDate = try {
                                        val date = isoSdf.parse(expense.date)
                                        if (date != null) dotSdf.format(date) else expense.date
                                    } catch (e: Exception) {
                                        expense.date
                                    }

                                    ExpenseItem(
                                        title = expense.title,
                                        date = formattedExpenseDate,
                                        amount = expense.amount,
                                        onClick = {
                                            navController.navigate("expense_details/$vehicleId/${expense.id}")
                                        },
                                        onDelete = {
                                            viewModel.deleteExpenseAndRefresh(expense.id, vehicleId, isoSdf.format(startDate), isoSdf.format(endDate))
                                        }
                                    )
                                }
                            }
                        }
                        1 -> { // ДОХОДЫ
                            if (isStatsLoading) {
                                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            } else if (incomeStats.isEmpty()) {
                                Text("Доходов за этот период нет", modifier = Modifier.padding(vertical = 16.dp), color = Color.Gray)
                            } else {
                                val sortedIncome = incomeStats.sortedBy { it.date }
                                val totalIncomes = sortedIncome.sumOf { it.amount - it.reduction }
                                GenericListItem(title = "ИТОГО", subtitle = "За выбранный период", amount = totalIncomes, color = Color(0xFF22C55E), fontWeight = FontWeight.ExtraBold)
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                sortedIncome.forEach { record ->
                                    val formattedDate = try {
                                        val d = isoSdf.parse(record.date)
                                        if (d != null) dotSdf.format(d) else record.date
                                    } catch (e: Exception) {
                                        record.date
                                    }
                                    GenericListItem(
                                        title = record.dayOfWeek,
                                        subtitle = formattedDate,
                                        amount = record.amount - record.reduction,
                                        color = Color(0xFF22C55E),
                                        label = if (record.reduction > 0) "компенсация" else null,
                                        onClick = {
                                            val encodedDate = Uri.encode(record.date)
                                            navController.navigate("income_detail/${vehicleId}/${record.rentRecordId}/${record.rateReductionId}/$encodedDate")
                                        }
                                    )
                                }
                            }
                        }
                        2 -> { // УПРАВЛЕНИЕ
                            if (isStatsLoading) {
                                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            } else if (managementStats.isEmpty()) {
                                Text("Записей управления за этот период нет", modifier = Modifier.padding(vertical = 16.dp), color = Color.Gray)
                            } else {
                                val sortedMgmt = managementStats.sortedBy { it.date }
                                val totalMgmt = sortedMgmt.sumOf { it.amount - it.reduction }
                                GenericListItem(title = "ИТОГО", subtitle = "За выбранный период", amount = totalMgmt, color = Color(0xFF0052CC), fontWeight = FontWeight.ExtraBold)
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                sortedMgmt.forEach { record ->
                                    val formattedDate = try {
                                        val d = isoSdf.parse(record.date)
                                        if (d != null) dotSdf.format(d) else record.date
                                    } catch (e: Exception) {
                                        record.date
                                    }
                                    GenericListItem(
                                        title = record.dayOfWeek,
                                        subtitle = formattedDate,
                                        amount = record.amount - record.reduction,
                                        color = Color(0xFF0052CC),
                                        label = if (record.reduction > 0) "компенсация" else null,
                                        onClick = {
                                            val encodedDate = Uri.encode(record.date)
                                            navController.navigate("income_detail/${vehicleId}/${record.rentRecordId}/${record.rateReductionId}/$encodedDate")
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(32.dp))

                    // Кнопка вывода из эксплуатации
                    OutlinedButton(
                        onClick = { /* Вывести */ },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                        border = BorderStroke(1.dp, Color.Red)
                    ) {
                        Text("ВЫВЕСТИ ИЗ ЭКСПЛУАТАЦИИ")
                    }
                }
            }
        }
    }
}

@Composable
fun DetailMetricCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ProfitCardDetail(value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0052CC))
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("ЧИСТАЯ ПРИБЫЛЬ", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
            Text(value, color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ExpenseItem(title: String, date: String, amount: Int, onClick: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Dot and description
        Box(
            modifier = Modifier
                .padding(start = 4.dp)
                .size(8.dp)
                .background(Color.Red, CircleShape)
        )
        
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(
                text = title, 
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = date, 
                style = MaterialTheme.typography.bodySmall, 
                color = Color.Gray
            )
        }
        
        // Fixed space for amount and delete button to avoid squeezing
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.width(IntrinsicSize.Min)
        ) {
            Text(
                text = "$amount ₽", 
                fontWeight = FontWeight.Bold, 
                color = Color.Red,
                modifier = Modifier.width(80.dp),
                textAlign = TextAlign.End,
                maxLines = 1,
                softWrap = false
            )
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete, 
                    contentDescription = "Удалить расход", 
                    tint = Color.Gray
                )
            }
        }
    }
}

@Composable
fun GenericListItem(
    title: String, 
    subtitle: String, 
    amount: Int, 
    color: Color,
    fontWeight: FontWeight = FontWeight.Medium,
    label: String? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Dot and title
        Box(
            modifier = Modifier
                .padding(start = 4.dp)
                .size(8.dp)
                .background(color, CircleShape)
        )
        
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(
                text = title, 
                fontWeight = fontWeight,
                style = MaterialTheme.typography.bodyLarge
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = subtitle, 
                    style = MaterialTheme.typography.bodySmall, 
                    color = Color.Gray
                )
                if (label != null) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = color
                    )
                }
            }
        }
        
        // Amount aligned with ExpenseItem
        Text(
            text = "$amount ₽", 
            fontWeight = FontWeight.Bold, 
            color = color,
            modifier = Modifier.width(80.dp),
            textAlign = TextAlign.End,
            maxLines = 1,
            softWrap = false
        )
        
        // Spacer to match ExpenseItem's delete button space
        Spacer(Modifier.width(48.dp))
    }
}
