package org.effectapp.pipeline

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import org.effectapp.effects.EffectRegistry
import org.effectapp.model.Project
import java.io.File

class ExportPipeline(private val context: Context) {
    fun buildComposition(project: Project): Composition {
        val items = project.clips.map { clip ->
            val videoEffects = clip.effectSegments.filter { it.enabled }.mapNotNull { segment -> EffectRegistry.byId(segment.effectId)?.toMediaEffect(segment) } + Presentation.createForHeight(1080)
            val mediaItem = MediaItem.Builder().setUri(Uri.parse(clip.sourceUri)).setClippingConfiguration(MediaItem.ClippingConfiguration.Builder().setStartPositionMs(clip.trimStartMs).setEndPositionMs(clip.trimEndMs).build()).build()
            EditedMediaItem.Builder(mediaItem).setEffects(Effects(emptyList(), videoEffects)).build()
        }
        return Composition.Builder(EditedMediaItemSequence.Builder(items).build()).build()
    }

    fun export(project: Project, output: File, listener: Transformer.Listener) {
        Transformer.Builder(context).addListener(listener).build().start(buildComposition(project), output.absolutePath)
    }
}

sealed interface ExportState {
    data object Idle : ExportState
    data class Running(val progress: Float) : ExportState
    data class Done(val result: ExportResult?) : ExportState
    data class Failed(val message: String) : ExportState
}
