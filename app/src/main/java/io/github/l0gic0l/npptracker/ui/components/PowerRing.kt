package io.github.l0gic0l.npptracker.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.l0gic0l.npptracker.R
import io.github.l0gic0l.npptracker.data.PowerStatus
import io.github.l0gic0l.npptracker.ui.theme.statusColor

/** A ring filled to the unit's power level, with the percentage in the middle. */
@Composable
fun PowerRing(
    power: Int,
    status: PowerStatus,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    strokeWidth: Dp = 4.dp,
    textStyle: TextStyle = MaterialTheme.typography.labelLarge,
) {
    val description = stringResource(R.string.power_percent, power)
    val color = MaterialTheme.colorScheme.statusColor(status)
    Box(
        modifier = modifier
            .size(size)
            .clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            progress = { power.coerceIn(0, 100) / 100f },
            modifier = Modifier.fillMaxSize(),
            color = color,
            // Tinted track so an empty (offline) ring still shows on any background.
            trackColor = color.copy(alpha = 0.24f),
            strokeWidth = strokeWidth,
            gapSize = 0.dp,
        )
        Text(text = power.toString(), style = textStyle, fontWeight = FontWeight.SemiBold)
    }
}

/** A small up/down arrow with the size of the change since the previous report. */
@Composable
fun ChangeIndicator(change: Int, modifier: Modifier = Modifier) {
    if (change == 0) return
    val up = change > 0
    val color = if (up) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val description = stringResource(if (up) R.string.change_up else R.string.change_down, kotlin.math.abs(change))
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (up) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = kotlin.math.abs(change).toString(),
            color = color,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
