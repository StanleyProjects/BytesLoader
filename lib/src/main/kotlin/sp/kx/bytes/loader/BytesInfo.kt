package sp.kx.bytes.loader

internal class BytesInfo(
    private val size: Long,
    val hash: ByteArray,
) {
    var loaded: Long = 0

    fun completed(): Boolean {
        // todo loaded < 0
        // todo loaded > info.size
        return loaded == size
    }
}
