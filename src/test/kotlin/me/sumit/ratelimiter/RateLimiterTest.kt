package me.sumit.ratelimiter

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.system.measureTimeMillis

internal class RateLimiterKtTest {

    @ExperimentalCoroutinesApi
    @Test
    fun rateLimiterTest() = runBlocking {
        val inputChannel = Channel<Int>()
        val buffer = RateLimitBufferImpl<Int>(2, Duration.ofSeconds(1))



        launch {
            val throttledChannel = rateLimiter(inputChannel, buffer) { startTime, endTime, item ->
                RateLimitingItemImpl(startTime, endTime, item)
            }
            println("[${Thread.currentThread().name}]: Let's start our jobs!")

            launch {
                val totalTime = measureTimeMillis {
                    for (x in 1..11) {
                        val item = throttledChannel.receive()
                        item.acknowledgement.endTime = System.nanoTime()
                        println("[${Thread.currentThread().name}]: Received ${item.item}")
                    }
                }
                assert(totalTime > 5000)
                assert(totalTime < 6000)
            }


            for (x in 1..11) {
                println("[${Thread.currentThread().name}]: Sending Data $x")
                inputChannel.send(x)
            }
            inputChannel.close()
        }

        println("All Done!")
    }
}
