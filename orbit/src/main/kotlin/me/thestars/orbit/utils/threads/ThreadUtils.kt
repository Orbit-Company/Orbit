package me.thestars.orbit.utils.threads

import com.google.common.util.concurrent.ThreadFactoryBuilder
import kotlinx.coroutines.Job
import me.thestars.orbit.OrbitLauncher
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object ThreadUtils {
    fun createThreadPool(name: String): ExecutorService {
        val classLoader = OrbitLauncher::class.java.classLoader

        val threadFactory = ThreadFactoryBuilder().setNameFormat(name).setThreadFactory { runnable ->
            val thread = Thread(runnable)
            thread.contextClassLoader = classLoader
            thread
        }.build()

        return Executors.newCachedThreadPool(threadFactory)
    }

    val activeJobs = ConcurrentLinkedDeque<Job>()
}