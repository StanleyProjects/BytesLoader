package sp.kx.bytes.loader

import java.net.URI

internal class MockBytesWrapperFactory(
    private val dsts: MutableMap<URI, ByteArray> = mutableMapOf(),
    private val hashes: List<Pair<ByteArray, ByteArray>> = emptyList(),
) : BytesWrapper.Factory {
    private val tmps = mutableMapOf<URI, ByteArray>()

    override fun build(uri: URI): BytesWrapper {
        return object : BytesWrapper {
            override fun check(loaded: Long) {
                if (loaded == 0L) {
                    tmps[uri] = ByteArray(0)
                } else {
                    val bytes = tmps[uri] ?: error("No bytes by $uri!")
                    val length = bytes.size.toLong()
                    if (length != loaded) error("Length: $length, but loaded: $loaded!")
                }
            }

            override fun append(bytes: ByteArray) {
                val actual = tmps[uri] ?: error("No bytes by $uri!")
                tmps[uri] = actual + bytes
            }

            override fun commit(hash: ByteArray) {
                val bytes = tmps[uri] ?: error("No bytes by $uri!")
                val equals = hashes.any { it.first.contentEquals(bytes) && it.second.contentEquals(hash) }
                if (!equals) error("Hashes error!")
                if (dsts.containsKey(uri)) error("Bytes $uri exist!")
                dsts[uri] = bytes
                tmps.remove(uri)
            }
        }
    }
}
