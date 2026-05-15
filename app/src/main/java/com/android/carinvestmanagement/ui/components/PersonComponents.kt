package com.android.carinvestmanagement.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import com.android.carinvestmanagement.data.Person
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPersonDialog(onDismiss: () -> Unit, onConfirm: (Person) -> Unit) {
    val context = LocalContext.current
    val isoSdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var patronymic by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var documentNumber by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf("") }
    var dateOfIssue by remember { mutableStateOf("") }
    var issuingAuthority by remember { mutableStateOf("") }
    var ownerDescription by remember { mutableStateOf("") }
    
    val messengers = remember { mutableStateListOf<Pair<String, String>>("WhatsApp" to "") }
    val messengerTypes = listOf("WhatsApp", "Telegram", "Signal", "imo", "Viber", "MAX")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый клиент") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = lastName, onValueChange = { lastName = it }, label = { Text("Фамилия*") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = firstName, onValueChange = { firstName = it }, label = { Text("Имя*") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = patronymic, onValueChange = { patronymic = it }, label = { Text("Отчество") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phoneNumber, onValueChange = { phoneNumber = it }, label = { Text("Телефон*") }, modifier = Modifier.fillMaxWidth())
                
                Text("Мессенджеры", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
                messengers.forEachIndexed { index, pair ->
                    var expanded by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = pair.first,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Тип") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                messengerTypes.forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(type) },
                                        onClick = {
                                            messengers[index] = type to pair.second
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                        if (messengers.size > 1) {
                            IconButton(onClick = { messengers.removeAt(index) }) {
                                Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        OutlinedTextField(
                            value = pair.second,
                            onValueChange = { messengers[index] = pair.first to it },
                            label = { Text("Аккаунт") },
                            modifier = Modifier.weight(1.5f)
                        )
                    }
                }
                TextButton(onClick = { messengers.add("" to "") }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Добавить мессенджер")
                }

                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Адрес") }, modifier = Modifier.fillMaxWidth())
                
                // Date of Birth Selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = dateOfBirth,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Дата рождения") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (dateOfBirth.isNotEmpty()) {
                                    IconButton(onClick = { dateOfBirth = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Очистить")
                                    }
                                }
                                Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                            }
                        }
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable {
                                val cal = Calendar.getInstance()
                                if (dateOfBirth.isNotEmpty()) {
                                    try { isoSdf.parse(dateOfBirth)?.let { cal.time = it } } catch (e: Exception) {}
                                }
                                DatePickerDialog(context, { _, y, m, d ->
                                    val newCal = Calendar.getInstance().apply { set(y, m, d) }
                                    dateOfBirth = isoSdf.format(newCal.time)
                                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).apply {
                                    show()
                                    window?.decorView?.isHapticFeedbackEnabled = false
                                }
                            }
                    )
                }

                OutlinedTextField(value = documentNumber, onValueChange = { documentNumber = it }, label = { Text("Номер документа") }, modifier = Modifier.fillMaxWidth())
                
                // Date of Issue Selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = dateOfIssue,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Дата выдачи") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (dateOfIssue.isNotEmpty()) {
                                    IconButton(onClick = { dateOfIssue = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Очистить")
                                    }
                                }
                                Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                            }
                        }
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable {
                                val cal = Calendar.getInstance()
                                if (dateOfIssue.isNotEmpty()) {
                                    try { isoSdf.parse(dateOfIssue)?.let { cal.time = it } } catch (e: Exception) {}
                                }
                                DatePickerDialog(context, { _, y, m, d ->
                                    val newCal = Calendar.getInstance().apply { set(y, m, d) }
                                    dateOfIssue = isoSdf.format(newCal.time)
                                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).apply {
                                    show()
                                    window?.decorView?.isHapticFeedbackEnabled = false
                                }
                            }
                    )
                }

                OutlinedTextField(value = issuingAuthority, onValueChange = { issuingAuthority = it }, label = { Text("Кем выдан") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = ownerDescription, 
                    onValueChange = { ownerDescription = it }, 
                    label = { Text("Описание") }, 
                    modifier = Modifier.fillMaxWidth().height(100.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val messengerMap = messengers
                        .filter { it.first.isNotBlank() && it.second.isNotBlank() }
                        .associate { it.first to it.second }
                    
                    onConfirm(
                        Person(
                            firstName = firstName,
                            lastName = lastName,
                            patronymic = patronymic,
                            phoneNumber = phoneNumber,
                            address = address,
                            documentNumber = documentNumber,
                            dateOfBirth = dateOfBirth.ifEmpty { null },
                            dateOfIssue = dateOfIssue,
                            issuingAuthority = issuingAuthority,
                            ownerDescription = ownerDescription,
                            messenger = messengerMap
                        )
                    )
                },
                enabled = firstName.isNotBlank() && lastName.isNotBlank() && phoneNumber.isNotBlank()
            ) {
                Text("Добавить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
