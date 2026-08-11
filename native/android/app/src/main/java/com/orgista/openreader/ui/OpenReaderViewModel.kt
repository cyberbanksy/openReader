package com.orgista.openreader.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.orgista.openreader.BuildConfig
import com.orgista.openreader.data.ApiException
import com.orgista.openreader.data.ApiKeyCredential
import com.orgista.openreader.data.AudiobookshelfApi
import com.orgista.openreader.data.ServerSession
import com.orgista.openreader.data.SessionStore
import com.orgista.openreader.domain.BookFormat
import com.orgista.openreader.domain.DemoCatalog
import com.orgista.openreader.domain.LibraryBook
import com.orgista.openreader.playback.PlaybackService
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
    val books: List<LibraryBook> = DemoCatalog.books,
    val connected: Boolean = false,
    val loading: Boolean = false,
    val showConnection: Boolean = false,
    val serverUrl: String = BuildConfig.DEFAULT_SERVER_URL,
    val username: String = "",
    val filter: LibraryFilter = LibraryFilter.All,
    val selectedBook: LibraryBook? = null,
    val nowPlaying: LibraryBook? = null,
    val error: String? = null,
) {
    val visibleBooks: List<LibraryBook>
        get() = when (filter) {
            LibraryFilter.All -> books
            LibraryFilter.Audiobooks -> books.filter { it.format == BookFormat.Audiobook }
            LibraryFilter.Ebooks -> books.filter { it.format == BookFormat.Ebook }
        }
}

class OpenReaderViewModel(application: Application) : AndroidViewModel(application) {
    private val api = AudiobookshelfApi()
    private val sessionStore = SessionStore(application)
    private val mutableState = MutableStateFlow(OpenReaderUiState())
    val state: StateFlow<OpenReaderUiState> = mutableState.asStateFlow()
    private var session: ServerSession? = null

    init {
        sessionStore.load()?.let { saved ->
            session = saved
            mutableState.update {
                it.copy(serverUrl = saved.serverUrl, username = saved.username, loading = true)
            }
            refreshCatalog(saved)
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
                loggedIn to api.catalog(loggedIn)
            }
                .onSuccess { (loggedIn, catalog) ->
                    session = loggedIn
                    sessionStore.save(loggedIn)
                    mutableState.update {
                        it.copy(
                            serverUrl = loggedIn.serverUrl,
                            username = loggedIn.username,
                            showConnection = false,
                        )
                    }
                    updateCatalog(loggedIn, catalog)
                }
                .onFailure(::showFailure)
        }
    }

    fun disconnect() {
        session = null
        sessionStore.clear()
        mutableState.value = OpenReaderUiState(showConnection = true)
    }

    fun refresh() {
        val current = session ?: return showConnection(true)
        mutableState.update { it.copy(loading = true, error = null) }
        refreshCatalog(current)
    }

    fun open(book: LibraryBook) {
        val current = session
        if (current == null || book.isDemo) {
            mutableState.update { it.copy(showConnection = true) }
            return
        }
        if (book.format == BookFormat.Ebook) {
            mutableState.update { it.copy(error = "The ebook reader is not available in this preview yet.") }
            return
        }
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

    private fun refreshCatalog(saved: ServerSession) {
        viewModelScope.launch {
            runCatching { api.catalog(saved) }
                .recoverCatching { failure ->
                    if (failure is ApiException && failure.statusCode == 401 && saved.refreshToken != null) {
                        val refreshed = api.refresh(saved)
                        session = refreshed
                        sessionStore.save(refreshed)
                        api.catalog(refreshed)
                    } else {
                        throw failure
                    }
                }
                .onSuccess { catalog -> updateCatalog(saved, catalog) }
                .onFailure(::showFailure)
        }
    }

    private fun updateCatalog(activeSession: ServerSession, catalog: List<LibraryBook>) {
        mutableState.update {
            it.copy(
                books = catalog,
                connected = true,
                loading = false,
                serverUrl = activeSession.serverUrl,
                username = activeSession.username,
                selectedBook = null,
                error = null,
            )
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
}
