package com.offlineai.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offlineai.data.model.UsbConnectionState

/**
 * A compact banner displayed at the top of the Chat screen that
 * shows the current USB / AI connection status and a connect/disconnect button.
 *
 * Animates in/out when the state changes.
 */
@Composable
fun UsbStatusBanner(
    state        : UsbConnectionState,
    onConnect    : () -> Unit,
    onDisconnect : () -> Unit,
    modifier     : Modifier = Modifier
) {
    val (bgColor, icon, label, actionLabel) = when (state) {
        is UsbConnectionState.Connected -> Quadruple(
            Color(0xFF166534),          // dark green
            Icons.Default.Usb,
            "Connected — ${state.modelPath.substringAfterLast('/')}",
            "Disconnect"
        )
        is UsbConnectionState.Connecting -> Quadruple(
            Color(0xFF92400E),          // amber
            Icons.Default.HourglassTop,
            "Connecting to USB…",
            null
        )
        is UsbConnectionState.Error -> Quadruple(
            Color(0xFF7F1D1D),          // dark red
            Icons.Default.ErrorOutline,
            state.message,
            "Retry"
        )
        UsbConnectionState.Disconnected -> Quadruple(
            MaterialTheme.colorScheme.surfaceVariant,
            Icons.Default.UsbOff,
            "No AI model connected",
            "Connect"
        )
    }

    AnimatedVisibility(
        visible = true,
        enter   = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit    = slideOutVertically(targetOffsetY  = { -it }) + fadeOut()
    ) {
        Surface(
            modifier      = modifier.fillMaxWidth(),
            color         = bgColor,
            shape         = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
            tonalElevation = 0.dp
        ) {
            Row(
                modifier            = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment   = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Status icon
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint   = Color.White,
                    modifier = Modifier.size(18.dp)
                )

                // Status label — expands to fill remaining space
                Text(
                    text     = label,
                    color    = Color.White,
                    style    = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Spinning indicator while connecting
                if (state is UsbConnectionState.Connecting) {
                    CircularProgressIndicator(
                        modifier  = Modifier.size(16.dp),
                        color     = Color.White,
                        strokeWidth = 2.dp
                    )
                }

                // Action button
                actionLabel?.let { label ->
                    TextButton(
                        onClick      = if (state is UsbConnectionState.Connected) onDisconnect else onConnect,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(label, color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

/** Simple data holder used in the when-expression above. */
private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
