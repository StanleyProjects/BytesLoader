package sp.kx.bytes.loader

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.net.URI
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal class BytesLoaderTest {
    @Test
    fun addTest() {
        runTest(timeout = 8.seconds) {
            withContext(Dispatchers.Default) {
                val uri = mockURI()
                val bytes = mockBytes(size = 16)
                val hash = mockBytes(size = 8)
                val dsts = mutableMapOf<URI, ByteArray>()
                val factory: BytesWrapper.Factory = MockBytesWrapperFactory(
                    dsts = dsts,
                    hashes = listOf(bytes to hash),
                )
                val requester: BytesRequester = MockBytesRequester(
                    map = mapOf(uri to bytes),
                    delay = 1.seconds,
                )
                val loader = BytesLoader(
                    factory = factory,
                    requester = requester,
                    count = 6,
                )
                val events = async {
                    withTimeout(6.seconds) {
                        loader.events.first()
                    }
                }
                launch {
                    loader.add(
                        uri = uri,
                        size = bytes.size.toLong(),
                        hash = hash,
                    )
                }
                when (val event = events.await()) {
                    is BytesLoader.Event.OnLoad -> {
                        assertEquals(uri, event.uri)
                        val it = dsts[uri] ?: error("No bytes by $uri!")
                        assertTrue(it.contentEquals(bytes))
                    }
                    else -> error("Event $event is not expected!")
                }
            }
        }
    }

    @Test
    fun addTestOld() {
        runTest(UnconfinedTestDispatcher(), timeout = 5.seconds) {
            val uri = mockURI()
            val bytes = mockBytes(size = 32)
            val hash = mockBytes(size = 16)
            val dsts = mutableMapOf<URI, ByteArray>()
            val factory: BytesWrapper.Factory = MockBytesWrapperFactory(
                dsts = dsts,
                hashes = listOf(bytes to hash),
            )
            val requester: BytesRequester = MockBytesRequester(
                map = mapOf(uri to bytes),
                delay = 1.seconds,
            )
            val count = 4
            val loader = BytesLoader(
                factory = factory,
                requester = requester,
                count = count,
            )
            val events = backgroundScope.async {
                withTimeout(timeout = 5.seconds) {
                    loader.events.first()
                }
            }
            backgroundScope.launch {
                loader.add(
                    uri = uri,
                    size = bytes.size.toLong(),
                    hash = hash,
                )
            }
            when (val event = events.await()) {
                is BytesLoader.Event.OnLoad -> {
                    assertEquals(uri, event.uri)
                    val it = dsts[uri] ?: error("No bytes by $uri!")
                    assertTrue(it.contentEquals(bytes))
                }
                else -> error("Event $event is not expected!")
            }
        }
    }

    @Test
    fun stopTest() {
        runTest(timeout = 8.seconds) {
            withContext(Dispatchers.Default) {
                val uri = mockURI()
                val bytes = mockBytes(size = 16)
                val hash = mockBytes(size = 8)
                val dsts = mutableMapOf<URI, ByteArray>()
                val factory: BytesWrapper.Factory = MockBytesWrapperFactory(
                    dsts = dsts,
                    hashes = listOf(bytes to hash),
                )
                val requester: BytesRequester = MockBytesRequester(
                    map = mapOf(uri to bytes),
                    delay = 1.seconds,
                )
                val loader = BytesLoader(
                    factory = factory,
                    requester = requester,
                    count = 6,
                )
                val job = launch {
                    runCatching {
                        withTimeout(6.seconds) {
                            loader.events.firstOrNull()
                        }
                    }.fold(
                        onSuccess = {
                            error("Flow completed!")
                        },
                        onFailure = { error ->
                            check(error is TimeoutCancellationException) { "Error: $error" }
                        },
                    )
                }
                launch {
                    loader.add(
                        uri = uri,
                        size = bytes.size.toLong(),
                        hash = hash,
                    )
                }
                delay(1.seconds)
                loader.stop()
                job.join()
                assertTrue(dsts.isEmpty())
            }
        }
    }
}
