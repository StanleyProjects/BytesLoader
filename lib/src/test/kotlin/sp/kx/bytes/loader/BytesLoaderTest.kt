package sp.kx.bytes.loader

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
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
                    withTimeout(4.seconds) {
                        loader.events.first()
                    }
                }
                val states: Deferred<Unit> = async(start = CoroutineStart.UNDISPATCHED) {
                    withTimeout(4.seconds) {
                        loader.states.take(5).collectIndexed { index, state ->
                            when (index) {
                                0 -> {
                                    assertNull(state)
                                }
                                1 -> {
                                    checkNotNull(state)
                                    val expected = mockState(
                                        uri = uri,
                                        bl = BytesLoaded(size = 16, loaded = 0),
                                    )
                                    state.assert(expected)
                                }
                                2 -> {
                                    checkNotNull(state)
                                    val expected = mockState(
                                        uri = uri,
                                        bl = BytesLoaded(size = 16, loaded = 6),
                                    )
                                    state.assert(expected)
                                }
                                3 -> {
                                    checkNotNull(state)
                                    val expected = mockState(
                                        uri = uri,
                                        bl = BytesLoaded(size = 16, loaded = 12),
                                    )
                                    state.assert(expected)
                                }
                                4 -> {
                                    assertNull(state)
                                }
                                else -> error("Index $index is unexpected!")
                            }
                        }
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
                states.await()
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
                val finished: Deferred<Unit> = async {
                    runCatching {
                        withTimeout(4.seconds) {
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
                finished.await()
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
                    withTimeout(4.seconds) {
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

    @Test
    fun containsTest() {
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
                    withTimeout(4.seconds) {
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
    fun loadingTest() {
        runTest(timeout = 8.seconds) {
            withContext(Dispatchers.Default) {
                val u1 = mockURI(1)
                val u2 = mockURI(2)
                check(u1 != u2)
                val b1 = mockBytes(size = 14, pointer = 1)
                val b2 = mockBytes(size = 15, pointer = 2)
                check(!b1.contentEquals(b2))
                val h1 = mockBytes(size = 8, pointer = 1)
                val h2 = mockBytes(size = 8, pointer = 2)
                check(!h1.contentEquals(h2))
                val dsts = mutableMapOf<URI, ByteArray>()
                val factory: BytesWrapper.Factory = MockBytesWrapperFactory(
                    dsts = dsts,
                    hashes = listOf(b1 to h1, b2 to h2),
                )
                val requester: BytesRequester = MockBytesRequester(
                    map = mapOf(u1 to b1, u2 to b2),
                    delay = 1.seconds,
                )
                val loader = BytesLoader(
                    factory = factory,
                    requester = requester,
                    count = 6,
                )
                val finished: Deferred<Unit> = async {
                    withTimeout(7.seconds) {
                        val e1 = loader.events.first()
                        check(e1 is BytesLoader.Event.OnLoad)
                        assertEquals(u1, e1.uri)
                        assertTrue(b1.contentEquals(dsts[u1] ?: error("No bytes by $u1!")))
                        val e2 = loader.events.first()
                        check(e2 is BytesLoader.Event.OnLoad)
                        assertEquals(u2, e2.uri)
                        assertTrue(b2.contentEquals(dsts[u2] ?: error("No bytes by $u2!")))
                    }
                }
                launch(Dispatchers.Default) {
                    loader.add(
                        uri = u1,
                        size = b1.size.toLong(),
                        hash = h1,
                    )
                }
                delay(1.seconds)
                launch(Dispatchers.Default) {
                    loader.add(
                        uri = u2,
                        size = b2.size.toLong(),
                        hash = h2,
                    )
                }
                finished.await()
                assertEquals(2, dsts.size)
            }
        }
    }

    companion object {
        private fun BytesLoader.State.assert(that: BytesLoader.State) {
            assertEquals(that.queue.size, this.queue.size)
            that.queue.forEach { (key, value) ->
                assertEquals(value, this.queue[key])
            }
            assertEquals(that.current, this.current)
        }
    }
}
