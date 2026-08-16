package com.orgista.openreader

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.orgista.openreader.domain.BookFormat
import com.orgista.openreader.domain.CatalogAvailability
import com.orgista.openreader.domain.CatalogSourceKind
import com.orgista.openreader.domain.DeviceClassifier
import com.orgista.openreader.domain.DeviceProfile
import com.orgista.openreader.domain.PlatformMode
import com.orgista.openreader.reader.ReaderActivity
import com.orgista.openreader.ui.OpenReaderApp
import com.orgista.openreader.ui.OpenReaderViewModel
import com.orgista.openreader.ui.boot.OpenReaderBootScreen
import com.orgista.openreader.ui.matchingFormats
import com.orgista.openreader.ui.theme.OpenReaderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: OpenReaderViewModel = viewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            var showBoot by rememberSaveable { mutableStateOf(true) }
            OpenReaderTheme {
                Box(Modifier.fillMaxSize()) {
                    OpenReaderApp(
                        state = state,
                        deviceProfile = currentDeviceProfile(),
                        onFilter = viewModel::setFilter,
                        onSelectBook = viewModel::selectBook,
                        onOpenBook = { book ->
                            val readyInAudiobookshelf = book.sources.any {
                                it.kind == CatalogSourceKind.Audiobookshelf &&
                                    it.availability == CatalogAvailability.Ready
                            }
                            val publicDomainSource = book.sources.firstOrNull {
                                it.kind == CatalogSourceKind.PublicDomain &&
                                    it.availability == CatalogAvailability.Available
                            }
                            if (book.format == BookFormat.Ebook && !book.isDemo && readyInAudiobookshelf) {
                                val pairedAudiobook = matchingFormats(state.books, book)
                                    .firstOrNull { it.format == BookFormat.Audiobook }
                                startActivity(
                                    ReaderActivity.intent(
                                        context = this@MainActivity,
                                        bookId = book.id,
                                        title = book.title,
                                        audiobookId = pairedAudiobook?.id,
                                        audiobookTitle = pairedAudiobook?.title,
                                        audiobookCreator = pairedAudiobook?.creator,
                                    ),
                                )
                            } else if (book.format == BookFormat.Ebook && !book.isDemo && publicDomainSource != null) {
                                startActivity(
                                    ReaderActivity.publicDomainIntent(
                                        context = this@MainActivity,
                                        bookId = book.id,
                                        sourceId = publicDomainSource.id,
                                        title = book.title,
                                    ),
                                )
                            } else {
                                viewModel.open(book)
                            }
                        },
                        onShowConnection = viewModel::showConnection,
                        onConnect = viewModel::connect,
                        onDisconnect = viewModel::disconnect,
                        onShowArrConnection = viewModel::showArrConnection,
                        onConnectArr = viewModel::connectArr,
                        onDisconnectArr = viewModel::disconnectArr,
                        onShowStandardEbooksConnection = viewModel::showStandardEbooksConnection,
                        onConnectStandardEbooks = viewModel::connectStandardEbooks,
                        onDisconnectStandardEbooks = viewModel::disconnectStandardEbooks,
                        onSearchPublicDomain = viewModel::searchPublicDomain,
                        onClearPublicDomainSearch = viewModel::clearPublicDomainSearch,
                        onRefresh = viewModel::refresh,
                        onDismissError = viewModel::dismissError,
                    )
                    if (showBoot) {
                        OpenReaderBootScreen(
                            isLoading = state.loading,
                            onFinished = { showBoot = false },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun currentDeviceProfile(): DeviceProfile {
    val configuration = LocalConfiguration.current
    val adaptiveInfo = currentWindowAdaptiveInfo(supportLargeAndXLargeWidth = true)
    val platformMode = when (configuration.uiMode and Configuration.UI_MODE_TYPE_MASK) {
        Configuration.UI_MODE_TYPE_TELEVISION -> PlatformMode.Television
        Configuration.UI_MODE_TYPE_CAR -> PlatformMode.Car
        Configuration.UI_MODE_TYPE_DESK -> PlatformMode.Desktop
        else -> PlatformMode.Normal
    }
    return DeviceClassifier.classify(
        widthClassDp = adaptiveInfo.windowSizeClass.minWidthDp,
        smallestWidthDp = configuration.smallestScreenWidthDp,
        hasHinge = adaptiveInfo.windowPosture.hingeList.isNotEmpty(),
        platformMode = platformMode,
    )
}
