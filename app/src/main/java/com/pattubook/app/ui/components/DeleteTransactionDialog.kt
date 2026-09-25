package com.pattubook.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.pattubook.app.data.local.entity.LedgerEntry
import com.pattubook.app.data.local.entity.LedgerEntryType
import com.pattubook.app.ui.theme.AccentRed
import com.pattubook.app.ui.theme.DarkSurface
import com.pattubook.app.ui.theme.PattubookTheme
import com.pattubook.app.ui.theme.TextSecondary
import com.pattubook.app.ui.util.CurrencyFormatter

@Composable
fun DeleteTransactionDialog(
    entry: LedgerEntry,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(DarkSurface)
                .padding(20.dp)
        ) {
            Text(
                text = "Delete transaction?",
                style = MaterialTheme.typography.titleLarge,
                color = AccentRed,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "This transaction (${CurrencyFormatter.formatPaiseToRupees(entry.amountPaise)}) will be moved to the Recycle Bin.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.align(Alignment.End)
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = TextSecondary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onConfirmDelete,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentRed,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Preview
@Composable
fun DeleteTransactionDialogPreview() {
    PattubookTheme {
        DeleteTransactionDialog(
            entry = LedgerEntry(
                id = 1,
                personId = 1,
                amountPaise = 50000L,
                type = LedgerEntryType.GIVEN
            ),
            onDismiss = {},
            onConfirmDelete = {}
        )
    }
}
