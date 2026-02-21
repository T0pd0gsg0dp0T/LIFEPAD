package com.lifepad.app.domain.parser

object ChecklistParser {

    private val CHECKLIST_PATTERN = Regex("""^- \[([ xX])] (.*)$""", RegexOption.MULTILINE)

    data class ChecklistItem(
        val text: String,
        val isChecked: Boolean
    )

    fun parseChecklist(content: String): List<ChecklistItem> {
        return CHECKLIST_PATTERN.findAll(content).map { match ->
            ChecklistItem(
                text = match.groupValues[2],
                isChecked = match.groupValues[1].lowercase() == "x"
            )
        }.toList()
    }

    fun toChecklistContent(items: List<ChecklistItem>): String {
        return items.joinToString("\n") { item ->
            val check = if (item.isChecked) "x" else " "
            "- [$check] ${item.text}"
        }
    }

    fun convertToChecklist(content: String): String {
        if (content.isBlank()) return "- [ ] "
        val lines = content.lines()
        return lines.mapNotNull { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) return@mapNotNull null
            // Skip if already a checklist item
            if (CHECKLIST_PATTERN.matches(trimmed)) return@mapNotNull trimmed
            // Strip common list prefixes
            val cleaned = trimmed
                .removePrefix("- ")
                .removePrefix("* ")
                .removePrefix("+ ")
                .replace(Regex("""^\d+\.\s*"""), "")
            "- [ ] $cleaned"
        }.joinToString("\n")
    }

    fun convertFromChecklist(content: String): String {
        return content.lines().joinToString("\n") { line ->
            val match = CHECKLIST_PATTERN.find(line.trim())
            if (match != null) {
                match.groupValues[2]
            } else {
                line
            }
        }
    }

    fun toggleItem(content: String, itemIndex: Int): String {
        val lines = content.lines().toMutableList()
        var checklistCount = 0
        for (i in lines.indices) {
            val match = CHECKLIST_PATTERN.find(lines[i].trim())
            if (match != null) {
                if (checklistCount == itemIndex) {
                    val newCheck = if (match.groupValues[1].lowercase() == "x") " " else "x"
                    lines[i] = "- [$newCheck] ${match.groupValues[2]}"
                    return lines.joinToString("\n")
                }
                checklistCount++
            }
        }
        return content
    }

    fun addItem(content: String, text: String): String {
        val newItem = "- [ ] $text"
        return if (content.isBlank()) newItem else "$content\n$newItem"
    }

    fun removeItem(content: String, itemIndex: Int): String {
        val lines = content.lines().toMutableList()
        var checklistCount = 0
        for (i in lines.indices) {
            val match = CHECKLIST_PATTERN.find(lines[i].trim())
            if (match != null) {
                if (checklistCount == itemIndex) {
                    lines.removeAt(i)
                    return lines.joinToString("\n")
                }
                checklistCount++
            }
        }
        return content
    }
}
