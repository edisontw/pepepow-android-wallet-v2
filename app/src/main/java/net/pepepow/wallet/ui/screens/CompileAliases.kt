package net.pepepow.wallet.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Compatibility shim for the generated Phase 1 UI source. */
val Alignment.Companion.CenterHorizer: Alignment.Horizontal
    get() = Alignment.CenterHorizontally

/** Compatibility overload for generated named argument usage: ActionButton(icon = ...). */
@Composable
fun ActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    phase1Compatibility: Boolean = true
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Surface(
            color = PepepowSurface,
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 2.dp,
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = PepepowPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color.DarkGray
        )
    }
}
