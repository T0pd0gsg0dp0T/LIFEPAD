package com.lifepad.app.domain.cbt

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

@Serializable
data class ThoughtJournalData(
    val situation: String = "",
    val automaticThoughts: String = "",
    val evidenceFor: String = "",
    val evidenceAgainst: String = "",
    val alternativeThought: String = "",
    val beliefBefore: Int = 0,  // 0-100
    val beliefAfter: Int = 0,   // 0-100
    val moodBefore: Int = 5,    // 1-10
    val moodAfter: Int = 5      // 1-10
)

@Serializable
data class ExposureJournalData(
    val fearDescription: String = "",
    val avoidanceBehavior: String = "",
    val sudsBefore: Int = 0,    // 0-100
    val sudsDuring: Int = 0,
    val sudsAfter: Int = 0,
    val exposurePlan: String = "",
    val reflection: String = "",
    val moodBefore: Int = 5,    // 1-10
    val moodAfter: Int = 5      // 1-10
)

@Serializable
data class SavoringJournalData(
    val experience: String = "",
    val sensoryDetails: String = "",
    val savoring: String = ""
)

object StructuredDataSerializer {
    private val json = Json { ignoreUnknownKeys = true }

    fun encodeThought(data: ThoughtJournalData): String = json.encodeToString(data)
    fun decodeThought(jsonStr: String): ThoughtJournalData {
        if (jsonStr.isBlank()) return ThoughtJournalData()
        return try { json.decodeFromString(jsonStr) } catch (_: Exception) { ThoughtJournalData() }
    }

    fun encodeExposure(data: ExposureJournalData): String = json.encodeToString(data)
    fun decodeExposure(jsonStr: String): ExposureJournalData {
        if (jsonStr.isBlank()) return ExposureJournalData()
        return try { json.decodeFromString(jsonStr) } catch (_: Exception) { ExposureJournalData() }
    }

    fun encodeSavoring(data: SavoringJournalData): String = json.encodeToString(data)
    fun decodeSavoring(jsonStr: String): SavoringJournalData {
        if (jsonStr.isBlank()) return SavoringJournalData()
        return try { json.decodeFromString(jsonStr) } catch (_: Exception) { SavoringJournalData() }
    }
}
