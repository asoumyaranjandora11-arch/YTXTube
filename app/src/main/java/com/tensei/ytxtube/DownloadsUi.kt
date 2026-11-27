@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.tensei.ytxtube

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed as staggeredItemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadQualitySheet(video: VideoItem, onDownloadStarted: ((String) -> Unit)? = null, onDismiss: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val context = LocalContext.current
    var options by remember(video.id) { mutableStateOf<List<DownloadOption>?>(null) }
    var error by remember(video.id) { mutableStateOf<String?>(null) }
    var selected by remember(video.id) { mutableStateOf<Int?>(null) }

    LaunchedEffect(video.id) {
        runCatching { Downloads.options(video.id) }
            .onSuccess { list ->
                options = list
                // Highest available by default: a 4K video should offer 4K, not 720p.
                selected = list.firstOrNull()?.height
            }
            .onFailure { error = it.message ?: "Could not load the available qualities" }
    }

    GlassModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 26.dp),
        ) {
            Text(
                "Download video",
                fontFamily = RoundedHeavy,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
            )
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Thumbnail(video.thumbnail, Modifier.size(width = 112.dp, height = 63.dp), radius = 12, videoId = video.id)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        video.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        video.channel,
                        color = c.onSurfaceVariant,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            when {
                error != null -> Text(
                    error!!,
                    color = c.error,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
                options == null -> Box(
                    Modifier.fillMaxWidth().padding(vertical = 28.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(30.dp), strokeWidth = 3.dp)
                }
                options!!.isEmpty() -> Text(
                    "This video has no downloadable qualities.",
                    color = c.onSurfaceVariant,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        options!!.forEach { option ->
                            QualityChoiceRow(option, selected == option.height) { selected = option.height }
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    val choice = options!!.firstOrNull { it.height == selected }
                    Button(
                        onClick = {
                            if (choice != null) {
                                val added = Downloads.enqueue(video, choice)
                                onDownloadStarted?.invoke(if (added) "Downloading ${choice.label} • ${video.title}" else "Already in downloads")
                                onDismiss()
                            }
                        },
                        enabled = choice != null,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = c.primary, contentColor = c.onPrimary),
                    ) {
                        Icon(Icons.Rounded.Download, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (choice == null) "Download" else "Download ${choice.label}${sizeSuffix(choice.estimatedBytes)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QualityChoiceRow(option: DownloadOption, selected: Boolean, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressable(pressedScale = 0.98f, onClick = onClick)
            .bubbleGlassSurface(RoundedCornerShape(24.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (selected) c.primary else c.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(option.label, fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
            val subtitle = buildString {
                qualityTag(option.height)?.let { append(it) }
                if (option.estimatedBytes > 0) {
                    if (isNotEmpty()) append(" · ")
                    append(formatBytes(option.estimatedBytes))
                }
                if (isNotEmpty()) append(" · ")
                append(option.containerLabel)
                append(if (option.needsMux) " · video + audio" else " · with audio")
            }
            Text(subtitle, color = c.onSurfaceVariant, fontSize = 11.5.sp)
        }
    }
    Spacer(Modifier.height(6.dp))
}


@Composable
fun DownloadsScreen(
    contentPadding: PaddingValues,
    onOpen: (VideoItem) -> Unit,
    onOpenMusic: (VideoItem) -> Unit,
    onBrowse: () -> Unit,
    onBrowseMusic: () -> Unit,
    onSearch: () -> Unit,
    onAccount: () -> Unit,
    scrollToTop: Int = 0,
) {
    val tasks = Downloads.tasks


    var section by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(0) }
    val videos = tasks.filter { it.height > 0 }
    val music = tasks.filter { it.height == 0 }
    val shown = if (section == 0) videos else music
    Box(Modifier.fillMaxSize().appScreenBackground()) {
    CollapsingHeaderLayout(
        resetKey = scrollToTop to listOf(tasks.size, tasks.count { it.status == DownloadStatus.Complete }),
        header = { ScreenHeader(title = "Downloads") { HeaderActions(onSearch, onAccount) } },
    ) { top ->
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Adaptive(180.dp),
            contentPadding = contentPadding.withTop(top),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalItemSpacing = 12.dp,
        ) {
            item(key = "sections", span = StaggeredGridItemSpan.FullLine) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DownloadSectionPill("Videos", videos.size, section == 0, Modifier.weight(1f)) { section = 0 }
                    DownloadSectionPill("Music", music.size, section == 1, Modifier.weight(1f)) { section = 1 }
                }
            }
            if (shown.isEmpty()) {
                item(key = "empty", span = StaggeredGridItemSpan.FullLine) {
                    Box(Modifier.fillMaxWidth().padding(top = 28.dp), contentAlignment = Alignment.Center) {
                        EmptyState(
                            icon = if (section == 0) Icons.Rounded.Download else Icons.Rounded.MusicNote,
                            title = if (section == 0) "No videos downloaded yet" else "No music downloaded yet",
                            body = if (section == 0) {
                                "Videos you download are saved here and play without a connection."
                            } else {
                                "Songs you download from Music are saved here for offline listening."
                            },
                            action = {
                                PillButton(
                                    if (section == 0) "Browse videos" else "Browse music",
                                    if (section == 0) onBrowse else onBrowseMusic,
                                )
                            },
                        )
                    }
                }
            } else {
                val active = shown.filter { it.status != DownloadStatus.Complete }
                val finished = shown.filter { it.status == DownloadStatus.Complete }
                if (active.isNotEmpty()) {
                    item(key = "activeTitle", span = StaggeredGridItemSpan.FullLine) { SectionTitle("Downloading") }
                    staggeredItemsIndexed(active, key = { _, task -> "a-" + task.key }) { index, task ->
                        DownloadRow(
                            task,
                            onOpen = null,
                            thumbnailAspectRatio = bentoVideoAspectRatio(index),
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
                if (finished.isNotEmpty()) {
                    item(key = "doneTitle", span = StaggeredGridItemSpan.FullLine) { SectionTitle(if (section == 0) "Videos" else "Music") }
                    staggeredItemsIndexed(finished, key = { _, task -> "d-" + task.key }) { index, task ->
                        DownloadRow(
                            task,
                            thumbnailAspectRatio = bentoVideoAspectRatio(active.size + index),
                            modifier = Modifier.animateItem(),
                            onOpen = {
                                if (section == 0) onOpen(task.toVideoItem())
                                else onOpenMusic(task.toVideoItem())
                            },
                        )
                    }
                    item(key = "footer", span = StaggeredGridItemSpan.FullLine) { Spacer(Modifier.height(20.dp)) }
                }
            }
            }
        }
    }
}

@Composable
private fun DownloadSectionPill(
    label: String,
    count: Int,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier
            .bubbleGlassSurface(RoundedCornerShape(24.dp))
            .pressable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            color = if (selected) GlassmorphicTheme.goldStart else Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
        if (count > 0) {
            Spacer(Modifier.width(6.dp))
            Text(
                count.toString(),
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun DownloadRow(
    task: DownloadTask,
    onOpen: (() -> Unit)?,
    thumbnailAspectRatio: Float,
    modifier: Modifier = Modifier,
) {
    val c = MaterialTheme.colorScheme
    var menu by remember(task.key) { mutableStateOf(false) }
    val running = task.status == DownloadStatus.Downloading || task.status == DownloadStatus.Queued ||
        task.status == DownloadStatus.Processing
    val compactLayout = thumbnailAspectRatio <= 1f

    Column(
        modifier
            .fillMaxWidth()
            .padding(4.dp)
            .bubbleGlassSurface(RoundedCornerShape(24.dp))
            .pressable(enabled = onOpen != null, pressedScale = 0.985f) { onOpen?.invoke() }
            .padding(12.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(thumbnailAspectRatio)
                .clip(RoundedCornerShape(16.dp)),
        ) {
            Thumbnail(task.thumbnail, Modifier.fillMaxSize(), radius = 0, videoId = task.videoId)
            if (compactLayout) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(96.dp)
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                0.35f to Color.Black.copy(alpha = 0.48f),
                                1f to Color.Black.copy(alpha = 0.88f),
                            ),
                        ),
                )
                Text(
                    task.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(10.dp),
                )
            }
            DurationPill(
                task.durationSec,
                Modifier.align(if (compactLayout) Alignment.TopStart else Alignment.BottomEnd).padding(6.dp),
                small = true,
            )
            Box(Modifier.align(Alignment.TopEnd)) {
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { menu = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.MoreVert, "More options", tint = Color.White, modifier = Modifier.size(20.dp))
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    when (task.status) {
                        DownloadStatus.Downloading, DownloadStatus.Queued, DownloadStatus.Processing -> DropdownMenuItem(
                            text = { Text("Pause") },
                            onClick = { menu = false; Downloads.pause(task) },
                        )
                        DownloadStatus.Paused, DownloadStatus.Failed -> DropdownMenuItem(
                            text = { Text("Resume") },
                            onClick = { menu = false; Downloads.resume(task) },
                        )
                        DownloadStatus.Complete -> DropdownMenuItem(
                            text = { Text("Play offline") },
                            onClick = { menu = false; onOpen?.invoke() },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Delete", color = c.error) },
                        onClick = { menu = false; Downloads.remove(task) },
                    )
                }
            }
        }

        if (!compactLayout) {
            Spacer(Modifier.height(8.dp))
            Text(
                task.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            if (task.height > 0) {
                "${task.channel} · ${task.height}p${if (task.hdr) " HDR" else ""}"
            } else {
                "${task.channel} · Audio"
            },
            color = Color.White.copy(alpha = 0.72f),
            fontSize = 11.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            statusText(task),
            color = if (task.status == DownloadStatus.Failed) c.error else Color.White.copy(alpha = 0.76f),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (running || task.status == DownloadStatus.Paused || task.status == DownloadStatus.Failed) {
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { task.progress },
                modifier = Modifier.fillMaxWidth().height(3.dp).clip(CircleShape),
                color = if (task.status == DownloadStatus.Failed) c.error else c.primary,
                trackColor = Color.White.copy(alpha = 0.18f),
                drawStopIndicator = {},
            )
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth()) {
                Text(
                    progressText(task),
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    when (task.status) {
                        DownloadStatus.Downloading -> "Downloading"
                        DownloadStatus.Queued -> "Waiting"
                        DownloadStatus.Processing -> "Finishing"
                        DownloadStatus.Paused -> "Paused"
                        DownloadStatus.Failed -> "Failed"
                        DownloadStatus.Complete -> "Downloaded"
                    },
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

private fun statusText(task: DownloadTask): String = when (task.status) {
    DownloadStatus.Complete -> "Downloaded · ${formatBytes(task.file.length())}"
    DownloadStatus.Downloading -> "Downloading ${(task.progress * 100).toInt()}%"
    DownloadStatus.Processing -> "Combining video and audio…"
    DownloadStatus.Queued -> "Waiting to download"
    DownloadStatus.Paused -> "Paused · ${formatBytes(task.downloaded)} downloaded"
    DownloadStatus.Failed -> task.error ?: "Download failed"
}

private fun progressText(task: DownloadTask): String {
    val total = task.total
    return if (total > 0) {
        "${formatBytes(task.downloaded)} of ${formatBytes(total)}"
    } else {
        formatBytes(task.downloaded)
    }
}

private fun qualityTag(height: Int): String? = when {
    height >= 2160 -> "4K"
    height >= 1440 -> "2K"
    height >= 1080 -> "Full HD"
    height >= 720 -> "HD"
    height <= 240 -> "Data saver"
    else -> null
}

private fun sizeSuffix(bytes: Long): String =
    if (bytes > 0) " · ${formatBytes(bytes)}" else ""

fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 MB"
    val mb = bytes / (1024.0 * 1024.0)
    return when {
        bytes >= 1024L * 1024 * 1024 -> String.format(Locale.US, "%.2f GB", bytes / (1024.0 * 1024 * 1024))
        mb >= 10 -> String.format(Locale.US, "%.0f MB", mb)
        mb >= 1 -> String.format(Locale.US, "%.1f MB", mb)
        else -> String.format(Locale.US, "%.0f KB", bytes / 1024.0)
    }
}
