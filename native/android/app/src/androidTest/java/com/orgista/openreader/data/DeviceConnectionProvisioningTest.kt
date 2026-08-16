package com.orgista.openreader.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.orgista.openreader.domain.BookFormat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeviceConnectionProvisioningTest {
    @Test
    fun provisionsEncryptedDeviceConnectionsFromInstrumentationArguments() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val arguments = InstrumentationRegistry.getArguments()
        val context = instrumentation.targetContext

        SessionStore(context).save(
            ServerSession(
                serverUrl = arguments.required("abs_url"),
                username = arguments.required("abs_username"),
                accessToken = arguments.required("abs_token"),
                refreshToken = null,
            ),
        )
        ArrConnectionStore(context).save(
            ArrConnections(
                ebook = ArrCredential(
                    serverUrl = arguments.required("ebook_url"),
                    apiKey = arguments.required("ebook_key"),
                    format = BookFormat.Ebook,
                ),
                audiobook = ArrCredential(
                    serverUrl = arguments.required("audiobook_url"),
                    apiKey = arguments.required("audiobook_key"),
                    format = BookFormat.Audiobook,
                ),
            ),
        )
    }

    private fun android.os.Bundle.required(key: String): String =
        requireNotNull(getString(key)?.takeIf(String::isNotBlank)) { "Missing instrumentation argument: $key" }
}
