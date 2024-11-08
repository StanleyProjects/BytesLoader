package sp.kx.bytes.loader

import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.URI
import kotlin.time.Duration.Companion.seconds

internal class BytesLoaderTest {
    @Test
    fun addTest() {
        runTest(timeout = 5.seconds) {
            val uri = mockURI()
            val bytes = mockBytes(size = 32)
            val hash = mockBytes(size = 16)
            val dsts = mutableMapOf<URI, ByteArray>()
            val factory: BytesWrapper.Factory = MockBytesWrapperFactory(
                dsts = dsts,
                hashes = listOf(bytes to hash)
            )
            val requester: BytesRequester = MockBytesRequester(
                map = mapOf(uri to bytes)
            )
            val count = 4
            val loader = BytesLoader(
                factory = factory,
                requester = requester,
                count = count,
            )
//            val events = Channel<BytesLoader.Event>()
//            backgroundScope.launch {
//                while (true) {
//                    val event = loader.events.firstOrNull()
//                    if (event != null) {
//                        events.send(event)
//                        break
//                    }
//                }
//            }
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
}
