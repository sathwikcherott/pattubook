package com.pattubook.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.pattubook.app.data.local.entity.LedgerEntryType
import com.pattubook.app.data.local.entity.Person
import com.pattubook.app.ui.theme.AccentBlue
import com.pattubook.app.ui.theme.AccentGreen
import com.pattubook.app.ui.theme.AccentRed
import com.pattubook.app.ui.theme.BorderSubtle
import com.pattubook.app.ui.theme.DarkSurface
import com.pattubook.app.ui.theme.DarkSurfaceVariant
import com.pattubook.app.ui.theme.PattubookTheme
import com.pattubook.app.ui.theme.TextPrimary
import com.pattubook.app.ui.theme.TextSecondary
import com.pattubook.app.ui.util.AmountInputUtil
import com.pattubook.app.ui.util.CurrencyFormatter
import com.pattubook.app.ui.util.DateFormatter
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    type: LedgerEntryType,
    people: List<Person>,
    personBalancesMap: Map<Long, Long>,
    onDismiss: () -> Unit,
    onConfirm: (personId: Long, amountPaise: Long, note: String?, timestamp: Long) -> Unit,
    modifier: Modifier = Modifier,
    initialPersonId: Long? = null,
    showPersonSelector: Boolean = true,
) {
    var selectedPerson by remember {
        mutableStateOf(
            people.find { it.id == initialPersonId } ?: people.firstOrNull()
        )
    }
    var personDropdownExpanded by remember { mutableStateOf(false) }

    var amountInput by remember { mutableStateOf("") }
    var noteInput by remember { mutableStateOf("") }
    var selectedTimestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    var personError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }

    val primaryAccent = if (type == LedgerEntryType.GIVEN) AccentGreen else AccentBlue
    val titleText = if (type == LedgerEntryType.GIVEN) "Give Money" else "Record Return"

    fun submit() {
        var isValid = true

        val person = selectedPerson
        if (person == null) {
            personError = "Please select a person"
            isValid = false
        } else {
            personError = null
        }

        val parsedPaise = AmountInputUtil.parseRupeesToPaise(amountInput)
        if (parsedPaise == null || parsedPaise <= 0) {
            amountError = "Enter a valid positive amount (e.g. 500 or 25.50)"
            isValid = false
        } else {
            amountError = null
        }

        if (isValid && person != null && parsedPaise != null) {
            val sanitizedNote = noteInput.trim().ifEmpty { null }
            onConfirm(person.id, parsedPaise, sanitizedNote, selectedTimestamp)
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedTimestamp
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false
                        val dateMillisUtc = datePickerState.selectedDateMillis
                        if (dateMillisUtc != null) {
                            val calUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = dateMillisUtc
                            }
                            val calLocal = Calendar.getInstance().apply {
                                timeInMillis = selectedTimestamp
                                set(Calendar.YEAR, calUtc.get(Calendar.YEAR))
                                set(Calendar.MONTH, calUtc.get(Calendar.MONTH))
                                set(Calendar.DAY_OF_MONTH, calUtc.get(Calendar.DAY_OF_MONTH))
                            }
                            selectedTimestamp = calLocal.timeInMillis
                        }
                        showTimePicker = true
                    }
                ) {
                    Text("Next", color = primaryAccent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val cal = remember(selectedTimestamp) {
            Calendar.getInstance().apply { timeInMillis = selectedTimestamp }
        }
        val timePickerState = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            is24Hour = false
        )

        Dialog(onDismissRequest = { showTimePicker = false }) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(DarkSurface)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Select Time",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(16.dp))

                TimePicker(state = timePickerState)

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showTimePicker = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            showTimePicker = false
                            val calLocal = Calendar.getInstance().apply {
                                timeInMillis = selectedTimestamp
                                set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                                set(Calendar.MINUTE, timePickerState.minute)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            selectedTimestamp = calLocal.timeInMillis
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryAccent,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("OK", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(DarkSurface)
                .padding(20.dp)
        ) {
            Text(
                text = titleText,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Person Selector (Only shown if showPersonSelector == true)
            if (showPersonSelector) {
                Text(
                    text = "Person",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))

                val selectorShape = RoundedCornerShape(12.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(selectorShape)
                        .background(DarkSurfaceVariant)
                        .border(1.dp, BorderSubtle, selectorShape)
                        .clickable { personDropdownExpanded = true }
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val personName = selectedPerson?.name ?: "Select Person"
                        Text(
                            text = personName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (selectedPerson != null) TextPrimary else TextSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = TextSecondary
                        )
                    }

                    DropdownMenu(
                        expanded = personDropdownExpanded,
                        onDismissRequest = { personDropdownExpanded = false },
                        modifier = Modifier.background(DarkSurfaceVariant)
                    ) {
                        people.forEach { p ->
                            val balance = personBalancesMap[p.id] ?: 0L
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = p.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = TextPrimary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = CurrencyFormatter.formatPaiseToRupees(balance),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                },
                                onClick = {
                                    selectedPerson = p
                                    personDropdownExpanded = false
                                    if (personError != null) personError = null
                                }
                            )
                        }
                    }
                }

                if (personError != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = personError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = AccentRed
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // Amount Field
            OutlinedTextField(
                value = amountInput,
                onValueChange = {
                    amountInput = it
                    if (amountError != null) amountError = null
                },
                label = { Text("Amount (₹)") },
                prefix = { Text("₹ ", color = TextPrimary, fontWeight = FontWeight.Bold) },
                singleLine = true,
                isError = amountError != null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkSurfaceVariant,
                    unfocusedContainerColor = DarkSurfaceVariant,
                    errorContainerColor = DarkSurfaceVariant,
                    focusedBorderColor = primaryAccent,
                    unfocusedBorderColor = BorderSubtle,
                    focusedLabelColor = primaryAccent,
                    unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )

            if (amountError != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = amountError!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = AccentRed
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Date & Time Selector
            Text(
                text = "Date & Time",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))

            val dateTimeShape = RoundedCornerShape(12.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(dateTimeShape)
                    .background(DarkSurfaceVariant)
                    .border(1.dp, BorderSubtle, dateTimeShape)
                    .clickable { showDatePicker = true }
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Select Date & Time",
                        tint = primaryAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = DateFormatter.formatEpochMillis(selectedTimestamp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Note Field
            OutlinedTextField(
                value = noteInput,
                onValueChange = { noteInput = it },
                label = { Text("Note (optional)") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkSurfaceVariant,
                    unfocusedContainerColor = DarkSurfaceVariant,
                    focusedBorderColor = primaryAccent,
                    unfocusedBorderColor = BorderSubtle,
                    focusedLabelColor = primaryAccent,
                    unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Actions
            Row(
                modifier = Modifier.align(Alignment.End)
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = TextSecondary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { submit() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryAccent,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Preview
@Composable
fun AddTransactionDialogPreview() {
    PattubookTheme {
        val samplePeople = listOf(
            Person(id = 1, name = "Jai"),
            Person(id = 2, name = "Ananya"),
        )
        AddTransactionDialog(
            type = LedgerEntryType.GIVEN,
            people = samplePeople,
            personBalancesMap = mapOf(1L to 245000L),
            onDismiss = {},
            onConfirm = { _, _, _, _ -> }
        )
    }
}
