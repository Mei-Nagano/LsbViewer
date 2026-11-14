package sb.linux.client.ui.patterns.topic

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import sb.linux.client.ui.util.onColorFor

@Composable
internal fun TopicTagCapsule(label: String, color: Color = MaterialTheme.colorScheme.primaryContainer,
    modifier: Modifier = Modifier, fill: Boolean = false) {
    Surface(shape = CircleShape, color = color, modifier = modifier) {
        Box(Modifier.then(if (fill) Modifier.fillMaxSize() else Modifier)
            .padding(horizontal = 6.dp, vertical = 2.dp), contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = onColorFor(color), softWrap = true)
        }
    }
}
