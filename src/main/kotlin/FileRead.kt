package com.mangatmodi.VirtualThreadsVsCoroutine

import kotlin.math.max
import kotlin.time.measureTime
import kotlin.time.toJavaDuration
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

private const val BASE_DIRECTORY = "/Users/mmod/workspace/virtualThreadTest/"
fun fileRead(parallelism: Int, virtual: Boolean): Statistic {
    System.setProperty("jdk.virtualThreadScheduler.maxPoolSize", parallelism.toString())
    val executorService =
        if (!virtual) {
            ThreadPoolExecutor(
                parallelism,
                parallelism,
                1,
                TimeUnit.MINUTES,
                ArrayBlockingQueue(1),
                ThreadPoolExecutor.CallerRunsPolicy()
            )
        } else {
            Executors.newVirtualThreadPerTaskExecutor()
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

    val timeTaken = measureTime {
        val totalSize = AtomicLong(0)
        val todo = mutableListOf<Callable<Any>>()
        for (i in (1..100)) {
            todo.add(Callable {
                active.incrementAndGet()
                val bytes = Files.readAllBytes(Path.of("${BASE_DIRECTORY}file${i}"))
                totalSize.addAndGet(bytes.size.toLong())
                val threadName = if (virtual) {
                    Thread.currentThread().toString().split("@")[1]
                } else {
                    Thread.currentThread().name
                }
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
    )
}