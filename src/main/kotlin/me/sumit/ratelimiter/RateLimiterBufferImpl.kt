package me.sumit.ratelimiter

import kotlinx.coroutines.delay
import mu.KotlinLogging
import java.time.Duration

private val logger = KotlinLogging.logger {}

class RateLimitBufferImpl<T>(private val capacity: Int, override val duration: Duration) : RateLimitBuffer<T> {
    override val data: Array<RateLimitingItem<T>?> = arrayOfNulls(capacity)

    @Synchronized
    override fun insert(item: RateLimitingItem<T>): Boolean {
        // Find write-able position
        // Either find a null position in the array or find an item that satisfies the isFulfilled for nullability
        val writePosition = findWritablePosition()

        return when {
            writePosition < 0 -> false
            else -> {
                return when {
                    data[writePosition] != null -> {
                        logger.debug { "Overwriting ${data[writePosition]?.data} with ${item.data}" }
                        data[writePosition] = item
                        true
                    }

                    else -> {
                        data[writePosition] = item
                        true
                    }
                }
            }
        }
    }

    @Synchronized
    private fun findWritablePosition(): Int {
        val indexedData = data.withIndex()
        val nullPosition = indexedData.find { it.value == null }
        if (nullPosition != null)
            return nullPosition.index

        val oldestItem = indexedData.filter { it.value != null }
            .minBy { (_, value) -> value!!.startTime }

        return if (oldestItem.value!!.isFulfilled()) {
            val timeElapsed = System.nanoTime().minus(oldestItem.value!!.endTime!!)
            logger.debug { "Elapsed Time: $timeElapsed" }
            if (timeElapsed >= duration.toNanos())
                oldestItem.index
            else
                -1
        } else {
            -1
        }
    }

    fun peekOldest(): RateLimitingItem<T>? {
        val sortedNonNullList = data.filterNotNull().sorted()
        return when {
            sortedNonNullList.isNotEmpty() -> sortedNonNullList.find { it.endTime != null } ?: sortedNonNullList[0]
            else -> null
        }
    }

    override suspend fun causeDelay() {
        delay(duration.toMillis() / capacity)
    }

}


class RateLimitingItemImpl<T>(override val startTime: Long, override var endTime: Long?, override val data: T) :
    RateLimitingItem<T> {
    override fun isFulfilled(): Boolean {
        return endTime != null
    }

    override fun compareTo(other: RateLimitingItem<T>): Int {
        return (other.endTime?.let { this.endTime?.compareTo(it) ?: 1 }) ?: -1
    }
}
