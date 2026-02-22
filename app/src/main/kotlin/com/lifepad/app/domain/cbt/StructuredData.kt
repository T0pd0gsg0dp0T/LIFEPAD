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
    val savoring: String = "",
    val mood: Int = 5
)

@Serializable
data class GratitudeJournalData(
    val itemOne: String = "",
    val itemTwo: String = "",
    val itemThree: String = "",
    val whyItMattered: String = "",
    val whoHelped: String = "",
    val mood: Int = 5
)

@Serializable
data class ReflectionJournalData(
    val intention: String = "",
    val highlights: String = "",
    val challenges: String = "",
    val improveTomorrow: String = "",
    val mood: Int = 5
)

@Serializable
data class CheckInJournalData(
    val mood: Int = 5,
    val energy: Int = 50,
    val stress: Int = 50,
    val notes: String = ""
)

@Serializable
data class FoodJournalData(
    val meal: String = "",
    val hungerBefore: Int = 50,
    val hungerAfter: Int = 50,
    val moodBefore: Int = 5,
    val moodAfter: Int = 5,
    val reflection: String = ""
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

    fun encodeGratitude(data: GratitudeJournalData): String = json.encodeToString(data)
    fun decodeGratitude(jsonStr: String): GratitudeJournalData {
        if (jsonStr.isBlank()) return GratitudeJournalData()
        return try { json.decodeFromString(jsonStr) } catch (_: Exception) { GratitudeJournalData() }
    }

    fun encodeReflection(data: ReflectionJournalData): String = json.encodeToString(data)
    fun decodeReflection(jsonStr: String): ReflectionJournalData {
        if (jsonStr.isBlank()) return ReflectionJournalData()
        return try { json.decodeFromString(jsonStr) } catch (_: Exception) { ReflectionJournalData() }
    }

    fun encodeCheckIn(data: CheckInJournalData): String = json.encodeToString(data)
    fun decodeCheckIn(jsonStr: String): CheckInJournalData {
        if (jsonStr.isBlank()) return CheckInJournalData()
        return try { json.decodeFromString(jsonStr) } catch (_: Exception) { CheckInJournalData() }
    }

    fun encodeFood(data: FoodJournalData): String = json.encodeToString(data)
    fun decodeFood(jsonStr: String): FoodJournalData {
        if (jsonStr.isBlank()) return FoodJournalData()
        return try { json.decodeFromString(jsonStr) } catch (_: Exception) { FoodJournalData() }
    }
}
