package me.sumit.ratelimiter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Duration

internal class RateLimitBufferImplTest {
    @Test
    fun peekOldestTest() {
        val buffer = RateLimitBufferImpl<Int>(10, Duration.ofSeconds(10))
        buffer.insert(RateLimitingItemImpl(1, -30, 1))
        buffer.insert(RateLimitingItemImpl(3, null, 2))
        buffer.insert(RateLimitingItemImpl(4, 1, 3))
        val oldest = RateLimitingItemImpl(-5, -50, 4)
        buffer.insert(oldest)

        assertEquals(buffer.peekOldest(), oldest)
    }
}
