package com.android.carinvestmanagement.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.android.carinvestmanagement.ui.viewmodels.FleetViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDetailsScreen(
    vehicleId: String,
    expenseId: String,
    onBack: () -> Unit,
    viewModel: FleetViewModel = viewModel()
) {
    val expenses by viewModel.expenses.collectAsState()
    val expense = expenses.find { it.id == expenseId }
    val selectedVehicle by viewModel.selectedVehicle.collectAsState()

    LaunchedEffect(vehicleId) {
        viewModel.getVehicleById(vehicleId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Детали расхода", fontWeight = FontWeight.Bold)
                        selectedVehicle?.let {
                            Text(it.plateNumber, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        if (expense == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Расход не найден")
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
                // Основная информация
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        val dotSdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                        val isoSdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val formattedRateDate = try {
                            val date = isoSdf.parse(expense.date)
                            if (date != null) dotSdf.format(date) else expense.date
                        } catch (e: Exception) {
                            expense.date
                        }
                        Text(expense.category.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text(expense.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("${expense.amount} RUB", style = MaterialTheme.typography.headlineMedium, color = Color.Red, fontWeight = FontWeight.Bold)
                        Text(formattedRateDate, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    }
                }

                // Документ/Изображение
                Text("ДОКУМЕНТ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                
                if (expense.document.isNullOrEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.LightGray.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                            Text("Нет прикрепленного изображения", color = Color.Gray)
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth().height(400.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        AsyncImage(
                            model = expense.document,
                            contentDescription = "Document Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }
    }
}
