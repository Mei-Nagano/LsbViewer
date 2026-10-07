package sb.linux.client.ui.export

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.toArgb
/** Adapts the active Compose palette to the platform-neutral color contract used by export rendering. */
internal object TopicExportThemeFactory {
    fun from(colorScheme: ColorScheme): TopicExport.ExportTheme {
        val background = (if (colorScheme.background.alpha < 1f) {
            colorScheme.surface
        } else {
            colorScheme.background
        }).toArgb()
        val primaryContainer = colorScheme.primaryContainer.toArgb()
        return TopicExport.ExportTheme(
            background = background,
            onBackground = colorScheme.onBackground.toArgb(),
            mainCard = TopicExport.ExportTheme.blendOver(primaryContainer, 0.13f, background),
            replyCard = colorScheme.surfaceContainer.toArgb(),
            onSurface = colorScheme.onSurface.toArgb(),
            onSurfaceVariant = colorScheme.onSurfaceVariant.toArgb(),
            primaryContainer = primaryContainer,
            onPrimaryContainer = colorScheme.onPrimaryContainer.toArgb(),
            secondaryContainer = colorScheme.secondaryContainer.toArgb(),
            onSecondaryContainer = colorScheme.onSecondaryContainer.toArgb(),
            outlineVariant = colorScheme.outlineVariant.toArgb(),
        )
    }
}
