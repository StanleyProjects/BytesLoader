package sp.kx.bytes.loader

import java.net.URI

internal class ErrorBytesRequester(private val expected: Throwable) : BytesRequester {
    override fun request(uri: URI, index: Long, count: Int): ByteArray {
        throw expected
    }
}
