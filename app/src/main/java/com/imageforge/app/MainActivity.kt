package com.imageforge.app

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.imageforge.app.ui.theme.ImageForgeTheme
import com.imageforge.app.image.ImageEngine
import com.imageforge.app.image.ImageProcessRequest
import com.imageforge.app.image.OutputFormat
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ImageForgeTheme { ImageForgeApp() } }
    }
}

enum class Destination(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Outlined.Home), Studio("Studio", Icons.Outlined.Tune),
    Batch("Batch", Icons.Outlined.Collections), Recipes("Recipes", Icons.Outlined.AutoAwesome),
    Settings("Settings", Icons.Outlined.Settings)
}

data class Goal(val title: String, val subtitle: String, val icon: ImageVector)
data class Recipe(val title: String, val detail: String, val icon: ImageVector)

@Composable
fun ImageForgeApp() {
    var destination by remember { mutableStateOf(Destination.Home) }
    var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var studioGoal by remember { mutableStateOf("Make File Smaller") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(50)) { selectedUris = it }
    val openPicker = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }

    Scaffold(
        bottomBar = {
            NavigationBar(tonalElevation = 3.dp) {
                Destination.entries.forEach { item ->
                    NavigationBarItem(
                        selected = destination == item,
                        onClick = { destination = item },
                        icon = { Icon(item.icon, item.label) },
                        label = { Text(item.label, maxLines = 1) }
                    )
                }
            }
        }
    ) { padding ->
        when (destination) {
            Destination.Home -> HomeScreen(Modifier.padding(padding), selectedUris, openPicker) { goal -> studioGoal = goal; destination = Destination.Studio }
            Destination.Studio -> StudioScreen(Modifier.padding(padding), selectedUris, studioGoal, openPicker)
            Destination.Batch -> BatchScreen(Modifier.padding(padding), selectedUris, openPicker)
            Destination.Recipes -> RecipesScreen(Modifier.padding(padding))
            Destination.Settings -> SettingsScreen(Modifier.padding(padding))
        }
    }
}

@Composable
private fun HomeScreen(modifier: Modifier, uris: List<Uri>, pick: () -> Unit, openGoal: (String) -> Unit) {
    val goals = listOf(
        Goal("Make File Smaller", "Smart compression with balanced quality", Icons.Outlined.Compress),
        Goal("Fit Upload Limit", "Hit a target like 100 KB or 1 MB", Icons.Outlined.TrackChanges),
        Goal("Prepare for Social", "Ready-to-share dimensions and formats", Icons.Outlined.Share),
        Goal("Convert Format", "JPG, PNG and WebP workflows", Icons.Outlined.SwapHoriz),
        Goal("Batch Process", "Apply one workflow to many images", Icons.Outlined.Collections),
        Goal("Protect Privacy", "Review and remove image metadata", Icons.Outlined.Security)
    )
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("IMAGEFORGE", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                Text("What do you want to create?", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Choose an outcome. ImageForge handles the technical settings.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) { Icon(Icons.Outlined.Bolt, null, Modifier.padding(13.dp), tint = MaterialTheme.colorScheme.primary) }
        }
        HeroPicker(uris, pick)
        Text("Goal mode", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        goals.forEach { goal -> GoalCard(goal) { openGoal(goal.title) } }
        OutputPredictorCard()
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun HeroPicker(uris: List<Uri>, pick: () -> Unit) {
    Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) { Icon(Icons.Outlined.AddPhotoAlternate, null, Modifier.padding(12.dp), tint = MaterialTheme.colorScheme.primary) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (uris.isEmpty()) "Start with your images" else "${uris.size} image${if (uris.size == 1) "" else "s"} ready", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Private selection with Android Photo Picker", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (uris.isNotEmpty()) UriPreview(uris.first())
            Button(onClick = pick, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.PhotoLibrary, null); Spacer(Modifier.width(8.dp)); Text(if (uris.isEmpty()) "Choose images" else "Change selection") }
        }
    }
}

@Composable
private fun UriPreview(uri: Uri) {
    AndroidView(
        factory = { context -> ImageView(context).apply { scaleType = ImageView.ScaleType.CENTER_CROP } },
        update = { it.setImageURI(uri) },
        modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(20.dp))
    )
}

@Composable
private fun GoalCard(goal: Goal, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(22.dp)) {
        Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.secondaryContainer) { Icon(goal.icon, null, Modifier.padding(11.dp), tint = MaterialTheme.colorScheme.primary) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) { Text(goal.title, fontWeight = FontWeight.SemiBold); Text(goal.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun OutputPredictorCard() {
    OutlinedCard(shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.Insights, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(8.dp)); Text("Output Predictor", fontWeight = FontWeight.Bold) }
            Text("Before processing, ImageForge will estimate file size, dimensions and expected savings.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { AssistChip(onClick = {}, label = { Text("Size estimate") }); AssistChip(onClick = {}, label = { Text("Quality") }) }
        }
    }
}

@Composable
private fun StudioScreen(modifier: Modifier, uris: List<Uri>, initialGoal: String, pick: () -> Unit) {
    val context = LocalContext.current
    var mode by remember(initialGoal) { mutableStateOf(initialGoal) }
    var quality by remember { mutableFloatStateOf(82f) }
    var format by remember { mutableStateOf(OutputFormat.JPEG) }
    var maxDimension by remember { mutableStateOf<Int?>(null) }
    var targetKb by remember { mutableFloatStateOf(500f) }
    var processing by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf<String?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) { onDispose { executor.shutdown() } }
    val targetMode = mode == "Fit Upload Limit"

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        ScreenHeader("Studio", "Smart target-size compression runs on-device", Icons.Outlined.Tune)
        if (uris.isEmpty()) EmptySelection(pick) else UriPreview(uris.first())
        Text("Mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Make File Smaller", "Fit Upload Limit", "Convert Format").forEach { label -> FilterChip(selected = mode == label, onClick = { mode = label }, label = { Text(label) }) }
        }
        Card(shape = RoundedCornerShape(24.dp)) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (targetMode) {
                Text("Target file size", fontWeight = FontWeight.SemiBold)
                Text("Under ${targetKb.toInt()} KB", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Slider(value = targetKb, onValueChange = { targetKb = it }, valueRange = 50f..2000f, steps = 38)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(100, 250, 500, 1024).forEach { kb -> FilterChip(selected = targetKb.toInt() == kb, onClick = { targetKb = kb.toFloat() }, label = { Text(if (kb == 1024) "1 MB" else "$kb KB") }) }
                }
                Text("ImageForge searches for the highest usable quality under the limit, then reduces dimensions only when needed.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text("Quality balance", fontWeight = FontWeight.SemiBold); Text("${quality.toInt()}%", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Slider(value = quality, onValueChange = { quality = it }, valueRange = 40f..100f)
            }
            HorizontalDivider()
            Text("Output format", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutputFormat.entries.forEach { f -> FilterChip(selected = format == f, onClick = { format = f }, enabled = !targetMode || f != OutputFormat.PNG, label = { Text(f.label) }) } }
            if (targetMode && format == OutputFormat.PNG) Text("Exact-size mode uses JPG or WebP because PNG quality is lossless.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            Text("Maximum dimension", fontWeight = FontWeight.SemiBold)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(null to "Original", 2048 to "2048 px", 1600 to "1600 px", 1080 to "1080 px").forEach { (value, label) -> FilterChip(selected = maxDimension == value, onClick = { maxDimension = value }, label = { Text(label) }) }
            }
        } }
        Button(
            onClick = {
                val source = uris.firstOrNull() ?: return@Button
                processing = true; resultText = null; errorText = null
                val targetBytes = if (targetMode) targetKb.toLong() * 1024L else null
                executor.execute {
                    runCatching { ImageEngine.processAndSave(context, ImageProcessRequest(source, quality.toInt(), maxDimension, format, targetBytes)) }
                        .onSuccess { r -> Handler(Looper.getMainLooper()).post {
                            processing = false
                            val targetStatus = if (r.targetBytes != null) if (r.targetMet) " • target met" else " • closest result"
                            else ""
                            resultText = "Saved • ${formatBytes(r.bytes)} • ${r.width}×${r.height} • ${r.format.label} • Q${r.qualityUsed}$targetStatus"
                        } }
                        .onFailure { e -> Handler(Looper.getMainLooper()).post { processing = false; errorText = e.message ?: "Processing failed" } }
                }
            },
            enabled = uris.isNotEmpty() && !processing && (!targetMode || format != OutputFormat.PNG),
            modifier = Modifier.fillMaxWidth().height(54.dp)
        ) { if (processing) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Icon(Icons.Outlined.AutoFixHigh, null); Spacer(Modifier.width(8.dp)); Text(if (processing) "Solving…" else if (targetMode) "Fit target & save" else "Optimize & save") }
        resultText?.let { Text(it, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold) }
        errorText?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Text("Processing stays on your device. Outputs are saved to Pictures/ImageForge.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> "%.2f MB".format(bytes / (1024f * 1024f))
    bytes >= 1024 -> "%.0f KB".format(bytes / 1024f)
    else -> "$bytes B"
}

@Composable private fun Stat(label: String, value: String) { Column { Text(value, fontWeight = FontWeight.Bold); Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }

@Composable
private fun BatchScreen(modifier: Modifier, uris: List<Uri>, pick: () -> Unit) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        ScreenHeader("Batch Studio", "One workflow for multiple images", Icons.Outlined.Collections)
        Card(shape = RoundedCornerShape(24.dp)) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (uris.isEmpty()) "No batch selected" else "${uris.size} images selected", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Free supports up to 5 images per batch. Unlimited batch processing is a Pro feature.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = pick, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.AddPhotoAlternate, null); Spacer(Modifier.width(8.dp)); Text("Select images") }
        } }
        SettingRow(Icons.Outlined.AspectRatio, "Resize", "Keep original dimensions")
        SettingRow(Icons.Outlined.SwapHoriz, "Format", "Keep original format")
        SettingRow(Icons.Outlined.TrackChanges, "Maximum size", "No limit")
        SettingRow(Icons.Outlined.Badge, "Rename", "Original filenames")
        Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text("Process batch — planned for V0.5") }
    }
}

@Composable
private fun RecipesScreen(modifier: Modifier) {
    val recipes = listOf(Recipe("Web Ready", "WebP • max 1600 px • balanced quality", Icons.Outlined.Language), Recipe("Marketplace Square", "1600 × 1600 • JPG • metadata off", Icons.Outlined.Storefront), Recipe("Private Share", "Keep size • remove metadata", Icons.Outlined.Security))
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ScreenHeader("Recipes", "Save repeatable image workflows", Icons.Outlined.AutoAwesome)
        recipes.forEach { r -> Card(shape = RoundedCornerShape(22.dp)) { Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) { Icon(r.icon, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) { Text(r.title, fontWeight = FontWeight.Bold); Text(r.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Icon(Icons.Outlined.Lock, "Pro", tint = MaterialTheme.colorScheme.onSurfaceVariant) } } }
        OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(8.dp)); Text("Create recipe — Pro") }
    }
}

@Composable
private fun SettingsScreen(modifier: Modifier) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ScreenHeader("Settings", "Privacy-first defaults", Icons.Outlined.Settings)
        SettingRow(Icons.Outlined.Security, "Privacy", "Processing will stay on-device")
        SettingRow(Icons.Outlined.FolderOpen, "Export", "Choose destination when saving")
        SettingRow(Icons.Outlined.DarkMode, "Appearance", "System-ready theme foundation")
        SettingRow(Icons.Outlined.Info, "About ImageForge", "Version 0.4.0 • Smart Target Size")
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer), shape = RoundedCornerShape(22.dp)) { Column(Modifier.padding(18.dp)) { Text("ImageForge Pro", fontWeight = FontWeight.Bold); Text("Planned lifetime unlock: unlimited batch, recipes, advanced workflows and no ads.", color = MaterialTheme.colorScheme.onSecondaryContainer) } }
    }
}

@Composable
private fun ScreenHeader(title: String, subtitle: String, icon: ImageVector) { Row(verticalAlignment = Alignment.CenterVertically) { Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer) { Icon(icon, null, Modifier.padding(12.dp), tint = MaterialTheme.colorScheme.primary) }; Spacer(Modifier.width(12.dp)); Column { Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }

@Composable
private fun EmptySelection(pick: () -> Unit) { OutlinedCard(shape = RoundedCornerShape(22.dp)) { Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Outlined.ImageSearch, null, Modifier.size(38.dp), tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(8.dp)); Text("Choose an image to preview", fontWeight = FontWeight.Bold); Spacer(Modifier.height(10.dp)); Button(onClick = pick) { Text("Choose image") } } } }

@Composable
private fun SettingRow(icon: ImageVector, title: String, subtitle: String) { Card(shape = RoundedCornerShape(20.dp)) { Row(Modifier.fillMaxWidth().padding(17.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.SemiBold); Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) } } }
