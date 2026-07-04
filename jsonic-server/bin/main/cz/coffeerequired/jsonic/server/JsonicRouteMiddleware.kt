package cz.coffeerequired.jsonic.server

/** Skript-defined route middleware; return false to stop the chain (route handler is skipped). */
fun interface JsonicRouteMiddleware {
    fun apply(call: JsonicCall): Boolean
}
