package sp.kx.bytes.loader

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class BytesLoadedTest {
    @Test
    fun classTest() {
        val bl = BytesLoaded(
            size = 1,
            loaded = 2,
        )
        check(bl.size != bl.loaded)
        assertEquals(1, bl.size)
        assertEquals(2, bl.loaded)
    }
}
