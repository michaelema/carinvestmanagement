package com.android.carinvestmanagement.ui.screens

import android.app.DatePickerDialog
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.carinvestmanagement.ui.viewmodels.FleetViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    vehicleId: String,
    initialImageUri: Uri? = null,
    onBack: () -> Unit,
    viewModel: FleetViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val isActionLoading by viewModel.isActionLoading.collectAsState()
    val selectedVehicle by viewModel.selectedVehicle.collectAsState()

    LaunchedEffect(vehicleId) {
        viewModel.getVehicleById(vehicleId)
    }

    var category by remember { mutableStateOf("Ремонт") }
    var amount by remember { mutableStateOf("") }

    val calendar = remember { Calendar.getInstance() }
    var selectedDate by remember { mutableStateOf(calendar.time) }

    val displaySdf = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }
    val isoSdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    var comment by remember { mutableStateOf("") }
    
    var selectedImageUri by remember { mutableStateOf<Uri?>(initialImageUri) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
        }
    }

    val categories = listOf("Ремонт", "Страховка", "Комплектующие", "Подписка", "Прочее")
    var expanded by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Добавить расход", fontWeight = FontWeight.Bold)
                        selectedVehicle?.let {
                            Text(it.plateNumber, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !isActionLoading) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Категория
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { if (!isActionLoading) expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("КАТЕГОРИЯ") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    enabled = !isActionLoading
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    categories.forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption) },
                            onClick = {
                                category = selectionOption
                                expanded = false
                            }
                        )
                    }
                }
            }

            // Сумма
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("СУММА") },
                suffix = { Text("RUB") },
                leadingIcon = { Icon(Icons.Default.Payments, null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isActionLoading
            )

            // Дата
            OutlinedTextField(
                value = displaySdf.format(selectedDate),
                onValueChange = {},
                readOnly = true,
                label = { Text("ДАТА") },
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

            // Комментарий
            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                label = { Text("КОММЕНТАРИЙ") },
                placeholder = { Text("Детали расхода...") },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                maxLines = 5,
                enabled = !isActionLoading
            )

            // Фотография
            Text("ДОКУМЕНТ", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            OutlinedCard(
                onClick = {
                    photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isActionLoading
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (selectedImageUri == null) Icons.Default.AddPhotoAlternate else Icons.Default.Image,
                        contentDescription = null,
                        tint = if (selectedImageUri == null) Color.Gray else MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = if (selectedImageUri == null) "Прикрепить фото чека" else "Фото выбрано",
                        color = if (selectedImageUri == null) Color.Gray else Color.Unspecified
                    )
                    if (selectedImageUri != null && !isActionLoading) {
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { selectedImageUri = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Удалить")
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    val amountInt = amount.toIntOrNull()
                    if (amountInt == null || amountInt <= 0) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Введите корректную сумму")
                        }
                        return@Button
                    }

                    scope.launch {
                        var fileBase64: String? = null
                        var documentName: String? = null

                        if (selectedImageUri != null) {
                            withContext(Dispatchers.IO) {
                                fileBase64 = getBase64FromUri(context, selectedImageUri!!)
                                documentName = getFileNameFromUri(context, selectedImageUri!!)
                            }
                        }

                        viewModel.addExpense(
                            vehicleId = vehicleId,
                            type = category,
                            amount = amountInt,
                            description = comment,
                            date = isoSdf.format(selectedDate),
                            documentName = documentName,
                            fileBase64 = fileBase64,
                            onSuccess = {
                                onBack()
                            },
                            onError = { error ->
                                scope.launch {
                                    snackbarHostState.showSnackbar(error)
                                }
                            }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.medium,
                enabled = !isActionLoading && amount.isNotEmpty()
            ) {
                if (isActionLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Save, null)
                    Spacer(Modifier.width(8.dp))
                    Text("СОХРАНИТЬ РАСХОД", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun getBase64FromUri(context: android.content.Context, uri: Uri): String? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val bytes = inputStream.readBytes()
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        }
    } catch (e: Exception) {
        null
    }
}

private fun getFileNameFromUri(context: android.content.Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = it.getString(index)
                }
            }
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result ?: "document.jpg"
}
