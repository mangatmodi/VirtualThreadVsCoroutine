package com.mangatmodi.VirtualThreadsVsCoroutine

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.math.max
import kotlin.time.measureTime
import kotlin.time.toJavaDuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel
import java.nio.channels.CompletionHandler
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun coroutineReadFile(parallelism: Int): Statistic {
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
        val list = (1..100).map { i ->
            val totalPerFile = AtomicLong(0)
            val path = Path.of("${BASE_DIRECTORY}file${i}")

            CoroutineScope(Dispatchers.IO.limitedParallelism(parallelism)).async {
                active.incrementAndGet()
                AsynchronousFileChannel.open(path).use { channel ->
                    var bytes = 0
                    do {
                        val buf = ByteBuffer.allocate(4_096_000)
                        totalSize.addAndGet(bytes.toLong())
                        totalPerFile.addAndGet(bytes.toLong())
                        bytes = channel.aRead(totalPerFile.toLong(), buf)
                        buf.clear()
                    } while (bytes != -1)
                }


                //  totalSize.addAndGet(bytes.size.toLong())
                threads.add(Thread.currentThread().name)
                active.decrementAndGet()

            }
        }

        list.awaitAll()
    }

    exit.set(true)

    return Statistic(
        timeTaken = timeTaken.toJavaDuration(),
        maxActive = maxActive,
        numberOfThreadsUsed = threads.size,
        totalBytes = totalSize.get()
    )
}

suspend fun AsynchronousFileChannel.aRead(pos: Long, buf: ByteBuffer): Int =
    suspendCoroutine { cont ->
        read(buf, pos, Unit, object : CompletionHandler<Int, Unit> {
            override fun completed(bytesRead: Int, attachment: Unit) {
                cont.resume(bytesRead)
            }

            override fun failed(exception: Throwable, attachment: Unit) {
                cont.resumeWithException(exception)
            }
        })
    }


suspend fun coroutineReadFileNonAsynchronous(parallelism: Int): Statistic {
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
        val list = (1..100).map { i ->
            val path = Path.of("${BASE_DIRECTORY}file${i}")

            CoroutineScope(Dispatchers.IO.limitedParallelism(parallelism)).async {
                active.incrementAndGet()
                val bytes = Files.readAllBytes(path)
                totalSize.addAndGet(bytes.size.toLong())
                val threadName = Thread.currentThread().name
                threads.add(threadName)
                active.decrementAndGet()
            }
        }

        list.awaitAll()
    }

    exit.set(true)

    return Statistic(
        timeTaken = timeTaken.toJavaDuration(),
        maxActive = maxActive,
        numberOfThreadsUsed = threads.size,
        totalBytes = totalSize.get()
    )
}
