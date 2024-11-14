package sp.kx.bytes.loader

import java.net.URI

internal fun mockState(uri: URI, bl: BytesLoaded): BytesLoader.State {
    return BytesLoader.State(
        queue = mapOf(uri to bl),
        current = uri,
    )
}
