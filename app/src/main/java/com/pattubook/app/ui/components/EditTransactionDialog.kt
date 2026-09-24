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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import com.pattubook.app.data.local.entity.LedgerEntry
import com.pattubook.app.data.local.entity.LedgerEntryType
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
import com.pattubook.app.ui.util.DateFormatter
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionDialog(
    entry: LedgerEntry,
    onDismiss: () -> Unit,
    onConfirm: (updatedEntry: LedgerEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isGiven = entry.type == LedgerEntryType.GIVEN
    val primaryAccent = if (isGiven) AccentGreen else AccentBlue

    val initialAmountText = remember(entry) {
        AmountInputUtil.formatPaiseToPlainRupees(entry.amountPaise)
    }

    var amountInput by remember { mutableStateOf(initialAmountText) }
    var noteInput by remember { mutableStateOf(entry.note ?: "") }
    var selectedTimestamp by remember { mutableLongStateOf(entry.timestamp) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    var amountError by remember { mutableStateOf<String?>(null) }

    fun submit() {
        val parsedPaise = AmountInputUtil.parseRupeesToPaise(amountInput)
        if (parsedPaise == null || parsedPaise <= 0) {
            amountError = "Enter a valid positive amount (e.g. 500 or 25.50)"
        } else {
            amountError = null
            val sanitizedNote = noteInput.trim().ifEmpty { null }
            val updatedEntry = entry.copy(
                amountPaise = parsedPaise,
                timestamp = selectedTimestamp,
                note = sanitizedNote
            )
            onConfirm(updatedEntry)
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
                text = "Edit Transaction",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(18.dp))

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
fun EditTransactionDialogPreview() {
    PattubookTheme {
        EditTransactionDialog(
            entry = LedgerEntry(
                id = 1,
                personId = 1,
                amountPaise = 50000L,
                type = LedgerEntryType.GIVEN,
                note = "Lunch"
            ),
            onDismiss = {},
            onConfirm = {}
        )
    }
}
