package dev.idock.app.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable

/** Maps a destination key from the shell to its feature screen. */
@Composable
fun FeatureScreen(key: String, contentPadding: PaddingValues, onOpenTool: (String) -> Unit) {
    when (key) {
        "tab:Photos" -> PhotosScreen(contentPadding = contentPadding)
        "tab:Files" -> FilesScreen(contentPadding = contentPadding)
        "tab:Apps" -> AppsScreen(contentPadding = contentPadding)
        "tab:Tools" -> ToolboxScreen(contentPadding = contentPadding, onOpenTool = onOpenTool)
        "backup" -> BackupScreen(contentPadding = contentPadding)
        "ringtones" -> RingtonesScreen(contentPadding = contentPadding)
        "screenshot" -> ScreenshotScreen(contentPadding = contentPadding)
        "logs" -> LogsScreen(contentPadding = contentPadding)
        "verify" -> VerifyScreen(contentPadding = contentPadding)
    }
}
