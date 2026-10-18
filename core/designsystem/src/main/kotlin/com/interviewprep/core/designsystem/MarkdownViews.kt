package com.interviewprep.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

@Composable
fun rememberInline(text: String): AnnotatedString {
    val ext = AppColors.extended
    val codeBg = ext.codeBackground
    val codeFg = ext.codeText
    return remember(text, codeBg) {
        buildAnnotatedString {
            for (s in Markdown.spans(text)) when (s) {
                is MdSpan.Plain -> append(s.text)
                is MdSpan.Bold -> withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(s.text) }
                is MdSpan.Code -> withStyle(SpanStyle(fontFamily = CodeFont, background = codeBg, color = codeFg, fontSize = 0.92.em)) { append(s.text) }
            }
        }
    }
}


@Composable
fun InlineText(text: String, modifier: Modifier = Modifier, style: TextStyle = MaterialTheme.typography.bodyLarge) {
    Text(rememberInline(text), modifier = modifier, style = style, color = MaterialTheme.colorScheme.onSurface)
}

/** Renders one parsed block. Lists of blocks go in a LazyColumn for long lessons. */
@Composable
fun MdBlockView(block: MdBlock, modifier: Modifier = Modifier) {
    when (block) {
        is MdBlock.Heading -> Text(
            block.text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = modifier.padding(top = 12.dp, bottom = 2.dp),
        )
        is MdBlock.Paragraph -> InlineText(block.text, modifier.padding(vertical = 4.dp))
        is MdBlock.Bullet -> Row(modifier.padding(vertical = 3.dp)) {
            Text(
                block.marker,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.widthIn(min = 22.dp),
            )
            InlineText(block.text, Modifier.weight(1f))
        }
        is MdBlock.Code -> CodeBlock(block.code, isOutput = block.isOutput, modifier = modifier.padding(vertical = 6.dp))
        is MdBlock.Table -> MdTable(block.rows, modifier.padding(vertical = 6.dp))
    }
}

/** Small documents (answers, explanations) rendered in a plain Column. */
@Composable
fun MarkdownText(text: String, modifier: Modifier = Modifier) {
    val blocks = remember(text) { Markdown.parse(text) }
    Column(modifier) { blocks.forEach { MdBlockView(it) } }
}

@Composable
fun CodeBlock(code: String, modifier: Modifier = Modifier, isOutput: Boolean = false, label: String? = null) {
    val ext = AppColors.extended
    val clipboard = LocalClipboardManager.current
    val bg = if (isOutput) ext.outputBackground else ext.codeBackground
    val fg = if (isOutput) ext.outputText else ext.codeText
    Column(
        modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(12.dp))
            .border(1.dp, if (isOutput) bg else ext.codeBorder, RoundedCornerShape(12.dp)),
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                label ?: if (isOutput) "OUTPUT" else "CODE",
                style = MaterialTheme.typography.labelSmall,
                color = fg.copy(alpha = 0.6f),
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { clipboard.setText(AnnotatedString(code)) }) {
                Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy code", tint = fg.copy(alpha = 0.7f))
            }
        }
        Box(
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                .semantics { contentDescription = if (isOutput) "Program output: $code" else "Code: $code" },
        ) {
            Text(code, fontFamily = CodeFont, fontSize = 13.5.sp, lineHeight = 19.sp, color = fg, softWrap = false)
        }
    }
}

@Composable
fun MdTable(rows: List<List<String>>, modifier: Modifier = Modifier) {
    val cols = rows.maxOfOrNull { it.size } ?: return
    Column(
        modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .padding(vertical = 4.dp),
    ) {
        rows.forEachIndexed { r, row ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(if (r == 0) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                for (c in 0 until cols) {
                    val cell = row.getOrNull(c).orEmpty()
                    Text(
                        rememberInline(cell),
                        style = if (r == 0) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(if (c == 0 && cols > 1) 0.8f else 1f),
                    )
                }
            }
        }
    }
}

@Composable
fun Gap(h: Int = 12) = Spacer(Modifier.height(h.dp))
