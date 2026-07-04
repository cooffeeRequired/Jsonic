package cz.coffeerequired.jsonic.server

object JsonPathUtils {

    /** Joins a route group prefix with a route path (`/api` + `/users/{id}` → `/api/users/{id}`). */
    @JvmStatic
    fun join(prefix: String, path: String): String {
        val p = prefix.trim().removeSuffix("/")
        var segment = path.trim()
        if (segment.isEmpty()) segment = "/"
        if (!segment.startsWith("/")) segment = "/$segment"
        if (p.isEmpty()) return normalize(segment)
        if (segment == "/") return normalize(p)
        return normalize("$p$segment")
    }

    /** Normalizes duplicate slashes and ensures a leading slash. */
    @JvmStatic
    fun normalize(path: String): String {
        val collapsed = path.trim().replace(Regex("/+"), "/")
        return when {
            collapsed.isEmpty() || collapsed == "/" -> "/"
            collapsed.startsWith("/") -> collapsed.removeSuffix("/").ifEmpty { "/" }
            else -> "/$collapsed".replace(Regex("/+"), "/")
        }
    }
}

fun parseMiddleware(name: String): MiddlewareType? = when (name.trim().lowercase().replace('-', '_')) {
    "cors" -> MiddlewareType.CORS
    "logger", "log", "logging" -> MiddlewareType.LOGGER
    "json" -> MiddlewareType.JSON
    "auth", "auth_bearer", "bearer" -> MiddlewareType.AUTH_BEARER
    else -> null
}

object MiddlewareTypes {
    @JvmStatic
    fun fromName(name: String): MiddlewareType? = parseMiddleware(name)
}
