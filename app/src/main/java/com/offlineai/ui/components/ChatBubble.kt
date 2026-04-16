package com.offlineai.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.offset
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offlineai.data.model.Message
import com.offlineai.ui.theme.BubbleUser
import java.text.SimpleDateFormat
import java.util.*

/**
 * Renders a single chat message as a styled bubble.
 * User messages: right-aligned, primary green background.
 * AI messages  : left-aligned, surfaceVariant background.
 */
@Composable
fun ChatBubble(message: Message) {
    val isUser = message.isUser
    val bubbleColor = if (isUser) BubbleUser else MaterialTheme.colorScheme.surfaceVariant
    val textColor   = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface

    val bubbleShape = RoundedCornerShape(
        topStart    = if (isUser) 18.dp else 4.dp,
        topEnd      = if (isUser) 4.dp  else 18.dp,
        bottomStart = 18.dp,
        bottomEnd   = 18.dp
    )

    val timeStr = remember(message.timestamp) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
    }

    Row(
        modifier              = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) { AiAvatar(); Spacer(Modifier.width(8.dp)) }

        Column(horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
            Surface(
                color          = bubbleColor,
                shape          = bubbleShape,
                tonalElevation = if (isUser) 0.dp else 2.dp,
                modifier       = Modifier.widthIn(max = 300.dp)
            ) {
                Text(
                    text     = message.content,
                    color    = textColor,
                    style    = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text     = timeStr,
                style    = MaterialTheme.typography.labelSmall,
                color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                fontSize = 10.sp
            )
        }

        if (!isUser) Spacer(Modifier.width(40.dp))
    }
}

@Composable
private fun AiAvatar() {
    Surface(modifier = Modifier.size(32.dp), shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primary) {
        Box(contentAlignment = Alignment.Center) {
            Text("AI", color = Color.White, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), textAlign = TextAlign.Center)
        }
    }
}

/** Animated three-dot typing indicator shown while AI generates a reply. */
@Composable
fun TypingIndicator() {
    Row(
        modifier              = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        AiAvatar()
        Spacer(Modifier.width(8.dp))
        Surface(
            color          = MaterialTheme.colorScheme.surfaceVariant,
            shape          = RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
            tonalElevation = 2.dp
        ) {
            Row(
                modifier              = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                repeat(3) { i -> BouncingDot(delayMillis = i * 160) }
            }
        }
    }
}

@Composable
private fun BouncingDot(delayMillis: Int) {
    val transition = rememberInfiniteTransition(label = "dot_$delayMillis")
    val offsetY by transition.animateFloat(
        initialValue  = 0f, targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = 400, delayMillis = delayMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce_$delayMillis"
    )
    Surface(modifier = Modifier.size(8.dp).offset(y = offsetY.dp), shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) {}
}
