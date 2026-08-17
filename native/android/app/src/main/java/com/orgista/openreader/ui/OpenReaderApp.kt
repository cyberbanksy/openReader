package com.orgista.openreader.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.LibraryBooks
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orgista.openreader.R
import com.orgista.openreader.data.CoverImageLoader
import com.orgista.openreader.domain.BookFormat
import com.orgista.openreader.domain.BookSource
import com.orgista.openreader.domain.CatalogAvailability
import com.orgista.openreader.domain.CatalogMerger
import com.orgista.openreader.domain.CatalogSourceKind
import com.orgista.openreader.domain.DeviceProfile
import com.orgista.openreader.domain.LibraryBook
import com.orgista.openreader.domain.NavigationStyle
import com.orgista.openreader.playback.PlaybackService
import com.orgista.openreader.ui.theme.OpenReaderColors
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

private enum class LibraryLayout {
    Grid,
    List,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenReaderApp(
    state: OpenReaderUiState,
    deviceProfile: DeviceProfile,
    onFilter: (LibraryFilter) -> Unit,
    onSelectBook: (LibraryBook?) -> Unit,
    onOpenBook: (LibraryBook) -> Unit,
    onShowConnection: (Boolean) -> Unit,
    onConnect: (String, String, String, Boolean) -> Unit,
    onDisconnect: () -> Unit,
    onShowArrConnection: (Boolean) -> Unit,
    onConnectArr: (String, String, String, String) -> Unit,
    onDisconnectArr: () -> Unit,
    onShowStandardEbooksConnection: (Boolean) -> Unit,
    onConnectStandardEbooks: (String) -> Unit,
    onDisconnectStandardEbooks: () -> Unit,
    onSearchPublicDomain: (String) -> Unit,
    onClearPublicDomainSearch: () -> Unit,
    onRefresh: () -> Unit,
    onDismissError: () -> Unit,
) {
    val wide = deviceProfile.navigationStyle == NavigationStyle.Rail
    var destination by remember { mutableStateOf(AppDestination.Home) }
    var searchOpen by remember { mutableStateOf(false) }
    var libraryLayout by remember { mutableStateOf(LibraryLayout.Grid) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            onDismissError()
        }
    }

    if (state.showConnection) {
        ConnectionDialog(
            serverUrl = state.serverUrl,
            username = state.username,
            loading = state.loading,
            connected = state.connected,
            onDismiss = { onShowConnection(false) },
            onConnect = onConnect,
            onDisconnect = onDisconnect,
        )
    }

    if (state.showArrConnection) {
        ArrConnectionDialog(
            ebookServerUrl = state.ebookArrUrl,
            audiobookServerUrl = state.audiobookArrUrl,
            loading = state.loading,
            connected = state.arrConnected,
            onDismiss = { onShowArrConnection(false) },
            onConnect = onConnectArr,
            onDisconnect = onDisconnectArr,
        )
    }

    if (state.showStandardEbooksConnection) {
        StandardEbooksDialog(
            email = state.standardEbooksEmail,
            loading = state.loading,
            connected = state.standardEbooksConnected,
            onDismiss = { onShowStandardEbooksConnection(false) },
            onConnect = onConnectStandardEbooks,
            onDisconnect = onDisconnectStandardEbooks,
        )
    }

    Scaffold(
        topBar = {
            OpenReaderTopBar(
                destination = destination,
                wide = wide,
                connected = state.connected,
                loading = state.loading,
                onDestination = { destination = it },
                onSearch = { searchOpen = true },
                onRefresh = onRefresh,
                onShowConnection = { onShowConnection(true) },
            )
        },
        bottomBar = {
            Column {
                state.nowPlaying?.let { MiniPlayer(it) }
                if (!wide) {
                    OpenReaderBottomBar(destination) { destination = it }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (destination) {
                AppDestination.Home -> HomeScreen(
                    books = state.books,
                    connected = state.anyConnected,
                    onSelectBook = onSelectBook,
                    onOpenBook = onOpenBook,
                    onBrowse = { destination = AppDestination.Browse },
                )
                AppDestination.Library -> LibraryScreen(
                    books = state.books,
                    filter = state.filter,
                    layout = libraryLayout,
                    connected = state.anyConnected,
                    onLayout = { libraryLayout = it },
                    onFilter = onFilter,
                    onSelectBook = onSelectBook,
                    onOpenBook = onOpenBook,
                    onBrowse = { destination = AppDestination.Browse },
                )
                AppDestination.Browse -> BrowseScreen(
                    books = state.books,
                    connected = state.anyConnected,
                    onSelectBook = onSelectBook,
                    onOpenBook = onOpenBook,
                )
                AppDestination.Sources -> SourcesScreen(
                    state = state,
                    onShowConnection = { onShowConnection(true) },
                    onShowArrConnection = { onShowArrConnection(true) },
                    onShowStandardEbooksConnection = { onShowStandardEbooksConnection(true) },
                    onSearchPublicDomain = { searchOpen = true },
                    onRefresh = onRefresh,
                )
            }
        }
    }

    if (searchOpen) {
        SearchDialog(
            books = state.books,
            publicDomainBooks = state.publicDomainResults,
            publicDomainSearching = state.publicDomainSearching,
            onSearchPublicDomain = onSearchPublicDomain,
            onDismiss = {
                searchOpen = false
                onClearPublicDomainSearch()
            },
            onSelectBook = {
                searchOpen = false
                onClearPublicDomainSearch()
                onSelectBook(it)
            },
        )
    }

    state.selectedBook?.let { selected ->
        if (wide) {
            BookDetailDialog(
                book = selected,
                connected = state.anyConnected,
                onDismiss = { onSelectBook(null) },
                onOpen = { onOpenBook(selected) },
                formatOptions = matchingFormats(state.books, selected),
                onSelectFormat = onSelectBook,
            )
        } else {
            ModalBottomSheet(onDismissRequest = { onSelectBook(null) }) {
                BookDetail(
                    book = selected,
                    connected = state.anyConnected,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    onOpen = { onOpenBook(selected) },
                    formatOptions = matchingFormats(state.books, selected),
                    onSelectFormat = onSelectBook,
                )
                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun OpenReaderTopBar(
    destination: AppDestination,
    wide: Boolean,
    connected: Boolean,
    loading: Boolean,
    onDestination: (AppDestination) -> Unit,
    onSearch: () -> Unit,
    onRefresh: () -> Unit,
    onShowConnection: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.background, shadowElevation = 1.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .heightIn(min = 68.dp)
                .padding(horizontal = if (wide) 26.dp else 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.AutoStories,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(27.dp),
                )
                Spacer(Modifier.width(9.dp))
                Text(
                    "OpenReader",
                    fontFamily = FontFamily.Serif,
                    fontSize = if (wide) 22.sp else 20.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (wide) {
                Spacer(Modifier.weight(1f))
                DestinationSwitcher(destination, onDestination)
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onSearch) {
                Icon(Icons.Rounded.Search, contentDescription = "Search")
            }
            if (connected) {
                IconButton(onClick = onRefresh, enabled = !loading) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Refresh library")
                }
            }
            IconButton(onClick = onShowConnection) {
                Icon(
                    if (connected) Icons.Rounded.CloudDone else Icons.Rounded.Cloud,
                    contentDescription = if (connected) "Manage sources" else "Connect a source",
                    tint = if (connected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            }
        }
    }
}

@Composable
private fun DestinationSwitcher(
    destination: AppDestination,
    onDestination: (AppDestination) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.padding(3.dp)) {
            AppDestination.entries.forEach { item ->
                Surface(
                    onClick = { onDestination(item) },
                    shape = RoundedCornerShape(18.dp),
                    color = if (item == destination) MaterialTheme.colorScheme.surface
                    else Color.Transparent,
                    shadowElevation = if (item == destination) 2.dp else 0.dp,
                ) {
                    Text(
                        item.label,
                        modifier = Modifier.padding(horizontal = 15.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (item == destination) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun PageFrame(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.fillMaxWidth().widthIn(max = 1500.dp)) { content() }
    }
}

@Composable
private fun PageHeading(title: String, subtitle: String? = null, action: (@Composable () -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 26.dp, vertical = 22.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            if (subtitle != null) {
                Spacer(Modifier.height(3.dp))
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        action?.invoke()
    }
}

@Composable
private fun HomeScreen(
    books: List<LibraryBook>,
    connected: Boolean,
    onSelectBook: (LibraryBook) -> Unit,
    onOpenBook: (LibraryBook) -> Unit,
    onBrowse: () -> Unit,
) {
    val libraryBooks = libraryCollection(books)
    val ready = libraryBooks.filter { book ->
        primaryBookSource(book)?.availability in setOf(CatalogAvailability.Ready, CatalogAvailability.Borrowed) ||
            book.isDemo
    }
    val continuing = ready.filter { it.progress > 0f }.take(10)
    val ebooks = ready.filter { it.format == BookFormat.Ebook }.take(12)
    val audiobooks = ready.filter { it.format == BookFormat.Audiobook }.take(12)
    val arriving = libraryBooks.filter { it !in ready }.take(12)
    PageFrame {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp),
        ) {
            item {
                PageHeading(
                    title = "Home",
                    subtitle = if (connected) "Your reading, ready when you are" else "Preview your OpenReader library",
                )
            }
            if (continuing.isNotEmpty()) {
                item {
                    ContinueCard(
                        book = continuing.first(),
                        connected = connected,
                        onOpen = { onOpenBook(continuing.first()) },
                    )
                }
                if (continuing.size > 1) {
                    item { ShelfHeader("Up next", "Continue where you left off") }
                    item { BookShelf(continuing.drop(1), onSelectBook, connected, onOpenBook) }
                }
            }
            if (ready.isNotEmpty()) {
                item { ShelfHeader("Ready now", "Read or listen without waiting") }
                item { BookShelf(ready.take(12), onSelectBook, connected, onOpenBook) }
            }
            if (ebooks.isNotEmpty()) {
                item { ShelfHeader("Read", "Ebooks ready to open") }
                item { BookShelf(ebooks, onSelectBook, connected, onOpenBook) }
            }
            if (audiobooks.isNotEmpty()) {
                item { ShelfHeader("Listen", "Audiobooks ready to play") }
                item { BookShelf(audiobooks, onSelectBook, connected, onOpenBook) }
            }
            if (arriving.isNotEmpty()) {
                item { ShelfHeader("Requests & downloads", "Titles on their way") }
                item { BookShelf(arriving, onSelectBook, connected, onOpenBook) }
            }
            if (libraryBooks.isEmpty()) {
                item { EmptyLibraryMessage(onBrowse) }
            }
        }
    }
}

@Composable
private fun ContinueCard(book: LibraryBook, connected: Boolean, onOpen: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 26.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CoverArt(book, Modifier.width(112.dp))
            Spacer(Modifier.width(22.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "CONTINUE",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                )
                Spacer(Modifier.height(7.dp))
                Text(
                    book.title,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(book.creator, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(15.dp))
                LinearProgressIndicator(
                    progress = { book.progress },
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    (book.progress * 100).roundToInt().toString() + "% complete",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(15.dp))
                Button(onClick = onOpen) {
                    Icon(
                        if (book.format == BookFormat.Audiobook) Icons.Rounded.PlayArrow
                        else Icons.AutoMirrored.Rounded.MenuBook,
                        contentDescription = null,
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(primaryBookAction(book, connected))
                }
            }
        }
    }
}

@Composable
private fun ShelfHeader(title: String, subtitle: String?) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 26.dp, end = 22.dp, top = 30.dp, bottom = 12.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Icon(
            Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BookShelf(
    books: List<LibraryBook>,
    onSelectBook: (LibraryBook) -> Unit,
    connected: Boolean = false,
    onOpenBook: ((LibraryBook) -> Unit)? = null,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 26.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(books, key = { it.id }) { book ->
            BookCard(book, Modifier.width(142.dp), onSelectBook, connected, onOpenBook)
        }
    }
}

@Composable
private fun BrowseScreen(
    books: List<LibraryBook>,
    connected: Boolean,
    onSelectBook: (LibraryBook) -> Unit,
    onOpenBook: (LibraryBook) -> Unit,
) {
    val kinds = books.flatMap { book -> book.sources.map(BookSource::kind) }.distinct()
    var selectedSource by remember { mutableStateOf<CatalogSourceKind?>(null) }
    val visibleBooks = if (selectedSource == null) books else books.filter { book ->
        book.sources.any { it.kind == selectedSource }
    }
    PageFrame {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp),
        ) {
            item {
                PageHeading(
                    title = "Browse",
                    subtitle = if (connected) "Across your connected sources" else "Preview available catalog sources",
                )
            }
            if (kinds.size > 1) {
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 26.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item {
                            FilterChip(
                                selected = selectedSource == null,
                                onClick = { selectedSource = null },
                                label = { Text("All sources") },
                            )
                        }
                        items(kinds) { kind ->
                            FilterChip(
                                selected = selectedSource == kind,
                                onClick = { selectedSource = kind },
                                label = { Text(sourceKindLabel(kind)) },
                            )
                        }
                    }
                }
            }
            if (visibleBooks.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Rounded.Explore,
                        title = "Nothing available here yet",
                        detail = "Connect or refresh a source to populate this view.",
                    )
                }
            } else {
                val ready = visibleBooks.filter { book ->
                    book.sources.any { it.availability in setOf(CatalogAvailability.Ready, CatalogAvailability.Borrowed) }
                }
                val available = visibleBooks.filter { book ->
                    book.sources.any { it.availability in setOf(CatalogAvailability.Available, CatalogAvailability.Requestable) }
                }
                if (ready.isNotEmpty()) {
                    item { ShelfHeader("Ready now", "Open from your library") }
                    item { BookShelf(ready, onSelectBook, connected, onOpenBook) }
                }
                if (available.isNotEmpty()) {
                    item { ShelfHeader("Available from your sources", "Borrow or request when a connector supports it") }
                    item { BookShelf(available, onSelectBook, connected, onOpenBook) }
                }
                item { ShelfHeader("All titles", null) }
                item {
                    DenseBookGrid(visibleBooks, connected, onSelectBook, onOpenBook)
                }
            }
        }
    }
}

@Composable
private fun DenseBookGrid(
    books: List<LibraryBook>,
    connected: Boolean,
    onSelectBook: (LibraryBook) -> Unit,
    onOpenBook: (LibraryBook) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 26.dp),
        horizontalArrangement = Arrangement.spacedBy(15.dp),
    ) {
        items(books, key = { "dense-" + it.id }) { book ->
            BookCard(book, Modifier.width(128.dp), onSelectBook, connected, onOpenBook)
        }
    }
}

@Composable
private fun LibraryScreen(
    books: List<LibraryBook>,
    filter: LibraryFilter,
    layout: LibraryLayout,
    connected: Boolean,
    onLayout: (LibraryLayout) -> Unit,
    onFilter: (LibraryFilter) -> Unit,
    onSelectBook: (LibraryBook) -> Unit,
    onOpenBook: (LibraryBook) -> Unit,
    onBrowse: () -> Unit,
) {
    val libraryBooks = libraryCollection(books)
    val groups = organizeLibrary(books, filter)
    val visibleCount = groups.sumOf { it.books.size }
    PageFrame {
        Column(Modifier.fillMaxSize()) {
            PageHeading(
                title = "Library",
                subtitle = when {
                    connected -> libraryBooks.size.toString() + " titles ready or on their way"
                    else -> "Preview collection"
                },
                action = {
                    Row {
                        IconButton(onClick = { onLayout(LibraryLayout.Grid) }) {
                            Icon(
                                Icons.Rounded.GridView,
                                contentDescription = "Grid view",
                                tint = if (layout == LibraryLayout.Grid) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { onLayout(LibraryLayout.List) }) {
                            Icon(
                                Icons.AutoMirrored.Rounded.List,
                                contentDescription = "List view",
                                tint = if (layout == LibraryLayout.List) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 26.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 26.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterControl(filter, libraryBooks, onFilter)
            }
            Spacer(Modifier.height(18.dp))
            if (visibleCount == 0) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    EmptyState(
                        icon = Icons.AutoMirrored.Rounded.LibraryBooks,
                        title = "No books in this collection",
                        detail = "Try another format, or find a title to request in Browse.",
                    )
                    Button(onClick = onBrowse) {
                        Icon(Icons.Rounded.Explore, contentDescription = null)
                        Spacer(Modifier.width(7.dp))
                        Text("Browse titles")
                    }
                }
            } else if (layout == LibraryLayout.Grid) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 132.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 26.dp, end = 26.dp, bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(17.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    groups.forEach { group ->
                        item(
                            key = "section-${group.section.name}",
                            span = { GridItemSpan(maxLineSpan) },
                        ) {
                            LibrarySectionHeader(group, filter)
                        }
                        items(group.books, key = { it.id }) { book ->
                            BookCard(book, Modifier.fillMaxWidth(), onSelectBook, connected, onOpenBook)
                        }
                    }
                    item(key = "browse-more", span = { GridItemSpan(maxLineSpan) }) {
                        BrowseMoreCard(onBrowse)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 26.dp, end = 26.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    groups.forEach { group ->
                        item(key = "section-${group.section.name}") {
                            LibrarySectionHeader(group, filter)
                        }
                        items(group.books, key = { it.id }) { book ->
                            LibraryListRow(book, onSelectBook, connected, onOpenBook)
                        }
                    }
                    item(key = "browse-more") {
                        BrowseMoreCard(onBrowse)
                    }
                }
            }
        }
    }
}

@Composable
private fun LibrarySectionHeader(group: LibraryGroup, filter: LibraryFilter) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(group.section.title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Text(
                group.books.size.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            when (filter) {
                LibraryFilter.All -> group.section.subtitle
                LibraryFilter.Ebooks -> group.section.subtitle.replace("open", "read")
                LibraryFilter.Audiobooks -> group.section.subtitle.replace("open", "listen to")
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BrowseMoreCard(onBrowse: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 20.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Explore, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Looking for something else?", fontWeight = FontWeight.SemiBold)
                Text(
                    "Requestable titles live in Browse, so this library stays focused.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onBrowse) { Text("Browse") }
        }
    }
}

@Composable
private fun SourcesScreen(
    state: OpenReaderUiState,
    onShowConnection: () -> Unit,
    onShowArrConnection: () -> Unit,
    onShowStandardEbooksConnection: () -> Unit,
    onSearchPublicDomain: () -> Unit,
    onRefresh: () -> Unit,
) {
    val connectedKinds = state.books.flatMap { book -> book.sources.map(BookSource::kind) }.toSet()
    PageFrame {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp),
        ) {
            item {
                PageHeading(
                    title = "Sources",
                    subtitle = "Where OpenReader finds books and audiobooks",
                )
            }
            item {
                SourceConnectionCard(
                    icon = Icons.Rounded.CloudDone,
                    title = "Audiobookshelf",
                    status = if (state.connected) "Connected" else "Not connected",
                    detail = if (state.connected) state.serverUrl else "Your self-hosted reading library",
                    action = if (state.connected) "Manage" else "Connect",
                    actionEnabled = true,
                    onAction = onShowConnection,
                )
            }
            item {
                SourceConnectionCard(
                    icon = Icons.Rounded.Settings,
                    title = "ARR-managed catalogs",
                    status = if (state.arrConnected) "Connected" else "Not connected",
                    detail = if (state.arrConnected) {
                        "Ebooks ${state.ebookArrUrl} · Audiobooks ${state.audiobookArrUrl}"
                    } else {
                        "Request and acquisition status from your two Bookshelf managers"
                    },
                    action = if (state.arrConnected) "Manage" else "Connect",
                    actionEnabled = true,
                    onAction = onShowArrConnection,
                )
            }
            item {
                SourceConnectionCard(
                    icon = Icons.AutoMirrored.Rounded.LibraryBooks,
                    title = "Public libraries",
                    status = if (CatalogSourceKind.PublicLibrary in connectedKinds) "Available" else "No connector configured",
                    detail = "Loans, holds, and availability from supported library adapters",
                    action = if (CatalogSourceKind.PublicLibrary in connectedKinds) "View" else "Not configured",
                    actionEnabled = false,
                    onAction = {},
                )
            }
            item {
                SourceConnectionCard(
                    icon = Icons.Rounded.AutoStories,
                    title = "Project Gutenberg",
                    status = "Available",
                    detail = "Public-domain EPUB catalog",
                    action = "Search",
                    actionEnabled = true,
                    onAction = onSearchPublicDomain,
                )
            }
            item {
                SourceConnectionCard(
                    icon = Icons.Rounded.AutoStories,
                    title = "Standard Ebooks",
                    status = if (state.standardEbooksConnected) "Connected" else "Email required",
                    detail = if (state.standardEbooksConnected) {
                        state.standardEbooksEmail
                    } else {
                        "Patrons Circle OPDS catalog"
                    },
                    action = if (state.standardEbooksConnected) "Manage" else "Connect",
                    actionEnabled = true,
                    onAction = onShowStandardEbooksConnection,
                )
            }
            if (state.anyConnected) {
                item {
                    OutlinedButton(
                        onClick = onRefresh,
                        enabled = !state.loading,
                        modifier = Modifier.padding(horizontal = 26.dp, vertical = 10.dp),
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null)
                        Spacer(Modifier.width(7.dp))
                        Text("Refresh all")
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceConnectionCard(
    icon: ImageVector,
    title: String,
    status: String,
    detail: String,
    action: String,
    actionEnabled: Boolean,
    onAction: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 26.dp, vertical = 7.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(13.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                modifier = Modifier.size(50.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.width(15.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(status, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                Text(
                    detail,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(14.dp))
            OutlinedButton(onClick = onAction, enabled = actionEnabled) { Text(action) }
        }
    }
}

@Composable
private fun FilterControl(
    selected: LibraryFilter,
    books: List<LibraryBook>,
    onFilter: (LibraryFilter) -> Unit,
) {
    val filters = LibraryFilter.entries
    SingleChoiceSegmentedButtonRow {
        filters.forEachIndexed { index, filter ->
            SegmentedButton(
                selected = selected == filter,
                onClick = { onFilter(filter) },
                shape = SegmentedButtonDefaults.itemShape(index, filters.size),
                icon = {},
                label = {
                    Text(
                        when (filter) {
                            LibraryFilter.All -> "All ${books.size}"
                            LibraryFilter.Audiobooks ->
                                "Listen ${books.count { it.format == BookFormat.Audiobook }}"
                            LibraryFilter.Ebooks ->
                                "Read ${books.count { it.format == BookFormat.Ebook }}"
                        },
                        maxLines = 1,
                    )
                },
            )
        }
    }
}

@Composable
private fun BookCard(
    book: LibraryBook,
    modifier: Modifier,
    onClick: (LibraryBook) -> Unit,
    connected: Boolean = false,
    onOpenBook: ((LibraryBook) -> Unit)? = null,
) {
    Column(modifier.clickable { onClick(book) }) {
        CoverArt(book, Modifier.fillMaxWidth())
        Spacer(Modifier.height(9.dp))
        Text(
            book.title,
            modifier = Modifier.height(40.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 18.sp,
        )
        Text(
            book.creator,
            modifier = Modifier.height(20.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val source = primaryBookSource(book)
        if (source != null) {
            Text(
                source.name,
                modifier = Modifier.height(18.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            Spacer(Modifier.height(18.dp))
        }
        if (onOpenBook != null) {
            Spacer(Modifier.height(8.dp))
            DirectBookAction(
                book = book,
                connected = connected,
                onOpen = { onOpenBook(book) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun LibraryListRow(
    book: LibraryBook,
    onClick: (LibraryBook) -> Unit,
    connected: Boolean = false,
    onOpenBook: ((LibraryBook) -> Unit)? = null,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick(book) },
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.fillMaxWidth().padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
            CoverArt(book, Modifier.width(66.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    book.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(book.creator, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(5.dp))
                Text(
                    sourceSummary(book),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (book.progress > 0f) {
                    Spacer(Modifier.height(7.dp))
                    LinearProgressIndicator(
                        progress = { book.progress },
                        modifier = Modifier.fillMaxWidth().height(3.dp),
                    )
                }
            }
            if (onOpenBook == null) {
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Details")
            } else {
                Spacer(Modifier.width(12.dp))
                DirectBookAction(
                    book = book,
                    connected = connected,
                    onOpen = { onOpenBook(book) },
                    modifier = Modifier.widthIn(min = 104.dp),
                )
            }
        }
    }
}

@Composable
private fun DirectBookAction(
    book: LibraryBook,
    connected: Boolean,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = primaryBookAction(book, connected)
    val enabled = primaryBookActionEnabled(book, connected)
    val content: @Composable () -> Unit = {
        if (label in setOf("Read", "Listen")) {
            Icon(
                if (book.format == BookFormat.Audiobook) Icons.Rounded.Headphones
                else Icons.AutoMirrored.Rounded.MenuBook,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(5.dp))
        }
        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    if (enabled) {
        Button(
            onClick = onOpen,
            modifier = modifier.height(40.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            content = { content() },
        )
    } else {
        OutlinedButton(
            onClick = {},
            enabled = false,
            modifier = modifier.height(40.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            content = { content() },
        )
    }
}

@Composable
private fun CoverArt(book: LibraryBook, modifier: Modifier = Modifier) {
    val palette = listOf(
        Color(0xFF173A55),
        Color(0xFF5A2435),
        Color(0xFF24594F),
        Color(0xFF765526),
        Color(0xFF3E426A),
        Color(0xFF6A3B28),
    )
    Box(
        modifier = modifier
            .aspectRatio(0.69f)
            .clip(RoundedCornerShape(7.dp))
            .background(palette[book.title.hashCode().absoluteValue % palette.size]),
    ) {
        val context = LocalContext.current.applicationContext
        val loadedCover by produceState<ImageBitmap?>(
            initialValue = null,
            key1 = book.id,
            key2 = book.coverEndpoint,
            key3 = book.coverUrl,
        ) {
            value = CoverImageLoader.load(context, book)?.asImageBitmap()
        }
        if (loadedCover != null) {
            Image(
                bitmap = requireNotNull(loadedCover),
                contentDescription = "Cover for ${book.title}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else if (book.title.startsWith("Alice's Adventures")) {
            Image(
                painter = painterResource(R.drawable.alice_cover),
                contentDescription = "Cover for " + book.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    if (book.format == BookFormat.Audiobook) "AUDIOBOOK" else "EBOOK",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
                Column {
                    Text(
                        book.title,
                        color = Color.White,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        lineHeight = 18.sp,
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        book.creator,
                        color = Color.White.copy(alpha = 0.75f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 10.sp,
                    )
                }
            }
        }
        if (book.progress > 0f) {
            LinearProgressIndicator(
                progress = { book.progress },
                modifier = Modifier.fillMaxWidth().height(4.dp).align(Alignment.BottomCenter),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Black.copy(alpha = 0.35f),
            )
        }
    }
}

@Composable
private fun BookDetailDialog(
    book: LibraryBook,
    connected: Boolean,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    formatOptions: List<LibraryBook>,
    onSelectFormat: (LibraryBook) -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 820.dp)
                .fillMaxWidth(0.78f)
                .heightIn(max = 720.dp)
                .fillMaxHeight(0.82f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 18.dp,
        ) {
            Box {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 30.dp, vertical = 28.dp),
                ) {
                    item {
                        Row(verticalAlignment = Alignment.Top) {
                            CoverArt(book, Modifier.width(190.dp))
                            Spacer(Modifier.width(28.dp))
                            BookDetail(
                                book = book,
                                connected = connected,
                                modifier = Modifier.weight(1f),
                                onOpen = onOpen,
                                formatOptions = formatOptions,
                                onSelectFormat = onSelectFormat,
                            )
                        }
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close details")
                }
            }
        }
    }
}

@Composable
private fun BookDetail(
    book: LibraryBook,
    connected: Boolean,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit,
    formatOptions: List<LibraryBook> = listOf(book),
    onSelectFormat: (LibraryBook) -> Unit = {},
) {
    val action = primaryBookAction(book, connected)
    Column(modifier) {
        Text(
            if (book.format == BookFormat.Audiobook) "AUDIOBOOK" else "EBOOK",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.1.sp,
        )
        Spacer(Modifier.height(7.dp))
        Text(
            book.title,
            style = MaterialTheme.typography.headlineMedium,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        Text(book.creator, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (!book.narrator.isNullOrBlank()) {
            Text("Narrated by " + book.narrator, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(15.dp))
        if (formatOptions.size > 1) {
            Text("Choose how to open it", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(7.dp))
            SingleChoiceSegmentedButtonRow {
                formatOptions.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = option.id == book.id,
                        onClick = { onSelectFormat(option) },
                        shape = SegmentedButtonDefaults.itemShape(index, formatOptions.size),
                        icon = {
                            Icon(
                                if (option.format == BookFormat.Audiobook) Icons.Rounded.Headphones
                                else Icons.AutoMirrored.Rounded.MenuBook,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                        label = {
                            Text(if (option.format == BookFormat.Audiobook) "Listen" else "Read")
                        },
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (book.progress > 0f) {
                AssistChip(
                    onClick = {},
                    label = { Text((book.progress * 100).roundToInt().toString() + "%") },
                    leadingIcon = {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                )
            }
            if (formatOptions.size == 1) {
                AssistChip(
                    onClick = {},
                    label = { Text(if (book.format == BookFormat.Audiobook) "Listen" else "Read") },
                    leadingIcon = {
                        Icon(
                            if (book.format == BookFormat.Audiobook) Icons.Rounded.Headphones
                            else Icons.AutoMirrored.Rounded.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                )
            }
        }
        if (book.progress > 0f) {
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(progress = { book.progress }, modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(17.dp))
        Button(
            onClick = onOpen,
            enabled = primaryBookActionEnabled(book, connected),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Icon(
                if (book.format == BookFormat.Audiobook) Icons.Rounded.PlayArrow
                else Icons.AutoMirrored.Rounded.MenuBook,
                contentDescription = null,
            )
            Spacer(Modifier.width(7.dp))
            Text(action)
        }
        Spacer(Modifier.height(22.dp))
        Text("Availability", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        if (book.sources.isEmpty()) {
            SourceRow(
                source = BookSource(
                    id = "unknown",
                    name = "OpenReader",
                    kind = CatalogSourceKind.Audiobookshelf,
                    availability = CatalogAvailability.Unavailable,
                ),
            )
        } else {
            for (source in book.sources) {
                SourceRow(source)
            }
        }
        book.description?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(22.dp))
            Text("About this book", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(7.dp))
            Text(
                it,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 12,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SourceRow(source: BookSource) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            sourceIcon(source.kind),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(source.name, fontWeight = FontWeight.Medium)
            source.detail?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(
            availabilityLabel(source.availability),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun SearchDialog(
    books: List<LibraryBook>,
    publicDomainBooks: List<LibraryBook>,
    publicDomainSearching: Boolean,
    onSearchPublicDomain: (String) -> Unit,
    onDismiss: () -> Unit,
    onSelectBook: (LibraryBook) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    LaunchedEffect(query) {
        delay(400)
        onSearchPublicDomain(query)
    }
    val localResults = filterLibraryBooks(books, query, LibraryFilter.All)
    val results = if (query.isBlank()) localResults else CatalogMerger.merge(localResults, publicDomainBooks)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.88f).widthIn(max = 720.dp).fillMaxHeight(0.82f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 18.dp,
        ) {
            Column(Modifier.fillMaxSize().padding(22.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Close search")
                    }
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Search titles, authors, and narrators") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    if (query.isBlank()) "Your library" else results.size.toString() + " results",
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(Modifier.height(10.dp))
                if (results.isEmpty()) {
                    if (publicDomainSearching) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        EmptyState(
                            icon = Icons.Rounded.Search,
                            title = "No matching books",
                            detail = "Try another title, author, or narrator.",
                        )
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        items(results, key = { "search-" + it.id }) { book ->
                            LibraryListRow(book, onSelectBook)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyLibraryMessage(onBrowse: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 26.dp, vertical = 52.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.AutoMirrored.Rounded.LibraryBooks,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(38.dp),
        )
        Spacer(Modifier.height(11.dp))
        Text("Your library is ready", style = MaterialTheme.typography.titleLarge)
        Text("Connect a source or browse available catalogs.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = onBrowse) { Text("Browse") }
    }
}

@Composable
private fun EmptyState(icon: ImageVector, title: String, detail: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(38.dp))
        Spacer(Modifier.height(11.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
private fun MiniPlayer(book: LibraryBook) {
    val playback by PlaybackService.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    Surface(
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 10.dp,
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().height(82.dp).padding(horizontal = 16.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MiniCover(book)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(book.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                    Text(
                        playback.trackTitle.ifBlank { book.creator },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Slider(
                        value = playback.progress,
                        onValueChange = { progress ->
                            if (playback.totalDurationMs > 0L) {
                                PlaybackService.seekTo(context, (playback.totalDurationMs * progress).toLong())
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(24.dp),
                        enabled = playback.loaded && playback.totalDurationMs > 0L,
                    )
                    Text(
                        "${playback.totalPositionMs.asClock()}  ·  ${playback.totalDurationMs.asClock()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { PlaybackService.seekBy(context, -10_000L) }) {
                    Icon(Icons.Rounded.Replay10, contentDescription = "Back 10 seconds")
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(40.dp),
                    onClick = { PlaybackService.toggle(context) },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (playback.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (playback.isPlaying) "Pause audiobook" else "Resume audiobook",
                        )
                    }
                }
                IconButton(onClick = { PlaybackService.seekBy(context, 10_000L) }) {
                    Icon(Icons.Rounded.Forward10, contentDescription = "Forward 10 seconds")
                }
            }
        }
    }
}

private fun Long.asClock(): String {
    if (this <= 0L) return "0:00"
    val totalSeconds = this / 1_000L
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%d:%02d".format(minutes, seconds)
}

@Composable
private fun MiniCover(book: LibraryBook) {
    Box(
        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(7.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        val context = LocalContext.current.applicationContext
        val loadedCover by produceState<ImageBitmap?>(
            initialValue = null,
            key1 = book.id,
            key2 = book.coverEndpoint,
            key3 = book.coverUrl,
        ) {
            value = CoverImageLoader.load(context, book)?.asImageBitmap()
        }
        if (loadedCover != null) {
            Image(
                bitmap = requireNotNull(loadedCover),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else if (book.title.startsWith("Alice's Adventures")) {
            Image(
                painter = painterResource(R.drawable.alice_cover),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(Icons.Rounded.Headphones, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun OpenReaderBottomBar(
    destination: AppDestination,
    onDestination: (AppDestination) -> Unit,
) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        AppDestination.entries.forEach { item ->
            NavigationBarItem(
                selected = destination == item,
                onClick = { onDestination(item) },
                icon = { Icon(destinationIcon(item), contentDescription = item.label) },
                label = { Text(item.label) },
            )
        }
    }
}

private fun destinationIcon(destination: AppDestination) = when (destination) {
    AppDestination.Home -> Icons.Rounded.Home
    AppDestination.Library -> Icons.AutoMirrored.Rounded.LibraryBooks
    AppDestination.Browse -> Icons.Rounded.Explore
    AppDestination.Sources -> Icons.Rounded.Cloud
}

private fun sourceIcon(kind: CatalogSourceKind) = when (kind) {
    CatalogSourceKind.Audiobookshelf -> Icons.Rounded.CloudDone
    CatalogSourceKind.Arr -> Icons.Rounded.Settings
    CatalogSourceKind.PublicLibrary -> Icons.AutoMirrored.Rounded.LibraryBooks
    CatalogSourceKind.PublicDomain -> Icons.Rounded.AutoStories
    CatalogSourceKind.Bundled -> Icons.AutoMirrored.Rounded.MenuBook
}

private fun sourceKindLabel(kind: CatalogSourceKind) = when (kind) {
    CatalogSourceKind.Audiobookshelf -> "Audiobookshelf"
    CatalogSourceKind.Arr -> "ARR"
    CatalogSourceKind.PublicLibrary -> "Public libraries"
    CatalogSourceKind.PublicDomain -> "Public domain"
    CatalogSourceKind.Bundled -> "Included with app"
}

private fun availabilityLabel(availability: CatalogAvailability) = when (availability) {
    CatalogAvailability.Ready -> "Ready"
    CatalogAvailability.Available -> "Available"
    CatalogAvailability.Borrowed -> "Borrowed"
    CatalogAvailability.OnHold -> "On hold"
    CatalogAvailability.Requestable -> "Request"
    CatalogAvailability.Requested -> "Requested"
    CatalogAvailability.Downloading -> "Downloading"
    CatalogAvailability.Importing -> "Importing"
    CatalogAvailability.Unavailable -> "Unavailable"
}

private fun sourceSummary(book: LibraryBook): String {
    val source = primaryBookSource(book) ?: return if (book.isDemo) "Preview" else "OpenReader"
    return source.name + " · " + availabilityLabel(source.availability)
}
