package org.effectapp.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay
import org.effectapp.effects.EffectCategory
import org.effectapp.effects.EffectParam
import org.effectapp.effects.EffectRegistry
import org.effectapp.model.Clip
import org.effectapp.model.Project
import org.effectapp.model.TimelineSegment
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

private const val DEFAULT_CLIP_DURATION_MS = 10_000L
private val TimelineScale = 0.05.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { EffectApp() } }
    }
}

@Composable
fun EffectApp() {
    val nav = rememberNavController()
    var project by remember { mutableStateOf(Project("Untitled", System.currentTimeMillis())) }
    NavHost(navController = nav, startDestination = "import") {
        composable("import") { ImportScreen(project, { project = it }, { nav.navigate("editor") }) }
        composable("editor") { EditorScreen(project, { project = it }, { nav.navigate("export") }) }
        composable("export") { ExportScreen(project) }
    }
}

@Composable
fun ImportScreen(project: Project, onProject: (Project) -> Unit, onEdit: () -> Unit) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        val clips = uris.map { uri -> Clip(id = UUID.randomUUID().toString(), sourceUri = uri.toString()) }
        onProject(project.copy(clips = project.clips + clips))
    }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Import clips", style = MaterialTheme.typography.headlineMedium)
        Button({ picker.launch(arrayOf("video/*")) }) { Text("Pick videos") }
        LazyColumn(Modifier.weight(1f)) { items(project.clips) { ClipCard(it) } }
        Button(onEdit, enabled = project.clips.isNotEmpty()) { Text("Enter editor") }
    }
}

private enum class LaneKind { EFFECTS, AUDIO }

private data class SegmentSelection(val clipId: String, val lane: LaneKind, val segmentId: String)
private data class PendingSegmentAdd(val clipId: String, val lane: LaneKind, val startMs: Long)

@Composable
fun EditorScreen(project: Project, onProject: (Project) -> Unit, onExport: () -> Unit) {
    var selectedClipId by remember(project.clips) { mutableStateOf(project.clips.firstOrNull()?.id) }
    var selectedSegment by remember { mutableStateOf<SegmentSelection?>(null) }
    var pendingAdd by remember { mutableStateOf<PendingSegmentAdd?>(null) }
    var tab by remember { mutableStateOf(EffectCategory.EFFECTS) }
    val currentClip = project.clips.firstOrNull { it.id == selectedClipId } ?: project.clips.firstOrNull()
    var playbackPositionMs by remember { mutableLongStateOf(0L) }

    Scaffold(bottomBar = {
        NavigationBar {
            EffectCategory.entries.forEach { category ->
                NavigationBarItem(
                    selected = tab == category,
                    onClick = {
                        tab = category
                        pendingAdd = null
                        if (category == EffectCategory.TRANSFORM && currentClip != null) {
                            selectedClipId = currentClip.id
                            selectedSegment = null
                        }
                    },
                    label = { Text(category.name.lowercase()) },
                    icon = {},
                )
            }
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
            if (currentClip == null) {
                Text("Import at least one clip to start editing.")
            } else {
                VideoPreview(currentClip, onPositionChanged = { playbackPositionMs = it })
                Spacer(Modifier.height(8.dp))
                TimelineView(
                    project = project,
                    selectedClipId = currentClip.id,
                    selectedSegment = selectedSegment,
                    playbackPositionMs = playbackPositionMs,
                    onClipSelected = {
                        selectedClipId = it
                        selectedSegment = null
                        pendingAdd = null
                        tab = EffectCategory.TRANSFORM
                    },
                    onEmptyLaneTapped = { clipId, lane, startMs ->
                        selectedClipId = clipId
                        selectedSegment = null
                        pendingAdd = PendingSegmentAdd(clipId, lane, startMs)
                        tab = if (lane == LaneKind.AUDIO) EffectCategory.AUDIO else EffectCategory.EFFECTS
                    },
                    onSegmentSelected = { selection ->
                        selectedClipId = selection.clipId
                        selectedSegment = selection
                        pendingAdd = null
                        tab = if (selection.lane == LaneKind.AUDIO) EffectCategory.AUDIO else EffectCategory.EFFECTS
                    },
                    onSegmentChanged = { clip -> onProject(project.copy(clips = project.clips.replaceClip(clip))) },
                )
                Spacer(Modifier.height(8.dp))
                EditPanel(
                    project = project,
                    selectedClipId = selectedClipId,
                    selectedSegment = selectedSegment,
                    pendingAdd = pendingAdd,
                    tab = tab,
                    onProject = onProject,
                    onPendingHandled = { pendingAdd = null },
                )
                Spacer(Modifier.height(8.dp))
                Button(onExport) { Text("Export") }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun VideoPreview(clip: Clip, onPositionChanged: (Long) -> Unit) {
    val context = LocalContext.current
    val player = remember { ExoPlayer.Builder(context).build() }
    DisposableEffect(player) { onDispose { player.release() } }
    LaunchedEffect(clip.sourceUri) {
        player.setMediaItem(MediaItem.fromUri(Uri.parse(clip.sourceUri)))
        player.prepare()
        player.playWhenReady = true
    }
    LaunchedEffect(player) {
        while (true) {
            onPositionChanged(player.currentPosition.coerceAtLeast(0L))
            delay(33)
        }
    }
    AndroidView(
        factory = { PlayerView(it).apply { this.player = player; useController = true } },
        update = { it.player = player },
        modifier = Modifier.fillMaxWidth().height(220.dp).background(Color.Black),
    )
}

@Composable
private fun TimelineView(
    project: Project,
    selectedClipId: String,
    selectedSegment: SegmentSelection?,
    playbackPositionMs: Long,
    onClipSelected: (String) -> Unit,
    onEmptyLaneTapped: (String, LaneKind, Long) -> Unit,
    onSegmentSelected: (SegmentSelection) -> Unit,
    onSegmentChanged: (Clip) -> Unit,
) {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val clipLayout = remember(project.clips) { project.clips.timelineLayout() }
    val totalWidth = with(density) { clipLayout.totalWidthDp(TimelineScale).toPx() }
    val playheadX = with(density) { (playbackPositionMs * TimelineScale.value).dp.toPx() } - scrollState.value

    Box(Modifier.fillMaxWidth().height(178.dp)) {
        Column(Modifier.horizontalScroll(scrollState)) {
            ClipStrip(clipLayout, selectedClipId, onClipSelected)
            TimelineLane(
                label = "Effects",
                clipLayout = clipLayout,
                lane = LaneKind.EFFECTS,
                selectedSegment = selectedSegment,
                onEmptyLaneTapped = onEmptyLaneTapped,
                onSegmentSelected = onSegmentSelected,
                onSegmentChanged = onSegmentChanged,
            )
            TimelineLane(
                label = "Audio",
                clipLayout = clipLayout,
                lane = LaneKind.AUDIO,
                selectedSegment = selectedSegment,
                onEmptyLaneTapped = onEmptyLaneTapped,
                onSegmentSelected = onSegmentSelected,
                onSegmentChanged = onSegmentChanged,
            )
        }
        Box(
            Modifier
                .offset { IntOffset(playheadX.roundToInt(), 0) }
                .width(2.dp)
                .height(168.dp)
                .background(Color.Black),
        )
        if (totalWidth <= 0f) Text("Timeline is empty")
    }
}

@Composable
private fun ClipStrip(layout: List<ClipTimeline>, selectedClipId: String, onClipSelected: (String) -> Unit) {
    Row(Modifier.height(48.dp)) {
        layout.forEachIndexed { index, item ->
            Box(
                Modifier
                    .width(item.widthDp)
                    .height(48.dp)
                    .background(if (index % 2 == 0) Color(0xFFE0E0E0) else Color(0xFFF5F5F5))
                    .border(2.dp, if (item.clip.id == selectedClipId) Color.Black else Color.Gray)
                    .clickable { onClipSelected(item.clip.id) },
                contentAlignment = Alignment.Center,
            ) {
                Text(item.clip.displayName(), maxLines = 1)
                Box(Modifier.align(Alignment.CenterEnd).width(1.dp).height(48.dp).background(Color.Black))
            }
        }
    }
}

@Composable
private fun TimelineLane(
    label: String,
    clipLayout: List<ClipTimeline>,
    lane: LaneKind,
    selectedSegment: SegmentSelection?,
    onEmptyLaneTapped: (String, LaneKind, Long) -> Unit,
    onSegmentSelected: (SegmentSelection) -> Unit,
    onSegmentChanged: (Clip) -> Unit,
) {
    val density = LocalDensity.current
    Box(
        Modifier
            .height(60.dp)
            .width(clipLayout.totalWidthDp(TimelineScale))
            .border(1.dp, Color.LightGray)
            .pointerInput(clipLayout, lane) {
                detectTapGestures { offset ->
                    clipLayout.clipAtPx(offset, density)?.let { hit -> onEmptyLaneTapped(hit.clip.id, lane, hit.localMs) }
                }
            },
    ) {
        Text(label, Modifier.align(Alignment.TopStart).background(Color.White).padding(horizontal = 4.dp))
        clipLayout.forEach { item ->
            val segments = if (lane == LaneKind.AUDIO) item.clip.audioSegments else item.clip.effectSegments
            segments.forEachIndexed { stackIndex, segment ->
                SegmentBlock(
                    item = item,
                    lane = lane,
                    segment = segment,
                    stackIndex = stackIndex,
                    selected = selectedSegment?.segmentId == segment.id,
                    onSelected = { onSegmentSelected(SegmentSelection(item.clip.id, lane, segment.id)) },
                    onSegmentResized = { resized ->
                        val updatedClip = if (lane == LaneKind.AUDIO) {
                            item.clip.copy(audioSegments = item.clip.audioSegments.replaceSegment(resized))
                        } else {
                            item.clip.copy(effectSegments = item.clip.effectSegments.replaceSegment(resized))
                        }
                        onSegmentChanged(updatedClip)
                    },
                )
            }
        }
    }
}

@Composable
private fun SegmentBlock(
    item: ClipTimeline,
    lane: LaneKind,
    segment: TimelineSegment,
    stackIndex: Int,
    selected: Boolean,
    onSelected: () -> Unit,
    onSegmentResized: (TimelineSegment) -> Unit,
) {
    val density = LocalDensity.current
    val leftDp = item.startDp + (segment.startMs.coerceAtLeast(0L) * TimelineScale.value).dp
    val widthDp = max(24f, ((segment.endMs - segment.startMs).coerceAtLeast(250L) * TimelineScale.value)).dp
    val topDp = (18 + (stackIndex % 2) * 18).dp
    Box(
        Modifier
            .offset(x = leftDp, y = topDp)
            .width(widthDp)
            .height(24.dp)
            .background(if (lane == LaneKind.AUDIO) Color(0xFFBBDEFB) else Color(0xFFC8E6C9))
            .border(2.dp, if (selected) Color.Black else Color.DarkGray)
            .clickable { onSelected() },
    ) {
        Text(segment.effectId, Modifier.align(Alignment.Center), maxLines = 1)
        ResizeHandle(Modifier.align(Alignment.CenterStart)) { deltaPx ->
            val deltaMs = with(density) { (deltaPx.toDp().value / TimelineScale.value).toLong() }
            val newStart = (segment.startMs + deltaMs).coerceIn(0L, segment.endMs - 250L)
            onSegmentResized(segment.copy(startMs = newStart))
        }
        ResizeHandle(Modifier.align(Alignment.CenterEnd)) { deltaPx ->
            val deltaMs = with(density) { (deltaPx.toDp().value / TimelineScale.value).toLong() }
            val newEnd = (segment.endMs + deltaMs).coerceAtLeast(segment.startMs + 250L)
            onSegmentResized(segment.copy(endMs = newEnd.coerceAtMost(item.durationMs)))
        }
    }
}

@Composable
private fun ResizeHandle(modifier: Modifier, onDragged: (Float) -> Unit) {
    Box(
        modifier
            .width(10.dp)
            .height(24.dp)
            .background(Color.Black.copy(alpha = 0.25f))
            .pointerInput(Unit) { detectDragGestures { change, dragAmount -> change.consume(); onDragged(dragAmount.x) } },
    )
}

@Composable
private fun EditPanel(
    project: Project,
    selectedClipId: String?,
    selectedSegment: SegmentSelection?,
    pendingAdd: PendingSegmentAdd?,
    tab: EffectCategory,
    onProject: (Project) -> Unit,
    onPendingHandled: () -> Unit,
) {
    val clip = project.clips.firstOrNull { it.id == selectedClipId }
    when {
        pendingAdd != null && clip != null -> EffectPicker(if (pendingAdd.lane == LaneKind.AUDIO) EffectCategory.AUDIO else EffectCategory.EFFECTS) { effectId ->
            val segment = TimelineSegment(UUID.randomUUID().toString(), effectId, pendingAdd.startMs, (pendingAdd.startMs + 1_000L).coerceAtMost(clip.durationMs()))
            val updated = if (pendingAdd.lane == LaneKind.AUDIO) clip.copy(audioSegments = clip.audioSegments + segment) else clip.copy(effectSegments = clip.effectSegments + segment)
            onProject(project.copy(clips = project.clips.replaceClip(updated)))
            onPendingHandled()
        }
        selectedSegment != null && clip != null -> SegmentParamPanel(clip, selectedSegment, project, onProject)
        tab == EffectCategory.TRANSFORM && clip != null -> TransformPanel(clip) { updated -> onProject(project.copy(clips = project.clips.replaceClip(updated))) }
        else -> Text("Tap a clip, segment, or empty lane region to edit.")
    }
}

@Composable
private fun SegmentParamPanel(clip: Clip, selection: SegmentSelection, project: Project, onProject: (Project) -> Unit) {
    val segment = if (selection.lane == LaneKind.AUDIO) clip.audioSegments.firstOrNull { it.id == selection.segmentId } else clip.effectSegments.firstOrNull { it.id == selection.segmentId }
    val effect = segment?.let { EffectRegistry.byId(it.effectId) }
    if (segment == null || effect == null) {
        Text("Select a segment to edit parameters.")
        return
    }
    Text(effect.displayName, style = MaterialTheme.typography.titleMedium)
    effect.params.forEach { param -> ParamInput(param, segment.params[param.id] ?: param.defaultValue) { value ->
        val updatedSegment = segment.copy(params = segment.params + (param.id to value))
        val updatedClip = if (selection.lane == LaneKind.AUDIO) clip.copy(audioSegments = clip.audioSegments.replaceSegment(updatedSegment)) else clip.copy(effectSegments = clip.effectSegments.replaceSegment(updatedSegment))
        onProject(project.copy(clips = project.clips.replaceClip(updatedClip)))
    } }
}

@Composable
fun TransformPanel(clip: Clip, onClip: (Clip) -> Unit) {
    Text("Transform: ${clip.displayName()}")
    Slider(clip.transform.scale, { onClip(clip.copy(transform = clip.transform.copy(scale = it))) }, valueRange = 0.1f..4f)
    Slider(clip.transform.rotationDegrees, { onClip(clip.copy(transform = clip.transform.copy(rotationDegrees = it))) }, valueRange = -180f..180f)
}

@Composable
fun EffectPicker(category: EffectCategory, onAdd: (String) -> Unit) {
    Column {
        Text("Add ${category.name.lowercase()} segment")
        EffectRegistry.byCategory(category).forEach { effect ->
            Card(Modifier.fillMaxWidth().padding(4.dp).clickable { onAdd(effect.id) }) {
                Text(effect.displayName, Modifier.padding(12.dp))
            }
        }
    }
}

@Composable
fun ParamInput(param: EffectParam, value: String = param.defaultValue, onValue: (String) -> Unit = {}) {
    OutlinedTextField(value, onValue, label = { Text(param.displayName) }, modifier = Modifier.fillMaxWidth())
}

@Composable
fun ClipCard(clip: Clip) { Card(Modifier.fillMaxWidth().padding(4.dp)) { Text(clip.displayName(), Modifier.padding(12.dp)) } }

@Composable
fun ExportScreen(project: Project) { Column(Modifier.padding(16.dp)) { Text("Export"); LinearProgressIndicator(progress = { 0f }); Text("${project.clips.size} clips ready for hard-cut export") } }

private data class ClipTimeline(val clip: Clip, val startMs: Long, val durationMs: Long, val startDp: androidx.compose.ui.unit.Dp, val widthDp: androidx.compose.ui.unit.Dp)
private data class TimelineHit(val clip: Clip, val localMs: Long)

private fun List<Clip>.timelineLayout(): List<ClipTimeline> {
    var timeCursor = 0L
    var dpCursor = 0.dp
    return map { clip ->
        val duration = clip.durationMs()
        val item = ClipTimeline(clip, timeCursor, duration, dpCursor, (duration * TimelineScale.value).dp)
        timeCursor += duration
        dpCursor += item.widthDp
        item
    }
}

private fun List<ClipTimeline>.totalWidthDp(scale: androidx.compose.ui.unit.Dp) = fold(0.dp) { acc, item -> acc + item.widthDp }

private fun List<ClipTimeline>.clipAtPx(offset: Offset, density: androidx.compose.ui.unit.Density): TimelineHit? = firstNotNullOfOrNull { item ->
    val startPx = with(density) { item.startDp.toPx() }
    val endPx = with(density) { (item.startDp + item.widthDp).toPx() }
    if (offset.x in startPx..endPx) {
        val localMs = with(density) { ((offset.x - startPx).toDp().value / TimelineScale.value).toLong() }.coerceIn(0L, item.durationMs)
        TimelineHit(item.clip, localMs)
    } else {
        null
    }
}

private fun Clip.durationMs(): Long = if (trimEndMs > trimStartMs) trimEndMs - trimStartMs else DEFAULT_CLIP_DURATION_MS
private fun Clip.displayName(): String = Uri.parse(sourceUri).lastPathSegment ?: sourceUri
private fun List<Clip>.replaceClip(updated: Clip): List<Clip> = map { if (it.id == updated.id) updated else it }
private fun List<TimelineSegment>.replaceSegment(updated: TimelineSegment): List<TimelineSegment> = map { if (it.id == updated.id) updated else it }
