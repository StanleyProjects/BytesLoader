package sp.kx.bytes.loader

internal fun mockBytes(size: Int = 0, pointer: Int = 0): ByteArray {
    return ByteArray(size) { index ->
        (size + pointer - index).toByte()
    }
}
