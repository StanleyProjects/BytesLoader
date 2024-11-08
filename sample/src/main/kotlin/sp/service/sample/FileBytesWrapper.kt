package sp.service.sample

import sp.kx.bytes.loader.BytesWrapper
import java.io.File
import java.net.URI
import java.security.MessageDigest

internal class FileBytesWrapper private constructor(
    private val tmp: File,
    private val dst: File,
    private val md: MessageDigest,
) : BytesWrapper {
    class Factory(
        private val root: File,
        private val md: MessageDigest,
    ) : BytesWrapper.Factory {
        init {
            root.deleteRecursively()
            root.resolve("tmp").mkdirs()
            root.resolve("dst").mkdirs()
        }

        override fun build(uri: URI): BytesWrapper {
            return FileBytesWrapper(
                tmp = root.resolve("tmp").resolve(uri.toString()),
                dst = root.resolve("dst").resolve(uri.toString()),
                md = md,
            )
        }
    }

    override fun check(loaded: Long) {
        if (loaded == 0L) {
            tmp.delete()
        } else {
            val length = tmp.length()
            if (length != loaded) TODO("Length: $length, but loaded: $loaded!")
        }
    }

    override fun append(bytes: ByteArray) {
        tmp.appendBytes(bytes)
    }

    override fun commit(hash: ByteArray) {
        if (!hash.contentEquals(md.digest(tmp.readBytes()))) TODO("Hashes error!")
        if (dst.exists()) TODO("file: ${dst.absolutePath} exists!")
        tmp.renameTo(dst)
    }
}
