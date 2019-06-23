package me.sumit.ratelimiter

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

interface RateLimitBuffer<T> {
    val data: Array<RateLimitingItem<T>?>
    val duration: Duration

    fun insert(item: RateLimitingItem<T>): Boolean
    fun peekOldest(): RateLimitingItem<T>?
    fun hasCapacity(): Boolean

    fun canBeScheduled(): Boolean
    suspend fun causeDelay()
}

interface RateLimitingItem<T> : Comparable<RateLimitingItem<T>> {
    val startTime: Long
    var endTime: Long?
    val data: T
    fun isFulfilled(): Boolean
}

data class Throttled<T>(
        val item: T,
        val acknowledgement: RateLimitingItem<T>
)

@ExperimentalCoroutinesApi
fun <T> CoroutineScope.rateLimiter(
        receiveChannel: ReceiveChannel<T>,
        buffer: RateLimitBuffer<T>,
        pushItemToBuffer: (Long, Long?, T) -> RateLimitingItem<T>
) = produce(capacity = buffer.data.size) {
    for (item in receiveChannel) {
        scheduler@ while (true) {
            if (!buffer.canBeScheduled()) {
                buffer.causeDelay()
                continue@scheduler
            } else break@scheduler
        }
        val acknowledgement = pushItemToBuffer(System.nanoTime(), null, item)

        buffer.insert(acknowledgement)
        send(Throttled(item, acknowledgement))
    }
}


@ExperimentalCoroutinesApi
suspend inline fun <E> ReceiveChannel<E>.consumeEach(
        maxConcurrency: Int,
        initialConcurrency: Int = 10,
        coroutineContext: CoroutineContext = EmptyCoroutineContext,
        crossinline action: suspend (E) -> Unit
) =
        withContext(coroutineContext) {

            if (maxConcurrency <= 0)
                if (initialConcurrency > maxConcurrency)
                    throw IllegalArgumentException("initialConcurrency must be less than or equal to maxConcurrency")
                else if (initialConcurrency < 0)
                    throw IllegalArgumentException("Can not have a negative initialConcurrency")


            val busy = AtomicInteger(0)

            val workers = MutableList(Integer.min(maxConcurrency, initialConcurrency)) {
                launch {
                    while (isActive && !(isClosedForReceive && isEmpty)) {
                        busy.incrementAndGet()
                        action(this@consumeEach.receive())
                        busy.decrementAndGet()
                    }
                }
            }

            if (maxConcurrency > initialConcurrency || maxConcurrency <= 0) {
                while (isActive && !(isClosedForReceive && isEmpty) && (workers.size < maxConcurrency || maxConcurrency <= 0)) {
                    if (busy.get() == workers.size) {
                        val received = receive()

                        workers += launch {
                            busy.incrementAndGet()
                            action(received)
                            busy.decrementAndGet()

                            while (isActive && !(isClosedForReceive && isEmpty)) {
                                busy.incrementAndGet()
                                action(this@consumeEach.receive())
                                busy.decrementAndGet()
                            }
                        }
                    }
                    delay(2)
                }
            }
            workers.joinAll()
        }
