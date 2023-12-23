package com.mangatmodi.VirtualThreadsVsCoroutine


suspend fun main(args: Array<String>) {
    val parallelism = args[0].toInt()
    val s1 = fileRead(parallelism, ConcurrencyModel.VIRTUAL)
    System.gc()

    val s2 = fileRead(parallelism, ConcurrencyModel.PLATFORM)
    System.gc()

    val s3 = coroutineReadFileNonAsynchronous(parallelism)
    System.gc()

    dump(parallelism, s1, s2, s3)
}


fun dump(
    parallel: Int,
    virtual: Statistic,
    nonVirtual: Statistic,
    coroutine: Statistic,
) {
    println("$parallel,$nonVirtual,$virtual,$coroutine")
}
