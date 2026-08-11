package com.orgista.openreader.reader

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commitNow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.orgista.openreader.data.AudiobookshelfApi
import com.orgista.openreader.data.SessionStore
import com.orgista.openreader.ui.theme.OpenReaderTheme
import kotlinx.coroutines.launch
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.navigator.preferences.FontFamily as ReaderFontFamily
import org.readium.r2.navigator.preferences.Theme as ReaderTheme
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.toUri
import org.readium.r2.shared.util.toUrl
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
import java.io.File
import kotlin.math.roundToInt

@OptIn(ExperimentalReadiumApi::class)
class ReaderActivity : FragmentActivity(), EpubNavigatorFragment.Listener {
    private lateinit var bookId: String
    private lateinit var bookTitle: String
    private lateinit var preferences: ReaderPreferences
    private var publication: Publication? = null
    private var navigator: EpubNavigatorFragment? = null
    private var locationLabel by mutableStateOf("Opening book")
    private var chapterLabel by mutableStateOf("")
    private var fontSize by mutableDoubleStateOf(1.0)
    private var themeIndex by mutableIntStateOf(0)

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
        if (bookId.isBlank()) {
            showFailure("The selected book is missing an ID.")
            return
        }
        preferences = ReaderPreferences(this, bookId)
        fontSize = preferences.fontSize
        themeIndex = THEMES.indexOf(preferences.theme).coerceAtLeast(0)
        showLoading()
        lifecycleScope.launch {
            runCatching { openPublication() }
                .onSuccess(::showPublication)
                .onFailure { showFailure(it.message ?: "The EPUB could not be opened.") }
        }
    }

    private suspend fun openPublication(): Publication {
        val session = requireNotNull(SessionStore(this).load()) {
            "Connect to Audiobookshelf before opening this book."
        }
        val file = AudiobookshelfApi().downloadEbook(
            session = session,
            bookId = bookId,
            cacheDirectory = File(cacheDir, "epubs"),
        )
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
        root.addView(
            container,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ).apply {
                topMargin = dp(72)
                bottomMargin = dp(72)
            },
        )
        addChrome(root)
        setContentView(root)

        val factory = EpubNavigatorFactory(openedPublication)
        supportFragmentManager.fragmentFactory = factory.createFragmentFactory(
            initialLocator = preferences.loadLocator(),
            initialPreferences = readerPreferences(),
            listener = this,
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
                    else -> return false
                }
                return true
            }
        })
        observeLocator()
    }

    private fun addChrome(root: FrameLayout) {
        root.addView(
            ComposeView(this).apply {
                setContent { OpenReaderTheme { ReaderTopBar() } }
            },
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dp(72)).apply {
                gravity = Gravity.TOP
            },
        )
        root.addView(
            ComposeView(this).apply {
                setContent { OpenReaderTheme { ReaderBottomBar() } }
            },
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dp(72)).apply {
                gravity = Gravity.BOTTOM
            },
        )
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
        val percent = ((locator.locations.totalProgression ?: 0.0) * 100).roundToInt()
        locationLabel = if (page != null && totalPages != null && totalPages > 0) {
            "Page $page of $totalPages  ·  $percent%"
        } else {
            "$percent% read"
        }
    }

    private fun readerPreferences() = EpubPreferences(
        fontFamily = ReaderFontFamily.SERIF,
        fontSize = fontSize,
        pageMargins = 1.15,
        publisherStyles = false,
        scroll = false,
        theme = THEMES[themeIndex],
    )

    private fun changeFont(delta: Double) {
        fontSize = (fontSize + delta).coerceIn(0.7, 2.0)
        preferences.fontSize = fontSize
        navigator?.submitPreferences(readerPreferences())
    }

    private fun cycleTheme() {
        themeIndex = (themeIndex + 1) % THEMES.size
        preferences.theme = THEMES[themeIndex]
        navigator?.submitPreferences(readerPreferences())
    }

    @Composable
    private fun ReaderTopBar() {
        val colors = readerChromeColors()
        Surface(color = colors.background, contentColor = colors.foreground, shadowElevation = 2.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = ::finish) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = bookTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontFamily = FontFamily.Serif,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (chapterLabel.isNotBlank()) {
                        Text(
                            text = chapterLabel,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = colors.muted,
                            fontSize = 12.sp,
                        )
                    }
                }
                IconButton(onClick = ::cycleTheme) {
                    Icon(Icons.Rounded.Brightness6, contentDescription = "Change reader theme")
                }
            }
        }
    }

    @Composable
    private fun ReaderBottomBar() {
        val colors = readerChromeColors()
        Surface(color = colors.background, contentColor = colors.foreground, shadowElevation = 4.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = { navigator?.goBackward(animated = true) }) {
                    Icon(Icons.Rounded.ChevronLeft, contentDescription = "Previous page")
                }
                IconButton(onClick = { changeFont(-0.1) }) {
                    Icon(Icons.Rounded.Remove, contentDescription = "Decrease text size")
                }
                Text(locationLabel, fontSize = 12.sp, color = colors.muted)
                IconButton(onClick = { changeFont(0.1) }) {
                    Icon(Icons.Rounded.Add, contentDescription = "Increase text size")
                }
                IconButton(onClick = { navigator?.goForward(animated = true) }) {
                    Icon(Icons.Rounded.ChevronRight, contentDescription = "Next page")
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
        private const val NAVIGATOR_TAG = "epub_navigator"
        private const val PARCHMENT = 0xFFFAF6F0.toInt()
        private const val AMBER = 0xFFC4802A.toInt()
        private const val ESPRESSO = 0xFF2B2118.toInt()
        private const val MUTED = 0xFF6F665E.toInt()
        private const val DARK_CHROME = 0xFF181512.toInt()
        private const val DARK_TEXT = 0xFFF4EEE7.toInt()
        private const val DARK_MUTED = 0xFFC9C0B7.toInt()
        private val THEMES = listOf(ReaderTheme.SEPIA, ReaderTheme.LIGHT, ReaderTheme.DARK)

        fun intent(context: android.content.Context, bookId: String, title: String) =
            Intent(context, ReaderActivity::class.java)
                .putExtra(EXTRA_BOOK_ID, bookId)
                .putExtra(EXTRA_BOOK_TITLE, title)
    }
}

private data class ReaderChromeColors(
    val background: Color,
    val foreground: Color,
    val muted: Color,
)
