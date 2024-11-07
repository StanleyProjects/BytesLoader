package sp.service.sample

import sp.kx.bytes.loader.BytesRequester
import java.net.URI

internal class SampleBytesRequester(
    private val map: Map<URI, ByteArray>,
) : BytesRequester {
    override fun request(uri: URI, index: Long, count: Int): ByteArray {
        val bytes = map[uri] ?: error("No bytes by $uri!")
        println("$tag:$uri: try copy of ${bytes.size} index: $index count: $count")
//        if (index > bytes.lastIndex) TODO("Index error!")
        val fromIndex = index.toInt()
        val toIndex = kotlin.math.min(bytes.size, (index + count).toInt())
        println("$tag:$uri: copy from $fromIndex to $toIndex")
        return bytes.copyOfRange(fromIndex = fromIndex, toIndex = toIndex)
    }

    companion object {
        private const val tag = "[SampleBytesRequester]"
    }
}
