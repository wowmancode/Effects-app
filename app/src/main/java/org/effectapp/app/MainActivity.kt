package org.effectapp.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.effectapp.effects.EffectCategory
import org.effectapp.effects.EffectParam
import org.effectapp.effects.EffectRegistry
import org.effectapp.model.Clip
import org.effectapp.model.Project
import org.effectapp.model.TimelineSegment
import org.effectapp.model.TransformSettings
import java.util.UUID

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

@Composable
fun EditorScreen(project: Project, onProject: (Project) -> Unit, onExport: () -> Unit) {
    var selectedClip by remember { mutableStateOf(project.clips.firstOrNull()?.id) }
    var tab by remember { mutableStateOf(EffectCategory.EFFECTS) }
    Scaffold(bottomBar = { NavigationBar { EffectCategory.entries.forEach { NavigationBarItem(selected = tab == it, onClick = { tab = it }, label = { Text(it.name.lowercase()) }, icon = {}) } } }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
            Text("Preview (ExoPlayer placeholder)", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text("Video strip: hard cuts only")
            Row(Modifier.fillMaxWidth()) { project.clips.forEach { clip -> Card(Modifier.weight(1f).padding(2.dp).clickable { selectedClip = clip.id }) { Text(if (clip.id == selectedClip) "Selected" else "Clip", Modifier.padding(12.dp)) } } }
            Text("Effects lane / Audio lane: overlapping segments scoped per clip")
            val clip = project.clips.firstOrNull { it.id == selectedClip }
            if (clip != null) {
                if (tab == EffectCategory.TRANSFORM) TransformPanel(clip) { updated -> onProject(project.copy(clips = project.clips.map { if (it.id == updated.id) updated else it })) }
                else EffectPicker(tab) { effectId ->
                    val segment = TimelineSegment(UUID.randomUUID().toString(), effectId, 0, 1_000)
                    val updated = if (tab == EffectCategory.AUDIO) clip.copy(audioSegments = clip.audioSegments + segment) else clip.copy(effectSegments = clip.effectSegments + segment)
                    onProject(project.copy(clips = project.clips.map { if (it.id == clip.id) updated else it }))
                }
            }
            Button(onExport) { Text("Export") }
        }
    }
}

@Composable
fun TransformPanel(clip: Clip, onClip: (Clip) -> Unit) {
    Text("Transform panel")
    Slider(clip.transform.scale, { onClip(clip.copy(transform = clip.transform.copy(scale = it))) }, valueRange = 0.1f..4f)
    Slider(clip.transform.rotationDegrees, { onClip(clip.copy(transform = clip.transform.copy(rotationDegrees = it))) }, valueRange = -180f..180f)
}

@Composable
fun EffectPicker(category: EffectCategory, onAdd: (String) -> Unit) {
    LazyColumn { items(EffectRegistry.byCategory(category)) { effect -> Card(Modifier.fillMaxWidth().padding(4.dp).clickable { onAdd(effect.id) }) { Text("Add ${effect.displayName}", Modifier.padding(12.dp)); effect.params.forEach { ParamInput(it) } } } }
}

@Composable
fun ParamInput(param: EffectParam) { OutlinedTextField(param.defaultValue, {}, label = { Text(param.displayName) }, modifier = Modifier.fillMaxWidth()) }

@Composable
fun ClipCard(clip: Clip) { Card(Modifier.fillMaxWidth().padding(4.dp)) { Text(Uri.parse(clip.sourceUri).lastPathSegment ?: clip.sourceUri, Modifier.padding(12.dp)) } }

@Composable
fun ExportScreen(project: Project) { Column(Modifier.padding(16.dp)) { Text("Export"); LinearProgressIndicator(0f); Text("${project.clips.size} clips ready for hard-cut export") } }
