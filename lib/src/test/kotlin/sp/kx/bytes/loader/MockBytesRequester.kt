package sp.kx.bytes.loader

import java.net.URI
import kotlin.time.Duration

internal class MockBytesRequester(
    private val map: Map<URI, ByteArray> = emptyMap(),
    private val delay: Duration = Duration.ZERO,
) : BytesRequester {
    override fun request(uri: URI, index: Long, count: Int): ByteArray {
        Thread.sleep(delay.inWholeMilliseconds)
        val bytes = map[uri] ?: error("No bytes by $uri!")
        val fromIndex = index.toInt()
        val toIndex = kotlin.math.min(bytes.size, (index + count).toInt())
        return bytes.copyOfRange(fromIndex = fromIndex, toIndex = toIndex)
    }
}
