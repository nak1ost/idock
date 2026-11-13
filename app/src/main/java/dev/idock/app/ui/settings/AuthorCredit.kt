package dev.idock.app.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.idock.app.ui.kit.DockSheet
import dev.idock.app.ui.kit.GradientButton
import dev.idock.app.ui.kit.TonalButton
import dev.idock.app.ui.kit.accentBrush

private const val AUTHOR = "nak1ost"
private const val AUTHOR_URL = "https://github.com/nak1ost"

/**
 * The author's GitHub name. Shown exactly once, right after the welcome guide on the first
 * launch; the caller records that it has been shown before making it visible.
 */
@Composable
fun BoxScope.AuthorCredit(visible: Boolean, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val brush = accentBrush()
    val underline by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(900, delayMillis = if (visible) 380 else 0, easing = FastOutSlowInEasing),
        label = "authorUnderline",
    )
    DockSheet(visible = visible, onDismiss = onDismiss) {
        Text(
            "Автор приложения",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(AUTHOR, style = MaterialTheme.typography.displayMedium)
        Canvas(
            Modifier
                .width(150.dp)
                .height(8.dp),
        ) {
            val y = size.height / 2
            drawLine(
                brush = brush,
                start = Offset(2.dp.toPx(), y),
                end = Offset(2.dp.toPx() + (size.width - 4.dp.toPx()) * underline, y),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.Code,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text("github.com/$AUTHOR", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Это окно появляется один раз, при первом запуске.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth()) {
            TonalButton(
                text = "Открыть GitHub",
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(AUTHOR_URL)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }
                },
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(10.dp))
            GradientButton(text = "Продолжить", onClick = onDismiss, modifier = Modifier.weight(1f))
        }
    }
}
