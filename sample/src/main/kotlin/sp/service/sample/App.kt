package sp.service.sample

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import sp.kx.bytes.loader.BytesLoader
import sp.kx.bytes.loader.BytesWrapper
import java.io.File
import java.net.URI
import java.security.MessageDigest

fun main() {
    runBlocking {
        val uri = URI("foobar:uri")
        val file = File("/tmp").resolve(uri.toString())
        file.delete()
        file.writeBytes("foobar:bytes:1".toByteArray())
        val md = MessageDigest.getInstance("md5")
        val root = File("/tmp/BytesLoader")
        val factory: BytesWrapper.Factory = FileBytesWrapper.Factory(
            root = root,
            md = md,
        )
        val loader = BytesLoader(
            factory = factory,
            requester = SampleBytesRequester(
                map = mapOf(uri to file.readBytes()),
            ),
            count = 3,
        )
        val events = async(Dispatchers.Default) {
            withTimeout(5_000) {
                loader.events.first()
            }
        }
        val job = launch(Dispatchers.Default) {
            loader.states.collect { state ->
                if (state == null) {
                    println("no state")
                } else {
                    println("state: $state")
                }
            }
        }
        println("Try load: $uri")
        println("bytes(${file.length()}): ${String(file.readBytes())}")
        loader.add(
            uri = uri,
            size = file.length(),
            hash = md.digest(file.readBytes()),
        )
        when (val event = events.await()) {
            is BytesLoader.Event.OnError -> TODO("App:event:$event")
            is BytesLoader.Event.OnLoad -> {
                println("On load: ${event.uri}")
                val it = root.resolve("dst").resolve(uri.toString())
                println("bytes(${it.length()}): ${String(it.readBytes())}")
                if (event.uri != uri) TODO()
                if (!it.readBytes().contentEquals(file.readBytes())) TODO()
            }
        }
        delay(1_000)
        job.cancel()
    }
}
