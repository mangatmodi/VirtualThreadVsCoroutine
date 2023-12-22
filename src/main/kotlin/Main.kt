import kotlin.math.max
import kotlin.system.measureTimeMillis
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong


private const val PARALLELISM = 15
private const val BASE_DIRECTORY = "/Users/mmod/workspace/virtualThreadTest/"
fun main() {

    val virtual = true

    val executorService =
        if (!virtual) {
            ThreadPoolExecutor(
                PARALLELISM,
                PARALLELISM,
                1,
                TimeUnit.MINUTES,
                ArrayBlockingQueue(1),
                CallerRunsPolicy()
            )
        } else {
            Executors.newVirtualThreadPerTaskExecutor()
        }

    val active = AtomicInteger(0)
    var maxActive = 0
    Thread {
        while (true) {
            maxActive = max(maxActive, active.get())
            Thread.sleep(1)
        }
    }.start()

    val timeTaken = measureTimeMillis {
        val totalSize = AtomicLong(0)
        val todo = mutableListOf<Callable<Any>>()
        for (i in (1..100)) {
            todo.add(Callable {
                active.incrementAndGet()
                val bytes = Files.readAllBytes(Path.of("${BASE_DIRECTORY}file${i}"))
                totalSize.addAndGet(bytes.size.toLong())
                active.decrementAndGet()
            })
        }

        executorService.invokeAll(todo)
    }
    println(Duration.ofMillis(timeTaken))
    executorService.shutdown()
    println(maxActive)
}