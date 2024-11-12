package sp.kx.bytes.loader

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.URI
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
                launch(Dispatchers.Default) {
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
                launch(Dispatchers.Default) {
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

    @Test
    fun errorTest() {
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
                val expected = IllegalStateException("$uri:${bytes.size}:${hash.size}")
                val requester: BytesRequester = ErrorBytesRequester(expected = expected)
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
                launch(Dispatchers.Default) {
                    loader.add(
                        uri = uri,
                        size = bytes.size.toLong(),
                        hash = hash,
                    )
                }
                when (val event = events.await()) {
                    is BytesLoader.Event.OnError -> {
                        assertEquals(expected, event.error)
                        assertTrue(dsts.isEmpty())
                    }
                    else -> error("Event $event is not expected!")
                }
            }
        }
    }
}
