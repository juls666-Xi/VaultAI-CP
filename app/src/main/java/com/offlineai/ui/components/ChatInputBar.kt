package com.offlineai.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

/**
 * The bottom input bar containing:
 *  - A multi-line text field for the user's message.
 *  - A microphone button for voice input.
 *  - A send button (disabled when input is blank or AI is loading).
 *
 * @param value          Current text value.
 * @param onValueChange  Called when the user types.
 * @param onSend         Called when the user taps Send or hits the keyboard action.
 * @param onVoiceInput   Called when the user taps the microphone button.
 * @param isLoading      When true, the send button is replaced with a spinner.
 */
@Composable
fun ChatInputBar(
    value         : String,
    onValueChange : (String) -> Unit,
    onSend        : () -> Unit,
    onVoiceInput  : () -> Unit,
    isLoading     : Boolean,
    modifier      : Modifier = Modifier
) {
    Surface(
        modifier      = modifier.fillMaxWidth(),
        color         = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .navigationBarsPadding(),     // keep clear of gesture nav bar
            verticalAlignment   = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── Text field ───────────────────────────────────────────────
            TextField(
                value         = value,
                onValueChange = onValueChange,
                placeholder   = {
                    Text(
                        "Message Offline AI…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    )
                },
                modifier  = Modifier.weight(1f),
                maxLines  = 5,
                shape     = RoundedCornerShape(24.dp),
                colors    = TextFieldDefaults.colors(
                    focusedContainerColor   = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedIndicatorColor   = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor  = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (value.isNotBlank() && !isLoading) onSend()
                })
            )

            // ── Voice button ─────────────────────────────────────────────
            AnimatedVisibility(
                visible = value.isBlank(),
                enter   = fadeIn() + scaleIn(),
                exit    = fadeOut() + scaleOut()
            ) {
                FilledTonalIconButton(
                    onClick   = onVoiceInput,
                    modifier  = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector        = Icons.Default.Mic,
                        contentDescription = "Voice input",
                        tint               = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // ── Send button ──────────────────────────────────────────────
            AnimatedVisibility(
                visible = value.isNotBlank() || isLoading,
                enter   = fadeIn() + scaleIn(),
                exit    = fadeOut() + scaleOut()
            ) {
                FilledIconButton(
                    onClick   = { if (!isLoading) onSend() },
                    enabled   = !isLoading && value.isNotBlank(),
                    modifier  = Modifier.size(48.dp),
                    colors    = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(22.dp),
                            color       = Color.White,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Icon(
                            imageVector        = Icons.Default.Send,
                            contentDescription = "Send message",
                            tint               = Color.White,
                            modifier           = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
