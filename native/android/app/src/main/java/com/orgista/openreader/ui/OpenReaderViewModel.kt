package com.orgista.openreader.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.orgista.openreader.BuildConfig
import com.orgista.openreader.data.ApiException
import com.orgista.openreader.data.ApiKeyCredential
import com.orgista.openreader.data.ArrConnectionStore
import com.orgista.openreader.data.ArrConnections
import com.orgista.openreader.data.ArrCredential
import com.orgista.openreader.data.AudiobookshelfApi
import com.orgista.openreader.data.BookshelfApi
import com.orgista.openreader.data.PlaybackDescriptor
import com.orgista.openreader.data.PlaybackTrack
import com.orgista.openreader.data.PublicDomainCatalogApi
import com.orgista.openreader.data.PublicDomainSettings
import com.orgista.openreader.data.ServerSession
import com.orgista.openreader.data.SessionStore
import com.orgista.openreader.domain.BookFormat
import com.orgista.openreader.domain.BundledContent
import com.orgista.openreader.domain.CatalogAvailability
import com.orgista.openreader.domain.CatalogMerger
import com.orgista.openreader.domain.CatalogSourceKind
import com.orgista.openreader.domain.DemoCatalog
import com.orgista.openreader.domain.LibraryBook
import com.orgista.openreader.playback.PlaybackService
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LibraryFilter {
    All,
    Audiobooks,
    Ebooks,
}

data class OpenReaderUiState(
    val books: List<LibraryBook> = DemoCatalog.books + BundledContent.books,
    val connected: Boolean = false,
    val arrConnected: Boolean = false,
    val loading: Boolean = false,
    val showConnection: Boolean = false,
    val showArrConnection: Boolean = false,
    val showStandardEbooksConnection: Boolean = false,
    val serverUrl: String = BuildConfig.DEFAULT_SERVER_URL,
    val username: String = "",
    val ebookArrUrl: String = BuildConfig.DEFAULT_EBOOK_ARR_URL,
    val audiobookArrUrl: String = BuildConfig.DEFAULT_AUDIOBOOK_ARR_URL,
    val standardEbooksEmail: String = "",
    val standardEbooksConnected: Boolean = false,
    val publicDomainResults: List<LibraryBook> = emptyList(),
    val publicDomainSearching: Boolean = false,
    val filter: LibraryFilter = LibraryFilter.All,
    val selectedBook: LibraryBook? = null,
    val nowPlaying: LibraryBook? = null,
    val error: String? = null,
) {
    val anyConnected: Boolean
        get() = connected || arrConnected

    val visibleBooks: List<LibraryBook>
        get() = when (filter) {
            LibraryFilter.All -> books
            LibraryFilter.Audiobooks -> books.filter { it.format == BookFormat.Audiobook }
            LibraryFilter.Ebooks -> books.filter { it.format == BookFormat.Ebook }
        }
}

class OpenReaderViewModel(application: Application) : AndroidViewModel(application) {
    private val api = AudiobookshelfApi()
    private val bookshelfApi = BookshelfApi()
    private val publicDomainApi = PublicDomainCatalogApi()
    private val sessionStore = SessionStore(application)
    private val arrConnectionStore = ArrConnectionStore(application)
    private val publicDomainSettings = PublicDomainSettings(application)
    private val mutableState = MutableStateFlow(OpenReaderUiState())
    val state: StateFlow<OpenReaderUiState> = mutableState.asStateFlow()
    private var session: ServerSession? = sessionStore.load()
    private var arrConnections: ArrConnections? = arrConnectionStore.load()
    private var acquisitionRefreshJob: Job? = null
    private var publicDomainSearchJob: Job? = null

    init {
        val savedSession = session
        val savedArr = arrConnections
        val standardEbooksEmail = publicDomainSettings.standardEbooksEmail()
        mutableState.update {
            it.copy(
                standardEbooksEmail = standardEbooksEmail.orEmpty(),
                standardEbooksConnected = standardEbooksEmail != null,
            )
        }
        if (savedSession != null || savedArr != null) {
            mutableState.update {
                it.copy(
                    connected = savedSession != null,
                    arrConnected = savedArr != null,
                    serverUrl = savedSession?.serverUrl ?: it.serverUrl,
                    username = savedSession?.username ?: it.username,
                    ebookArrUrl = savedArr?.ebook?.serverUrl ?: it.ebookArrUrl,
                    audiobookArrUrl = savedArr?.audiobook?.serverUrl ?: it.audiobookArrUrl,
                    loading = true,
                )
            }
            refreshCatalog(showLoading = false)
        }
    }

    fun setFilter(filter: LibraryFilter) {
        mutableState.update { it.copy(filter = filter) }
    }

    fun selectBook(book: LibraryBook?) {
        mutableState.update { it.copy(selectedBook = book) }
    }

    fun showConnection(show: Boolean) {
        mutableState.update { it.copy(showConnection = show, error = null) }
    }

    fun showArrConnection(show: Boolean) {
        mutableState.update { it.copy(showArrConnection = show, error = null) }
    }

    fun showStandardEbooksConnection(show: Boolean) {
        mutableState.update { it.copy(showStandardEbooksConnection = show, error = null) }
    }

    fun dismissError() {
        mutableState.update { it.copy(error = null) }
    }

    fun connect(serverUrl: String, username: String, credential: String, useApiKey: Boolean) {
        if (mutableState.value.loading) return
        mutableState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching {
                val loggedIn = if (useApiKey) {
                    ApiKeyCredential.create(serverUrl, credential)
                } else {
                    api.login(serverUrl, username, credential)
                }
                loggedIn to loadCatalog(loggedIn, arrConnections)
            }
                .onSuccess { (loggedIn, loaded) ->
                    session = loaded.session ?: loggedIn
                    sessionStore.save(requireNotNull(session))
                    mutableState.update { it.copy(showConnection = false) }
                    updateCatalog(loaded)
                }
                .onFailure(::showFailure)
        }
    }

    fun connectArr(
        ebookServerUrl: String,
        ebookApiKey: String,
        audiobookServerUrl: String,
        audiobookApiKey: String,
    ) {
        if (mutableState.value.loading) return
        mutableState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching {
                val connections = ArrConnections(
                    ebook = ArrCredential.create(ebookServerUrl, ebookApiKey, BookFormat.Ebook),
                    audiobook = ArrCredential.create(audiobookServerUrl, audiobookApiKey, BookFormat.Audiobook),
                )
                bookshelfApi.test(connections)
                connections to loadCatalog(session, connections)
                }
                .onSuccess { (connections, loaded) ->
                    arrConnections = connections
                    arrConnectionStore.save(connections)
                    session = loaded.session
                    loaded.session?.let(sessionStore::save)
                    mutableState.update { it.copy(showArrConnection = false) }
                    updateCatalog(loaded)
                }
                .onFailure(::showFailure)
        }
    }

    fun disconnect() {
        session = null
        sessionStore.clear()
        mutableState.update { it.copy(connected = false, showConnection = false, error = null) }
        if (arrConnections == null) {
            acquisitionRefreshJob?.cancel()
            mutableState.value = disconnectedState(showConnection = true)
        } else {
            refreshCatalog()
        }
    }

    fun disconnectArr() {
        arrConnections = null
        arrConnectionStore.clear()
        mutableState.update { it.copy(arrConnected = false, showArrConnection = false, error = null) }
        if (session == null) {
            acquisitionRefreshJob?.cancel()
            mutableState.value = disconnectedState(showConnection = true)
        } else {
            refreshCatalog()
        }
    }

    fun connectStandardEbooks(email: String) {
        if (mutableState.value.loading) return
        mutableState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching { publicDomainApi.connectStandardEbooks(email) }
                .onSuccess {
                    val normalized = email.trim()
                    publicDomainSettings.saveStandardEbooksEmail(normalized)
                    mutableState.update {
                        it.copy(
                            loading = false,
                            showStandardEbooksConnection = false,
                            standardEbooksEmail = normalized,
                            standardEbooksConnected = true,
                            error = null,
                        )
                    }
                }
                .onFailure(::showFailure)
        }
    }

    fun disconnectStandardEbooks() {
        publicDomainSettings.clearStandardEbooksEmail()
        publicDomainApi.clearStandardEbooks()
        mutableState.update {
            it.copy(
                showStandardEbooksConnection = false,
                standardEbooksEmail = "",
                standardEbooksConnected = false,
                publicDomainResults = it.publicDomainResults.filterNot { book ->
                    book.sources.any { source -> source.name == "Standard Ebooks" }
                },
                error = null,
            )
        }
    }

    fun searchPublicDomain(query: String) {
        publicDomainSearchJob?.cancel()
        if (query.trim().length < 2) {
            mutableState.update { it.copy(publicDomainResults = emptyList(), publicDomainSearching = false) }
            return
        }
        publicDomainSearchJob = viewModelScope.launch {
            mutableState.update { it.copy(publicDomainResults = emptyList(), publicDomainSearching = true) }
            runCatching {
                publicDomainApi.search(query, publicDomainSettings.standardEbooksEmail())
            }
                .onSuccess { books ->
                    mutableState.update { it.copy(publicDomainResults = books, publicDomainSearching = false) }
                }
                .onFailure { failure ->
                    mutableState.update {
                        it.copy(
                            publicDomainResults = emptyList(),
                            publicDomainSearching = false,
                            error = failure.message ?: "The public-domain search failed.",
                        )
                    }
                }
        }
    }

    fun clearPublicDomainSearch() {
        publicDomainSearchJob?.cancel()
        mutableState.update { it.copy(publicDomainResults = emptyList(), publicDomainSearching = false) }
    }

    fun refresh() {
        if (session == null && arrConnections == null) {
            showConnection(true)
            return
        }
        refreshCatalog()
    }

    fun open(book: LibraryBook) {
        if (book.isDemo) {
            if (arrConnections == null) showArrConnection(true) else showConnection(true)
            return
        }
        val bundledSource = book.sources.firstOrNull {
            it.kind == CatalogSourceKind.Bundled && it.availability == CatalogAvailability.Ready
        }
        if (bundledSource != null) {
            if (book.format == BookFormat.Ebook) return
            val asset = BundledContent.audiobookAsset(book.id) ?: return
            PlaybackService.play(
                getApplication(),
                PlaybackDescriptor(
                    sessionId = "bundled-${book.id}",
                    title = book.title,
                    creator = book.creator,
                    coverUrl = "",
                    currentTimeSeconds = 0.0,
                    tracks = listOf(
                        PlaybackTrack(
                            index = 1,
                            title = book.title,
                            url = "asset:///${asset.assetPath}",
                            durationSeconds = asset.durationSeconds,
                        ),
                    ),
                ),
            )
            mutableState.update { it.copy(nowPlaying = book) }
            return
        }
        val readyInAudiobookshelf = book.sources.any {
            it.kind == CatalogSourceKind.Audiobookshelf && it.availability == CatalogAvailability.Ready
        }
        if (!readyInAudiobookshelf) {
            val arrSource = primaryBookSource(book)?.takeIf {
                it.kind == CatalogSourceKind.Arr && it.availability == CatalogAvailability.Requestable
            }
            if (arrSource != null) {
                request(arrSource.id)
            } else {
                refreshCatalog()
            }
            return
        }
        val current = session ?: return showConnection(true)
        if (book.format == BookFormat.Ebook) return
        if (mutableState.value.loading) return
        mutableState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching { api.startPlayback(current, book) }
                .onSuccess { descriptor ->
                    PlaybackService.play(getApplication(), descriptor)
                    mutableState.update { it.copy(loading = false, nowPlaying = book) }
                }
                .onFailure(::showFailure)
        }
    }

    private fun request(sourceId: String) {
        val connections = arrConnections ?: return showArrConnection(true)
        if (mutableState.value.loading) return
        mutableState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching { bookshelfApi.request(connections, sourceId) }
                .onSuccess { refreshCatalog(showLoading = false) }
                .onFailure(::showFailure)
        }
    }

    private fun refreshCatalog(showLoading: Boolean = true) {
        if (showLoading) mutableState.update { it.copy(loading = true, error = null) }
        val savedSession = session
        val savedArr = arrConnections
        viewModelScope.launch {
            runCatching { loadCatalog(savedSession, savedArr) }
                .onSuccess { loaded ->
                    session = loaded.session
                    loaded.session?.let(sessionStore::save)
                    updateCatalog(loaded)
                }
                .onFailure(::showFailure)
        }
    }

    private suspend fun loadCatalog(
        savedSession: ServerSession?,
        savedArr: ArrConnections?,
    ): LoadedCatalog = coroutineScope {
        val ownedDeferred = savedSession?.let { current -> async { loadAudiobookshelf(current) } }
        val managedDeferred = savedArr?.let { connections -> async { bookshelfApi.catalog(connections) } }
        val ownedResult = ownedDeferred?.await()
        var owned = ownedResult?.second.orEmpty()
        val activeSession = ownedResult?.first
        val managed = managedDeferred?.await().orEmpty()

        if (activeSession != null && managed.any { book ->
                book.sources.any { it.availability == CatalogAvailability.Importing }
            }
        ) {
            api.scanLibraries(activeSession)
            delay(1_500)
            owned = api.catalog(activeSession)
        }

        LoadedCatalog(
            session = activeSession,
            books = CatalogMerger.merge(owned, managed),
        )
    }

    private suspend fun loadAudiobookshelf(saved: ServerSession): Pair<ServerSession, List<LibraryBook>> =
        runCatching { saved to api.catalog(saved) }
            .recoverCatching { failure ->
                if (failure is ApiException && failure.statusCode == 401 && saved.refreshToken != null) {
                    val refreshed = api.refresh(saved)
                    refreshed to api.catalog(refreshed)
                } else {
                    throw failure
                }
            }.getOrThrow()

    private fun updateCatalog(loaded: LoadedCatalog) {
        val currentArr = arrConnections
        val hasConnections = loaded.session != null || currentArr != null
        mutableState.update {
            it.copy(
                books = (if (hasConnections) loaded.books else DemoCatalog.books) + BundledContent.books,
                connected = loaded.session != null,
                arrConnected = currentArr != null,
                loading = false,
                serverUrl = loaded.session?.serverUrl ?: it.serverUrl,
                username = loaded.session?.username ?: it.username,
                ebookArrUrl = currentArr?.ebook?.serverUrl ?: it.ebookArrUrl,
                audiobookArrUrl = currentArr?.audiobook?.serverUrl ?: it.audiobookArrUrl,
                selectedBook = null,
                error = null,
            )
        }
        scheduleAcquisitionRefresh(loaded.books)
    }

    private fun scheduleAcquisitionRefresh(books: List<LibraryBook>) {
        acquisitionRefreshJob?.cancel()
        val pending = books.any { book ->
            book.sources.any { source ->
                source.kind == CatalogSourceKind.Arr && source.availability in setOf(
                    CatalogAvailability.Requested,
                    CatalogAvailability.Downloading,
                    CatalogAvailability.Importing,
                )
            }
        }
        if (pending) {
            acquisitionRefreshJob = viewModelScope.launch {
                delay(20_000)
                refreshCatalog(showLoading = false)
            }
        }
    }

    private fun showFailure(failure: Throwable) {
        mutableState.update {
            it.copy(
                loading = false,
                error = failure.message ?: "The request failed.",
            )
        }
    }

    private fun disconnectedState(showConnection: Boolean): OpenReaderUiState {
        val standardEmail = publicDomainSettings.standardEbooksEmail()
        return OpenReaderUiState(
            showConnection = showConnection,
            standardEbooksEmail = standardEmail.orEmpty(),
            standardEbooksConnected = standardEmail != null,
        )
    }
}

private data class LoadedCatalog(
    val session: ServerSession?,
    val books: List<LibraryBook>,
)
