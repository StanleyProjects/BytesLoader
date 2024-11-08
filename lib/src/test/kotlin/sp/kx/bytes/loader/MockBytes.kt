package sp.kx.bytes.loader

internal fun mockBytes(size: Int): ByteArray {
    return ByteArray(size) { index ->
        (size - index).toByte()
    }
}
