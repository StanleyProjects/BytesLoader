package sp.kx.bytes.loader

import java.net.URI

interface BytesRequester {
    fun request(
        uri: URI,
        index: Long,
        count: Int,
    ): ByteArray
}
