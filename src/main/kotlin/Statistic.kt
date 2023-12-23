package com.mangatmodi.VirtualThreadsVsCoroutine

import java.time.Duration

data class Statistic(val timeTaken: Duration, val maxActive: Int, val numberOfThreadsUsed: Int)
