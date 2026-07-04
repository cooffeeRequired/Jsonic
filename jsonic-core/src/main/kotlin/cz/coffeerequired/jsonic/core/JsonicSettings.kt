package cz.coffeerequired.jsonic.core

object JsonicSettings {
    var debug: Boolean = false
    var autoUpdater: Boolean = false
    var pathDelimiter: String = "."
    var pathTokenCacheSize: Int = 1024
    var httpEnabled: Boolean = true
    var httpMaxThreads: Int = 2
    var httpRequestTimeoutSeconds: Int = 30
    var httpConnectTimeoutSeconds: Int = 10
    var serverEnabled: Boolean = true
    var serverPort: Int = 8080
    var serverHost: String = "127.0.0.1"
    var serverPublicBind: Boolean = false
    var serverMaxBodySizeBytes: Long = 1_048_576
    var serverThreadModel: ThreadModel = ThreadModel.MAIN
    var serverHttpsEnabled: Boolean = false
    var serverHttpsPort: Int = 8443
    var serverHttpsCertFile: String? = null
    var serverHttpsKeyFile: String? = null
    var serverHttpsKeyPassword: String = ""
    var serverMiddlewareCors: Boolean = false
    var serverMiddlewareLogger: Boolean = false
    var serverMiddlewareJson: Boolean = false
    var watcherIntervalMs: Long = 100
    var watcherRefreshRateMs: Long = 50

    enum class ThreadModel { MAIN, ASYNC }
}
