package com.orgista.openreader

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.orgista.openreader.domain.BookFormat
import com.orgista.openreader.domain.DeviceClassifier
import com.orgista.openreader.domain.DeviceProfile
import com.orgista.openreader.domain.PlatformMode
import com.orgista.openreader.reader.ReaderActivity
import com.orgista.openreader.ui.OpenReaderApp
import com.orgista.openreader.ui.OpenReaderViewModel
import com.orgista.openreader.ui.theme.OpenReaderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: OpenReaderViewModel = viewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            OpenReaderTheme {
                OpenReaderApp(
                    state = state,
                    deviceProfile = currentDeviceProfile(),
                    onFilter = viewModel::setFilter,
                    onSelectBook = viewModel::selectBook,
                    onOpenBook = { book ->
                        if (book.format == BookFormat.Ebook && !book.isDemo && state.connected) {
                            startActivity(ReaderActivity.intent(this, book.id, book.title))
                        } else {
                            viewModel.open(book)
                        }
                    },
                    onShowConnection = viewModel::showConnection,
                    onConnect = viewModel::connect,
                    onDisconnect = viewModel::disconnect,
                    onRefresh = viewModel::refresh,
                    onDismissError = viewModel::dismissError,
                )
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
