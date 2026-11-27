@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.tensei.ytxtube

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed as staggeredItemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

fun isMusicTrack(video: VideoItem): Boolean {
    val t = video.title.lowercase()
    val ch = video.channel.lowercase()
    return t.contains("music") || t.contains("song") || t.contains("audio") ||
        t.contains("official") || t.contains("lyrics") || t.contains("feat") ||
        t.contains("ft.") || t.contains("remix") || t.contains("album") ||
        t.contains("track") || ch.contains("vevo") || ch.contains("records") ||
        ch.contains("music") || ch.contains("topic") || video.durationSec in 60..720
}
@Stable
class MusicFeedState(private val scope: CoroutineScope) {
    var shelves by mutableStateOf<List<MusicShelf>>(emptyList())
    var recentPlays by mutableStateOf<List<VideoItem>>(emptyList())
    var likedTracks by mutableStateOf<List<VideoItem>>(emptyList())
    var loading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    private var initialLoaded = false
    private var job: Job? = null

    fun ensureLoaded(lib: LibraryData) {
        if (!initialLoaded && !loading) loadAll(lib)
    }

    fun loadAll(lib: LibraryData) {
        RecentMusic.load()
        initialLoaded = true
        loading = true
        error = null
        job?.cancel()
        job = scope.launch {
            try {
                coroutineScope {
                    val signedIn = Account.signedIn


                    val homeDeferred = async {
                        runCatching { InnerTube.musicHome() }.getOrDefault(emptyList())
                    }



                    val historyDeferred = async {
                        val network = if (signedIn) {
                            runCatching { InnerTube.musicHistory() }.getOrDefault(emptyList())
                        } else emptyList()
                        val local = lib.history.videos.filter { isMusicTrack(it) }
                        (RecentMusic.items + network + local)
                            .distinctBy { it.id }
                            .map {
                                it.copy(
                                    thumbnail = highResMusicThumbnail(it.thumbnail)
                                        ?: sharpenFeedThumbnail(null, it.id),
                                )
                            }
                    }


                    val likedDeferred = async {
                        if (signedIn) {
                            runCatching { InnerTube.musicLiked() }.getOrDefault(emptyList()).map {
                                it.copy(thumbnail = highResMusicThumbnail(it.thumbnail) ?: sharpenFeedThumbnail(null, it.id))
                            }
                        } else emptyList()
                    }

                    val fetchedShelves = homeDeferred.await()
                    val fetchedHistory = historyDeferred.await()
                    val fetchedLiked = likedDeferred.await()

                    shelves = fetchedShelves
                    recentPlays = fetchedHistory
                    likedTracks = fetchedLiked
                }
            } catch (t: Throwable) {
                error = t.message ?: "Failed to load YouTube Music"
            } finally {
                loading = false
            }
        }
    }
}

@Composable
fun MusicScreen(
    lib: LibraryData,
    contentPadding: PaddingValues,
    onOpen: (VideoItem) -> Unit,
    onSearch: () -> Unit,
    onAccount: () -> Unit,
    onPlayNext: (VideoItem) -> Unit = {},
    onOpenQueue: (List<VideoItem>, Int) -> Unit = { list, index -> list.getOrNull(index)?.let(onOpen) },
    scrollToTop: Int = 0,
) {
    val scope = rememberCoroutineScope()
    val state = remember { MusicFeedState(scope) }


    val listState = rememberLazyStaggeredGridState()
    var lastScrollSignal by remember { mutableIntStateOf(scrollToTop) }
    LaunchedEffect(scrollToTop) {
        if (scrollToTop != lastScrollSignal) {
            lastScrollSignal = scrollToTop
            listState.animateScrollToItem(0)
        }
    }


    LaunchedEffect(state.loading, state.recentPlays.size, state.likedTracks.size, state.shelves.size) {
        if (!state.loading) listState.scrollToItem(0, 0)
    }
    val context = LocalContext.current

    var trackOptionsFor by remember { mutableStateOf<VideoItem?>(null) }


    var isSearching by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var searchLoading by remember { mutableStateOf(false) }
    var searchSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }


    var mix by remember { mutableStateOf<MusicShelfItem?>(null) }
    var mixSongs by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var mixLoading by remember { mutableStateOf(false) }
    var mixError by remember { mutableStateOf<String?>(null) }
    val loadMix: (MusicShelfItem) -> Unit = { item ->
        mix = item
        mixSongs = emptyList()
        mixError = null
        mixLoading = true
        scope.launch {
            runCatching { InnerTube.musicItemSongs(item) }
                .onSuccess { if (mix == item) mixSongs = it }
                .onFailure { if (mix == item) mixError = YouTube.friendlyError(it) }
            if (mix == item) mixLoading = false
        }
    }

    BackHandler(enabled = mix != null) { mix = null }

    BackHandler(enabled = isSearching) {
        if (searchQuery.isNotEmpty()) {
            searchQuery = ""
            searchResults = emptyList()
        } else {
            isSearching = false
        }
    }

    LaunchedEffect(Unit) {
        state.ensureLoaded(lib)
        lib.history.ensureLoaded()
        listState.scrollToItem(0, 0)
    }

    val isDark = isDarkTheme()
    val isAmoled = ThemeSettings.mode == "amoled"
    val c = MaterialTheme.colorScheme
    val signedIn = Account.signedIn

    fun downloadAudio(video: VideoItem) {
        val ok = Downloads.enqueueMusic(video)
        if (ok) {
            Toast.makeText(context, "Downloading audio: ${video.title}", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Already queued or downloaded", Toast.LENGTH_SHORT).show()
        }
    }

    trackOptionsFor?.let { track ->
        MusicTrackOptionsSheet(
            track = track,
            onPlayNext = {
                onPlayNext(track)
                Toast.makeText(context, "Playing next: ${track.title}", Toast.LENGTH_SHORT).show()
            },
            onDownloadAudio = { downloadAudio(track) },
            onShare = {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "https://youtu.be/${track.id}")
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share track"))
            },
            onDismiss = { trackOptionsFor = null },
        )
    }

    Box(Modifier.fillMaxSize()) {
        if (isSearching) {
            DedicatedMusicSearchView(
                query = searchQuery,
                onQueryChange = { q ->
                    searchQuery = q
                    if (q.isBlank()) {
                        searchSuggestions = emptyList()
                        searchResults = emptyList()
                    } else {
                        scope.launch {
                            runCatching {
                                searchSuggestions = YouTube.searchSuggestions("$q music").take(6)
                            }
                        }
                    }
                },
                onSearchSubmit = { q ->
                    searchQuery = q
                    searchLoading = true
                    scope.launch {
                        runCatching {
                            searchResults = YouTube.searchMusic(q)
                        }
                        searchLoading = false
                    }
                },
                onClose = {
                    isSearching = false
                    searchQuery = ""
                    searchResults = emptyList()
                },
                results = searchResults,
                loading = searchLoading,
                suggestions = searchSuggestions,
                onOpenTrack = { _, index -> onOpenQueue(searchResults, index) },
                onDownloadTrack = ::downloadAudio,
                contentPadding = contentPadding,
            )
        } else {
            val headerBackdrop = rememberLayerBackdrop()
            var headerHeight by remember { mutableIntStateOf(0) }
            val density = LocalDensity.current
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize().layerBackdrop(headerBackdrop)) {
                    Column(Modifier.fillMaxSize()) {
                        Spacer(Modifier.height(with(density) { headerHeight.toDp() }))
                        when {
                    state.loading && state.shelves.isEmpty() && state.recentPlays.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(contentPadding),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                color = c.primary,
                                strokeWidth = 2.5.dp,
                            )
                        }
                    }
                    state.error != null && state.shelves.isEmpty() && state.recentPlays.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(contentPadding),
                        ) {
                            EmptyState(
                                icon = Icons.Rounded.MusicNote,
                                title = "Couldn't load YouTube Music",
                                body = state.error ?: "Unable to connect to music servers.",
                                action = { PillButton("Retry", onClick = { state.loadAll(lib) }) },
                            )
                        }
                    }
                    else -> {
                        LazyVerticalStaggeredGrid(
                            columns = StaggeredGridCells.Adaptive(180.dp),
                            state = listState,
                            contentPadding = contentPadding,
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalItemSpacing = 12.dp,
                        ) {

                            if (state.recentPlays.isNotEmpty()) {
                                item(key = "section_recent_plays", span = StaggeredGridItemSpan.FullLine) {
                                    MusicSectionHeader(
                                        title = "Listen Again",
                                        subtitle = if (signedIn) "Your recent YouTube Music listening history" else "Recently played",
                                        icon = Icons.Rounded.History,
                                    )
                                }
                                staggeredItemsIndexed(state.recentPlays, key = { _, video -> "recent_" + video.id }) { index, video ->
                                    MusicTrackSquareCard(
                                        video = video,
                                        aspectRatio = bentoVideoAspectRatio(index),
                                        modifier = Modifier.animateItem(),
                                        onClick = { onOpenQueue(state.recentPlays, index) },
                                        onOptions = { trackOptionsFor = video },
                                    )
                                }
                            }


                            if (state.likedTracks.isNotEmpty()) {
                                item(key = "section_liked_music", span = StaggeredGridItemSpan.FullLine) {
                                    MusicSectionHeader(
                                        title = "Liked Music",
                                        subtitle = "Your favorite saved tracks",
                                        icon = Icons.Rounded.Favorite,
                                    )
                                }
                                staggeredItemsIndexed(state.likedTracks, key = { _, video -> "liked_" + video.id }) { index, video ->
                                    MusicTrackSquareCard(
                                        video = video,
                                        aspectRatio = bentoVideoAspectRatio(index + state.recentPlays.size),
                                        modifier = Modifier.animateItem(),
                                        onClick = { onOpenQueue(state.likedTracks, index) },
                                        onOptions = { trackOptionsFor = video },
                                    )
                                }
                            }

                            var shelfOffset = state.recentPlays.size + state.likedTracks.size
                            state.shelves.forEachIndexed { shelfIndex, shelf ->
                                item(key = "shelf_${shelfIndex}_${shelf.title}", span = StaggeredGridItemSpan.FullLine) {
                                    MusicSectionHeader(
                                        title = shelf.title,
                                        subtitle = shelf.subtitle ?: "Curated for you by YouTube Music",
                                        icon = Icons.Rounded.PlayArrow,
                                    )
                                }
                                val directTracks = shelf.items
                                    .filter { it.videoId != null && it.browseId.isNullOrEmpty() && it.playlistId.isNullOrEmpty() }
                                    .mapNotNull { it.toVideoItem() }
                                staggeredItemsIndexed(
                                    shelf.items,
                                    key = { _, item -> "shelf_${shelfIndex}_" + (item.videoId ?: item.browseId ?: item.title) },
                                ) { index, item ->
                                    MusicShelfCard(
                                        item = item,
                                        aspectRatio = bentoVideoAspectRatio(shelfOffset + index),
                                        modifier = Modifier.animateItem(),
                                        onClick = {
                                            val direct = item.toVideoItem()
                                            val isTrack = direct != null &&
                                                item.browseId.isNullOrEmpty() &&
                                                item.playlistId.isNullOrEmpty()
                                            if (isTrack && direct != null) {
                                                val trackIndex = directTracks.indexOfFirst { it.id == direct.id }
                                                onOpenQueue(directTracks, if (trackIndex >= 0) trackIndex else 0)
                                            } else {
                                                loadMix(item)
                                            }
                                        },
                                        onOptions = {
                                            val direct = item.toVideoItem()
                                            if (direct != null && item.browseId.isNullOrEmpty() && item.playlistId.isNullOrEmpty()) {
                                                trackOptionsFor = direct
                                            } else {
                                                scope.launch {
                                                    InnerTube.resolveMusicItem(item)?.let { trackOptionsFor = it }
                                                }
                                            }
                                        },
                                    )
                                }
                                shelfOffset += shelf.items.size
                            }

                            item(key = "bottom_space", span = StaggeredGridItemSpan.FullLine) {
                                Spacer(Modifier.height(16.dp))
                            }
                        }
                    }
                }
                    }
                }
                CompositionLocalProvider(LocalGlassBackdrop provides headerBackdrop) {
                    MusicScreenHeader(
                        onSearch = { isSearching = true },
                        onAccount = onAccount,
                        modifier = Modifier.onSizeChanged { headerHeight = it.height },
                    )
                }
            }
        }


        mix?.let { selected ->
            MusicMixScreen(
                item = selected,
                songs = mixSongs,
                loading = mixLoading,
                error = mixError,
                contentPadding = contentPadding,
                onBack = { mix = null },
                onRetry = { loadMix(selected) },
                onOpenSong = { index -> onOpenQueue(mixSongs, index) },
                onDownloadSong = ::downloadAudio,
                onOptions = { trackOptionsFor = it },
            )
        }
    }
}

@Composable
private fun MusicScreenHeader(
    onSearch: () -> Unit,
    onAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp, 26.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFFFF0033), Color(0xFFFF5E62)),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(17.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = "YTX Music",
            modifier = Modifier.weight(1f),
            fontFamily = RoundedHeavy,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            letterSpacing = (-0.5).sp,
            maxLines = 1,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = onSearch) {
                Icon(Icons.Rounded.Search, contentDescription = "Music Search", tint = c.onSurface)
            }
            AccountAction(onClick = onAccount)
        }
    }
}

@Composable
private fun MusicSectionHeader(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    val c = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = c.primary,
                modifier = Modifier.size(18.dp),
            )
            Text(
                title,
                fontFamily = RoundedHeavy,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                letterSpacing = (-0.3).sp,
                color = c.onSurface,
            )
        }
        Text(
            subtitle,
            fontSize = 12.5.sp,
            color = c.onSurfaceVariant.copy(alpha = 0.75f),
            fontWeight = FontWeight.Medium,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicTrackOptionsSheet(
    track: VideoItem,
    onPlayNext: () -> Unit,
    onDownloadAudio: () -> Unit,
    onShare: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = MaterialTheme.colorScheme
    GlassModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(c.surfaceContainerHighest),
                ) {
                    AsyncImage(
                        model = highResMusicThumbnail(track.thumbnail) ?: sharpenFeedThumbnail(null, track.id),
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        track.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = c.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        track.channel,
                        fontSize = 12.5.sp,
                        color = c.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))


            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable {
                        onPlayNext()
                        onDismiss()
                    }
                    .padding(vertical = 14.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.QueueMusic,
                    contentDescription = null,
                    tint = c.primary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    "Play next",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = c.onSurface,
                )
            }


            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable {
                        onDownloadAudio()
                        onDismiss()
                    }
                    .padding(vertical = 14.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.Download,
                    contentDescription = null,
                    tint = c.primary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        "Download (Audio only)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = c.onSurface,
                    )
                    Text(
                        "High quality audio (256 kbps)",
                        fontSize = 11.5.sp,
                        color = c.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
            }


            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable {
                        onShare()
                        onDismiss()
                    }
                    .padding(vertical = 14.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.Share,
                    contentDescription = null,
                    tint = c.primary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    "Share track",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = c.onSurface,
                )
            }
        }
    }
}

@Composable
private fun MusicShelfCard(
    item: MusicShelfItem,
    aspectRatio: Float,
    modifier: Modifier,
    onClick: () -> Unit,
    onOptions: () -> Unit,
) {
    MusicArtworkCard(
        title = item.title,
        subtitle = item.subtitle,
        thumbnail = highResMusicThumbnail(item.thumbnail),
        aspectRatio = aspectRatio,
        modifier = modifier,
        onClick = onClick,
        onOptions = onOptions,
    )
}

@Composable
private fun MusicTrackSquareCard(
    video: VideoItem,
    aspectRatio: Float,
    modifier: Modifier,
    onClick: () -> Unit,
    onOptions: () -> Unit,
    onDownload: (() -> Unit)? = null,
) {
    MusicArtworkCard(
        title = video.title,
        subtitle = video.channel,
        thumbnail = highResMusicThumbnail(video.thumbnail) ?: sharpenFeedThumbnail(null, video.id),
        videoId = video.id,
        aspectRatio = aspectRatio,
        avatarVideo = video,
        modifier = modifier,
        onClick = onClick,
        onOptions = onOptions,
        onDownload = onDownload,
        durationSec = video.durationSec,
    )
}

@Composable
private fun MusicArtworkCard(
    title: String,
    subtitle: String?,
    thumbnail: String?,
    aspectRatio: Float,
    modifier: Modifier,
    onClick: () -> Unit,
    onOptions: () -> Unit,
    videoId: String? = null,
    avatarVideo: VideoItem? = null,
    onDownload: (() -> Unit)? = null,
    durationSec: Long? = null,
) {
    val compactLayout = aspectRatio <= 1f
    Column(
        modifier = modifier
            .fillMaxWidth()
            .bubbleGlassSurface(RoundedCornerShape(24.dp))
            .then(if (videoId != null) Modifier.prefetchOnPress(videoId) else Modifier)
            .pressable(pressedScale = 0.97f, onClick = onClick),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(aspectRatio),
        ) {
            AsyncImage(
                model = thumbnail,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
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
            }
            durationSec?.takeIf { it > 0 }?.let {
                DurationPill(it, Modifier.align(Alignment.TopStart).padding(8.dp), small = true)
            }
            Row(
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                onDownload?.let { download ->
                    IconButton(
                        onClick = download,
                        modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.42f)),
                    ) {
                        Icon(Icons.Rounded.Download, "Download audio", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
                IconButton(
                    onClick = onOptions,
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.42f)),
                ) {
                    Icon(Icons.Rounded.MoreVert, "Options", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
            if (compactLayout) {
                Row(
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    avatarVideo?.let {
                        ChannelAvatar(it.feedAvatar(), it.channel, size = 26)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        title,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        lineHeight = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        if (!compactLayout) {
            Row(
                Modifier.fillMaxWidth().padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                avatarVideo?.let {
                    ChannelAvatar(it.feedAvatar(), it.channel, size = 28)
                    Spacer(Modifier.width(8.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.5.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    subtitle?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            it,
                            color = Color.White.copy(alpha = 0.72f),
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
@Composable
private fun DedicatedMusicSearchView(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchSubmit: (String) -> Unit,
    onClose: () -> Unit,
    results: List<VideoItem>,
    loading: Boolean,
    suggestions: List<String>,
    onOpenTrack: (VideoItem, Int) -> Unit,
    onDownloadTrack: (VideoItem) -> Unit,
    contentPadding: PaddingValues,
) {
    val c = MaterialTheme.colorScheme
    val isDark = isDarkTheme()
    val isAmoled = ThemeSettings.mode == "amoled"
    val videoMenu = LocalVideoMenu.current
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        Modifier
            .fillMaxSize()
            .appScreenBackground()
            .statusBarsPadding(),
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = c.onSurface,
                )
            }
            Spacer(Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .bubbleGlassSurface(RoundedCornerShape(24.dp))
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(
                    Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.Search,
                        contentDescription = null,
                        tint = GlassmorphicTheme.goldStart,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
                            Text(
                                "Search songs, artists, albums...",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.72f),
                            )
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = onQueryChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            singleLine = true,
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                            cursorBrush = SolidColor(GlassmorphicTheme.goldStart),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    keyboardController?.hide()
                                    if (query.isNotBlank()) onSearchSubmit(query)
                                },
                            ),
                        )
                    }
                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = { onQueryChange("") },
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Clear",
                                tint = Color.White.copy(alpha = 0.76f),
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }


        when {
            loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = c.primary, strokeWidth = 2.5.dp)
                }
            }
            results.isNotEmpty() -> {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Adaptive(180.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = contentPadding,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalItemSpacing = 12.dp,
                ) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        Text(
                            "Top Results",
                            fontFamily = RoundedHeavy,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            color = c.onSurfaceVariant,
                        )
                    }
                    staggeredItemsIndexed(results, key = { _, video -> "sr_" + video.id }) { index, video ->
                        MusicTrackSquareCard(
                            video = video,
                            aspectRatio = bentoVideoAspectRatio(index),
                            modifier = Modifier.animateItem(),
                            onClick = { onOpenTrack(video, index) },
                            onOptions = { videoMenu(video) },
                            onDownload = { onDownloadTrack(video) },
                        )
                    }
                }
            }
            suggestions.isNotEmpty() && query.isNotEmpty() -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = contentPadding,
                ) {
                    items(suggestions) { sugg ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    keyboardController?.hide()
                                    onSearchSubmit(sugg)
                                }
                                .padding(horizontal = 18.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Rounded.Search,
                                contentDescription = null,
                                tint = c.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(14.dp))
                            Text(
                                sugg,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = c.onSurface,
                            )
                        }
                    }
                }
            }
            else -> {

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = contentPadding,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item {
                        Text(
                            "Trending Music Searches",
                            fontFamily = RoundedHeavy,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(horizontal = 16.dp).padding(top = 8.dp),
                            color = c.onSurfaceVariant,
                        )
                    }
                    item {
                        val trendingTags = listOf(
                            "The Weeknd", "Taylor Swift", "Kendrick Lamar",
                            "Billie Eilish", "Drake", "Coldplay",
                            "Arijit Singh", "Dua Lipa", "Post Malone", "Travis Scott",
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(trendingTags) { tag ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            if (isAmoled) Color(0xFF1E1E1E)
                                            else if (isDark) Color(0xFF26262A)
                                            else Color(0xFFE8E8EE),
                                        )
                                        .clickable {
                                            keyboardController?.hide()
                                            onSearchSubmit(tag)
                                        }
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                ) {
                                    Text(
                                        tag,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = c.onSurface,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MusicMixScreen(
    item: MusicShelfItem,
    songs: List<VideoItem>,
    loading: Boolean,
    error: String?,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onOpenSong: (Int) -> Unit,
    onDownloadSong: (VideoItem) -> Unit,
    onOptions: (VideoItem) -> Unit,
) {
    val c = MaterialTheme.colorScheme
    Box(
        Modifier
            .fillMaxSize()
            .appScreenBackground(),
    ) {
        Column(Modifier.fillMaxSize()) {
            BackScreenHeader(onBack = onBack) {
                Text(
                    item.title,
                    modifier = Modifier.weight(1f),
                    fontFamily = RoundedHeavy,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    letterSpacing = (-0.5).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(c.surfaceContainerHighest),
                ) {
                    AsyncImage(
                        model = highResMusicThumbnail(item.thumbnail)
                            ?: sharpenFeedThumbnail(null, item.videoId.orEmpty()),
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        item.title,
                        fontFamily = RoundedHeavy,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 21.sp,
                        letterSpacing = (-0.4).sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = c.onSurface,
                    )
                    if (item.subtitle.isNotBlank()) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            item.subtitle,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = c.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        when {
                            loading -> "Loading songs…"
                            songs.isNotEmpty() -> "${songs.size} songs"
                            else -> "Mix"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = c.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            when {
                loading -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 56.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = c.primary, strokeWidth = 2.5.dp)
                }
                error != null -> EmptyState(
                    icon = Icons.Rounded.MusicNote,
                    title = "Couldn't load this mix",
                    body = error,
                    action = { PillButton("Retry", onClick = onRetry) },
                )
                songs.isEmpty() -> EmptyState(
                    icon = Icons.Rounded.MusicNote,
                    title = "No songs here",
                    body = "This mix didn't return any playable songs.",
                )
                else -> LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Adaptive(180.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = contentPadding,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalItemSpacing = 12.dp,
                ) {
                    staggeredItemsIndexed(songs, key = { _, song -> "mix_" + song.id }) { index, song ->
                        MusicTrackSquareCard(
                            video = song,
                            aspectRatio = bentoVideoAspectRatio(index),
                            modifier = Modifier.animateItem(),
                            onClick = { onOpenSong(index) },
                            onDownload = { onDownloadSong(song) },
                            onOptions = { onOptions(song) },
                        )
                    }
                    item(key = "mix_bottom", span = StaggeredGridItemSpan.FullLine) {
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}
