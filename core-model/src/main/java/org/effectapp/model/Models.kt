package org.effectapp.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class Project(
    val name: String,
    val createdAt: Long,
    val clips: List<Clip> = emptyList(),
)

@Serializable
data class Clip(
    val id: String,
    val sourceUri: String,
    val trimStartMs: Long = 0,
    val trimEndMs: Long = 0,
    val transform: TransformSettings = TransformSettings(),
    val effectSegments: List<TimelineSegment> = emptyList(),
    val audioSegments: List<TimelineSegment> = emptyList(),
)

@Serializable
data class TransformSettings(
    val scale: Float = 1f,
    val rotationDegrees: Float = 0f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
)

@Serializable
data class TimelineSegment(
    val id: String,
    val effectId: String,
    val startMs: Long,
    val endMs: Long,
    val enabled: Boolean = true,
    val params: Map<String, String> = emptyMap(),
)

object ProjectJson {
    val format = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }
    fun encode(project: Project): String = format.encodeToString(project)
    fun decode(json: String): Project = format.decodeFromString(json)
}
