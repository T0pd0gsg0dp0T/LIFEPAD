package com.lifepad.app.util

object MarkdownImageInserter {
    private val listLinePattern = Regex("""^\s*(?:[-*+]\s+|\d+\.\s+|- \[[ xX]]\s+)""")

    fun insertImage(content: String, cursorPosition: Int, filePath: String): String {
        val safeCursor = cursorPosition.coerceIn(0, content.length)
        val lineStart = content.lastIndexOf('\n', safeCursor - 1).let { if (it == -1) 0 else it + 1 }
        val lineEnd = content.indexOf('\n', safeCursor).let { if (it == -1) content.length else it }
        val line = content.substring(lineStart, lineEnd)
        val insertAt = if (listLinePattern.containsMatchIn(line)) lineEnd else safeCursor

        val before = content.substring(0, insertAt)
        val after = content.substring(insertAt)
        val leading = if (before.isNotEmpty() && !before.endsWith("\n")) "\n" else ""
        val trailing = if (after.isNotEmpty() && !after.startsWith("\n")) "\n" else ""

        return before + leading + "![]($filePath)" + trailing + after
    }
}
