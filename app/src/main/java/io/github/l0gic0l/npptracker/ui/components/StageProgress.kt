package io.github.l0gic0l.npptracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import io.github.l0gic0l.npptracker.R
import io.github.l0gic0l.npptracker.data.RestartStage

/** A segmented bar showing how far a restart has progressed, with the current and next stage. */
@Composable
fun StageProgress(stage: RestartStage, modifier: Modifier = Modifier) {
    val stages = RestartStage.entries
    val colors = MaterialTheme.colorScheme
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clearAndSetSemantics {},
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            for (s in stages) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (s.ordinal <= stage.ordinal) colors.primary else colors.primary.copy(alpha = 0.18f)),
                )
            }
        }
        Text(
            text = stringResource(R.string.stage_of, stage.ordinal + 1, stages.size, stringResource(stage.labelRes())),
            style = MaterialTheme.typography.titleSmall,
        )
        stages.getOrNull(stage.ordinal + 1)?.let { next ->
            Text(
                text = stringResource(R.string.stage_next, stringResource(next.labelRes())),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
    }
}
