package com.mangatmodi.VirtualThreadsVsCoroutine

import java.time.Duration


fun main(args: Array<String>) {
    val parallelism = args[0].toInt()
    val s1 = fileRead(parallelism, true)
    val s2 = fileRead(parallelism, false)

    dump(parallelism, s1 to s2)
}


fun dump(parallel: Int, stats: Pair<Statistic, Statistic>) {
    val (virtual, nonVirtual) = stats
    println("$parallel,${virtual.timeTaken.pretty()},${nonVirtual.timeTaken.pretty()},${virtual.maxActive},${nonVirtual.maxActive},${virtual.numberOfThreadsUsed},${nonVirtual.numberOfThreadsUsed}")

}

fun Duration.pretty(): String {
    return toString()
        .substring(2)
        .replace("(\\d[HMS])(?!$)", "$1 ")
        .lowercase()
}