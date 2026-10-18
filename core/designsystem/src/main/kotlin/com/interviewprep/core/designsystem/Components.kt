package com.interviewprep.core.designsystem

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.interviewprep.core.model.Difficulty
import com.interviewprep.core.model.QuizOrigin
import com.interviewprep.core.model.ResumeTag
import com.interviewprep.core.model.SourceRef
import kotlin.math.roundToInt

object Handbooks {
    private val names = mapOf(
        "python" to "Python Automation Engineer Handbook",
        "api" to "API & Protocol Automation Handbook",
        "linux" to "Linux, Bash & Embedded Debugging Handbook",
        "cicd" to "Git, Jenkins & CI/CD Handbook",
        "bmcssd" to "BMC/SSD Interview Prep Handbook (your resume)",
    )
    private val short = mapOf("python" to "Python", "api" to "API & Protocol", "linux" to "Linux & Bash", "cicd" to "Git & CI/CD", "bmcssd" to "BMC/SSD Prep")
    fun title(id: String) = names[id] ?: id
    fun short(id: String) = short[id] ?: id
}

@Composable
fun ScreenTopBar(title: String, onBack: (() -> Unit)? = null, actions: @Composable () -> Unit = {}) {
    TopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            if (onBack != null) IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = { actions() },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) TextButton(onClick = onAction) { Text(action) }
    }
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    container: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CardDefaults.cardColors(containerColor = container)
    val border = CardDefaults.outlinedCardBorder()
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier.fillMaxWidth(), colors = colors, border = border) {
            Column(Modifier.padding(16.dp), content = content)
        }
    } else {
        Card(modifier = modifier.fillMaxWidth(), colors = colors, border = border) {
            Column(Modifier.padding(16.dp), content = content)
        }
    }
}

@Composable
fun ProgressLine(
    fraction: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    height: Dp = 8.dp,
) {
    val animated by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(500), label = "progress")
    LinearProgressIndicator(
        progress = { animated },
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height))
            .semantics { stateDescription = "${(fraction * 100).roundToInt()} percent" },
        color = color,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        strokeCap = StrokeCap.Round,
        gapSize = 0.dp,
        drawStopIndicator = {},
    )
}

@Composable
fun LabeledProgress(label: String, fraction: Float, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary, trailing: String? = null) {
    Column(modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(trailing ?: "${(fraction * 100).roundToInt()}%", style = MaterialTheme.typography.labelLarge, color = color)
        }
        Spacer(Modifier.height(6.dp))
        ProgressLine(fraction, color = color)
    }
}

@Composable
fun ProgressRing(fraction: Float, modifier: Modifier = Modifier, size: Dp = 96.dp, stroke: Dp = 10.dp, color: Color = MaterialTheme.colorScheme.primary, center: @Composable () -> Unit) {
    val animated by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(700), label = "ring")
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = { 1f },
            modifier = Modifier.size(size),
            color = MaterialTheme.colorScheme.surfaceVariant,
            strokeWidth = stroke,
            trackColor = Color.Transparent,
        )
        CircularProgressIndicator(
            progress = { animated },
            modifier = Modifier.size(size),
            color = color,
            strokeWidth = stroke,
            trackColor = Color.Transparent,
            strokeCap = StrokeCap.Round,
        )
        center()
    }
}

@Composable
fun Pill(text: String, modifier: Modifier = Modifier, container: Color = MaterialTheme.colorScheme.surfaceVariant, content: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Surface(color = container, contentColor = content, shape = RoundedCornerShape(50), modifier = modifier) {
        Text(text, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), maxLines = 1)
    }
}

@Composable
fun DifficultyPill(d: Difficulty, modifier: Modifier = Modifier) {
    val ext = AppColors.extended
    val (emoji, bg) = when (d) {
        Difficulty.EASY -> "🟢" to ext.successContainer
        Difficulty.MEDIUM -> "🟡" to ext.warningContainer
        Difficulty.HARD -> "🔴" to ext.dangerContainer
        Difficulty.EXPERT -> "🔥" to MaterialTheme.colorScheme.tertiaryContainer
    }
    Pill("$emoji ${d.label}", modifier, container = bg, content = MaterialTheme.colorScheme.onSurface)
}

@Composable
fun ResumeTagPill(tag: ResumeTag, modifier: Modifier = Modifier) {
    val ext = AppColors.extended
    val bg = when (tag) {
        ResumeTag.ON_RESUME -> ext.dangerContainer
        ResumeTag.IMPLIED -> ext.warningContainer
        ResumeTag.NOT_ON_RESUME -> MaterialTheme.colorScheme.surfaceVariant
    }
    Pill(tag.label, modifier, container = bg, content = MaterialTheme.colorScheme.onSurface)
}

@Composable
fun OriginPill(origin: QuizOrigin, modifier: Modifier = Modifier) {
    Pill(
        if (origin == QuizOrigin.HANDBOOK) "✓ Source-backed" else "✓ Answer from handbook · options written by app",
        modifier,
        container = AppColors.extended.successContainer,
        content = MaterialTheme.colorScheme.onSurface,
    )
}

/** Small "Source" button; opens a dialog with handbook, chapter, section and page. */
@Composable
fun SourceButton(source: SourceRef, modifier: Modifier = Modifier, extra: List<SourceRef> = emptyList(), quote: String? = null) {
    var open by remember { mutableStateOf(false) }
    TextButton(onClick = { open = true }, modifier = modifier) {
        Icon(Icons.Outlined.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("Source · ${Handbooks.short(source.book)} p.${source.page}", style = MaterialTheme.typography.labelMedium)
    }
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            confirmButton = { TextButton(onClick = { open = false }) { Text("Close") } },
            title = { Text("Source") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SourceLines(source)
                    quote?.let {
                        Text("Supporting text in the handbook:", style = MaterialTheme.typography.labelLarge)
                        Text("“$it”", style = MaterialTheme.typography.bodyMedium)
                    }
                    if (extra.isNotEmpty()) {
                        Text("Also covered in:", style = MaterialTheme.typography.labelLarge)
                        extra.forEach { SourceLines(it) }
                    }
                }
            },
        )
    }
}

@Composable
private fun SourceLines(s: SourceRef) {
    Column {
        Text(Handbooks.title(s.book), style = MaterialTheme.typography.titleSmall)
        if (s.chapter.isNotBlank()) Text("Chapter ${s.chapter}", style = MaterialTheme.typography.bodyMedium)
        if (s.section.isNotBlank()) Text("Section ${s.section}", style = MaterialTheme.typography.bodyMedium)
        Text("PDF page ${s.page}", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun BookmarkToggle(bookmarked: Boolean, onToggle: () -> Unit) {
    IconButton(onClick = onToggle) {
        Icon(
            if (bookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
            contentDescription = if (bookmarked) "Remove bookmark" else "Add bookmark",
            tint = if (bookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun <T> Segmented(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
        options.forEachIndexed { i, o ->
            SegmentedButton(
                selected = o == selected,
                onClick = { onSelect(o) },
                shape = SegmentedButtonDefaults.itemShape(i, options.size),
            ) { Text(label(o), maxLines = 1) }
        }
    }
}

@Composable
fun StatTile(value: String, label: String, modifier: Modifier = Modifier, emoji: String? = null) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (emoji != null) Text(emoji, modifier = Modifier.padding(end = 6.dp))
                Text(value, style = MaterialTheme.typography.titleLarge)
            }
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
        }
    }
}

@Composable
fun EmptyState(emoji: String, title: String, message: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Column(
        modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(emoji, style = MaterialTheme.typography.displaySmall)
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (action != null && onAction != null) {
            Spacer(Modifier.height(8.dp))
            Button(onClick = onAction) { Text(action) }
        }
    }
}

@Composable
fun ErrorState(message: String, onRetry: (() -> Unit)? = null, modifier: Modifier = Modifier) =
    EmptyState("⚠️", "Something went wrong", message, modifier, if (onRetry != null) "Try again" else null, onRetry)

@Composable
fun LoadingState(text: String = "Loading…", modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        CircularProgressIndicator()
        Spacer(Modifier.height(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** A list row with a leading emoji/icon bubble, title, subtitle and optional trailing content. */
@Composable
fun ListRow(
    title: String,
    subtitle: String? = null,
    leading: String? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            Box(
                Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Text(leading) }
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (!subtitle.isNullOrBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}

@Composable
fun PrimaryAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(onClick = onClick, enabled = enabled, modifier = modifier.fillMaxWidth().height(52.dp)) { Text(text) }
}

@Composable
fun SecondaryAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(onClick = onClick, enabled = enabled, modifier = modifier.fillMaxWidth().height(52.dp)) { Text(text) }
}

/** Accessible description helper for score widgets. */
fun Modifier.describe(text: String): Modifier = this.semantics { contentDescription = text }
