package com.pattubook.app.ui.more

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pattubook.app.ui.components.DeleteAllDataDialog
import com.pattubook.app.ui.components.NavDestination
import com.pattubook.app.ui.components.PattubookBottomBar
import com.pattubook.app.ui.theme.AccentGreen
import com.pattubook.app.ui.theme.AccentGreenContainer
import com.pattubook.app.ui.theme.AccentRed
import com.pattubook.app.ui.theme.AccentRedContainer
import com.pattubook.app.ui.theme.BorderSubtle
import com.pattubook.app.ui.theme.DarkBackground
import com.pattubook.app.ui.theme.DarkSurface
import com.pattubook.app.ui.theme.PattubookTheme
import com.pattubook.app.ui.theme.TextPrimary
import com.pattubook.app.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MoreScreen(
    onDestinationSelected: (NavDestination) -> Unit,
    modifier: Modifier = Modifier,
    onExportBackupToUri: (Uri) -> Unit = {},
    onImportBackupFromUri: (Uri) -> Unit = {},
    onDeleteAllDataConfirm: () -> Unit = {},
) {
    val scrollState = rememberScrollState()
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    if (showDeleteAllDialog) {
        DeleteAllDataDialog(
            onDismiss = { showDeleteAllDialog = false },
            onConfirmDelete = {
                showDeleteAllDialog = false
                onDeleteAllDataConfirm()
            }
        )
    }

    val defaultFileName = remember {
        val dateStr = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(Date())
        "Pattubook_Backup_$dateStr.json"
    }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            onExportBackupToUri(uri)
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) {
            onImportBackupFromUri(uri)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        bottomBar = {
            PattubookBottomBar(
                currentDestination = NavDestination.MORE,
                onDestinationSelected = onDestinationSelected
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "More",
                style = MaterialTheme.typography.displayMedium,
                color = TextPrimary
            )
            Text(
                text = "App settings & privacy overview",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Information Cards
            InfoCard(
                icon = Icons.Default.Lock,
                title = "Privacy & Security",
                description = "100% offline personal ledger. Your financial records never leave this device."
            )

            Spacer(modifier = Modifier.height(12.dp))

            InfoCard(
                icon = Icons.Default.Info,
                title = "Local Database",
                description = "Powered by Android Room SQLite engine for instant offline persistence."
            )

            Spacer(modifier = Modifier.height(12.dp))

            InfoCard(
                icon = Icons.Default.Info,
                title = "Pattubook Version",
                description = "Version 1.0.0 • Personal Debt & Lending Ledger"
            )

            Spacer(modifier = Modifier.height(28.dp))

            // DATA Section Header
            Text(
                text = "DATA MANAGEMENT",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                letterSpacing = 1.2.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Data Action Cards
            DataActionCard(
                icon = Icons.Default.Refresh,
                title = "Backup Data",
                description = "Save a copy of your Pattubook data",
                onClick = {
                    backupLauncher.launch(defaultFileName)
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            DataActionCard(
                icon = Icons.Default.Refresh,
                title = "Restore Data",
                description = "Restore from a previous backup",
                onClick = {
                    restoreLauncher.launch("*/*")
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            DataActionCard(
                icon = Icons.Default.Delete,
                title = "Delete All Data",
                description = "Remove all people and transactions",
                onClick = {
                    showDeleteAllDialog = true
                },
                isDestructive = true
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun InfoCard(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    val cardShape = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(DarkSurface)
            .border(1.dp, BorderSubtle, cardShape)
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(AccentGreenContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = AccentGreen,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun DataActionCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false,
) {
    val cardShape = RoundedCornerShape(20.dp)
    val iconColor = if (isDestructive) AccentRed else AccentGreen
    val iconContainer = if (isDestructive) AccentRedContainer else AccentGreenContainer

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(DarkSurface)
            .border(
                width = 1.dp,
                color = if (isDestructive) AccentRed.copy(alpha = 0.3f) else BorderSubtle,
                shape = cardShape
            )
            .clickable { onClick() }
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isDestructive) AccentRed else TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Preview
@Composable
fun MoreScreenPreview() {
    PattubookTheme {
        MoreScreen(onDestinationSelected = {})
    }
}
