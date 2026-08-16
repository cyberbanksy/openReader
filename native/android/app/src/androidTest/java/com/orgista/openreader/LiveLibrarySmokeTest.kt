package com.orgista.openreader

import android.content.ComponentName
import android.content.Context
import android.os.SystemClock
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.orgista.openreader.data.AudiobookshelfApi
import com.orgista.openreader.data.CoverImageLoader
import com.orgista.openreader.data.EbookFile
import com.orgista.openreader.data.SessionStore
import com.orgista.openreader.domain.BookFormat
import com.orgista.openreader.playback.PlaybackService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.toUrl
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
import java.io.File
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalReadiumApi::class)
class LiveLibrarySmokeTest {
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun everyReadyEbookAndCoverLoadsOnTheDevice() = runBlocking {
        val session = SessionStore(context).load()
        assumeNotNull(session)
        val activeSession = requireNotNull(session)
        val books = AudiobookshelfApi().catalog(activeSession)
        assertTrue("Audiobookshelf returned no ready books.", books.isNotEmpty())

        books.forEach { book ->
            assertNotNull("Missing cover for ${book.title}", CoverImageLoader.load(context, book))
        }

        val ebooks = books.filter { it.format == BookFormat.Ebook }
        assertTrue("Audiobookshelf returned no EPUBs.", ebooks.isNotEmpty())
        val httpClient = DefaultHttpClient()
        val retriever = AssetRetriever(context.contentResolver, httpClient)
        val opener = PublicationOpener(
            DefaultPublicationParser(context, httpClient, retriever, null),
        )
        ebooks.forEach { book ->
            val file = AudiobookshelfApi().downloadEbook(
                session = activeSession,
                bookId = book.id,
                cacheDirectory = File(context.cacheDir, "epubs"),
            )
            EbookFile.requireValid(file)
            val asset = retriever.retrieve(file.toUrl(isDirectory = false))
                .getOrElse { throw AssertionError("Readium could not retrieve ${book.title}: ${it.message}") }
            val publication = opener.open(asset, allowUserInteraction = false)
                .getOrElse {
                    asset.close()
                    throw AssertionError("Readium could not open ${book.title}: ${it.message}")
                }
            try {
                assertTrue("${book.title} has no readable content.", publication.readingOrder.isNotEmpty())
            } finally {
                publication.close()
            }
        }
    }

    @Test
    fun everyReadyAudiobookStartsMedia3PlaybackOnTheDevice() = runBlocking {
        val session = SessionStore(context).load()
        assumeNotNull(session)
        val activeSession = requireNotNull(session)
        val api = AudiobookshelfApi()
        val audiobooks = api.catalog(activeSession).filter { it.format == BookFormat.Audiobook }
        assertTrue("Audiobookshelf returned no audiobooks.", audiobooks.isNotEmpty())

        audiobooks.forEach { book ->
            val descriptor = api.startPlayback(activeSession, book)
            assertTrue("${book.title} has no playable tracks.", descriptor.tracks.isNotEmpty())
            PlaybackService.play(context, descriptor)
            SystemClock.sleep(4_000)

            val controllerFuture = MediaController.Builder(
                context,
                SessionToken(context, ComponentName(context, PlaybackService::class.java)),
            ).buildAsync()
            try {
                val controller = controllerFuture.get(10, TimeUnit.SECONDS)
                var playable = false
                var advanced = false
                InstrumentationRegistry.getInstrumentation().runOnMainSync {
                    playable = controller.isPlaying || controller.playbackState == Player.STATE_READY
                    advanced = controller.currentPosition > 0L
                }
                assertTrue(
                    "${book.title} did not reach a playable Media3 state.",
                    playable,
                )
                assertTrue("${book.title} did not begin playback.", advanced)
            } finally {
                InstrumentationRegistry.getInstrumentation().runOnMainSync {
                    MediaController.releaseFuture(controllerFuture)
                }
            }
        }
    }
}
