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
import com.imageforge.app.image.MetadataEngine
import com.imageforge.app.image.ImageMetadata
import com.imageforge.app.image.ImageRecipe
import com.imageforge.app.image.RecipeEngine
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

@Composable
fun ImageForgeApp() {
    var destination by remember { mutableStateOf(Destination.Home) }
    var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var studioGoal by remember { mutableStateOf("Make File Smaller") }
    var pendingRecipe by remember { mutableStateOf<ImageRecipe?>(null) }
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
            Destination.Studio -> StudioScreen(Modifier.padding(padding), selectedUris, studioGoal, openPicker, pendingRecipe) { pendingRecipe = null }
            Destination.Batch -> BatchScreen(Modifier.padding(padding), selectedUris, openPicker)
            Destination.Recipes -> RecipesScreen(Modifier.padding(padding)) { recipe -> pendingRecipe = recipe; studioGoal = if (recipe.removeMetadata) "Protect Privacy" else "Make File Smaller"; destination = Destination.Studio }
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
private fun StudioScreen(modifier: Modifier, uris: List<Uri>, initialGoal: String, pick: () -> Unit, appliedRecipe: ImageRecipe?, onRecipeConsumed: () -> Unit) {
    val context = LocalContext.current
    var mode by remember(initialGoal) { mutableStateOf(initialGoal) }
    var quality by remember { mutableFloatStateOf(82f) }
    var format by remember { mutableStateOf(OutputFormat.JPEG) }
    var maxDimension by remember { mutableStateOf<Int?>(null) }
    var targetKb by remember { mutableFloatStateOf(500f) }
    var processing by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf<String?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var outputUri by remember { mutableStateOf<Uri?>(null) }
    var actualBytes by remember { mutableStateOf<Long?>(null) }
    var actualWidth by remember { mutableStateOf<Int?>(null) }
    var actualHeight by remember { mutableStateOf<Int?>(null) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) { onDispose { executor.shutdown() } }
    val targetMode = mode == "Fit Upload Limit"
    val privacyMode = mode == "Protect Privacy"
    val source = uris.firstOrNull()
    val targetBytes = if (targetMode) targetKb.toLong() * 1000L else null
    val request = source?.let { ImageProcessRequest(it, quality.toInt(), maxDimension, format, targetBytes) }
    val prediction = remember(source, quality.toInt(), maxDimension, format, targetBytes) {
        request?.let { runCatching { ImageEngine.predict(context, it) }.getOrNull() }
    }
    val original = remember(source) { source?.let { runCatching { ImageEngine.inspect(context, it) }.getOrNull() } }
    val metadata = remember(source) { source?.let { runCatching { MetadataEngine.read(context, it) }.getOrNull() } }

    LaunchedEffect(appliedRecipe?.id) {
        appliedRecipe?.let { recipe ->
            mode = if (recipe.removeMetadata) "Protect Privacy" else "Make File Smaller"
            quality = recipe.quality.toFloat()
            format = recipe.format
            maxDimension = recipe.maxDimension
            outputUri = null; actualBytes = null; actualWidth = null; actualHeight = null; resultText = null
            onRecipeConsumed()
        }
    }

    LaunchedEffect(source) { outputUri = null; actualBytes = null; actualWidth = null; actualHeight = null; resultText = null }

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        ScreenHeader("Studio", "Preview the result before you save", Icons.Outlined.Tune)
        if (source == null) EmptySelection(pick) else {
            if (outputUri == null) UriPreview(source) else BeforeAfterPreview(source, outputUri!!)
        }

        if (source != null && privacyMode) {
            PrivacyMetadataCard(metadata)
        }

        if (source != null && prediction != null && original != null && !privacyMode) {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.Insights, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(8.dp)); Text(if (actualBytes == null) "Output Predictor" else "Actual Output", fontWeight = FontWeight.Bold) }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Stat("Original", formatBytes(original.bytes))
                        Stat(if (actualBytes == null) "Estimated" else "Actual", formatBytes(actualBytes ?: prediction.estimatedBytes))
                        val saving = if (actualBytes != null && original.bytes > 0) ((1f - actualBytes!!.toFloat()/original.bytes) * 100).toInt() else prediction.savingsPercent
                        Stat("Savings", "$saving%")
                    }
                    val w = actualWidth ?: prediction.width; val h = actualHeight ?: prediction.height
                    Text("${w}×${h} • ${format.label}${if (actualBytes == null) " • estimate" else " • measured"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    if (actualBytes == null) Text("Estimate is a planning aid; final encoded size can differ by image content.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
        }

        Text("Mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Make File Smaller", "Fit Upload Limit", "Convert Format", "Protect Privacy").forEach { label -> FilterChip(selected = mode == label, onClick = { mode = label; outputUri = null; actualBytes = null }, label = { Text(label) }) }
        }
        Card(shape = RoundedCornerShape(24.dp)) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (privacyMode) {
                Text("Privacy-safe copy", fontWeight = FontWeight.SemiBold)
                Text("ImageForge re-encodes the image without EXIF metadata such as GPS, camera model, capture date and software tags.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else if (targetMode) {
                Text("Target file size", fontWeight = FontWeight.SemiBold)
                Text("Under ${targetKb.toInt()} KB", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Slider(value = targetKb, onValueChange = { targetKb = it; outputUri = null; actualBytes = null }, valueRange = 50f..2000f, steps = 38)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(100, 250, 500, 1024).forEach { kb -> FilterChip(selected = targetKb.toInt() == kb, onClick = { targetKb = kb.toFloat(); outputUri = null; actualBytes = null }, label = { Text(if (kb == 1024) "1 MB" else "$kb KB") }) }
                }
            } else {
                Text("Quality balance", fontWeight = FontWeight.SemiBold); Text("${quality.toInt()}%", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Slider(value = quality, onValueChange = { quality = it; outputUri = null; actualBytes = null }, valueRange = 40f..100f)
            }
            HorizontalDivider()
            Text("Output format", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutputFormat.entries.forEach { f -> FilterChip(selected = format == f, onClick = { format = f; outputUri = null; actualBytes = null }, enabled = !targetMode || f != OutputFormat.PNG, label = { Text(f.label) }) } }
            Text("Maximum dimension", fontWeight = FontWeight.SemiBold)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(null to "Original", 2048 to "2048 px", 1600 to "1600 px", 1080 to "1080 px").forEach { (value, label) -> FilterChip(selected = maxDimension == value, onClick = { maxDimension = value; outputUri = null; actualBytes = null }, label = { Text(label) }) }
            }
        } }
        Button(
            onClick = {
                val req = request ?: return@Button
                processing = true; resultText = null; errorText = null
                executor.execute {
                    runCatching { ImageEngine.processAndSave(context, req) }
                        .onSuccess { r -> Handler(Looper.getMainLooper()).post {
                            processing = false; outputUri = r.outputUri; actualBytes = r.bytes; actualWidth = r.width; actualHeight = r.height
                            val targetStatus = if (r.targetBytes != null) if (r.targetMet) " • target met" else " • closest result" else ""
                            resultText = if (privacyMode) "Privacy-safe copy saved • metadata removed • ${formatBytes(r.bytes)}" else "Saved • ${formatBytes(r.bytes)} • ${r.width}×${r.height} • ${r.format.label} • Q${r.qualityUsed}$targetStatus"
                        } }
                        .onFailure { e -> Handler(Looper.getMainLooper()).post { processing = false; errorText = e.message ?: "Processing failed" } }
                }
            },
            enabled = source != null && !processing && (!targetMode || format != OutputFormat.PNG),
            modifier = Modifier.fillMaxWidth().height(54.dp)
        ) { if (processing) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Icon(Icons.Outlined.AutoFixHigh, null); Spacer(Modifier.width(8.dp)); Text(if (processing) "Processing…" else if (privacyMode) "Remove metadata & save" else if (targetMode) "Fit target & compare" else "Optimize & compare") }
        resultText?.let { Text(it, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold) }
        errorText?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Text("Processing stays on your device. Outputs are saved to Pictures/ImageForge.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun BeforeAfterPreview(before: Uri, after: Uri) {
    var reveal by remember(before, after) { mutableFloatStateOf(0.5f) }
    Card(shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.fillMaxWidth().height(230.dp).clip(RoundedCornerShape(18.dp))) {
                UriImage(before, Modifier.matchParentSize())
                Box(Modifier.fillMaxHeight().fillMaxWidth(reveal).clip(RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp))) { UriImage(after, Modifier.matchParentSize()) }
                Surface(Modifier.align(Alignment.TopStart).padding(8.dp), shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f)) { Text("AFTER", Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
                Surface(Modifier.align(Alignment.TopEnd).padding(8.dp), shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f)) { Text("BEFORE", Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
            }
            Slider(value = reveal, onValueChange = { reveal = it }, valueRange = 0.05f..0.95f)
            Text("Drag to compare processed and original image", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun UriImage(uri: Uri, modifier: Modifier = Modifier) {
    AndroidView(factory = { context -> ImageView(context).apply { scaleType = ImageView.ScaleType.CENTER_CROP } }, update = { it.setImageURI(uri) }, modifier = modifier)
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000 -> "%.2f MB".format(bytes / 1_000_000f)
    bytes >= 1_000 -> "%.0f KB".format(bytes / 1_000f)
    else -> "$bytes B"
}

@Composable private fun Stat(label: String, value: String) { Column { Text(value, fontWeight = FontWeight.Bold); Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }

@Composable
private fun PrivacyMetadataCard(metadata: ImageMetadata?) {
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Security, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Privacy scan", fontWeight = FontWeight.Bold)
            }
            if (metadata == null) {
                Text("Metadata could not be read from this image.", color = MaterialTheme.colorScheme.onSecondaryContainer)
            } else if (metadata.presentCount == 0) {
                Text("No supported privacy-sensitive EXIF fields were found.", color = MaterialTheme.colorScheme.onSecondaryContainer)
            } else {
                Text("${metadata.presentCount} metadata group${if (metadata.presentCount == 1) "" else "s"} found", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                MetadataRow("Location", if (metadata.hasLocation) "${"%.5f".format(metadata.latitude)}, ${"%.5f".format(metadata.longitude)}" else "Not present")
                MetadataRow("Device", listOfNotNull(metadata.make, metadata.model).joinToString(" ").ifBlank { "Not present" })
                MetadataRow("Lens", metadata.lensModel ?: "Not present")
                MetadataRow("Captured", metadata.dateTime ?: "Not present")
                MetadataRow("Software", metadata.software ?: "Not present")
            }
            Text("Remove Metadata creates a new re-encoded copy. Your original image is not modified.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, modifier = Modifier.widthIn(max = 210.dp), maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun BatchScreen(modifier: Modifier, uris: List<Uri>, pick: () -> Unit) {
    val context = LocalContext.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    var quality by remember { mutableFloatStateOf(82f) }
    var format by remember { mutableStateOf(OutputFormat.JPEG) }
    var maxDimension by remember { mutableStateOf<Int?>(null) }
    var processing by remember { mutableStateOf(false) }
    var completed by remember { mutableIntStateOf(0) }
    var successCount by remember { mutableIntStateOf(0) }
    var failedCount by remember { mutableIntStateOf(0) }
    var resultText by remember { mutableStateOf<String?>(null) }
    val batch = uris.take(5)

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        ScreenHeader("Batch Studio", "Process up to 5 images in one workflow", Icons.Outlined.Collections)
        Card(shape = RoundedCornerShape(24.dp)) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (uris.isEmpty()) "No batch selected" else "${uris.size} images selected • ${batch.size} queued", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(if (uris.size > 5) "Free mode will process the first 5 images. Unlimited batches will be a Pro feature." else "Free mode supports up to 5 images per batch.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = pick, enabled = !processing, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.AddPhotoAlternate, null); Spacer(Modifier.width(8.dp)); Text("Select images") }
        } }

        Card(shape = RoundedCornerShape(24.dp)) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Batch settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Quality ${quality.toInt()}%", fontWeight = FontWeight.SemiBold)
            Slider(value = quality, onValueChange = { quality = it }, valueRange = 40f..100f, enabled = !processing && format != OutputFormat.PNG)
            Text("Output format", fontWeight = FontWeight.SemiBold)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutputFormat.entries.forEach { f -> FilterChip(selected = format == f, onClick = { format = f }, enabled = !processing, label = { Text(f.label) }) }
            }
            Text("Maximum dimension", fontWeight = FontWeight.SemiBold)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(null to "Original", 2048 to "2048 px", 1600 to "1600 px", 1080 to "1080 px").forEach { (value, label) ->
                    FilterChip(selected = maxDimension == value, onClick = { maxDimension = value }, enabled = !processing, label = { Text(label) })
                }
            }
        } }

        if (processing) {
            LinearProgressIndicator(progress = { if (batch.isEmpty()) 0f else completed.toFloat() / batch.size }, modifier = Modifier.fillMaxWidth())
            Text("Processing $completed / ${batch.size} • saved $successCount • failed $failedCount", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Button(
            onClick = {
                if (batch.isEmpty()) return@Button
                processing = true; completed = 0; successCount = 0; failedCount = 0; resultText = null
                val requestQuality = quality.toInt(); val requestFormat = format; val requestMax = maxDimension
                executor.execute {
                    var ok = 0; var failed = 0
                    batch.forEachIndexed { index, uri ->
                        runCatching { ImageEngine.processAndSave(context, ImageProcessRequest(uri, requestQuality, requestMax, requestFormat)) }
                            .onSuccess { ok++ }.onFailure { failed++ }
                        val done = index + 1; val okNow = ok; val failNow = failed
                        Handler(Looper.getMainLooper()).post { completed = done; successCount = okNow; failedCount = failNow }
                    }
                    Handler(Looper.getMainLooper()).post {
                        processing = false
                        resultText = "Batch complete • $ok saved${if (failed > 0) " • $failed failed" else ""} • Pictures/ImageForge"
                    }
                }
            },
            enabled = batch.isNotEmpty() && !processing,
            modifier = Modifier.fillMaxWidth().height(54.dp)
        ) {
            if (processing) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Icon(Icons.Outlined.Collections, null)
            Spacer(Modifier.width(8.dp)); Text(if (processing) "Processing batch…" else "Process ${batch.size} images")
        }
        resultText?.let { Text(it, color = if (failedCount == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold) }
        Text("Each image is processed sequentially to reduce peak memory use. Outputs stay on-device and are saved to Pictures/ImageForge.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RecipesScreen(modifier: Modifier, applyRecipe: (ImageRecipe) -> Unit) {
    val context = LocalContext.current
    var custom by remember { mutableStateOf(RecipeEngine.loadCustom(context)) }
    var showCreator by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var format by remember { mutableStateOf(OutputFormat.JPEG) }
    var quality by remember { mutableFloatStateOf(85f) }
    var maxDimension by remember { mutableStateOf<Int?>(1600) }
    var removeMetadata by remember { mutableStateOf(false) }
    val all = RecipeEngine.builtIns + custom

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ScreenHeader("Recipes", "One tap applies a repeatable workflow", Icons.Outlined.AutoAwesome)
        Text("Built-in and saved recipes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        all.forEach { recipe ->
            Card(shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (recipe.removeMetadata) Icons.Outlined.Security else Icons.Outlined.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(recipe.name, fontWeight = FontWeight.Bold)
                            Text(recipe.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (recipe.builtIn) AssistChip(onClick = {}, label = { Text("Built-in") })
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { applyRecipe(recipe) }, modifier = Modifier.weight(1f)) { Text("Apply in Studio") }
                        if (!recipe.builtIn) OutlinedButton(onClick = { RecipeEngine.delete(context, recipe.id); custom = RecipeEngine.loadCustom(context) }) { Icon(Icons.Outlined.Delete, "Delete") }
                    }
                }
            }
        }
        OutlinedButton(onClick = { showCreator = !showCreator }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(8.dp)); Text(if (showCreator) "Close creator" else "Create custom recipe") }
        if (showCreator) {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("New recipe", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = name, onValueChange = { name = it.take(32) }, label = { Text("Recipe name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Text("Format", fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutputFormat.entries.forEach { f -> FilterChip(selected = format == f, onClick = { format = f }, label = { Text(f.label) }) } }
                    Text("Quality ${quality.toInt()}%", fontWeight = FontWeight.SemiBold)
                    Slider(value = quality, onValueChange = { quality = it }, valueRange = 40f..100f, enabled = format != OutputFormat.PNG)
                    Text("Maximum dimension", fontWeight = FontWeight.SemiBold)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(null to "Original", 2048 to "2048", 1600 to "1600", 1080 to "1080").forEach { (v, label) -> FilterChip(selected = maxDimension == v, onClick = { maxDimension = v }, label = { Text(label) }) }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) { Switch(checked = removeMetadata, onCheckedChange = { removeMetadata = it }); Spacer(Modifier.width(10.dp)); Text("Remove metadata") }
                    Button(onClick = {
                        val cleanName = name.trim()
                        if (cleanName.isNotEmpty()) {
                            RecipeEngine.save(context, ImageRecipe("custom-${System.currentTimeMillis()}", cleanName, format, quality.toInt(), maxDimension, removeMetadata))
                            custom = RecipeEngine.loadCustom(context); name = ""; showCreator = false
                        }
                    }, enabled = name.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.Save, null); Spacer(Modifier.width(8.dp)); Text("Save recipe") }
                }
            }
        }
        Text("Recipes are stored locally on this device. Applying a recipe loads its settings into Studio before processing.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingsScreen(modifier: Modifier) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ScreenHeader("Settings", "Privacy-first defaults", Icons.Outlined.Settings)
        SettingRow(Icons.Outlined.Security, "Privacy", "Processing will stay on-device")
        SettingRow(Icons.Outlined.FolderOpen, "Export", "Choose destination when saving")
        SettingRow(Icons.Outlined.DarkMode, "Appearance", "System-ready theme foundation")
        SettingRow(Icons.Outlined.Info, "About ImageForge", "Version 0.8.0 • Recipes / Workflow Engine")
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer), shape = RoundedCornerShape(22.dp)) { Column(Modifier.padding(18.dp)) { Text("ImageForge Pro", fontWeight = FontWeight.Bold); Text("Planned lifetime unlock: unlimited batch, recipes, advanced workflows and no ads.", color = MaterialTheme.colorScheme.onSecondaryContainer) } }
    }
}

@Composable
private fun ScreenHeader(title: String, subtitle: String, icon: ImageVector) { Row(verticalAlignment = Alignment.CenterVertically) { Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer) { Icon(icon, null, Modifier.padding(12.dp), tint = MaterialTheme.colorScheme.primary) }; Spacer(Modifier.width(12.dp)); Column { Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }

@Composable
private fun EmptySelection(pick: () -> Unit) { OutlinedCard(shape = RoundedCornerShape(22.dp)) { Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Outlined.ImageSearch, null, Modifier.size(38.dp), tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(8.dp)); Text("Choose an image to preview", fontWeight = FontWeight.Bold); Spacer(Modifier.height(10.dp)); Button(onClick = pick) { Text("Choose image") } } } }

@Composable
private fun SettingRow(icon: ImageVector, title: String, subtitle: String) { Card(shape = RoundedCornerShape(20.dp)) { Row(Modifier.fillMaxWidth().padding(17.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.SemiBold); Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) } } }
