package me.sumit.ratelimiter

import kotlinx.coroutines.delay
import java.time.Duration

class RateLimitBufferImpl<T>(private val capacity: Int, override val duration: Duration) : RateLimitBuffer<T> {
    override val data: Array<RateLimitingItem<T>?> = arrayOfNulls(capacity)
    var size = 0
        private set

    @Synchronized
    override fun insert(item: RateLimitingItem<T>): Boolean {
        // Find write-able position
        // Either find a null position in the array or find an item that satisfies the isFulfilled for nullability
        var writePosition = data.indexOfFirst { it == null }
        if (writePosition == -1) {
            writePosition = data.filterNotNull().indexOfFirst { it.isFulfilled() }
        }
        return when {
            writePosition < 0 -> false
            else -> {
                return when {
                    data[writePosition] != null -> {
                        data[writePosition] = item
                        true
                    }
                    else -> {
                        data[writePosition] = item
                        size++
                        true
                    }
                }
            }
        }
    }

    @Synchronized
    override fun peekOldest(): RateLimitingItem<T>? {
        val sortedNonNullList = data.filterNotNull().sorted()
        return when {
            sortedNonNullList.isNotEmpty() -> sortedNonNullList.find { it.endTime != null } ?: sortedNonNullList[0]
            else -> null
        }
    }

    @Synchronized
    override fun hasCapacity(): Boolean {
        return when {
            size < capacity -> true
            else -> false
        }
    }

    override fun canBeScheduled(): Boolean {
        if (this.hasCapacity()) return true
        val oldestItem = this.peekOldest()

        return when {
            oldestItem?.isFulfilled() == false -> false
            oldestItem?.isFulfilled() == true && System.nanoTime().minus(oldestItem.endTime!!) >= duration.toNanos() -> true
            else -> false
        }
    }

    override suspend fun causeDelay() {
        delay(duration.toMillis() / capacity)
    }

}


class RateLimitingItemImpl<T>(override val startTime: Long, override var endTime: Long?, override val data: T) : RateLimitingItem<T> {
    override fun isFulfilled(): Boolean {
        return endTime != null
    }

    override fun compareTo(other: RateLimitingItem<T>): Int {
        return (other.endTime?.let { this.endTime?.compareTo(it) ?: 1}) ?: -1
    }
}
