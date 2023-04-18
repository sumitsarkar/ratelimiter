package me.sumit.ratelimiter

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import mu.KotlinLogging
import java.time.Duration

private val logger = KotlinLogging.logger {}

interface RateLimitBuffer<T> {
    val data: Array<RateLimitingItem<T>?>
    val duration: Duration
    fun insert(item: RateLimitingItem<T>): Boolean
    suspend fun hasCapacity(): Boolean

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

fun <T> CoroutineScope.rateLimiter(
    receiveFlow: Flow<T>,
    buffer: RateLimitBuffer<T>,
    pushItemToBuffer: (Long, Long?, T) -> RateLimitingItem<T>
): Flow<Throttled<T>> = flow {
    receiveFlow.collect { item ->
        var acknowledgement: RateLimitingItem<T>
        scheduler@ while (true) {
            acknowledgement = pushItemToBuffer(System.nanoTime(), null, item)
            if (!buffer.insert(acknowledgement)) {
                logger.debug { "Causing Delay" }
                buffer.causeDelay()
                continue@scheduler
            } else break@scheduler
        }

        emit(Throttled(item, acknowledgement))
    }
}
