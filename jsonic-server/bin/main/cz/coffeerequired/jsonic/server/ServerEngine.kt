package cz.coffeerequired.jsonic.server

import com.google.gson.JsonElement
import cz.coffeerequired.jsonic.core.JsonEngine
import cz.coffeerequired.jsonic.core.JsonicLogger
import cz.coffeerequired.jsonic.core.JsonicSettings
import cz.coffeerequired.jsonic.core.support.AnsiColorConverter
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.engine.sslConnector
import io.ktor.server.netty.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.time.Duration.Companion.seconds

enum class MiddlewareType { CORS, JSON, LOGGER, AUTH_BEARER }

private fun mimeForExtension(ext: String): String = when (ext.lowercase()) {
    "html", "htm" -> ContentType.Text.Html.toString()
    "css" -> ContentType.Text.CSS.toString()
    "js" -> "application/javascript"
    "json" -> ContentType.Application.Json.toString()
    "svg" -> "image/svg+xml"
    "png" -> "image/png"
    "jpg", "jpeg" -> "image/jpeg"
    "ico" -> "image/x-icon"
    "txt" -> ContentType.Text.Plain.toString()
    else -> ContentType.Application.OctetStream.toString()
}

data class RouteDefinition(
    val method: HttpMethod,
    val path: String,
    val handler: suspend (JsonicCall) -> Unit,
    val builtInMiddlewares: List<MiddlewareType> = emptyList(),
    val skriptMiddlewares: List<JsonicRouteMiddleware> = emptyList()
)

data class WebSocketDefinition(
    val path: String,
    val onConnect: suspend (JsonicWebSocketSession) -> Unit,
    val onMessage: suspend (JsonicWebSocketSession, String) -> Unit,
    val onClose: suspend (JsonicWebSocketSession) -> Unit
)

data class ErrorHandlerDefinition(
    val statusCode: Int,
    val handler: suspend (JsonicCall) -> Unit
)

data class StaticMount(
    val urlPrefix: String,
    val folderPath: String
)

class JsonicWebSocketSession internal constructor(
    val path: String,
    internal val session: DefaultWebSocketServerSession
) {
    suspend fun send(text: String) = session.send(Frame.Text(text))
    suspend fun close(reason: CloseReason = CloseReason(CloseReason.Codes.NORMAL, "closed")) = session.close(reason)
}

class JsonicCall internal constructor(
    private val call: ApplicationCall,
    private val routeParams: Map<String, String>
) {
    val method: String get() = call.request.httpMethod.value
    val path: String get() = call.request.path()
    private var responded = false
    var pendingStatus: Int = 200
    var pendingBody: String? = null
    var pendingContentType: String = "text/plain"

    fun queueText(text: String) {
        pendingBody = text
        pendingContentType = "text/plain"
    }

    fun queueJson(json: JsonElement) {
        pendingBody = JsonEngine.toJsonString(json)
        pendingContentType = "application/json"
    }

    fun queueStatus(status: Int) {
        pendingStatus = status
        pendingBody = null
    }

    fun queueFile(relativePath: String, dataRoot: File) {
        val root = dataRoot.canonicalFile
        val file = File(root, relativePath).canonicalFile
        if (!file.path.startsWith(root.path) || !file.isFile) {
            pendingBody = null
            if (pendingStatus == 200) pendingStatus = 404
            return
        }
        pendingBody = file.readText(Charsets.UTF_8)
        pendingContentType = mimeForExtension(file.extension)
    }

    fun needsErrorHandler(): Boolean = pendingBody == null && pendingStatus != 200

    fun statusCode(): Int = pendingStatus

    suspend fun finishPendingReply() {
        if (responded || call.response.isSent) return
        when {
            pendingBody != null -> {
                call.response.header(HttpHeaders.ContentType, pendingContentType)
                call.respond(HttpStatusCode.fromValue(pendingStatus), pendingBody!!)
            }
            pendingStatus != 200 -> call.respond(HttpStatusCode.fromValue(pendingStatus))
            else -> call.respond(HttpStatusCode.NotFound, "No reply")
        }
        responded = true
    }

    fun param(name: String): String? = routeParams[name] ?: call.request.queryParameters[name]
    fun query(name: String): String? = call.request.queryParameters[name]
    fun header(name: String): String? = call.request.headers[name]

    suspend fun bodyAsString(): String = call.receiveText()
    suspend fun bodyAsJson(): JsonElement = JsonEngine.toJson(bodyAsString())

    suspend fun replyText(text: String, status: HttpStatusCode = HttpStatusCode.OK) {
        if (responded) return
        call.respond(status, text)
        responded = true
    }

    suspend fun replyJson(json: JsonElement, status: HttpStatusCode = HttpStatusCode.OK) {
        if (responded) return
        call.response.header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        call.respond(status, JsonEngine.toJsonString(json))
        responded = true
    }

    suspend fun reply(status: HttpStatusCode) {
        if (responded) return
        call.respond(status)
        responded = true
    }

    suspend fun setResponseHeader(name: String, value: String) {
        call.response.header(name, value)
    }
}

class JsonicApp {
    var port: Int = JsonicSettings.serverPort
    var host: String = JsonicSettings.serverHost
    var httpsEnabled: Boolean = JsonicSettings.serverHttpsEnabled
    var httpsPort: Int = JsonicSettings.serverHttpsPort
    var httpsCertPath: String? = JsonicSettings.serverHttpsCertFile
    var httpsKeyPath: String? = JsonicSettings.serverHttpsKeyFile
    var httpsKeyPassword: String = JsonicSettings.serverHttpsKeyPassword
    val middlewares = mutableListOf<MiddlewareType>()
    val routes = CopyOnWriteArrayList<RouteDefinition>()
    val webSockets = CopyOnWriteArrayList<WebSocketDefinition>()
    val errorHandlers = CopyOnWriteArrayList<ErrorHandlerDefinition>()
    val staticMounts = CopyOnWriteArrayList<StaticMount>()
    internal val mountedApps = mutableListOf<Pair<String, JsonicApp>>()

    @JvmName("clearRoutes")
    fun clearRoutes() {
        routes.clear()
    }

    fun port(p: Int) = apply { port = p }
    fun host(h: String) = apply { host = h }
    fun use(middleware: MiddlewareType) = apply { middlewares += middleware }
    fun get(path: String, handler: suspend (JsonicCall) -> Unit) = route(HttpMethod.Get, path, handler)

    @JvmName("registerGet")
    fun registerGet(path: String, handler: java.util.function.Consumer<JsonicCall>): JsonicApp {
        return registerRouteNamed("GET", path, handler, emptyList())
    }

    @JvmName("registerGetWithMiddleware")
    fun registerGet(path: String, handler: java.util.function.Consumer<JsonicCall>, middlewares: List<MiddlewareType>): JsonicApp {
        return registerRouteNamed("GET", path, handler, middlewares)
    }

    @JvmName("registerPost")
    fun registerPost(path: String, handler: java.util.function.Consumer<JsonicCall>): JsonicApp {
        return registerRouteNamed("POST", path, handler, emptyList())
    }

    @JvmName("registerPostWithMiddleware")
    fun registerPost(path: String, handler: java.util.function.Consumer<JsonicCall>, middlewares: List<MiddlewareType>): JsonicApp {
        return registerRouteNamed("POST", path, handler, middlewares)
    }

    @JvmName("registerRouteNamed")
    fun registerRouteNamed(
        methodName: String,
        path: String,
        handler: java.util.function.Consumer<JsonicCall>,
        middlewares: List<MiddlewareType>
    ): JsonicApp = registerRouteNamed(methodName, path, handler, middlewares, emptyList())

    @JvmName("registerRouteNamedWithSkriptMiddleware")
    fun registerRouteNamed(
        methodName: String,
        path: String,
        handler: java.util.function.Consumer<JsonicCall>,
        builtInMiddlewares: List<MiddlewareType>,
        skriptMiddlewares: List<JsonicRouteMiddleware>
    ): JsonicApp {
        val method = HttpMethod.parse(methodName) ?: HttpMethod.Get
        routes += RouteDefinition(
            method,
            JsonPathUtils.normalize(path),
            { call -> handler.accept(call) },
            builtInMiddlewares,
            skriptMiddlewares
        )
        return this
    }

    @JvmName("registerError")
    fun registerError(statusCode: Int, handler: java.util.function.Consumer<JsonicCall>): JsonicApp {
        errorHandlers.removeIf { it.statusCode == statusCode }
        errorHandlers += ErrorHandlerDefinition(statusCode) { call -> handler.accept(call) }
        return this
    }

    fun registerStaticMount(urlPrefix: String, folderPath: String): JsonicApp {
        staticMounts += StaticMount(JsonPathUtils.normalize(urlPrefix), folderPath)
        return this
    }

    fun errorHandlerFor(statusCode: Int): (suspend (JsonicCall) -> Unit)? =
        errorHandlers.firstOrNull { it.statusCode == statusCode }?.handler
    fun post(path: String, handler: suspend (JsonicCall) -> Unit) = route(HttpMethod.Post, path, handler)
    fun put(path: String, handler: suspend (JsonicCall) -> Unit) = route(HttpMethod.Put, path, handler)
    fun patch(path: String, handler: suspend (JsonicCall) -> Unit) = route(HttpMethod.Patch, path, handler)
    fun delete(path: String, handler: suspend (JsonicCall) -> Unit) = route(HttpMethod.Delete, path, handler)

    fun route(method: HttpMethod, path: String, handler: suspend (JsonicCall) -> Unit): JsonicApp {
        routes += RouteDefinition(method, JsonPathUtils.normalize(path), handler)
        return this
    }

    fun mount(prefix: String, subApp: JsonicApp): JsonicApp {
        mountedApps += JsonPathUtils.normalize(prefix) to subApp
        return this
    }

    fun webSocket(
        path: String,
        onConnect: suspend (JsonicWebSocketSession) -> Unit = {},
        onMessage: suspend (JsonicWebSocketSession, String) -> Unit = { _, _ -> },
        onClose: suspend (JsonicWebSocketSession) -> Unit = {}
    ): JsonicApp {
        webSockets += WebSocketDefinition(path, onConnect, onMessage, onClose)
        return this
    }
}

object ServerEngine {
    @Volatile
    private var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null

    @Volatile
    private var activeApp: JsonicApp? = null

    private val wsSessions = java.util.concurrent.ConcurrentHashMap<String, CopyOnWriteArrayList<JsonicWebSocketSession>>()

    fun isRunning(): Boolean = server != null

    fun getActiveApp(): JsonicApp? = activeApp

    fun start(plugin: JavaPlugin, app: JsonicApp): CompletableFuture<Void> {
        stop()
        activeApp = app
        val bindHost = if (JsonicSettings.serverPublicBind) "0.0.0.0" else app.host

        if (JsonicSettings.serverPublicBind) {
            JsonicLogger.warning("Server binding to 0.0.0.0:${app.port} — expose only on trusted networks")
        }

        val globalMiddlewares = buildGlobalMiddlewares(app)
        server = embeddedServer(Netty, configure = {
            connector {
                host = bindHost
                port = app.port
            }
            configureHttpsConnector(this, plugin, app, bindHost)
        }) {
            configureApplication(plugin, app, globalMiddlewares)
        }.start(wait = false)
        logRegisteredRoutes(plugin, app, bindHost)
        return CompletableFuture.completedFuture(null)
    }

    private fun logRegisteredRoutes(plugin: JavaPlugin, app: JsonicApp, bindHost: String) {
        val scheme = if (app.httpsEnabled) "https" else "http"
        val routes = buildList {
            app.routes.forEach { add(it.method.value.padEnd(6) to it.path) }
            app.mountedApps.forEach { (prefix, sub) ->
                sub.routes.forEach { add(it.method.value.padEnd(6) to JsonPathUtils.join(prefix, it.path)) }
            }
        }.sortedWith(compareBy({ it.second }, { it.first }))

        JsonicLogger.accent(
            "HTTP listening on &f$scheme://$bindHost:${app.port}&r &8(${"%d".format(routes.size)} route(s))"
        )
        for ((method, path) in routes) {
            JsonicLogger.info("  ${AnsiColorConverter.colorizeHttpMethod(method.trim())} &7$path")
        }
        if (app.staticMounts.isNotEmpty()) {
            JsonicLogger.info("  &8Static mounts (${app.staticMounts.size}):")
            app.staticMounts.sortedBy { it.urlPrefix }.forEach { mount ->
                JsonicLogger.info(
                    "    ${AnsiColorConverter.colorizeHttpMethod("GET")} &7${mount.urlPrefix.trimEnd('/')}/{path...} &8← ${mount.folderPath}"
                )
            }
        }
        if (app.errorHandlers.isNotEmpty()) {
            JsonicLogger.info("  &8Error handlers (${app.errorHandlers.size}):")
            app.errorHandlers.sortedBy { it.statusCode }.forEach { handler ->
                JsonicLogger.info("    &cHTTP ${handler.statusCode}")
            }
        }
        if (app.webSockets.isNotEmpty()) {
            JsonicLogger.info("  &8WebSockets (${app.webSockets.size}):")
            app.webSockets.sortedBy { it.path }.forEach { ws ->
                JsonicLogger.info("    &dWS &7${ws.path}")
            }
        }
    }

    private fun buildGlobalMiddlewares(app: JsonicApp): Set<MiddlewareType> {
        val merged = app.middlewares.toMutableSet()
        if (JsonicSettings.serverMiddlewareCors) merged += MiddlewareType.CORS
        if (JsonicSettings.serverMiddlewareLogger) merged += MiddlewareType.LOGGER
        if (JsonicSettings.serverMiddlewareJson) merged += MiddlewareType.JSON
        return merged
    }

    private fun Application.configureApplication(
        plugin: JavaPlugin,
        app: JsonicApp,
        globalMiddlewares: Set<MiddlewareType>
    ) {
        if (MiddlewareType.CORS in globalMiddlewares) {
            install(CORS) { anyHost() }
        }
        if (MiddlewareType.LOGGER in globalMiddlewares) {
            install(CallLogging) {
                format { call ->
                    val status = call.response.status()?.value ?: 0
                    val statusColor = when {
                        status in 200..299 -> "\u001B[32m"
                        status in 300..399 -> "\u001B[33m"
                        else -> "\u001B[31m"
                    }
                    val method = AnsiColorConverter.colorizeHttpMethod(call.request.httpMethod.value)
                    val path = call.request.path()
                    val ms = call.processingTimeMillis()
                    AnsiColorConverter.convertToAnsi(
                        "&8[HTTP] $method &7$path $statusColor$status&r &8(${ms}ms)"
                    )
                }
            }
        }
        install(WebSockets) {
            pingPeriod = 15.seconds
            timeout = 30.seconds
        }

        routing {
            app.routes.forEach { registerRoute(plugin, app, it) }
            app.mountedApps.forEach { (prefix, sub) ->
                route(prefix) {
                    sub.routes.forEach { registerRoute(plugin, sub, it) }
                }
            }
            app.staticMounts.forEach { registerStaticMount(plugin, app, it) }
            registerNotFoundHandler(plugin, app)
            app.webSockets.forEach { ws ->
                webSocket(ws.path) {
                    val session = JsonicWebSocketSession(ws.path, this)
                    wsSessions.computeIfAbsent(ws.path) { CopyOnWriteArrayList() }.add(session)
                    runOnThread(plugin, JsonicSettings.serverThreadModel) { ws.onConnect(session) }
                    try {
                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                val text = frame.readText()
                                runOnThread(plugin, JsonicSettings.serverThreadModel) {
                                    ws.onMessage(session, text)
                                    fireWebSocketEvent(plugin, ws.path, text)
                                }
                            }
                        }
                    } finally {
                        wsSessions[ws.path]?.remove(session)
                        runOnThread(plugin, JsonicSettings.serverThreadModel) { ws.onClose(session) }
                    }
                }
            }
        }
    }

    private fun configureHttpsConnector(
        config: ApplicationEngine.Configuration,
        plugin: JavaPlugin,
        app: JsonicApp,
        bindHost: String
    ) {
        if (!app.httpsEnabled) return
        val certRel = app.httpsCertPath ?: return
        val keyRel = app.httpsKeyPath ?: return
        val certFile = File(plugin.dataFolder, certRel)
        val keyFile = File(plugin.dataFolder, keyRel)
        if (!certFile.isFile || !keyFile.isFile) {
            JsonicLogger.warning("HTTPS enabled but cert/key missing in data folder: $certRel, $keyRel")
            return
        }
        try {
            val password = app.httpsKeyPassword.ifEmpty { JsonicSettings.serverHttpsKeyPassword }.toCharArray()
            val keyStore = HttpsKeyStore.load(certFile, keyFile, password)
            if (keyStore == null) {
                JsonicLogger.warning("HTTPS: could not load key store from $certRel / $keyRel (use PKCS#8 PEM or .p12)")
                return
            }
            config.sslConnector(
                keyStore = keyStore,
                keyAlias = "0",
                keyStorePassword = { password },
                privateKeyPassword = { password }
            ) {
                host = bindHost
                port = app.httpsPort
            }
            JsonicLogger.accent("HTTPS listening on &fhttps://$bindHost:${app.httpsPort}")
        } catch (e: Exception) {
            JsonicLogger.warning("HTTPS setup failed: ${e.message}")
        }
    }

    fun stop() {
        server?.stop(1000, 2000)
        server = null
        activeApp = null
        wsSessions.clear()
    }

    suspend fun broadcastWebSocket(path: String, message: String) {
        wsSessions[path]?.forEach { it.send(message) }
    }

    private fun Routing.registerRoute(plugin: JavaPlugin, app: JsonicApp, route: RouteDefinition) {
        val routeHandler: suspend RoutingContext.() -> Unit = routeHandler@{
            if (!applyRouteMiddleware(route.builtInMiddlewares, call)) return@routeHandler
            val params = call.parameters.entries().associate { it.key to (it.value.firstOrNull() ?: "") }

            for (skriptMw in route.skriptMiddlewares) {
                val jsonicCall = JsonicCall(call, params)
                var continueChain = true
                runOnThreadSuspend(plugin, JsonicSettings.serverThreadModel) {
                    continueChain = skriptMw.apply(jsonicCall)
                    if (!continueChain) {
                        maybeRunErrorHandler(app, jsonicCall)
                        jsonicCall.finishPendingReply()
                    }
                }
                if (!continueChain) return@routeHandler
            }

            val jsonicCall = JsonicCall(call, params)
            runOnThreadSuspend(plugin, JsonicSettings.serverThreadModel) {
                route.handler(jsonicCall)
                maybeRunErrorHandler(app, jsonicCall)
                jsonicCall.finishPendingReply()
            }
        }
        when (route.method) {
            HttpMethod.Get -> get(route.path, routeHandler)
            HttpMethod.Post -> post(route.path, routeHandler)
            HttpMethod.Put -> put(route.path, routeHandler)
            HttpMethod.Patch -> patch(route.path, routeHandler)
            HttpMethod.Delete -> delete(route.path, routeHandler)
            else -> get(route.path, routeHandler)
        }
    }

    private suspend fun applyRouteMiddleware(middlewares: List<MiddlewareType>, call: ApplicationCall): Boolean {
        if (MiddlewareType.AUTH_BEARER in middlewares) {
            val auth = call.request.header(HttpHeaders.Authorization)
            if (auth == null || !auth.startsWith("Bearer ", ignoreCase = true)) {
                call.respond(HttpStatusCode.Unauthorized, """{"error":"missing_bearer_token"}""")
                return false
            }
        }
        if (MiddlewareType.JSON in middlewares && !call.response.isSent) {
            call.response.header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        }
        return true
    }

    private suspend fun maybeRunErrorHandler(app: JsonicApp, call: JsonicCall) {
        if (!call.needsErrorHandler()) return
        app.errorHandlerFor(call.statusCode())?.invoke(call)
    }

    private fun Routing.registerStaticMount(plugin: JavaPlugin, app: JsonicApp, mount: StaticMount) {
        val prefix = mount.urlPrefix.trimEnd('/').ifEmpty { "" }
        val routePath = if (prefix.isEmpty()) "/{path...}" else "$prefix/{path...}"
        get(routePath) {
            val subPath = call.parameters.getAll("path")?.joinToString("/") ?: ""
            val root = File(plugin.dataFolder, mount.folderPath).canonicalFile
            val file = if (subPath.isEmpty()) File(root, "index.html") else File(root, subPath).canonicalFile
            if (!file.path.startsWith(root.path) || !file.isFile) {
                val jsonicCall = JsonicCall(call, emptyMap())
                jsonicCall.queueStatus(404)
                runOnThreadSuspend(plugin, JsonicSettings.serverThreadModel) {
                    maybeRunErrorHandler(app, jsonicCall)
                    jsonicCall.finishPendingReply()
                }
                return@get
            }
            call.response.header(HttpHeaders.ContentType, mimeForExtension(file.extension))
            call.respond(HttpStatusCode.OK, file.readText(Charsets.UTF_8))
        }
    }

    private fun Routing.registerNotFoundHandler(plugin: JavaPlugin, app: JsonicApp) {
        val handler: suspend RoutingContext.() -> Unit = {
            val jsonicCall = JsonicCall(call, emptyMap())
            jsonicCall.queueStatus(404)
            runOnThreadSuspend(plugin, JsonicSettings.serverThreadModel) {
                maybeRunErrorHandler(app, jsonicCall)
                jsonicCall.finishPendingReply()
            }
        }
        get("{path...}", handler)
        post("{path...}", handler)
        put("{path...}", handler)
        patch("{path...}", handler)
        delete("{path...}", handler)
        head("{path...}", handler)
    }

    private suspend fun runOnThreadSuspend(
        plugin: JavaPlugin,
        model: JsonicSettings.ThreadModel,
        block: suspend () -> Unit
    ) {
        if (model == JsonicSettings.ThreadModel.ASYNC) {
            block()
            return
        }
        val future = CompletableFuture<Unit>()
        plugin.server.scheduler.runTask(plugin, Runnable {
            kotlinx.coroutines.runBlocking {
                try {
                    block()
                    future.complete(Unit)
                } catch (e: Exception) {
                    future.completeExceptionally(e)
                }
            }
        })
        future.join()
    }

    private fun runOnThread(
        plugin: JavaPlugin,
        model: JsonicSettings.ThreadModel,
        block: suspend () -> Unit
    ) {
        if (model == JsonicSettings.ThreadModel.ASYNC) {
            kotlinx.coroutines.runBlocking { block() }
            return
        }
        plugin.server.scheduler.runTask(plugin, Runnable {
            kotlinx.coroutines.runBlocking { block() }
        })
    }

    private fun fireWebSocketEvent(plugin: JavaPlugin, path: String, message: String) {
        try {
            val eventClass = Class.forName("cz.coffeerequired.jsonic.skript.server.JsonicWebSocketMessageEvent")
            val event = eventClass.getConstructor(String::class.java, String::class.java)
                .newInstance(path, message) as org.bukkit.event.Event
            plugin.server.pluginManager.callEvent(event)
        } catch (_: Exception) {
            // skript module optional at runtime
        }
    }
}
