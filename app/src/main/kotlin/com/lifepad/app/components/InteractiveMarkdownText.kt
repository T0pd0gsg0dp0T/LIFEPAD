package com.lifepad.app.components

import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.lifepad.app.domain.parser.WikilinkParser

private const val TAG_WIKILINK = "wikilink"
private const val TAG_HASHTAG = "hashtag"

@Composable
fun InteractiveMarkdownText(
    content: String,
    onWikilinkClick: (String) -> Unit,
    onHashtagClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val linkColor = MaterialTheme.colorScheme.primary
    val hashtagColor = MaterialTheme.colorScheme.tertiary
    val textStyle = MaterialTheme.typography.bodyLarge

    val annotated = remember(content, linkColor, hashtagColor) {
        buildInteractiveAnnotatedString(
            content = content,
            wikilinkStyle = SpanStyle(
                color = linkColor,
                textDecoration = TextDecoration.Underline,
                fontWeight = FontWeight.Medium
            ),
            hashtagStyle = SpanStyle(
                color = hashtagColor,
                fontWeight = FontWeight.Medium
            )
        )
    }

    ClickableText(
        text = annotated,
        modifier = modifier,
        style = textStyle,
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

private fun buildInteractiveAnnotatedString(
    content: String,
    wikilinkStyle: SpanStyle,
    hashtagStyle: SpanStyle
): AnnotatedString {
    val builder = AnnotatedString.Builder()
    val wikilinks = WikilinkParser.parseWikilinks(content)
    var cursor = 0

    wikilinks.forEach { token ->
        if (token.range.first > cursor) {
            appendWithHashtagAnnotations(
                builder = builder,
                text = content.substring(cursor, token.range.first),
                hashtagStyle = hashtagStyle
            )
        }

        val start = builder.length
        builder.append(token.displayText)
        val end = builder.length
        builder.addStringAnnotation(TAG_WIKILINK, token.target, start, end)
        builder.addStyle(wikilinkStyle, start, end)

        cursor = token.range.last + 1
    }

    if (cursor < content.length) {
        appendWithHashtagAnnotations(
            builder = builder,
            text = content.substring(cursor),
            hashtagStyle = hashtagStyle
        )
    }

    return builder.toAnnotatedString()
}

private fun appendWithHashtagAnnotations(
    builder: AnnotatedString.Builder,
    text: String,
    hashtagStyle: SpanStyle
) {
    val hashtagRegex = Regex("""#([a-zA-Z][a-zA-Z0-9_]*)""")
    var cursor = 0

    hashtagRegex.findAll(text).forEach { match ->
        if (match.range.first > cursor) {
            builder.append(text.substring(cursor, match.range.first))
        }

        val start = builder.length
        builder.append(match.value)
        val end = builder.length
        builder.addStringAnnotation(TAG_HASHTAG, match.groupValues[1].lowercase(), start, end)
        builder.addStyle(hashtagStyle, start, end)

        cursor = match.range.last + 1
    }

    if (cursor < text.length) {
        builder.append(text.substring(cursor))
    }
}
