package sp.service.sample

import sp.kx.bytes.loader.BytesWrapper
import java.net.URI

internal class SampleBytesWrapper private constructor(
    private val hashes: List<Pair<ByteArray, ByteArray>>,
    private val uri: URI,
) : BytesWrapper {
    class Factory(
        private val hashes: List<Pair<ByteArray, ByteArray>>,
    ) : BytesWrapper.Factory {
        override fun build(uri: URI): BytesWrapper {
            return SampleBytesWrapper(
                hashes = hashes,
                uri = uri,
            )
        }
    }

    override fun check(loaded: Long) {
        if (loaded == 0L) {
            tmps[uri] = ByteArray(0)
        } else {
            val bytes = tmps[uri] ?: error("No bytes by $uri!")
            val length = bytes.size.toLong()
            if (length != loaded) TODO("Length: $length, but loaded: $loaded!")
        }
    }

    override fun append(bytes: ByteArray) {
        val actual = tmps[uri] ?: error("No bytes by $uri!")
        println("$tag:$uri: append ${actual.size} + ${bytes.size}")
        tmps[uri] = actual + bytes
    }

    override fun commit(hash: ByteArray) {
        val bytes = tmps[uri] ?: error("No bytes by $uri!")
        val equals = hashes.any { it.first.contentEquals(bytes) && it.second.contentEquals(hash) }
        if (!equals) TODO("Hashes error!")
        if (dsts.containsKey(uri)) TODO("Bytes $uri exist!")
        dsts[uri] = bytes
        tmps.remove(uri)
    }

    companion object {
        private const val tag = "[SampleBytesWrapper]"
        private val tmps = mutableMapOf<URI, ByteArray>()
        val dsts = mutableMapOf<URI, ByteArray>()
    }
}
