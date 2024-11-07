package sp.service.sample

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import sp.kx.bytes.loader.BytesLoader
import java.net.URI

private suspend fun <T : Any> Flow<T?>.firstOrThrow(): T {
    while (true) return firstOrNull() ?: continue
}

fun main() {
    runBlocking {
        val uri = URI("foobar:uri")
        val bytes = "foobar:bytes".toByteArray()
        val hash = "foobar:hash".toByteArray()
        val loader = BytesLoader(
            factory = SampleBytesWrapper.Factory(
                hashes = listOf(
                    bytes to hash,
                ),
            ),
            requester = SampleBytesRequester(
                map = mapOf(uri to bytes),
            ),
            count = 2,
        )
        val events = async(Dispatchers.Default) {
            withTimeout(5_000) {
                loader.events.firstOrThrow()
            }
        }
        println("Try load: $uri")
        println("bytes(${bytes.size}): ${String(bytes)}")
        loader.add(
            uri = uri,
            size = bytes.size.toLong(),
            hash = hash,
        )
        when (val event = events.await()) {
            is BytesLoader.Event.OnError -> TODO("App:event:$event")
            is BytesLoader.Event.OnLoad -> {
                println("On load: ${event.uri}")
                val it = SampleBytesWrapper.dsts[event.uri]!!
                println("bytes(${it.size}): ${String(it)}")
                if (event.uri != uri) TODO()
                if (!it.contentEquals(bytes)) TODO()
            }
        }
    }
}
