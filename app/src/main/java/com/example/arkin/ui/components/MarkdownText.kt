package com.example.arkin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ArkinPrimary

/**
 * Rich Compose Markdown renderer designed for Arkin AI responses.
 * Renders Headings (#, ##, ###), bold/italics, bulleted/numbered lists,
 * code blocks with copy actions, markdown tables with horizontal scroll,
 * blockquotes, and links.
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    isUser: Boolean = false
) {
    if (markdown.isBlank()) return

    val blocks = parseMarkdownBlocks(markdown)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Heading -> {
                    HeadingBlock(block, if (isUser) Color.White else textColor)
                }
                is MarkdownBlock.Paragraph -> {
                    ParagraphBlock(block.text, if (isUser) Color.White else textColor)
                }
                is MarkdownBlock.BulletItem -> {
                    BulletBlock(block.level, block.text, if (isUser) Color.White else textColor)
                }
                is MarkdownBlock.NumberedItem -> {
                    NumberedBlock(block.number, block.text, if (isUser) Color.White else textColor)
                }
                is MarkdownBlock.CodeBlock -> {
                    CodeBlockView(block.language, block.code)
                }
                is MarkdownBlock.Table -> {
                    TableView(block)
                }
                is MarkdownBlock.Blockquote -> {
                    BlockquoteView(block.text, textColor)
                }
                is MarkdownBlock.HorizontalRule -> {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

private sealed class MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock()
    data class Paragraph(val text: String) : MarkdownBlock()
    data class BulletItem(val level: Int, val text: String) : MarkdownBlock()
    data class NumberedItem(val number: String, val text: String) : MarkdownBlock()
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock()
    data class Table(val headers: List<String>, val rows: List<List<String>>) : MarkdownBlock()
    data class Blockquote(val text: String) : MarkdownBlock()
    object HorizontalRule : MarkdownBlock()
}

private fun parseMarkdownBlocks(rawMarkdown: String): List<MarkdownBlock> {
    val lines = rawMarkdown.replace("\r\n", "\n").replace('\r', '\n').split('\n')
    val blocks = mutableListOf<MarkdownBlock>()

    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trim()

        when {
            // Horizontal Rule
            trimmed.matches(Regex("^(-{3,}|\\*{3,}|_{3,})$")) -> {
                blocks.add(MarkdownBlock.HorizontalRule)
                i++
            }

            // Fenced Code Block
            trimmed.startsWith("```") -> {
                val language = trimmed.removePrefix("```").trim()
                val codeLines = mutableListOf<String>()
                i++
                while (i < lines.size && !lines[i].trim().startsWith("```")) {
                    codeLines.add(lines[i])
                    i++
                }
                if (i < lines.size && lines[i].trim().startsWith("```")) {
                    i++ // Skip closing ```
                }
                blocks.add(MarkdownBlock.CodeBlock(language, codeLines.joinToString("\n")))
            }

            // Headings
            trimmed.startsWith("#### ") -> {
                blocks.add(MarkdownBlock.Heading(4, trimmed.removePrefix("#### ").trim()))
                i++
            }
            trimmed.startsWith("### ") -> {
                blocks.add(MarkdownBlock.Heading(3, trimmed.removePrefix("### ").trim()))
                i++
            }
            trimmed.startsWith("## ") -> {
                blocks.add(MarkdownBlock.Heading(2, trimmed.removePrefix("## ").trim()))
                i++
            }
            trimmed.startsWith("# ") -> {
                blocks.add(MarkdownBlock.Heading(1, trimmed.removePrefix("# ").trim()))
                i++
            }

            // Blockquote
            trimmed.startsWith("> ") -> {
                val quoteLines = mutableListOf(trimmed.removePrefix("> ").trim())
                i++
                while (i < lines.size && lines[i].trim().startsWith("> ")) {
                    quoteLines.add(lines[i].trim().removePrefix("> ").trim())
                    i++
                }
                blocks.add(MarkdownBlock.Blockquote(quoteLines.joinToString(" ")))
            }

            // Markdown Table (Starts with | and contains |)
            trimmed.startsWith("|") && trimmed.endsWith("|") && i + 1 < lines.size && lines[i + 1].trim().matches(Regex("^\\|[\\s\\-:\\|]+\\|$")) -> {
                val headers = parseTableRow(trimmed)
                i += 2 // skip header and separator row
                val rows = mutableListOf<List<String>>()
                while (i < lines.size && lines[i].trim().startsWith("|") && lines[i].trim().endsWith("|")) {
                    rows.add(parseTableRow(lines[i].trim()))
                    i++
                }
                blocks.add(MarkdownBlock.Table(headers, rows))
            }

            // Bullet list items
            trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ") -> {
                val indent = line.takeWhile { it == ' ' || it == '\t' }.length
                val level = indent / 2
                val content = trimmed.removePrefix("- ").removePrefix("* ").removePrefix("• ").trim()
                blocks.add(MarkdownBlock.BulletItem(level, content))
                i++
            }

            // Numbered list items (e.g. "1. ", "2) ", etc.)
            trimmed.matches(Regex("^\\d+[\\.\\)]\\s+.*")) -> {
                val match = Regex("^(\\d+[\\.\\)])\\s+(.*)").find(trimmed)
                if (match != null) {
                    val num = match.groupValues[1]
                    val content = match.groupValues[2]
                    blocks.add(MarkdownBlock.NumberedItem(num, content))
                } else {
                    blocks.add(MarkdownBlock.Paragraph(trimmed))
                }
                i++
            }

            // Empty line
            trimmed.isEmpty() -> {
                i++
            }

            // Regular Paragraph text
            else -> {
                // Collect continuous paragraph lines
                val paraLines = mutableListOf(trimmed)
                i++
                while (i < lines.size) {
                    val next = lines[i].trim()
                    if (next.isEmpty() ||
                        next.startsWith("#") ||
                        next.startsWith("```") ||
                        next.startsWith(">") ||
                        next.startsWith("|") ||
                        next.startsWith("- ") ||
                        next.startsWith("* ") ||
                        next.startsWith("• ") ||
                        next.matches(Regex("^\\d+[\\.\\)]\\s+.*"))
                    ) {
                        break
                    }
                    paraLines.add(next)
                    i++
                }
                blocks.add(MarkdownBlock.Paragraph(paraLines.joinToString(" ")))
            }
        }
    }

    return blocks
}

private fun parseTableRow(row: String): List<String> {
    return row.removeSurrounding("|", "|")
        .split("|")
        .map { it.trim() }
}

@Composable
private fun HeadingBlock(heading: MarkdownBlock.Heading, color: Color) {
    val (size, weight) = when (heading.level) {
        1 -> 18.sp to FontWeight.ExtraBold
        2 -> 16.sp to FontWeight.Bold
        3 -> 15.sp to FontWeight.Bold
        else -> 14.sp to FontWeight.SemiBold
    }

    Spacer(modifier = Modifier.height(2.dp))
    Text(
        text = buildInlineMarkdown(heading.text, color),
        fontSize = size,
        fontWeight = weight,
        color = color,
        lineHeight = (size.value + 6).sp,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ParagraphBlock(text: String, color: Color) {
    val annotated = buildInlineMarkdown(text, color)
    val uriHandler = LocalUriHandler.current

    ClickableText(
        text = annotated,
        style = TextStyle(
            fontSize = 14.sp,
            lineHeight = 21.sp,
            color = color
        ),
        onClick = { offset ->
            annotated.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    try {
                        uriHandler.openUri(annotation.item)
                    } catch (_: Exception) {}
                }
        }
    )
}

@Composable
private fun BulletBlock(level: Int, text: String, color: Color) {
    val paddingStart = (level * 14).dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = paddingStart, top = 1.dp, bottom = 1.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 7.dp, end = 8.dp)
                .size(5.dp)
                .clip(CircleShape)
                .background(if (color == Color.White) Color.White.copy(alpha = 0.9f) else ArkinPrimary)
        )
        ParagraphBlock(text, color)
    }
}

@Composable
private fun NumberedBlock(number: String, text: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 1.dp, bottom = 1.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = number,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = if (color == Color.White) Color.White else ArkinPrimary,
            modifier = Modifier
                .padding(end = 6.dp)
                .width(24.dp)
        )
        ParagraphBlock(text, color)
    }
}

@Composable
private fun CodeBlockView(language: String, code: String) {
    val clipboardManager = LocalClipboardManager.current

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1E1E2E), // Modern dark syntax background
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = language.ifBlank { "code" }.uppercase(),
                    color = Color(0xFFA6ADC8),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                IconButton(
                    onClick = { clipboardManager.setText(AnnotatedString(code)) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Code",
                        tint = Color(0xFFA6ADC8),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                Text(
                    text = code,
                    color = Color(0xFFCDD6F4),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun TableView(table: MarkdownBlock.Table) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(10.dp)
        ) {
            Column {
                // Table Header Row
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    table.headers.forEach { header ->
                        Text(
                            text = header,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = ArkinPrimary,
                            modifier = Modifier
                                .width(120.dp)
                                .padding(end = 8.dp)
                        )
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                // Table Data Rows
                table.rows.forEachIndexed { index, row ->
                    Row(
                        modifier = Modifier
                            .background(
                                if (index % 2 == 1) MaterialTheme.colorScheme.surface.copy(alpha = 0.6f) else Color.Transparent,
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        row.forEachIndexed { colIndex, cell ->
                            Text(
                                text = cell,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .width(120.dp)
                                    .padding(end = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BlockquoteView(text: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(30.dp)
                .background(ArkinPrimary, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            fontSize = 13.sp,
            fontStyle = FontStyle.Italic,
            color = color.copy(alpha = 0.9f),
            lineHeight = 18.sp
        )
    }
}

/**
 * Builds an AnnotatedString parsing inline formatting:
 * **bold**, *italic*, `inline code`, [link text](url), ~~strikethrough~~.
 */
private fun buildInlineMarkdown(text: String, defaultColor: Color): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        val regex = Regex("(\\*\\*\\*(.*?)\\*\\*\\*)|(\\*\\*(.*?)\\*\\*)|(\\*(.*?)\\*)|(`(.*?)`)|(\\[(.*?)\\]\\((.*?)\\))|(~~(.*?)~~)")

        val matches = regex.findAll(text).toList()

        for (match in matches) {
            val start = match.range.first
            val end = match.range.last + 1

            if (start > cursor) {
                append(text.substring(cursor, start))
            }

            val fullMatch = match.value
            when {
                // Bold + Italic: ***text***
                fullMatch.startsWith("***") && fullMatch.endsWith("***") -> {
                    val inner = fullMatch.removeSurrounding("***")
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic))
                    append(inner)
                    pop()
                }
                // Bold: **text**
                fullMatch.startsWith("**") && fullMatch.endsWith("**") -> {
                    val inner = fullMatch.removeSurrounding("**")
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(inner)
                    pop()
                }
                // Italic: *text*
                fullMatch.startsWith("*") && fullMatch.endsWith("*") -> {
                    val inner = fullMatch.removeSurrounding("*")
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    append(inner)
                    pop()
                }
                // Inline code: `code`
                fullMatch.startsWith("`") && fullMatch.endsWith("`") -> {
                    val inner = fullMatch.removeSurrounding("`")
                    pushStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            background = Color(0x229333EA),
                            color = Color(0xFF9333EA)
                        )
                    )
                    append(" $inner ")
                    pop()
                }
                // Link: [text](url)
                fullMatch.startsWith("[") && fullMatch.contains("](") && fullMatch.endsWith(")") -> {
                    val linkText = match.groups[10]?.value ?: "link"
                    val url = match.groups[11]?.value ?: ""
                    pushStringAnnotation(tag = "URL", annotation = url)
                    pushStyle(
                        SpanStyle(
                            color = ArkinPrimary,
                            textDecoration = TextDecoration.Underline,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    append(linkText)
                    pop()
                    pop()
                }
                // Strikethrough: ~~text~~
                fullMatch.startsWith("~~") && fullMatch.endsWith("~~") -> {
                    val inner = fullMatch.removeSurrounding("~~")
                    pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                    append(inner)
                    pop()
                }
                else -> {
                    append(fullMatch)
                }
            }

            cursor = end
        }

        if (cursor < text.length) {
            append(text.substring(cursor))
        }
    }
}
