package com.orgista.openreader.ui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.LibraryBooks
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orgista.openreader.R
import com.orgista.openreader.domain.BookFormat
import com.orgista.openreader.domain.DeviceProfile
import com.orgista.openreader.domain.LibraryBook
import com.orgista.openreader.domain.NavigationStyle
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

private enum class AppSection(val label: String) {
    Home("Home"),
    Library("Library"),
    Listen("Listen"),
    Read("Read"),
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
    onRefresh: () -> Unit,
    onDismissError: () -> Unit,
) {
    var section by remember { mutableStateOf(AppSection.Home) }
    var searchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val selectSection: (AppSection) -> Unit = { selected ->
        section = selected
        onFilter(
            when (selected) {
                AppSection.Listen -> LibraryFilter.Audiobooks
                AppSection.Read -> LibraryFilter.Ebooks
                else -> LibraryFilter.All
            },
        )
    }

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

    if (deviceProfile.navigationStyle == NavigationStyle.Rail) {
        Row(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            OpenReaderNavigationRail(section, selectSection)
            VerticalDivider(modifier = Modifier.fillMaxHeight())
            Scaffold(
                modifier = Modifier.weight(1f),
                snackbarHost = { SnackbarHost(snackbarHostState) },
                containerColor = MaterialTheme.colorScheme.background,
            ) { padding ->
                LibraryWorkspace(
                    state = state,
                    section = section,
                    deviceProfile = deviceProfile,
                    modifier = Modifier.padding(padding),
                    onFilter = onFilter,
                    onSelectBook = onSelectBook,
                    onOpenBook = onOpenBook,
                    onShowConnection = onShowConnection,
                    onRefresh = onRefresh,
                    searchActive = searchActive,
                    searchQuery = searchQuery,
                    onSearchActiveChange = { searchActive = it },
                    onSearchQueryChange = { searchQuery = it },
                )
            }
        }
    } else {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = { OpenReaderBottomBar(section, selectSection) },
            containerColor = MaterialTheme.colorScheme.background,
        ) { padding ->
            LibraryWorkspace(
                state = state,
                section = section,
                deviceProfile = deviceProfile,
                modifier = Modifier.padding(padding),
                onFilter = onFilter,
                onSelectBook = onSelectBook,
                onOpenBook = onOpenBook,
                onShowConnection = onShowConnection,
                onRefresh = onRefresh,
                searchActive = searchActive,
                searchQuery = searchQuery,
                onSearchActiveChange = { searchActive = it },
                onSearchQueryChange = { searchQuery = it },
            )
        }
    }
    if (!deviceProfile.supportsListDetail) {
        state.selectedBook?.let { selected ->
            ModalBottomSheet(onDismissRequest = { onSelectBook(null) }) {
                BookDetail(
                    book = selected,
                    connected = state.connected,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    onOpen = { onOpenBook(selected) },
                )
                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun LibraryWorkspace(
    state: OpenReaderUiState,
    section: AppSection,
    deviceProfile: DeviceProfile,
    modifier: Modifier,
    onFilter: (LibraryFilter) -> Unit,
    onSelectBook: (LibraryBook?) -> Unit,
    onOpenBook: (LibraryBook) -> Unit,
    onShowConnection: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    searchActive: Boolean,
    searchQuery: String,
    onSearchActiveChange: (Boolean) -> Unit,
    onSearchQueryChange: (String) -> Unit,
) {
    Column(modifier.fillMaxSize()) {
        LibraryTopBar(
            connected = state.connected,
            username = state.username,
            loading = state.loading,
            onShowConnection = { onShowConnection(true) },
            onRefresh = onRefresh,
            searchActive = searchActive,
            searchQuery = searchQuery,
            onSearchActiveChange = onSearchActiveChange,
            onSearchQueryChange = onSearchQueryChange,
        )
        if (!state.connected) {
            ConnectionBanner { onShowConnection(true) }
        }
        Row(Modifier.weight(1f).fillMaxWidth()) {
            LibraryFeed(
                state = state,
                showContinue = section == AppSection.Home,
                modifier = Modifier.weight(1f),
                searchQuery = searchQuery,
                onFilter = onFilter,
                onSelectBook = onSelectBook,
            )
            if (deviceProfile.supportsListDetail) {
                VerticalDivider(modifier = Modifier.fillMaxHeight())
                Box(
                    modifier = Modifier.widthIn(min = 330.dp, max = 420.dp).fillMaxHeight(),
                    contentAlignment = Alignment.Center,
                ) {
                    val selected = state.selectedBook ?: state.visibleBooks.firstOrNull()
                    if (selected != null) {
                        BookDetail(
                            book = selected,
                            connected = state.connected,
                            modifier = Modifier.padding(28.dp),
                            onOpen = { onOpenBook(selected) },
                        )
                    } else {
                        Text("No books found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        state.nowPlaying?.let { MiniPlayer(it) }
    }
}

@Composable
private fun LibraryTopBar(
    connected: Boolean,
    username: String,
    loading: Boolean,
    onShowConnection: () -> Unit,
    onRefresh: () -> Unit,
    searchActive: Boolean,
    searchQuery: String,
    onSearchActiveChange: (Boolean) -> Unit,
    onSearchQueryChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (searchActive) {
            TextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search your library") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            onSearchQueryChange("")
                            onSearchActiveChange(false)
                        },
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close search")
                    }
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                ),
            )
        } else {
            Icon(Icons.Rounded.AutoStories, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Text("OpenReader", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { onSearchActiveChange(true) }) {
                Icon(Icons.Rounded.Search, contentDescription = "Search")
            }
        }
        if (connected) {
            IconButton(onClick = onRefresh, enabled = !loading) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Refresh library")
            }
        }
        IconButton(onClick = onShowConnection) {
            Icon(
                Icons.Rounded.AccountCircle,
                contentDescription = if (connected) "Account for $username" else "Connect account",
                tint = if (connected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        }
    }
    HorizontalDivider()
}

@Composable
private fun ConnectionBanner(onConnect: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Preview library",
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Medium,
            )
            Button(onClick = onConnect) { Text("Connect") }
        }
    }
}

@Composable
private fun LibraryFeed(
    state: OpenReaderUiState,
    showContinue: Boolean,
    modifier: Modifier,
    searchQuery: String,
    onFilter: (LibraryFilter) -> Unit,
    onSelectBook: (LibraryBook) -> Unit,
) {
    val visibleBooks = state.visibleBooks.filter { book ->
        searchQuery.isBlank() ||
            book.title.contains(searchQuery, ignoreCase = true) ||
            book.creator.contains(searchQuery, ignoreCase = true)
    }
    Column(modifier.padding(top = 20.dp)) {
        if (showContinue) {
            val continuing = visibleBooks.filter { it.progress > 0f }.take(8)
            if (continuing.isNotEmpty()) {
                SectionTitle("Continue")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(continuing, key = { it.id }) { book ->
                        BookCard(book, Modifier.width(142.dp), onSelectBook)
                    }
                }
                Spacer(Modifier.height(22.dp))
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Library", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            FilterControl(state.filter, onFilter)
        }
        Spacer(Modifier.height(14.dp))
        if (visibleBooks.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No books found", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 142.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                items(visibleBooks, key = { it.id }) { book ->
                    BookCard(book, Modifier.fillMaxWidth(), onSelectBook)
                }
            }
        }
    }
}

@Composable
private fun FilterControl(selected: LibraryFilter, onFilter: (LibraryFilter) -> Unit) {
    val filters = LibraryFilter.entries
    SingleChoiceSegmentedButtonRow {
        filters.forEachIndexed { index, filter ->
            SegmentedButton(
                selected = selected == filter,
                onClick = { onFilter(filter) },
                shape = SegmentedButtonDefaults.itemShape(index, filters.size),
                icon = {},
                label = {
                    Icon(
                        when (filter) {
                            LibraryFilter.All -> Icons.AutoMirrored.Rounded.LibraryBooks
                            LibraryFilter.Audiobooks -> Icons.Rounded.Headphones
                            LibraryFilter.Ebooks -> Icons.AutoMirrored.Rounded.MenuBook
                        },
                        contentDescription = filter.name,
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun BookCard(book: LibraryBook, modifier: Modifier, onClick: (LibraryBook) -> Unit) {
    Card(
        modifier = modifier.clickable { onClick(book) },
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column {
            CoverArt(book, Modifier.fillMaxWidth())
            Spacer(Modifier.height(9.dp))
            Text(
                book.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                book.creator,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CoverArt(book: LibraryBook, modifier: Modifier = Modifier) {
    val palette = listOf(
        Color(0xFF26547C),
        Color(0xFF5D2A42),
        Color(0xFF2A6F62),
        Color(0xFF725A2C),
        Color(0xFF4C4F7C),
    )
    Box(
        modifier = modifier.aspectRatio(0.72f).clip(RoundedCornerShape(6.dp))
            .background(palette[book.title.hashCode().absoluteValue % palette.size]),
    ) {
        if (book.title.startsWith("Alice's Adventures")) {
            Image(
                painter = painterResource(R.drawable.alice_cover),
                contentDescription = "Cover for ${book.title}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Icon(
                    if (book.format == BookFormat.Audiobook) Icons.Rounded.Headphones else Icons.AutoMirrored.Rounded.MenuBook,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.9f),
                )
                Text(
                    book.title,
                    color = Color.White,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                )
            }
        }
        if (book.progress > 0f) {
            LinearProgressIndicator(
                progress = { book.progress },
                modifier = Modifier.fillMaxWidth().height(4.dp).align(Alignment.BottomCenter),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Black.copy(alpha = 0.25f),
            )
        }
    }
}

@Composable
private fun BookDetail(
    book: LibraryBook,
    connected: Boolean,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        CoverArt(book, Modifier.widthIn(max = 220.dp))
        Spacer(Modifier.height(20.dp))
        Text(
            book.title,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            book.creator,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (book.progress > 0f) {
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(progress = { book.progress }, modifier = Modifier.fillMaxWidth())
            Text(
                "${(book.progress * 100).roundToInt()}%",
                modifier = Modifier.align(Alignment.End).padding(top = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(20.dp))
        Button(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
            Icon(
                if (book.format == BookFormat.Audiobook) Icons.Rounded.PlayArrow else Icons.AutoMirrored.Rounded.MenuBook,
                contentDescription = null,
            )
            Spacer(Modifier.width(8.dp))
            Text(if (!connected || book.isDemo) "Connect" else if (book.format == BookFormat.Audiobook) "Play" else "Read")
        }
        book.description?.let {
            Spacer(Modifier.height(20.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MiniPlayer(book: LibraryBook) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, tonalElevation = 3.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Headphones, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(book.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                Text(book.creator, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Rounded.PlayArrow, contentDescription = "Playing")
        }
    }
}

@Composable
private fun OpenReaderNavigationRail(section: AppSection, onSelect: (AppSection) -> Unit) {
    NavigationRail(header = {
        Icon(
            Icons.Rounded.AutoStories,
            contentDescription = "OpenReader",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 18.dp),
        )
    }) {
        Spacer(Modifier.weight(1f))
        AppSection.entries.forEach { item ->
            NavigationRailItem(
                selected = section == item,
                onClick = { onSelect(item) },
                icon = { Icon(sectionIcon(item), contentDescription = item.label) },
                label = { Text(item.label) },
            )
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun OpenReaderBottomBar(section: AppSection, onSelect: (AppSection) -> Unit) {
    NavigationBar {
        AppSection.entries.forEach { item ->
            NavigationBarItem(
                selected = section == item,
                onClick = { onSelect(item) },
                icon = { Icon(sectionIcon(item), contentDescription = item.label) },
                label = { Text(item.label) },
            )
        }
    }
}

private fun sectionIcon(section: AppSection) = when (section) {
    AppSection.Home -> Icons.Rounded.Home
    AppSection.Library -> Icons.AutoMirrored.Rounded.LibraryBooks
    AppSection.Listen -> Icons.Rounded.Headphones
    AppSection.Read -> Icons.AutoMirrored.Rounded.MenuBook
}
