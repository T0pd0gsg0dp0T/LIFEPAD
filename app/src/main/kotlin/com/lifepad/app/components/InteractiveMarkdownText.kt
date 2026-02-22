package com.lifepad.app.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.lifepad.app.domain.parser.WikilinkParser
import java.io.File
import androidx.compose.foundation.text.ClickableText

private const val TAG_WIKILINK = "wikilink"
private const val TAG_HASHTAG = "hashtag"

@Composable
fun InteractiveMarkdownText(
    content: String,
    onWikilinkClick: (String) -> Unit,
    onHashtagClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val blocks = remember(content) { parseMarkdownBlocks(content) }
    Column(modifier = modifier) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Heading -> {
                    MarkdownTextBlock(
                        text = block.text,
                        style = when (block.level) {
                            1 -> MaterialTheme.typography.headlineSmall
                            2 -> MaterialTheme.typography.titleLarge
                            3 -> MaterialTheme.typography.titleMedium
                            else -> MaterialTheme.typography.titleSmall
                        },
                        onWikilinkClick = onWikilinkClick,
                        onHashtagClick = onHashtagClick
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                is MarkdownBlock.Paragraph -> {
                    MarkdownTextBlock(
                        text = block.text,
                        style = MaterialTheme.typography.bodyLarge,
                        onWikilinkClick = onWikilinkClick,
                        onHashtagClick = onHashtagClick
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                is MarkdownBlock.Quote -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(4.dp)
                                .height(24.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                        )
                        MarkdownTextBlock(
                            text = block.text,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            onWikilinkClick = onWikilinkClick,
                            onHashtagClick = onHashtagClick
                        )
                    }
                }
                is MarkdownBlock.CodeBlock -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = block.code,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
                is MarkdownBlock.BulletList -> {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        block.items.forEach { item ->
                            Row {
                                TextBullet()
                                MarkdownTextBlock(
                                    text = item,
                                    style = MaterialTheme.typography.bodyLarge,
                                    onWikilinkClick = onWikilinkClick,
                                    onHashtagClick = onHashtagClick
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                is MarkdownBlock.NumberedList -> {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        block.items.forEachIndexed { index, item ->
                            Row {
                                NumberBullet(index + 1)
                                MarkdownTextBlock(
                                    text = item,
                                    style = MaterialTheme.typography.bodyLarge,
                                    onWikilinkClick = onWikilinkClick,
                                    onHashtagClick = onHashtagClick
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                is MarkdownBlock.Image -> {
                    val painter = when {
                        block.path.startsWith("http") -> rememberAsyncImagePainter(block.path)
                        else -> rememberAsyncImagePainter(File(block.path))
                    }
                    Image(
                        painter = painter,
                        contentDescription = block.alt,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                }
                is MarkdownBlock.HorizontalRule -> {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun MarkdownTextBlock(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    onWikilinkClick: (String) -> Unit,
    onHashtagClick: (String) -> Unit,
    background: Color? = null
) {
    val linkColor = MaterialTheme.colorScheme.primary
    val hashtagColor = MaterialTheme.colorScheme.tertiary
    val codeBackground = MaterialTheme.colorScheme.surfaceVariant
    val annotated = remember(text, linkColor, hashtagColor, codeBackground) {
        buildAnnotatedInline(
            text = text,
            wikilinkStyle = SpanStyle(
                color = linkColor,
                textDecoration = TextDecoration.Underline,
                fontWeight = FontWeight.Medium
            ),
            hashtagStyle = SpanStyle(
                color = hashtagColor,
                fontWeight = FontWeight.Medium
            ),
            codeStyle = SpanStyle(
                fontFamily = FontFamily.Monospace,
                background = codeBackground
            ),
            boldStyle = SpanStyle(fontWeight = FontWeight.Bold),
            italicStyle = SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
            strikeStyle = SpanStyle(textDecoration = TextDecoration.LineThrough)
        )
    }

    val modifier = if (background != null) {
        Modifier
            .fillMaxWidth()
            .padding(6.dp)
            .background(background)
    } else {
        Modifier
    }

    ClickableText(
        text = annotated,
        modifier = modifier,
        style = style,
        onClick = { offset ->
            annotated.getStringAnnotations(TAG_WIKILINK, offset, offset)
                .firstOrNull()?.let {
                    onWikilinkClick(it.item)
                    return@ClickableText
                }
            annotated.getStringAnnotations(TAG_HASHTAG, offset, offset)
                .firstOrNull()?.let {
                    onHashtagClick(it.item)
                }
        }
    )
}

private sealed class MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock()
    data class Paragraph(val text: String) : MarkdownBlock()
    data class Quote(val text: String) : MarkdownBlock()
    data class CodeBlock(val code: String) : MarkdownBlock()
    data class BulletList(val items: List<String>) : MarkdownBlock()
    data class NumberedList(val items: List<String>) : MarkdownBlock()
    data class Image(val alt: String, val path: String) : MarkdownBlock()
    data object HorizontalRule : MarkdownBlock()
}

private fun parseMarkdownBlocks(content: String): List<MarkdownBlock> {
    val lines = content.lines()
    val blocks = mutableListOf<MarkdownBlock>()
    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trim()
        if (trimmed.isEmpty()) {
            i++
            continue
        }

        if (trimmed.startsWith("```")) {
            val codeLines = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trim().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }
            blocks.add(MarkdownBlock.CodeBlock(codeLines.joinToString("\n")))
            i++
            continue
        }

        val imageMatch = Regex("""^!\[(.*?)]\((.+?)\)$""").find(trimmed)
        if (imageMatch != null) {
            blocks.add(MarkdownBlock.Image(imageMatch.groupValues[1], imageMatch.groupValues[2]))
            i++
            continue
        }

        if (Regex("""^#{1,6}\s+""").containsMatchIn(trimmed)) {
            val level = trimmed.takeWhile { it == '#' }.length
            val text = trimmed.drop(level).trim()
            blocks.add(MarkdownBlock.Heading(level, text))
            i++
            continue
        }

        if (trimmed == "---" || trimmed == "***") {
            blocks.add(MarkdownBlock.HorizontalRule)
            i++
            continue
        }

        if (trimmed.startsWith(">")) {
            val quoteLines = mutableListOf<String>()
            while (i < lines.size && lines[i].trim().startsWith(">")) {
                quoteLines.add(lines[i].trim().removePrefix(">").trim())
                i++
            }
            blocks.add(MarkdownBlock.Quote(quoteLines.joinToString("\n")))
            continue
        }

        if (Regex("""^(\d+)\.\s+""").containsMatchIn(trimmed)) {
            val items = mutableListOf<String>()
            while (i < lines.size && Regex("""^(\d+)\.\s+""").containsMatchIn(lines[i].trim())) {
                items.add(lines[i].trim().replaceFirst(Regex("""^\d+\.\s+"""), ""))
                i++
            }
            blocks.add(MarkdownBlock.NumberedList(items))
            continue
        }

        if (Regex("""^[-*+]\s+""").containsMatchIn(trimmed)) {
            val items = mutableListOf<String>()
            while (i < lines.size && Regex("""^[-*+]\s+""").containsMatchIn(lines[i].trim())) {
                items.add(lines[i].trim().replaceFirst(Regex("""^[-*+]\s+"""), ""))
                i++
            }
            blocks.add(MarkdownBlock.BulletList(items))
            continue
        }

        val paragraphLines = mutableListOf<String>()
        while (i < lines.size && lines[i].trim().isNotEmpty()) {
            val current = lines[i].trim()
            if (current.startsWith("```") ||
                Regex("""^#{1,6}\s+""").containsMatchIn(current) ||
                current.startsWith(">") ||
                Regex("""^(\d+)\.\s+""").containsMatchIn(current) ||
                Regex("""^[-*+]\s+""").containsMatchIn(current) ||
                Regex("""^!\[(.*?)]\((.+?)\)$""").containsMatchIn(current)
            ) {
                break
            }
            paragraphLines.add(lines[i])
            i++
        }
        if (paragraphLines.isNotEmpty()) {
            blocks.add(MarkdownBlock.Paragraph(paragraphLines.joinToString("\n")))
        } else {
            i++
        }
    }
    return blocks
}

private fun buildAnnotatedInline(
    text: String,
    wikilinkStyle: SpanStyle,
    hashtagStyle: SpanStyle,
    codeStyle: SpanStyle,
    boldStyle: SpanStyle,
    italicStyle: SpanStyle,
    strikeStyle: SpanStyle
): AnnotatedString {
    val builder = AnnotatedString.Builder()
    appendInline(
        builder = builder,
        text = text,
        baseStyle = null,
        wikilinkStyle = wikilinkStyle,
        hashtagStyle = hashtagStyle,
        codeStyle = codeStyle,
        boldStyle = boldStyle,
        italicStyle = italicStyle,
        strikeStyle = strikeStyle
    )
    return builder.toAnnotatedString()
}

private fun appendInline(
    builder: AnnotatedString.Builder,
    text: String,
    baseStyle: SpanStyle?,
    wikilinkStyle: SpanStyle,
    hashtagStyle: SpanStyle,
    codeStyle: SpanStyle,
    boldStyle: SpanStyle,
    italicStyle: SpanStyle,
    strikeStyle: SpanStyle
) {
    var i = 0
    while (i < text.length) {
        if (text.startsWith("[[", i)) {
            val end = text.indexOf("]]", i + 2)
            if (end != -1) {
                val raw = text.substring(i + 2, end)
                val token = WikilinkParser.parseWikilinks("[[$raw]]").firstOrNull()
                val display = token?.displayText ?: raw
                val target = token?.target ?: raw
                appendStyled(builder, display, mergeStyles(baseStyle, wikilinkStyle))
                val start = builder.length - display.length
                val finish = builder.length
                builder.addStringAnnotation(TAG_WIKILINK, target, start, finish)
                i = end + 2
                continue
            }
        }

        if (text[i] == '#' && (i == 0 || text[i - 1].isWhitespace())) {
            val match = Regex("""#([a-zA-Z][a-zA-Z0-9_]*)""").find(text, i)
            if (match != null && match.range.first == i) {
                val value = match.value
                val tag = match.groupValues[1].lowercase()
                appendStyled(builder, value, mergeStyles(baseStyle, hashtagStyle))
                val start = builder.length - value.length
                val finish = builder.length
                builder.addStringAnnotation(TAG_HASHTAG, tag, start, finish)
                i = match.range.last + 1
                continue
            }
        }

        if (text.startsWith("`", i)) {
            val end = text.indexOf("`", i + 1)
            if (end != -1) {
                val code = text.substring(i + 1, end)
                appendStyled(builder, code, mergeStyles(baseStyle, codeStyle))
                i = end + 1
                continue
            }
        }

        if (text.startsWith("***", i)) {
            val end = text.indexOf("***", i + 3)
            if (end != -1) {
                val inner = text.substring(i + 3, end)
                appendInline(
                    builder,
                    inner,
                    mergeStyles(baseStyle, mergeStyles(boldStyle, italicStyle)),
                    wikilinkStyle,
                    hashtagStyle,
                    codeStyle,
                    boldStyle,
                    italicStyle,
                    strikeStyle
                )
                i = end + 3
                continue
            }
        }

        if (text.startsWith("**", i) || text.startsWith("__", i)) {
            val marker = text.substring(i, i + 2)
            val end = text.indexOf(marker, i + 2)
            if (end != -1) {
                val inner = text.substring(i + 2, end)
                appendInline(
                    builder,
                    inner,
                    mergeStyles(baseStyle, boldStyle),
                    wikilinkStyle,
                    hashtagStyle,
                    codeStyle,
                    boldStyle,
                    italicStyle,
                    strikeStyle
                )
                i = end + 2
                continue
            }
        }

        if (text.startsWith("*", i) || text.startsWith("_", i)) {
            val marker = text.substring(i, i + 1)
            val end = text.indexOf(marker, i + 1)
            if (end != -1) {
                val inner = text.substring(i + 1, end)
                appendInline(
                    builder,
                    inner,
                    mergeStyles(baseStyle, italicStyle),
                    wikilinkStyle,
                    hashtagStyle,
                    codeStyle,
                    boldStyle,
                    italicStyle,
                    strikeStyle
                )
                i = end + 1
                continue
            }
        }

        if (text.startsWith("~~", i)) {
            val end = text.indexOf("~~", i + 2)
            if (end != -1) {
                val inner = text.substring(i + 2, end)
                appendInline(
                    builder,
                    inner,
                    mergeStyles(baseStyle, strikeStyle),
                    wikilinkStyle,
                    hashtagStyle,
                    codeStyle,
                    boldStyle,
                    italicStyle,
                    strikeStyle
                )
                i = end + 2
                continue
            }
        }

        appendStyled(builder, text[i].toString(), baseStyle)
        i++
    }
}

private fun appendStyled(builder: AnnotatedString.Builder, text: String, style: SpanStyle?) {
    val start = builder.length
    builder.append(text)
    val end = builder.length
    if (style != null) builder.addStyle(style, start, end)
}

private fun mergeStyles(base: SpanStyle?, extra: SpanStyle): SpanStyle {
    return base?.merge(extra) ?: extra
}

@Composable
private fun TextBullet() {
    Text(
        text = "•",
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(end = 8.dp)
    )
}

@Composable
private fun NumberBullet(number: Int) {
    Text(
        text = "$number.",
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(end = 8.dp)
    )
}
