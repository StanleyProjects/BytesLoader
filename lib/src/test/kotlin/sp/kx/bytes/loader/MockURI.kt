package sp.kx.bytes.loader

import java.net.URI

internal fun mockURI(pointer: Int = 0): URI {
    return URI("uri:$pointer")
}
