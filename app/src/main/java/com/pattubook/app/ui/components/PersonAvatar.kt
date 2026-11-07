package com.pattubook.app.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pattubook.app.ui.theme.AccentGreen
import com.pattubook.app.ui.theme.AccentGreenContainer
import com.pattubook.app.ui.util.ProfilePhotoManager

@Composable
fun PersonAvatar(
    personName: String,
    photoPath: String?,
    modifier: Modifier = Modifier.size(48.dp),
    textStyle: TextStyle = MaterialTheme.typography.titleMedium,
    backgroundColor: Color = AccentGreenContainer,
    textColor: Color = AccentGreen,
    borderWidth: Dp = 1.dp,
    borderColor: Color = textColor.copy(alpha = 0.35f),
    contentDescription: String? = null,
) {
    val context = LocalContext.current
    var avatarBitmap by remember(photoPath) { mutableStateOf<Bitmap?>(null) }
    val initialLetter = remember(personName) {
        personName.trim().take(1).uppercase().ifEmpty { "?" }
    }

    LaunchedEffect(photoPath) {
        if (!photoPath.isNullOrBlank()) {
            avatarBitmap = ProfilePhotoManager.loadBitmap(context, photoPath)
        } else {
            avatarBitmap = null
        }
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(backgroundColor)
            .border(borderWidth, borderColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        val currentBitmap = avatarBitmap
        if (currentBitmap != null && !currentBitmap.isRecycled) {
            Image(
                bitmap = currentBitmap.asImageBitmap(),
                contentDescription = contentDescription ?: personName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = initialLetter,
                style = textStyle,
                color = textColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
