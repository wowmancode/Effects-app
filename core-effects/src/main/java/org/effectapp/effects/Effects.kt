package org.effectapp.effects

import androidx.media3.common.Effect
import androidx.media3.effect.RgbFilter
import org.effectapp.model.TimelineSegment

enum class EffectCategory { EFFECTS, TRANSFORM, AUDIO }
enum class ParamType { FLOAT, INT, BOOLEAN }

data class EffectParam(
    val id: String,
    val displayName: String,
    val type: ParamType,
    val defaultValue: String,
    val min: Float? = null,
    val max: Float? = null,
)

interface LecEffect {
    val id: String
    val displayName: String
    val category: EffectCategory
    val params: List<EffectParam>
    fun toMediaEffect(segment: TimelineSegment): Effect?
}

object HueRotateEffect : LecEffect {
    override val id = "hue_rotate"
    override val displayName = "Hue rotate"
    override val category = EffectCategory.EFFECTS
    override val params = listOf(EffectParam("degrees", "Degrees", ParamType.FLOAT, "90", 0f, 360f))
    override fun toMediaEffect(segment: TimelineSegment): Effect = RgbFilter.createInvertedFilter()
}

object PitchChangeEffect : LecEffect {
    override val id = "pitch_change"
    override val displayName = "Pitch / speed change"
    override val category = EffectCategory.AUDIO
    override val params = listOf(EffectParam("pitch", "Pitch", ParamType.FLOAT, "1", 0.5f, 2f), EffectParam("speed", "Speed", ParamType.FLOAT, "1", 0.25f, 4f))
    override fun toMediaEffect(segment: TimelineSegment): Effect? = null
}

object WaveWarpEffect : LecEffect {
    override val id = "wave_warp"
    override val displayName = "Wave warp"
    override val category = EffectCategory.EFFECTS
    override val params = listOf(EffectParam("amplitude", "Amplitude", ParamType.FLOAT, "0.04", 0f, 0.2f), EffectParam("frequency", "Frequency", ParamType.FLOAT, "8", 1f, 30f))
    override fun toMediaEffect(segment: TimelineSegment): Effect = RgbFilter.createInvertedFilter()
}

object EffectRegistry {
    private val effects = listOf(HueRotateEffect, PitchChangeEffect, WaveWarpEffect)
    fun all(): List<LecEffect> = effects
    fun byId(id: String): LecEffect? = effects.firstOrNull { it.id == id }
    fun byCategory(category: EffectCategory): List<LecEffect> = effects.filter { it.category == category }
}
