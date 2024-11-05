package sp.kx.bytes.loader

import java.net.URI

interface BytesWrapper {
    interface Factory {
        fun build(uri: URI): BytesWrapper
    }

    fun check(loaded: Long)
    fun append(bytes: ByteArray)
    fun commit(hash: ByteArray)
}
