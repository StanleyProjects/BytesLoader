package sp.kx.bytes.loader

import java.net.URI

internal class MockBytesRequester(
    private val map: Map<URI, ByteArray>,
) : BytesRequester {
    override fun request(uri: URI, index: Long, count: Int): ByteArray {
        val bytes = map[uri] ?: error("No bytes by $uri!")
        val fromIndex = index.toInt()
        val toIndex = kotlin.math.min(bytes.size, (index + count).toInt())
        return bytes.copyOfRange(fromIndex = fromIndex, toIndex = toIndex)
    }
}
