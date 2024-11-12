package sp.kx.bytes.loader

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class BytesLoader(
    private val factory: BytesWrapper.Factory,
    private val requester: BytesRequester,
    private val count: Int,
) {
    sealed interface Event {
        data class OnLoad(val uri: URI) : Event
        data class OnError(val error: Throwable) : Event
    }

    private val _events = MutableSharedFlow<Event>()
    val events = _events.asSharedFlow()

    private val loading = AtomicBoolean(false)

    private val queue: MutableMap<URI, BytesInfo> = ConcurrentHashMap()

    private suspend fun perform() {
        if (!loading.compareAndSet(false, true)) return
        while (true) {
            val (uri, info) = queue.entries.firstOrNull() ?: break
            val wrapper = factory.build(uri = uri)
            try {
                info.loaded = load(
                    uri = uri,
                    loaded = info.loaded,
                    wrapper = wrapper,
                )
                if (info.completed()) {
                    wrapper.commit(hash = info.hash)
                }
            } catch (error: Throwable) {
                queue.clear() // todo or queue.remove(uri)
                _events.emit(Event.OnError(error = error))
                break
            }
            if (info.completed()) {
                queue.remove(uri)
                _events.emit(Event.OnLoad(uri = uri))
            }
        }
        loading.set(false)
    }

    private fun load(
        uri: URI,
        loaded: Long,
        wrapper: BytesWrapper,
    ): Long {
        wrapper.check(loaded = loaded)
        val bytes = requester.request(
            uri = uri,
            index = loaded,
            count = count,
        )
        wrapper.append(bytes = bytes)
        return loaded + bytes.size
    }

    suspend fun add(
        uri: URI,
        size: Long,
        hash: ByteArray,
    ) {
        synchronized(this) {
            if (queue.containsKey(uri)) return
            queue[uri] = BytesInfo(
                size = size,
                hash = hash,
            )
        }
        perform()
    }

    fun stop() {
        queue.clear()
    }
}
