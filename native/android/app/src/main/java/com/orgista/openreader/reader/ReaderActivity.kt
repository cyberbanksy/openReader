package com.orgista.openreader.reader

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.ActionMode
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commitNow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.orgista.openreader.data.AudiobookshelfApi
import com.orgista.openreader.data.CoverImageLoader
import com.orgista.openreader.data.EbookFile
import com.orgista.openreader.data.PlaybackDescriptor
import com.orgista.openreader.data.PlaybackTrack
import com.orgista.openreader.data.PublicDomainCatalogApi
import com.orgista.openreader.data.PublicDomainSettings
import com.orgista.openreader.data.SessionStore
import com.orgista.openreader.domain.BookFormat
import com.orgista.openreader.domain.BundledContent
import com.orgista.openreader.domain.LibraryBook
import com.orgista.openreader.playback.PlaybackService
import com.orgista.openreader.playback.PlaybackUiState
import com.orgista.openreader.ui.theme.OpenReaderTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.readium.r2.navigator.Decoration
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.html.HtmlDecorationTemplates
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.navigator.preferences.ColumnCount
import org.readium.r2.navigator.preferences.FontFamily as ReaderFontFamily
import org.readium.r2.navigator.preferences.Spread
import org.readium.r2.navigator.preferences.Theme as ReaderTheme
import org.readium.r2.navigator.util.BaseActionModeCallback
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.positions
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.toUri
import org.readium.r2.shared.util.toUrl
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
import java.io.File
import java.util.UUID
import kotlin.math.roundToInt

@OptIn(ExperimentalReadiumApi::class)
class ReaderActivity : FragmentActivity(), EpubNavigatorFragment.Listener {
    private lateinit var bookId: String
    private lateinit var bookTitle: String
    private var publicDomainSourceId: String? = null
    private var bundledEbookAsset: String? = null
    private var audiobookId: String? = null
    private var audiobookTitle: String? = null
    private var audiobookCreator: String? = null
    private lateinit var preferences: ReaderPreferences
    private var publication: Publication? = null
    private var navigator: EpubNavigatorFragment? = null
    private var navigatorContainer: FragmentContainerView? = null
    private var topChrome: ComposeView? = null
    private var bottomChrome: ComposeView? = null
    private var followChrome: ComposeView? = null
    private var chromeVisible = true
    private var showAppearance by mutableStateOf(false)
    private var locationLabel by mutableStateOf("Opening book")
    private var chapterLabel by mutableStateOf("")
    private var readingProgress by mutableFloatStateOf(0f)
    private var fontSize by mutableDoubleStateOf(1.0)
    private var themeIndex by mutableIntStateOf(0)
    private var readerLayout by mutableStateOf(ReaderLayout.Page)
    private var highlights by mutableStateOf(emptyList<ReaderHighlight>())
    private var publicationPositions = emptyList<Locator>()
    private var followAlong by mutableStateOf(false)
    private var followPassages by mutableStateOf(FollowAlongPassages())
    private var readerDocumentPassages = emptyList<String>()
    private var lastFollowAlongPosition = -1
    private var audioStarting by mutableStateOf(false)
    private var audioError by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            supportFragmentManager.fragmentFactory = EpubNavigatorFragment.createDummyFactory()
        }
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            finish()
            return
        }

        bookId = intent.getStringExtra(EXTRA_BOOK_ID).orEmpty()
        bookTitle = intent.getStringExtra(EXTRA_BOOK_TITLE).orEmpty().ifBlank { "Book" }
        publicDomainSourceId = intent.getStringExtra(EXTRA_PUBLIC_DOMAIN_SOURCE_ID)
        bundledEbookAsset = intent.getStringExtra(EXTRA_BUNDLED_EBOOK_ASSET)
        audiobookId = intent.getStringExtra(EXTRA_AUDIOBOOK_ID)
        audiobookTitle = intent.getStringExtra(EXTRA_AUDIOBOOK_TITLE)
        audiobookCreator = intent.getStringExtra(EXTRA_AUDIOBOOK_CREATOR)
        if (bookId.isBlank()) {
            showFailure("The selected book is missing an ID.")
            return
        }
        preferences = ReaderPreferences(this, bookId)
        fontSize = preferences.fontSize
        themeIndex = THEMES.indexOf(preferences.theme).coerceAtLeast(0)
        readerLayout = preferences.layout
        highlights = preferences.loadHighlights()
        showLoading()
        lifecycleScope.launch {
            runCatching { openPublication() }
                .onSuccess(::showPublication)
                .onFailure { showFailure(it.message ?: "The EPUB could not be opened.") }
        }
    }

    private suspend fun copyBundledAsset(assetPath: String): File = withContext(Dispatchers.IO) {
        val cacheDirectory = File(cacheDir, "epubs").apply { mkdirs() }
        val destination = File(cacheDirectory, EbookFile.cacheName(bookId))
        if (!destination.isFile || destination.length() == 0L) {
            assets.open(assetPath).use { input ->
                destination.outputStream().use { output -> input.copyTo(output) }
            }
        }
        destination
    }

    private suspend fun openPublication(): Publication {
        val file = bundledEbookAsset?.let { assetPath ->
            copyBundledAsset(assetPath)
        } ?: publicDomainSourceId?.let { sourceId ->
            PublicDomainCatalogApi().downloadEbook(
                sourceId = sourceId,
                standardEbooksEmail = PublicDomainSettings(this).standardEbooksEmail(),
                cacheDirectory = File(cacheDir, "epubs"),
            )
        } ?: run {
            val session = requireNotNull(SessionStore(this).load()) {
                "Connect to Audiobookshelf before opening this book."
            }
            AudiobookshelfApi().downloadEbook(
                session = session,
                bookId = bookId,
                cacheDirectory = File(cacheDir, "epubs"),
            )
        }
        Log.i(LOG_TAG, "Opening ebook ${file.absolutePath} (${file.length()} bytes)")
        val client = DefaultHttpClient()
        val retriever = AssetRetriever(contentResolver, client)
        val parser = DefaultPublicationParser(this, client, retriever, null)
        val opener = PublicationOpener(parser)
        val asset = retriever.retrieve(file.toUrl(isDirectory = false))
            .getOrElse { throw IllegalStateException(it.message) }
        return opener.open(asset, allowUserInteraction = false)
            .getOrElse {
                asset.close()
                throw IllegalStateException(it.message)
            }
    }

    private fun showPublication(openedPublication: Publication) {
        publication = openedPublication
        val containerId = View.generateViewId()
        val root = FrameLayout(this).apply { setBackgroundColor(PARCHMENT) }
        val container = FragmentContainerView(this).apply { id = containerId }
        navigatorContainer = container
        root.addView(
            container,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )
        addChrome(root)
        setContentView(root)

        val factory = EpubNavigatorFactory(openedPublication)
        supportFragmentManager.fragmentFactory = factory.createFragmentFactory(
            initialLocator = preferences.loadLocator(),
            initialPreferences = readerPreferences(),
            listener = this,
            configuration = EpubNavigatorFragment.Configuration {
                selectionActionModeCallback = selectionActionModeCallback
                decorationTemplates = HtmlDecorationTemplates.defaultTemplates(
                    alpha = 1.0,
                    experimentalPositioning = true,
                )
            },
            paginationListener = object : EpubNavigatorFragment.PaginationListener {
                override fun onPageChanged(pageIndex: Int, totalPages: Int, locator: org.readium.r2.shared.publication.Locator) {
                    updateLocation(locator, pageIndex + 1, totalPages)
                }
            },
        )
        supportFragmentManager.commitNow {
            add(containerId, EpubNavigatorFragment::class.java, Bundle(), NAVIGATOR_TAG)
        }
        navigator = supportFragmentManager.findFragmentByTag(NAVIGATOR_TAG) as EpubNavigatorFragment
        navigator?.addInputListener(object : InputListener {
            override fun onTap(event: TapEvent): Boolean {
                val width = navigator?.publicationView?.width?.takeIf { it > 0 } ?: return false
                when {
                    event.point.x < width * 0.28f -> navigator?.goBackward(animated = true)
                    event.point.x > width * 0.72f -> navigator?.goForward(animated = true)
                    else -> toggleReaderChrome()
                }
                return true
            }
        })
        observeLocator()
        lifecycleScope.launch {
            publicationPositions = openedPublication.positions()
            applyHighlights()
            if (followAlong) syncFollowAlong(PlaybackService.state.value)
        }
        observePlayback()
        lifecycleScope.launch {
            delay(2_800)
            if (chromeVisible && !showAppearance && !followAlong) setReaderChromeVisible(false)
        }
    }

    private fun addChrome(root: FrameLayout) {
        val topBar = ComposeView(this).apply {
            setContent { OpenReaderTheme { ReaderTopBar() } }
        }
        val bottomBar = ComposeView(this).apply {
            setContent { OpenReaderTheme { ReaderBottomBar() } }
        }
        val followView = ComposeView(this).apply {
            visibility = View.GONE
            setContent { OpenReaderTheme { ReaderFollowAlong() } }
        }
        topChrome = topBar
        bottomChrome = bottomBar
        followChrome = followView
        root.addView(
            topBar,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dp(TOP_CHROME_HEIGHT)).apply {
                gravity = Gravity.TOP
            },
        )
        root.addView(
            bottomBar,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dp(BOTTOM_CHROME_HEIGHT)).apply {
                gravity = Gravity.BOTTOM
            },
        )
        root.addView(
            followView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )
    }

    private fun toggleReaderChrome() {
        setReaderChromeVisible(!chromeVisible)
    }

    private fun setReaderChromeVisible(visible: Boolean) {
        chromeVisible = visible
        if (!visible) showAppearance = false
        topChrome?.visibility = if (visible) View.VISIBLE else View.GONE
        bottomChrome?.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun observeLocator() {
        val activeNavigator = navigator ?: return
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                activeNavigator.currentLocator.collect { locator ->
                    preferences.saveLocator(locator)
                    updateLocation(locator)
                }
            }
        }
    }

    private fun updateLocation(
        locator: org.readium.r2.shared.publication.Locator,
        page: Int? = null,
        totalPages: Int? = null,
    ) {
        locator.title?.takeIf(String::isNotBlank)?.let { chapterLabel = it }
        readingProgress = (locator.locations.totalProgression ?: 0.0).toFloat().coerceIn(0f, 1f)
        val percent = (readingProgress * 100).roundToInt()
        locationLabel = if (page != null && totalPages != null && totalPages > 0) {
            "Page $page of $totalPages  ·  $percent%"
        } else {
            "$percent% read"
        }
    }

    private val selectionActionModeCallback: ActionMode.Callback by lazy {
        object : BaseActionModeCallback() {
            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                menu.add(0, MENU_HIGHLIGHT_AMBER, 0, "Highlight amber")
                menu.add(0, MENU_HIGHLIGHT_BLUE, 1, "Highlight blue")
                menu.add(0, MENU_HIGHLIGHT_ROSE, 2, "Highlight rose")
                return true
            }

            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                val tint = when (item.itemId) {
                    MENU_HIGHLIGHT_AMBER -> HIGHLIGHT_AMBER
                    MENU_HIGHLIGHT_BLUE -> HIGHLIGHT_BLUE
                    MENU_HIGHLIGHT_ROSE -> HIGHLIGHT_ROSE
                    else -> return false
                }
                lifecycleScope.launch { saveCurrentSelection(tint) }
                mode.finish()
                return true
            }
        }
    }

    private suspend fun saveCurrentSelection(tint: Int) {
        val activeNavigator = navigator ?: return
        val selection = activeNavigator.currentSelection() ?: return
        highlights = highlights + ReaderHighlight(
            id = UUID.randomUUID().toString(),
            locator = selection.locator,
            tint = tint,
        )
        preferences.saveHighlights(highlights)
        activeNavigator.clearSelection()
        applyHighlights()
    }

    private suspend fun applyHighlights() {
        navigator?.applyDecorations(
            decorations = highlights.map { highlight ->
                Decoration(
                    id = highlight.id,
                    locator = highlight.locator,
                    style = Decoration.Style.Highlight(tint = highlight.tint),
                )
            },
            group = HIGHLIGHTS_GROUP,
        )
    }

    private fun observePlayback() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
            PlaybackService.state.collect { playback ->
                if (followAlong && playback.belongsToPairedAudiobook()) {
                    runCatching { syncFollowAlong(playback) }
                        .onFailure { Log.w(LOG_TAG, "Could not refresh follow-along text", it) }
                }
            }
            }
        }
    }

    private suspend fun syncFollowAlong(playback: PlaybackUiState) {
        if (publicationPositions.isEmpty() || playback.totalDurationMs <= 0L) return
        val positionIndex = (playback.progress * publicationPositions.lastIndex)
            .roundToInt()
            .coerceIn(publicationPositions.indices)
        if (
            positionIndex == lastFollowAlongPosition &&
            followPassages.current != "Preparing the next passage…" &&
            followPassages.current != "Listening…"
        ) return
        lastFollowAlongPosition = positionIndex
        val mappedPassages = FollowAlongPassageMapper.fromLocators(publicationPositions, positionIndex)
        if (mappedPassages.current != "Listening…") followPassages = mappedPassages
        if (followPassages.current == "Preparing the next passage…" || followPassages.current == "Listening…") {
            documentPassages().takeIf(List<String>::isNotEmpty)?.let { text ->
                followPassages = FollowAlongPassageMapper.fromTexts(
                    text,
                    (playback.progress * text.lastIndex).roundToInt(),
                )
            }
        }
        val locator = publicationPositions[positionIndex]
        navigator?.go(locator, animated = false)
        navigator?.applyDecorations(
            listOf(
                Decoration(
                    id = FOLLOW_ALONG_DECORATION,
                    locator = locator,
                    style = Decoration.Style.Highlight(tint = HIGHLIGHT_AMBER, isActive = true),
                ),
            ),
            FOLLOW_ALONG_GROUP,
        )
        delay(250)
        val visiblePassages = extractVisiblePassages()
        if (visiblePassages.isNotEmpty()) {
            val activePassageIndex = findActivePassageIndex(visiblePassages, locator)
            followPassages = FollowAlongPassageMapper.fromTexts(visiblePassages, activePassageIndex)
        }
        if (followPassages.current == "Preparing the next passage…") {
            followPassages = FollowAlongPassages(current = "Listening…")
        }
        applyLineGuide(locator)
    }

    private suspend fun extractVisiblePassages(): List<String> {
        val raw = navigator?.evaluateJavascript(
            """
            JSON.stringify(
              [...document.querySelectorAll('p, li, h1, h2, h3, blockquote')]
                .map(node => (node.innerText || node.textContent || '').replace(/\s+/g, ' ').trim())
                .filter(text => text.length >= 12)
            )
            """.trimIndent(),
        )
        return FollowAlongDocumentTextParser.parse(raw)
    }

    private suspend fun documentPassages(): List<String> {
        if (readerDocumentPassages.isNotEmpty()) return readerDocumentPassages
        val source = File(cacheDir, "epubs/$bookId.epub").takeIf(File::isFile) ?: return emptyList()
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            java.util.zip.ZipFile(source).use { archive ->
                val allPassages = archive.entries().asSequence()
                    .filter { entry ->
                        !entry.isDirectory &&
                            (entry.name.endsWith(".xhtml", true) || entry.name.endsWith(".html", true)) &&
                            !entry.name.contains("toc", true) &&
                            !entry.name.contains("cover", true)
                    }
                    .flatMap { entry ->
                        val document = archive.getInputStream(entry).bufferedReader().use { it.readText() }
                        FOLLOW_ALONG_BLOCK_REGEX.findAll(document)
                            .map { match ->
                                match.groupValues[1]
                                    .replace(FOLLOW_ALONG_TAG_REGEX, " ")
                                    .replace(FOLLOW_ALONG_ENTITY_REGEX) { entity ->
                                        when (entity.value) {
                                            "&amp;" -> "&"
                                            "&quot;" -> "\""
                                            "&apos;", "&#39;" -> "'"
                                            "&lt;" -> "<"
                                            "&gt;" -> ">"
                                            else -> " "
                                        }
                                    }
                                    .replace(Regex("&#(\\d+);")) { entity ->
                                        entity.groupValues[1].toIntOrNull()?.toChar()?.toString().orEmpty()
                                    }
                                    .replace(Regex("\\s+"), " ")
                                    .trim()
                            }
                    }
                    .filter { it.length >= 12 }
                    .distinct()
                    .toList()
                val firstStoryPassage = allPassages.indexOfFirst { passage ->
                    passage.startsWith("Alice ", ignoreCase = true) ||
                        passage.contains("Alice was beginning", ignoreCase = true)
                }.takeIf { it >= 0 } ?: 0
                allPassages.drop(firstStoryPassage)
            }
        }.also { readerDocumentPassages = it }
    }

    private fun findActivePassageIndex(passages: List<String>, locator: Locator): Int {
        val needle = locator.text.highlight.orEmpty()
            .ifBlank { locator.text.after.orEmpty() }
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(45)
            .lowercase()
        if (needle.isNotBlank()) {
            passages.indexOfFirst { it.lowercase().contains(needle) }
                .takeIf { it >= 0 }
                ?.let { return it }
        }
        return (passages.lastIndex * (locator.locations.progression ?: 0.0))
            .roundToInt()
            .coerceIn(passages.indices)
    }

    private fun updateFollowAlong(enabled: Boolean) {
        followAlong = enabled
        lastFollowAlongPosition = -1
        if (!enabled) {
            followChrome?.visibility = View.GONE
            setReaderChromeVisible(true)
            lifecycleScope.launch {
                navigator?.applyDecorations(emptyList(), FOLLOW_ALONG_GROUP)
                clearLineGuide()
            }
            return
        }
        followChrome?.visibility = View.VISIBLE
        setReaderChromeVisible(false)
        val playback = PlaybackService.state.value
        if (playback.belongsToPairedAudiobook()) {
            lifecycleScope.launch { syncFollowAlong(playback) }
        } else {
            startPairedAudiobook(followText = true)
        }
    }

    private fun startPairedAudiobook(followText: Boolean) {
        val id = audiobookId ?: return
        if (audioStarting) return
        audioStarting = true
        audioError = null
        lifecycleScope.launch {
            runCatching {
                val bundledAsset = BundledContent.audiobookAsset(id)
                if (bundledAsset != null) {
                    PlaybackDescriptor(
                        sessionId = "bundled-$id",
                        title = audiobookTitle ?: bookTitle,
                        creator = audiobookCreator.orEmpty(),
                        coverUrl = "",
                        currentTimeSeconds = 0.0,
                        tracks = listOf(
                            PlaybackTrack(
                                index = 1,
                                title = audiobookTitle ?: bookTitle,
                                url = "asset:///${bundledAsset.assetPath}",
                                durationSeconds = bundledAsset.durationSeconds,
                            ),
                        ),
                    )
                } else {
                    val session = requireNotNull(SessionStore(this@ReaderActivity).load()) {
                        "Connect to Audiobookshelf before listening."
                    }
                    AudiobookshelfApi().startPlayback(
                        session,
                        LibraryBook(
                            id = id,
                            libraryId = "",
                            title = audiobookTitle ?: bookTitle,
                            creator = audiobookCreator.orEmpty(),
                            format = BookFormat.Audiobook,
                        ),
                    )
                }
            }.onSuccess { descriptor ->
                PlaybackService.play(this@ReaderActivity, descriptor)
                if (followText) updateFollowAlong(true)
                audioStarting = false
            }.onFailure { failure ->
                if (followText) updateFollowAlong(false)
                audioStarting = false
                audioError = failure.message ?: "The audiobook could not start."
            }
        }
    }

    private fun PlaybackUiState.belongsToPairedAudiobook(): Boolean =
        loaded && normalizePairTitle(title) == normalizePairTitle(audiobookTitle ?: bookTitle)

    private fun normalizePairTitle(value: String): String = value
        .replace(Regex("\\s*\\(version\\s+\\d+\\)\\s*$", RegexOption.IGNORE_CASE), "")
        .lowercase()
        .filter(Char::isLetterOrDigit)

    private suspend fun applyLineGuide(locator: Locator) {
        val needle = locator.text.highlight.orEmpty()
            .ifBlank { locator.text.after.orEmpty() }
            .trim()
            .take(90)
        val encodedNeedle = JSONObject.quote(needle)
        navigator?.evaluateJavascript(
            """
            (() => {
              const styleId = 'openreader-follow-style';
              let style = document.getElementById(styleId);
              if (!style) {
                style = document.createElement('style');
                style.id = styleId;
                style.textContent = `
                  .openreader-following p, .openreader-following li,
                  .openreader-following h1, .openreader-following h2,
                  .openreader-following h3, .openreader-following blockquote {
                    opacity: .18 !important;
                    transition: opacity 180ms ease, background-color 180ms ease;
                  }
                  .openreader-following .openreader-follow-current {
                    opacity: 1 !important;
                    background: rgba(242, 201, 109, .42) !important;
                    border-radius: .55em;
                    box-shadow: 0 0 0 .35em rgba(242, 201, 109, .42);
                  }
                `;
                document.head.appendChild(style);
              }
              const blocks = [...document.querySelectorAll('p, li, h1, h2, h3, blockquote')];
              blocks.forEach(node => node.classList.remove('openreader-follow-current'));
              document.documentElement.classList.add('openreader-following');
              const normalize = value => (value || '').replace(/\s+/g, ' ').trim().toLowerCase();
              const needle = normalize($encodedNeedle);
              const target = needle
                ? blocks.find(node => normalize(node.textContent).includes(needle.slice(0, 45)))
                : null;
              if (target) {
                target.classList.add('openreader-follow-current');
                target.scrollIntoView({block: 'center', behavior: 'smooth'});
              }
              return Boolean(target);
            })();
            """.trimIndent(),
        )
    }

    private suspend fun clearLineGuide() {
        navigator?.evaluateJavascript(
            """
            (() => {
              document.documentElement.classList.remove('openreader-following');
              document.querySelectorAll('.openreader-follow-current')
                .forEach(node => node.classList.remove('openreader-follow-current'));
            })();
            """.trimIndent(),
        )
    }

    private fun readerPreferences() = EpubPreferences(
        columnCount = when (readerLayout) {
            ReaderLayout.Page, ReaderLayout.Scroll -> ColumnCount.ONE
            ReaderLayout.Spread -> ColumnCount.TWO
        },
        fontFamily = ReaderFontFamily.SERIF,
        fontSize = fontSize,
        lineHeight = 1.45,
        pageMargins = when (readerLayout) {
            ReaderLayout.Page -> 1.65
            ReaderLayout.Spread -> 1.45
            ReaderLayout.Scroll -> 1.85
        },
        paragraphSpacing = 0.25,
        publisherStyles = false,
        scroll = readerLayout == ReaderLayout.Scroll,
        spread = when (readerLayout) {
            ReaderLayout.Page, ReaderLayout.Scroll -> Spread.NEVER
            ReaderLayout.Spread -> Spread.ALWAYS
        },
        theme = THEMES[themeIndex],
    )

    private fun setFontScale(value: Double) {
        fontSize = value.coerceIn(0.7, 2.0)
        preferences.fontSize = fontSize
        navigator?.submitPreferences(readerPreferences())
    }

    private fun selectTheme(index: Int) {
        themeIndex = index.coerceIn(THEMES.indices)
        preferences.theme = THEMES[themeIndex]
        navigator?.submitPreferences(readerPreferences())
    }

    private fun selectLayout(layout: ReaderLayout) {
        readerLayout = layout
        preferences.layout = layout
        navigator?.submitPreferences(readerPreferences())
    }

    @Composable
    private fun ReaderTopBar() {
        val colors = readerChromeColors()
        val playback by PlaybackService.state.collectAsStateWithLifecycle()
        Surface(
            color = colors.background.copy(alpha = 0.96f),
            contentColor = colors.foreground,
            shadowElevation = 2.dp,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(TOP_CHROME_HEIGHT.dp).padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = ::finish) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = chapterLabel.ifBlank { bookTitle },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontFamily = FontFamily.Serif,
                        color = colors.muted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                    )
                }
                if (audiobookId != null) {
                    IconButton(onClick = { updateFollowAlong(true) }) {
                        Icon(
                            Icons.Rounded.Headphones,
                            contentDescription = if (playback.belongsToPairedAudiobook()) {
                                "Open audiobook follow along"
                            } else {
                                "Listen and follow along"
                            },
                        )
                    }
                }
                TextButton(onClick = { showAppearance = true }, modifier = Modifier.width(48.dp)) {
                    Text("Aa", color = colors.foreground, fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        if (showAppearance) ReaderSettingsDialog(colors)
    }

    @Composable
    private fun ReaderBottomBar() {
        val colors = readerChromeColors()
        Surface(color = colors.background.copy(alpha = 0.96f), contentColor = colors.foreground) {
            Column(
                modifier = Modifier.fillMaxWidth().height(BOTTOM_CHROME_HEIGHT.dp).padding(horizontal = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(Modifier.fillMaxWidth().height(2.dp).background(colors.muted.copy(alpha = 0.16f))) {
                    Box(
                        Modifier.fillMaxWidth(readingProgress.coerceIn(0f, 1f))
                            .height(2.dp)
                            .background(Color(AMBER)),
                    )
                }
                Spacer(Modifier.height(7.dp))
                Text(locationLabel, fontSize = 10.sp, color = colors.muted)
            }
        }
    }

    @Composable
    private fun ReaderSettingsDialog(colors: ReaderChromeColors) {
        Dialog(
            onDismissRequest = { showAppearance = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 24.dp),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Surface(
                    modifier = Modifier.widthIn(max = 520.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = colors.background,
                    contentColor = colors.foreground,
                    shadowElevation = 18.dp,
                    border = BorderStroke(1.dp, colors.muted.copy(alpha = 0.16f)),
                ) {
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Themes & Settings",
                                modifier = Modifier.weight(1f),
                                fontFamily = FontFamily.Serif,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            IconButton(onClick = { showAppearance = false }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Close settings")
                            }
                        }
                        HorizontalDivider(color = colors.muted.copy(alpha = 0.18f))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextButton(onClick = { setFontScale(fontSize - 0.1) }) { Text("A", fontSize = 12.sp) }
                            Slider(
                                value = fontSize.toFloat(),
                                onValueChange = { setFontScale(it.toDouble()) },
                                modifier = Modifier.weight(1f),
                                valueRange = 0.7f..2f,
                                steps = 12,
                            )
                            TextButton(onClick = { setFontScale(fontSize + 0.1) }) { Text("A", fontSize = 21.sp) }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Reading style", color = colors.muted, fontSize = 11.sp)
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 7.dp),
                                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                                ) {
                                    ReaderLayout.entries.forEach { layout ->
                                        ReaderLayoutOption(
                                            layout = layout,
                                            selected = readerLayout == layout,
                                            modifier = Modifier.weight(1f),
                                            colors = colors,
                                        )
                                    }
                                }
                            }
                            Column(Modifier.weight(1f)) {
                                Text("Theme", color = colors.muted, fontSize = 11.sp)
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 7.dp),
                                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                                ) {
                                    THEMES.forEachIndexed { index, theme ->
                                        ThemeTile(
                                            label = themeLabel(theme),
                                            theme = theme,
                                            selected = themeIndex == index,
                                            onClick = { selectTheme(index) },
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Long-press text to highlight · tap page center for controls",
                                modifier = Modifier.weight(1f),
                                color = colors.muted,
                                fontSize = 10.sp,
                            )
                            if (highlights.isNotEmpty()) {
                                TextButton(
                                    onClick = {
                                        highlights = emptyList()
                                        preferences.saveHighlights(highlights)
                                        lifecycleScope.launch { applyHighlights() }
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                ) {
                                    Text("Clear ${highlights.size}", fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ReaderLayoutOption(
        layout: ReaderLayout,
        selected: Boolean,
        modifier: Modifier,
        colors: ReaderChromeColors,
    ) {
        val label = when (layout) {
            ReaderLayout.Page -> "Page"
            ReaderLayout.Spread -> "Spread"
            ReaderLayout.Scroll -> "Scroll"
        }
        Surface(
            onClick = { selectLayout(layout) },
            modifier = modifier.height(46.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(13.dp),
            color = if (selected) Color(AMBER).copy(alpha = 0.18f) else colors.background,
            border = BorderStroke(1.dp, if (selected) Color(AMBER) else colors.muted.copy(alpha = 0.28f)),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (selected) {
                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(label, fontSize = 12.sp)
                }
            }
        }
    }

    @Composable
    private fun ThemeTile(
        label: String,
        theme: ReaderTheme,
        selected: Boolean,
        onClick: () -> Unit,
        modifier: Modifier,
    ) {
        val background = when (theme) {
            ReaderTheme.SEPIA -> Color(0xFFF5E7CF)
            ReaderTheme.LIGHT -> Color(0xFFFFFCF8)
            ReaderTheme.DARK -> Color(0xFF25211D)
        }
        val foreground = if (theme == ReaderTheme.DARK) Color(0xFFF5EEE4) else Color(ESPRESSO)
        Surface(
            onClick = onClick,
            modifier = modifier.height(64.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
            color = background,
            contentColor = foreground,
            border = BorderStroke(2.dp, if (selected) Color(AMBER) else Color.Transparent),
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Aa", fontFamily = FontFamily.Serif, fontSize = 17.sp)
                Text(label, fontSize = 10.sp)
            }
        }
    }

    @Composable
    private fun ReaderFollowAlong() {
        if (audiobookId == null) return
        val playback by PlaybackService.state.collectAsStateWithLifecycle()
        val context = LocalContext.current
        val active = playback.belongsToPairedAudiobook()
        val cover by produceState<ImageBitmap?>(initialValue = null, audiobookId) {
            value = CoverImageLoader.load(
                context,
                LibraryBook(
                    id = audiobookId.orEmpty(),
                    libraryId = "",
                    title = audiobookTitle ?: bookTitle,
                    creator = audiobookCreator.orEmpty(),
                    format = BookFormat.Audiobook,
                    coverEndpoint = "/api/items/${audiobookId.orEmpty()}/cover",
                ),
            )?.asImageBitmap()
        }

        Box(Modifier.fillMaxSize().background(FOLLOW_BACKGROUND)) {
            cover?.let { image ->
                Image(
                    bitmap = image,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                        .graphicsLayer {
                            scaleX = 1.14f
                            scaleY = 1.14f
                        }
                        .blur(54.dp)
                        .alpha(0.9f),
                )
            }
            Box(Modifier.fillMaxSize().background(FOLLOW_SCRIM))

            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 46.dp, vertical = 18.dp),
            ) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier.width(44.dp).height(5.dp)
                            .background(Color.White.copy(alpha = 0.35f), CircleShape),
                    )
                }
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { updateFollowAlong(false) },
                        modifier = Modifier.size(44.dp),
                    ) {
                        Icon(
                            Icons.Rounded.KeyboardArrowDown,
                            contentDescription = "Return to reading",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Surface(
                        modifier = Modifier.size(66.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.12f),
                        shadowElevation = 6.dp,
                    ) {
                        if (cover != null) {
                            Image(
                                bitmap = cover!!,
                                contentDescription = "Audiobook cover",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Headphones, contentDescription = null, tint = Color.White)
                            }
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            playback.title.takeIf { active }.orEmpty().ifBlank { audiobookTitle ?: bookTitle },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            playback.creator.takeIf { active }.orEmpty().ifBlank { audiobookCreator.orEmpty() },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                        )
                    }
                    IconButton(onClick = { PlaybackService.toggle(context) }, enabled = active) {
                        Icon(
                            Icons.Rounded.MoreHoriz,
                            contentDescription = "Pause or resume audiobook",
                            tint = Color.White.copy(alpha = if (active) 0.9f else 0.35f),
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f).widthIn(max = 980.dp).padding(start = 78.dp, top = 16.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    if (followPassages.previous.isNotBlank()) {
                        Text(
                            followPassages.previous,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = Color.White.copy(alpha = 0.24f),
                            fontSize = 25.sp,
                            lineHeight = 31.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.blur(1.2.dp),
                        )
                        Spacer(Modifier.height(18.dp))
                    }
                    Text(
                        followPassages.current,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White,
                        fontSize = 42.sp,
                        lineHeight = 49.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    if (followPassages.next.isNotBlank()) {
                        Spacer(Modifier.height(22.dp))
                        Text(
                            followPassages.next,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = Color.White.copy(alpha = 0.28f),
                            fontSize = 27.sp,
                            lineHeight = 34.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.blur(1.dp),
                        )
                    }
                }

                if (audioStarting) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(2.dp),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.2f),
                    )
                    Text(
                        "Starting audiobook…",
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 12.dp).align(Alignment.CenterHorizontally),
                    )
                } else if (active) {
                    Slider(
                        value = playback.progress,
                        onValueChange = { progress ->
                            PlaybackService.seekTo(context, (playback.totalDurationMs * progress).toLong())
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = playback.totalDurationMs > 0L,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.24f),
                        ),
                    )
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(playback.totalPositionMs.asClock(), color = Color.White.copy(alpha = 0.72f), fontSize = 11.sp)
                        Spacer(Modifier.weight(1f))
                        Text(
                            "-${(playback.totalDurationMs - playback.totalPositionMs).coerceAtLeast(0L).asClock()}",
                            color = Color.White.copy(alpha = 0.72f),
                            fontSize = 11.sp,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        IconButton(
                            onClick = { PlaybackService.seekBy(context, -10_000L) },
                            modifier = Modifier.size(58.dp),
                        ) {
                            Icon(Icons.Rounded.Replay10, contentDescription = "Back 10 seconds", tint = Color.White, modifier = Modifier.size(31.dp))
                        }
                        Spacer(Modifier.width(28.dp))
                        Surface(
                            onClick = { PlaybackService.toggle(context) },
                            modifier = Modifier.size(70.dp),
                            shape = CircleShape,
                            color = Color.White,
                            contentColor = Color(0xFF11131B),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    if (playback.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = if (playback.isPlaying) "Pause audiobook" else "Resume audiobook",
                                    modifier = Modifier.size(38.dp),
                                )
                            }
                        }
                        Spacer(Modifier.width(28.dp))
                        IconButton(
                            onClick = { PlaybackService.seekBy(context, 10_000L) },
                            modifier = Modifier.size(58.dp),
                        ) {
                            Icon(Icons.Rounded.Forward10, contentDescription = "Forward 10 seconds", tint = Color.White, modifier = Modifier.size(31.dp))
                        }
                    }
                } else {
                    Button(
                        onClick = { startPairedAudiobook(followText = true) },
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF11131B)),
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Start audiobook")
                    }
                }
                audioError?.let { message ->
                    Text(
                        message,
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp).align(Alignment.CenterHorizontally),
                    )
                }
            }
        }
    }

    @Composable
    private fun readerChromeColors(): ReaderChromeColors =
        if (THEMES[themeIndex] == ReaderTheme.DARK) {
            ReaderChromeColors(
                background = Color(DARK_CHROME),
                foreground = Color(DARK_TEXT),
                muted = Color(DARK_MUTED),
            )
        } else {
            ReaderChromeColors(
                background = Color(PARCHMENT),
                foreground = Color(ESPRESSO),
                muted = Color(MUTED),
            )
        }

    private fun showLoading() {
        setContent {
            OpenReaderTheme {
                Box(
                    Modifier.fillMaxSize().background(Color(PARCHMENT)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(AMBER))
                        Spacer(Modifier.height(20.dp))
                        Text(
                            "Opening $bookTitle",
                            color = Color(ESPRESSO),
                            fontFamily = FontFamily.Serif,
                            fontSize = 20.sp,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text("Preparing EPUB", color = Color(MUTED))
                    }
                }
            }
        }
    }

    private fun showFailure(message: String) {
        setContent {
            OpenReaderTheme {
                Box(
                    Modifier.fillMaxSize().background(Color(PARCHMENT)).padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Could not open book",
                            color = Color(ESPRESSO),
                            fontFamily = FontFamily.Serif,
                            fontSize = 24.sp,
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(message, color = Color(MUTED))
                        Spacer(Modifier.height(18.dp))
                        Surface(onClick = ::finish, color = Color(AMBER), contentColor = Color(ESPRESSO)) {
                            Text("Back to library", Modifier.padding(horizontal = 18.dp, vertical = 12.dp))
                        }
                    }
                }
            }
        }
    }

    override fun onExternalLinkActivated(url: AbsoluteUrl) {
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
    }

    override fun onPause() {
        navigator?.currentLocator?.value?.let(preferences::saveLocator)
        super.onPause()
    }

    override fun onDestroy() {
        val shouldClosePublication = isFinishing
        super.onDestroy()
        if (shouldClosePublication) {
            publication?.close()
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).roundToInt()

    companion object {
        private const val EXTRA_BOOK_ID = "book_id"
        private const val EXTRA_BOOK_TITLE = "book_title"
        private const val EXTRA_PUBLIC_DOMAIN_SOURCE_ID = "public_domain_source_id"
        private const val EXTRA_BUNDLED_EBOOK_ASSET = "bundled_ebook_asset"
        private const val EXTRA_AUDIOBOOK_ID = "audiobook_id"
        private const val EXTRA_AUDIOBOOK_TITLE = "audiobook_title"
        private const val EXTRA_AUDIOBOOK_CREATOR = "audiobook_creator"
        private const val LOG_TAG = "OpenReader"
        private const val NAVIGATOR_TAG = "epub_navigator"
        private const val TOP_CHROME_HEIGHT = 48
        private const val BOTTOM_CHROME_HEIGHT = 32
        private const val PARCHMENT = 0xFFFAF6F0.toInt()
        private const val AMBER = 0xFFC4802A.toInt()
        private const val ESPRESSO = 0xFF1C1409.toInt()
        private const val MUTED = 0xFF8A7560.toInt()
        private const val DARK_CHROME = 0xFF181512.toInt()
        private const val DARK_TEXT = 0xFFF4EEE7.toInt()
        private const val DARK_MUTED = 0xFFC9C0B7.toInt()
        private const val HIGHLIGHT_AMBER = 0xFFF2C96D.toInt()
        private const val HIGHLIGHT_BLUE = 0xFFA7D8ED.toInt()
        private const val HIGHLIGHT_ROSE = 0xFFF1B8AD.toInt()
        private const val MENU_HIGHLIGHT_AMBER = 1001
        private const val MENU_HIGHLIGHT_BLUE = 1002
        private const val MENU_HIGHLIGHT_ROSE = 1003
        private const val HIGHLIGHTS_GROUP = "reader-highlights"
        private const val FOLLOW_ALONG_GROUP = "audiobook-follow-along"
        private const val FOLLOW_ALONG_DECORATION = "audiobook-position"
        private val FOLLOW_BACKGROUND = Color(0xFF171A24)
        private val FOLLOW_SCRIM = Color(0x89101521)
        private val FOLLOW_ALONG_BLOCK_REGEX = Regex(
            "<(?:p|li|h1|h2|h3|blockquote)\\b[^>]*>(.*?)</(?:p|li|h1|h2|h3|blockquote)>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
        private val FOLLOW_ALONG_TAG_REGEX = Regex("<[^>]+>")
        private val FOLLOW_ALONG_ENTITY_REGEX = Regex("&(?:amp|quot|apos|lt|gt|nbsp|#39);")
        private val THEMES = listOf(ReaderTheme.SEPIA, ReaderTheme.LIGHT, ReaderTheme.DARK)

        private fun themeLabel(theme: ReaderTheme) = when (theme) {
            ReaderTheme.SEPIA -> "Calm"
            ReaderTheme.LIGHT -> "Paper"
            ReaderTheme.DARK -> "Night"
        }

        fun intent(
            context: android.content.Context,
            bookId: String,
            title: String,
            audiobookId: String? = null,
            audiobookTitle: String? = null,
            audiobookCreator: String? = null,
            bundledEbookAsset: String? = null,
        ) = Intent(context, ReaderActivity::class.java)
                .putExtra(EXTRA_BOOK_ID, bookId)
                .putExtra(EXTRA_BOOK_TITLE, title)
                .putExtra(EXTRA_AUDIOBOOK_ID, audiobookId)
                .putExtra(EXTRA_AUDIOBOOK_TITLE, audiobookTitle)
                .putExtra(EXTRA_AUDIOBOOK_CREATOR, audiobookCreator)
                .putExtra(EXTRA_BUNDLED_EBOOK_ASSET, bundledEbookAsset)

        fun publicDomainIntent(
            context: android.content.Context,
            bookId: String,
            sourceId: String,
            title: String,
        ) = Intent(context, ReaderActivity::class.java)
            .putExtra(EXTRA_BOOK_ID, bookId)
            .putExtra(EXTRA_BOOK_TITLE, title)
            .putExtra(EXTRA_PUBLIC_DOMAIN_SOURCE_ID, sourceId)
    }
}

private data class ReaderChromeColors(
    val background: Color,
    val foreground: Color,
    val muted: Color,
)

private fun Long.asClock(): String {
    if (this <= 0L) return "0:00"
    val totalSeconds = this / 1_000L
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%d:%02d".format(minutes, seconds)
}
