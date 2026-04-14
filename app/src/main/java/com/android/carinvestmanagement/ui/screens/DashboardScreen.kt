package com.android.carinvestmanagement.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.android.carinvestmanagement.data.Vehicle
import com.android.carinvestmanagement.ui.components.BottomNavigationBar
import com.android.carinvestmanagement.ui.viewmodels.FleetUiState
import com.android.carinvestmanagement.ui.viewmodels.FleetViewModel
import java.text.SimpleDateFormat
import java.util.*

data class ChartLineData(
    val label: String,
    val color: Color,
    val points: List<Pair<String, Int>>
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(navController: NavController, viewModel: FleetViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val dashboardStats by viewModel.dashboardStats.collectAsState()
    val isDashboardLoading by viewModel.isDashboardLoading.collectAsState()
    val context = LocalContext.current

    var selectedPeriod by remember { mutableStateOf("month") }
    val periods = listOf("day" to "День", "week" to "Неделя", "month" to "Месяц", "year" to "Год")

    var startDate by remember { mutableStateOf<Date?>(null) }
    var endDate by remember { mutableStateOf<Date?>(null) }

    val displaySdf = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }
    val isoSdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    
    // Multiple choice for cars
    var selectedCarIds by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(selectedPeriod, startDate, endDate) {
        val startStr = startDate?.let { isoSdf.format(it) }
        val endStr = endDate?.let { isoSdf.format(it) }
        viewModel.fetchDashboardStats(null, selectedPeriod, startStr, endStr)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Дашборд", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = {}) { Icon(Icons.Default.Notifications, null) }
                }
            )
        },
        bottomBar = { BottomNavigationBar(navController) }
    ) { padding ->
        val carColors = listOf(
            Color(0xFF0052CC), Color(0xFF22C55E), Color(0xFFEF4444),
            Color(0xFFF59E0B), Color(0xFF8B5CF6), Color(0xFFEC4899),
            Color(0xFF06B6D4), Color(0xFF10B981)
        )

        val vehicles = (uiState as? FleetUiState.Success)?.vehicles ?: emptyList()
        val chartLines = mutableListOf<ChartLineData>()

        if (selectedCarIds.isEmpty()) {
            vehicles.forEachIndexed { index, vehicle ->
                val points = dashboardStats.filter { it.carId == vehicle.id }
                    .map { it.date to it.totalProfit }
                    .sortedBy { it.first }
                if (points.isNotEmpty()) {
                    chartLines.add(ChartLineData(vehicle.plateNumber, carColors[index % carColors.size], points))
                }
            }
        } else {
            selectedCarIds.forEachIndexed { index, carId ->
                val vehicle = vehicles.find { it.id == carId }
                val points = dashboardStats.filter { it.carId == carId }
                    .map { it.date to it.totalProfit }
                    .sortedBy { it.first }
                if (points.isNotEmpty()) {
                    chartLines.add(ChartLineData(vehicle?.plateNumber ?: carId, carColors[index % carColors.size], points))
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Period Selector
            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    periods.forEachIndexed { index, (value, label) ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = periods.size),
                            onClick = { selectedPeriod = value },
                            selected = selectedPeriod == value
                        ) {
                            Text(label)
                        }
                    }
                }
            }

            // Date Range Selectors
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Start Date
                    OutlinedCard(
                        onClick = {
                            val cal = Calendar.getInstance().apply { time = startDate ?: Date() }
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
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("От", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.DateRange, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = startDate?.let { displaySdf.format(it) } ?: "Не выбрано",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (startDate == null) Color.Gray else Color.Unspecified
                                    )
                                }
                            }
                            if (startDate != null) {
                                IconButton(
                                    onClick = { startDate = null },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Clear, contentDescription = "Очистить", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    // End Date
                    OutlinedCard(
                        onClick = {
                            val cal = Calendar.getInstance().apply { time = endDate ?: Date() }
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
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("До", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.DateRange, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = endDate?.let { displaySdf.format(it) } ?: "Не выбрано",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (endDate == null) Color.Gray else Color.Unspecified
                                    )
                                }
                            }
                            if (endDate != null) {
                                IconButton(
                                    onClick = { endDate = null },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Clear, contentDescription = "Очистить", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Car Filter
            if (uiState is FleetUiState.Success) {
                item {
                    Text("Фильтр по автомобилям", style = MaterialTheme.typography.titleSmall, color = Color.Gray)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedCarIds.isEmpty(),
                            onClick = { selectedCarIds = emptySet() },
                            label = { Text("Все") }
                        )
                        vehicles.forEach { vehicle ->
                            FilterChip(
                                selected = selectedCarIds.contains(vehicle.id),
                                onClick = {
                                    selectedCarIds = if (selectedCarIds.contains(vehicle.id)) {
                                        selectedCarIds - vehicle.id
                                    } else {
                                        selectedCarIds + vehicle.id
                                    }
                                },
                                label = { Text(vehicle.plateNumber) }
                            )
                        }
                    }
                }
            }

            // Chart Section 1: Profit per period
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().height(450.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Прибыль по периодам", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(16.dp))
                        
                        if (isDashboardLoading) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        } else {
                            if (chartLines.isEmpty()) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("Нет данных за этот период", color = Color.Gray)
                                }
                            } else {
                                Column(Modifier.fillMaxSize()) {
                                    // Legend
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        chartLines.forEach { line ->
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(Modifier.size(8.dp).background(line.color, CircleShape))
                                                Spacer(Modifier.width(4.dp))
                                                Text(line.label, style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                    
                                    Box(Modifier.weight(1f)) {
                                        LineChart(chartLines)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Chart Section 2: Cumulative Profit
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().height(450.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Накопительная прибыль", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(16.dp))
                        
                        if (isDashboardLoading) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        } else {
                            if (chartLines.isEmpty()) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("Нет данных за этот период", color = Color.Gray)
                                }
                            } else {
                                val cumulativeLines = chartLines.map { line ->
                                    var sum = 0
                                    val points = line.points.map { (date, profit) ->
                                        sum += profit
                                        date to sum
                                    }
                                    line.copy(points = points)
                                }

                                Column(Modifier.fillMaxSize()) {
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        cumulativeLines.forEach { line ->
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(Modifier.size(8.dp).background(line.color, CircleShape))
                                                Spacer(Modifier.width(4.dp))
                                                Text(line.label, style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                    
                                    Box(Modifier.weight(1f)) {
                                        LineChart(cumulativeLines)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Data Table Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Статистика по автомобилям", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(16.dp))

                        if (isDashboardLoading) {
                            CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
                        } else {
                            val selectedVehicles = vehicles.filter { selectedCarIds.isEmpty() || selectedCarIds.contains(it.id) }
                            
                            val tableData = selectedVehicles.map { vehicle ->
                                val vehicleStats = dashboardStats.filter { it.carId == vehicle.id }
                                val totalProfit = vehicleStats.sumOf { it.totalProfit }
                                val totalExpenses = vehicleStats.sumOf { it.expenses }
                                val entranceFee = if (startDate == null) {vehicle.entranceFee} else 0
                                
                                val profitability = if (vehicle.purchasePrice > 0) {
                                    ((totalProfit - entranceFee).toDouble() / vehicle.purchasePrice.toDouble()) * 100
                                } else 0.0

                                val expenseRatio = if (totalProfit + totalExpenses != 0) {
                                    (totalExpenses.toDouble() / (totalProfit + totalExpenses).toDouble()) * 100
                                } else 0.0
                                
                                Triple(vehicle.plateNumber, totalProfit, totalExpenses) to (profitability to expenseRatio)
                            }

                            if (tableData.isEmpty()) {
                                Text("Нет данных", color = Color.Gray, modifier = Modifier.align(Alignment.CenterHorizontally))
                            } else {
                                // Calculate Totals
                                val grandTotalProfit = tableData.sumOf { it.first.second }
                                val grandTotalExpenses = tableData.sumOf { it.first.third }
                                val totalEntranceFee = if (startDate == null) {selectedVehicles.sumOf { it.entranceFee }} else 0
                                val totalPurchasePrice = selectedVehicles.sumOf { it.purchasePrice }
                                
                                val grandProfitability = if (totalPurchasePrice > 0) {
                                    ((grandTotalProfit - totalEntranceFee).toDouble() / totalPurchasePrice.toDouble()) * 100
                                } else 0.0

                                val grandExpenseRatio = if (grandTotalProfit + grandTotalExpenses != 0) {
                                    (grandTotalExpenses.toDouble() / (grandTotalProfit + grandTotalExpenses).toDouble()) * 100
                                } else 0.0

                                val scrollState = rememberScrollState()
                                Box(Modifier.horizontalScroll(scrollState)) {
                                    Column {
                                        // Header
                                        Row(
                                            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TableCell("Госномер", 100.dp, isHeader = true)
                                            TableCell("Прибыль", 70.dp, isHeader = true)
                                            TableCell("Расходы", 70.dp, isHeader = true)
                                            TableCell("% Расх", 70.dp, isHeader = true)
                                            TableCell("Доходность", 70.dp, isHeader = true)
                                        }
                                        
                                        // Rows
                                        tableData.forEach { (data, ratios) ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 0.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                TableCell(data.first, 100.dp)
                                                TableCell("${data.second} ₽", 70.dp)
                                                TableCell("${data.third} ₽", 70.dp)
                                                TableCell("%.1f%%".format(ratios.second), 70.dp)
                                                TableCell("%.2f%%".format(ratios.first), 70.dp, color = if (ratios.first >= 0) Color(0xFF22C55E) else Color.Red)
                                            }
                                            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                                        }

                                        // Totals Row
                                        Row(
                                            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)).padding(vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TableCell("ИТОГО", 100.dp, isHeader = true)
                                            TableCell("$grandTotalProfit ₽", 70.dp, isHeader = true)
                                            TableCell("$grandTotalExpenses ₽", 70.dp, isHeader = true)
                                            TableCell("%.1f%%".format(grandExpenseRatio), 70.dp, isHeader = true)
                                            TableCell("%.2f%%".format(grandProfitability), 70.dp, isHeader = true, color = if (grandProfitability >= 0) Color(0xFF22C55E) else Color.Red)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Summary Stats
            item {
                val currentSelectionStats = dashboardStats.filter { selectedCarIds.isEmpty() || selectedCarIds.contains(it.carId) }
                val totalProfit = currentSelectionStats.sumOf { it.totalProfit }
                val totalExp = currentSelectionStats.sumOf { it.expenses }
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DashboardMetricCard("ОБЩАЯ ПРИБЫЛЬ", "${totalProfit} ₽", Modifier.weight(1f), Color(0xFF22C55E))
                    DashboardMetricCard("ОБЩИЕ РАСХОДЫ", "${totalExp} ₽", Modifier.weight(1f), Color.Red)
                }
            }
        }
    }
}

@Composable
fun TableCell(text: String, width: androidx.compose.ui.unit.Dp, isHeader: Boolean = false, color: Color = Color.Unspecified) {
    Text(
        text = text,
        modifier = Modifier.width(width).padding(horizontal = 8.dp),
        style = if (isHeader) MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Start,
        maxLines = 1,
        color = color
    )
}

@Composable
fun LineChart(lines: List<ChartLineData>) {
    if (lines.isEmpty()) return
    
    val allPoints = lines.flatMap { it.points }
    val maxVal = allPoints.maxOf { it.second }.coerceAtLeast(1)
    val minVal = allPoints.minOf { it.second }.coerceAtMost(0)
    val range = (maxVal - minVal).toFloat()

    val allDates = allPoints.map { it.first }.distinct().sorted()
    if (allDates.isEmpty()) return

    Canvas(modifier = Modifier.fillMaxSize().padding(bottom = 30.dp, start = 40.dp, end = 10.dp)) {
        val width = size.width
        val height = size.height
        val spacing = if (allDates.size > 1) width / (allDates.size - 1) else width

        drawLine(Color.LightGray, Offset(0f, 0f), Offset(0f, height), strokeWidth = 1.dp.toPx())
        drawLine(Color.LightGray, Offset(0f, height), Offset(width, height), strokeWidth = 1.dp.toPx())

        lines.forEach { line ->
            val path = Path().apply {
                line.points.forEachIndexed { index, pair ->
                    val dateIndex = allDates.indexOf(pair.first)
                    if (dateIndex != -1) {
                        val x = dateIndex * spacing
                        val normalizedY = (pair.second - minVal) / range
                        val y = height - (normalizedY * height)
                        if (index == 0) moveTo(x, y) else lineTo(x, y)
                    }
                }
            }
            drawPath(path, color = line.color, style = Stroke(width = 2.dp.toPx()))
            
            line.points.forEach { pair ->
                val dateIndex = allDates.indexOf(pair.first)
                if (dateIndex != -1) {
                    val x = dateIndex * spacing
                    val normalizedY = (pair.second - minVal) / range
                    val y = height - (normalizedY * height)
                    drawCircle(line.color, radius = 3.dp.toPx(), center = Offset(x, y))
                }
            }
        }

        val sdfIn = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sdfOut = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())

        allDates.forEachIndexed { index, dateStr ->
            if (allDates.size < 8 || index % (allDates.size / 4).coerceAtLeast(1) == 0 || index == allDates.size - 1) {
                val x = index * spacing
                val label = try {
                    val date = sdfIn.parse(dateStr)
                    if (date != null) sdfOut.format(date) else dateStr
                } catch (e: Exception) {
                    dateStr
                }
                
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    x,
                    height + 20.dp.toPx(),
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.GRAY
                        textSize = 9.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                )
            }
        }
        
        drawContext.canvas.nativeCanvas.drawText(
            "$maxVal",
            -5.dp.toPx(),
            10.dp.toPx(),
            android.graphics.Paint().apply {
                color = android.graphics.Color.GRAY
                textSize = 9.sp.toPx()
                textAlign = android.graphics.Paint.Align.RIGHT
            }
        )
        drawContext.canvas.nativeCanvas.drawText(
            "$minVal",
            -5.dp.toPx(),
            height,
            android.graphics.Paint().apply {
                color = android.graphics.Color.GRAY
                textSize = 9.sp.toPx()
                textAlign = android.graphics.Paint.Align.RIGHT
            }
        )
    }
}

@Composable
fun DashboardMetricCard(title: String, value: String, modifier: Modifier = Modifier, color: Color) {
    Card(modifier = modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        }
    }
}
