package sb.linux.client.ui.screens

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import sb.linux.client.ui.components.layout.AppMenuGroup
import sb.linux.client.ui.components.layout.AppMenuRow

@Composable
internal fun MenuGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    AppMenuGroup(title, Modifier.padding(horizontal = 16.dp), content)
}

@Composable
internal fun MenuRow(icon: ImageVector, label: String, tint: Color = Color.Unspecified, onClick: () -> Unit) {
    AppMenuRow(label, icon = icon, tint = tint, onClick = onClick)
}
