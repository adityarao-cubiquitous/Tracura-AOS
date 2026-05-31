package com.cubiquitous.tracura.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cubiquitous.tracura.model.NotificationBadge

@Composable
fun ProminentNotificationBadge(
    badge: NotificationBadge,
    modifier: Modifier = Modifier
) {
    if (badge.hasUnread && badge.count > 0) {
        Box(
            modifier = modifier
                .size(20.dp)
                .background(
                    color = Color.Red,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (badge.count > 99) "99+" else badge.count.toString(),
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(2.dp)
            )
        }
    }
} 