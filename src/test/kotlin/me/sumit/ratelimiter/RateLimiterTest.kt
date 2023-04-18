package me.sumit.ratelimiter

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.system.measureTimeMillis

private val logger = KotlinLogging.logger {}

internal class RateLimiterKtTest {
    @Test
    fun rateLimiterFlowTest() = runBlocking {

        val buffer = RateLimitBufferImpl<Int>(2, Duration.ofSeconds(1))

        launch {
            val throttledFlow = rateLimiter(flowCreator(), buffer) { startTime, endTime, item ->
                RateLimitingItemImpl(startTime, endTime, item)
            }
            logger.info { "[${Thread.currentThread().name}]: Let's start our jobs!" }

            launch {
                val totalTime = measureTimeMillis {
                    throttledFlow.collect { item ->
                        logger.debug { "Received $item" }
                        item.acknowledgement.endTime = System.nanoTime()
                    }
                }
                logger.info { "Total time: $totalTime" }
                assert(totalTime > 5000)
                assert(totalTime < 6000)
            }

        }

        logger.info { "All Done!" }
    }

    private fun flowCreator(): Flow<Int> = flow {
        for (x in 1..11) {
            emit(x)
        }
    }
}
