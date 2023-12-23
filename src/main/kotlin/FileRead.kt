package com.mangatmodi.VirtualThreadsVsCoroutine

import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.math.max
import kotlin.time.measureTime
import kotlin.time.toJavaDuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import java.net.URI
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel
import java.nio.channels.CompletionHandler
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

const val BASE_DIRECTORY = "/Users/mmod/workspace/virtualThreadTest/"
fun fileRead(parallelism: Int, mode: ConcurrencyModel): Statistic {
    System.setProperty("jdk.virtualThreadScheduler.maxPoolSize", parallelism.toString())
    val executorService = when (mode) {
        ConcurrencyModel.PLATFORM -> ThreadPoolExecutor(
            parallelism,
            parallelism,
            1,
            TimeUnit.MINUTES,
            ArrayBlockingQueue(1),
            ThreadPoolExecutor.CallerRunsPolicy()
        )

        ConcurrencyModel.VIRTUAL -> Executors.newVirtualThreadPerTaskExecutor()
        ConcurrencyModel.COROUTINE -> TODO()
    }

    val active = AtomicInteger(0)
    var maxActive = 0
    val threads = ConcurrentHashMap.newKeySet<String>()
    val exit = AtomicBoolean(false)
    Thread {
        while (true) {
            maxActive = max(maxActive, active.get())
            Thread.sleep(1)
            if (exit.get()) return@Thread else continue
        }
    }.start()

    val totalSize = AtomicLong(0)

    val timeTaken = measureTime {
        val todo = mutableListOf<Callable<Any>>()
        for (i in (1..100)) {
            todo.add(Callable {
                active.incrementAndGet()
                val path = Path.of("${BASE_DIRECTORY}file${i}")

                val bytes = Files.readAllBytes(path)
                totalSize.addAndGet(bytes.size.toLong())
                val threadName = threadName(mode)
                threads.add(threadName)
                active.decrementAndGet()
            })
        }

        executorService.invokeAll(todo)
    }
    executorService.shutdown()
    exit.set(true)

    System.clearProperty("jdk.virtualThreadScheduler.maxPoolSize")

    return Statistic(
        timeTaken = timeTaken.toJavaDuration(),
        maxActive = maxActive,
        numberOfThreadsUsed = threads.size,
        totalBytes = totalSize.get()
    )
}

fun threadName(mode: ConcurrencyModel) = when (mode) {
    ConcurrencyModel.PLATFORM -> Thread.currentThread().name
    ConcurrencyModel.VIRTUAL -> Thread.currentThread().toString().split("@")[1]
    ConcurrencyModel.COROUTINE -> TODO()
}
