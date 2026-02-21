package com.lifepad.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "assessments",
    indices = [
        Index("type"),
        Index("date")
    ]
)
data class AssessmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String, // "GAD7" or "PHQ9"
    val score: Int,
    val answers: String, // JSON array of int answers
    val date: Long,
    val createdAt: Long = System.currentTimeMillis()
)
