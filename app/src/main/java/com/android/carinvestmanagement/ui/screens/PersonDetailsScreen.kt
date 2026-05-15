package com.android.carinvestmanagement.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.carinvestmanagement.ui.viewmodels.PersonViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonDetailsScreen(
    personId: String,
    onBack: () -> Unit,
    viewModel: PersonViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val person = (uiState as? com.android.carinvestmanagement.ui.viewmodels.PersonUiState.Success)
        ?.persons?.find { it.id == personId }

    val dotSdf = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }
    val isoSdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Детали клиента") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        if (person == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                if (uiState is com.android.carinvestmanagement.ui.viewmodels.PersonUiState.Loading) {
                    CircularProgressIndicator()
                } else {
                    Text("Клиент не найден")
                }
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
                // Profile Header
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(64.dp),
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "${person.lastName} ${person.firstName}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (person.patronymic.isNotEmpty()) {
                                Text(
                                    text = person.patronymic,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }

                // Personal Information
                InfoSection(title = "ЛИЧНАЯ ИНФОРМАЦИЯ") {
                    val formattedDoB = try {
                        val date = isoSdf.parse(person.dateOfBirth)
                        if (date != null) dotSdf.format(date) else person.dateOfBirth
                    } catch (e: Exception) {
                        person.dateOfBirth
                    }

                    InfoRow(label = "Дата рождения", value = formattedDoB ?: "Не указана")
                    InfoRow(label = "Телефон", value = person.phoneNumber)
                    
                    // Show all available messengers
                    person.messenger.forEach { (messengerName, account) ->
                        InfoRow(label = messengerName, value = account)
                    }

                    InfoRow(label = "Адрес", value = person.address)
                }

                // Document Information
                InfoSection(title = "ДОКУМЕНТЫ") {
                    val docNum = person.documentNumber
                    val formattedDoI = try {
                        val date = isoSdf.parse(person.dateOfIssue)
                        if (date != null) dotSdf.format(date) else person.dateOfIssue
                    } catch (e: Exception) {
                        person.dateOfIssue
                    }

                    InfoRow(label = "Номер документа", value = docNum)
                    InfoRow(label = "Дата выдачи", value = formattedDoI)
                    InfoRow(label = "Кем выдан", value = person.issuingAuthority)
                }

                // Description
                if (person.ownerDescription.isNotEmpty()) {
                    InfoSection(title = "ОПИСАНИЕ") {
                        Text(
                            text = person.ownerDescription,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
                
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun InfoSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    if (value.isNotEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(2f),
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )
        }
    }
}
