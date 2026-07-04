package cz.coffeerequired.jsonic.core.http

import cz.coffeerequired.jsonic.core.JsonicSettings
import java.net.http.HttpClient
import java.time.Duration
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

object HttpClientProvider {
    @Volatile
    private var client: HttpClient? = null

    @Volatile
    private var executor: ThreadPoolExecutor? = null

    fun getClient(): HttpClient {
        if (client == null) {
            synchronized(this) {
                if (client == null) {
                    client = HttpClient.newBuilder()
                        .executor(getExecutor())
                        .connectTimeout(Duration.ofSeconds(JsonicSettings.httpConnectTimeoutSeconds.toLong()))
                        .version(HttpClient.Version.HTTP_1_1)
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .build()
                }
            }
        }
        return client!!
    }

    fun getExecutor(): ThreadPoolExecutor {
        if (executor == null) {
            synchronized(this) {
                if (executor == null) {
                    val threads = JsonicSettings.httpMaxThreads.coerceAtLeast(1)
                    executor = ThreadPoolExecutor(
                        threads,
                        threads * 4,
                        60L,
                        TimeUnit.SECONDS,
                        LinkedBlockingQueue(128),
                        { r ->
                            Thread(r, "Jsonic-HTTP").apply { isDaemon = true }
                        },
                        { r, pool ->
                            if (!pool.isShutdown) r.run()
                        }
                    )
                }
            }
        }
        return executor!!
    }

    fun requestTimeoutSeconds(): Int = JsonicSettings.httpRequestTimeoutSeconds

    fun shutdown() {
        synchronized(this) {
            client?.close()
            client = null
            executor?.shutdownNow()
            executor = null
        }
    }
}
