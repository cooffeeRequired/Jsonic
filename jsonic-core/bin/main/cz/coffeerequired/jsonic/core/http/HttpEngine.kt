package cz.coffeerequired.jsonic.core.http

import com.google.gson.Gson
import com.google.gson.JsonObject
import cz.coffeerequired.jsonic.core.JsonEngine
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.function.BiConsumer

data class RawHttpResult(
    val statusCode: Int,
    val body: String,
    val headers: JsonObject,
    val successful: Boolean
) {
    companion object {
        fun synthetic(code: Int, body: String) = RawHttpResult(code, body, JsonObject(), code in 200..299)
    }
}

object HttpEngine {
    private val gson = Gson()

    fun createRequest(uri: String, method: HttpMethod = HttpMethod.GET): HttpRequest =
        HttpRequest(uri, method)

    fun sendBlocking(request: HttpRequest): RawHttpResult? {
        if (request.method == HttpMethod.MOCK) {
            return RawHttpResult.synthetic(200, """{"mock":true,"url":"${request.uri}"}""")
        }
        return try {
            val built = buildHttpRequest(request) ?: return null
            val response = HttpClientProvider.getClient().send(built, HttpResponse.BodyHandlers.ofString())
            RawHttpResult.of(response)
        } catch (_: Exception) {
            null
        }
    }

    fun sendAsync(request: HttpRequest, callback: BiConsumer<RawHttpResult?, Throwable?>) {
        if (request.method == HttpMethod.MOCK) {
            callback.accept(RawHttpResult.synthetic(200, """{"mock":true}"""), null)
            return
        }
        try {
            val built = buildHttpRequest(request) ?: run {
                callback.accept(null, IllegalStateException("Could not build HTTP request"))
                return
            }
            HttpClientProvider.getClient()
                .sendAsync(built, HttpResponse.BodyHandlers.ofString())
                .whenComplete { response, error ->
                    if (error != null) {
                        callback.accept(null, error)
                    } else {
                        callback.accept(RawHttpResult.of(response), null)
                    }
                }
        } catch (e: Exception) {
            callback.accept(null, e)
        }
    }

    fun applyResponse(request: HttpRequest, result: RawHttpResult?, error: Throwable?) {
        if (error != null || result == null) {
            request.status = RequestStatus.FAILED
            request.response = HttpResponseData.empty()
            return
        }
        request.status = if (result.successful) RequestStatus.OK else RequestStatus.FAILED
        request.response = HttpResponseData(
            result.statusCode,
            result.body,
            result.headers,
            request.status
        )
    }

    private fun buildHttpRequest(req: HttpRequest): java.net.http.HttpRequest? {
        val uri = buildUri(req)
        val builder = java.net.http.HttpRequest.newBuilder(URI.create(uri))
            .timeout(Duration.ofSeconds(HttpClientProvider.requestTimeoutSeconds().toLong()))

        req.headers.filter { it.key.isNotEmpty() && !it.key.equals("Content-Type", true) }
            .forEach { builder.header(it.key, it.value) }

        val body = when (req.method) {
            HttpMethod.GET, HttpMethod.HEAD, HttpMethod.DELETE -> java.net.http.HttpRequest.BodyPublishers.noBody()
            else -> {
                val json = req.content
                if (json != null && !json.isJsonNull) {
                    builder.header("Content-Type", "application/json")
                    java.net.http.HttpRequest.BodyPublishers.ofString(gson.toJson(json))
                } else {
                    java.net.http.HttpRequest.BodyPublishers.noBody()
                }
            }
        }

        builder.method(req.method.name, body)
        return builder.build()
    }

    private fun buildUri(request: HttpRequest): String {
        var uri = request.uri
        if (!uri.matches(Regex("^[a-zA-Z]+://.*"))) uri = "http://$uri"
        if (request.queryParams.isEmpty()) return uri
        val sb = StringBuilder(uri)
        sb.append(if ('?' in uri) '&' else '?')
        request.queryParams.entries.joinTo(sb, "&") { (k, v) ->
            URLEncoder.encode(k, StandardCharsets.UTF_8) + "=" + URLEncoder.encode(v, StandardCharsets.UTF_8)
        }
        return sb.toString()
    }

    private fun RawHttpResult.Companion.of(response: HttpResponse<String>): RawHttpResult {
        val headers = JsonObject()
        response.headers().map().forEach { (k, values) ->
            headers.addProperty(k, values.joinToString(","))
        }
        return RawHttpResult(response.statusCode(), response.body(), headers, response.statusCode() in 200..299)
    }
}

class HttpRequestBuilder(private val method: HttpMethod, private var uri: String) {
    private val headers = linkedMapOf<String, String>()
    private val queryParams = linkedMapOf<String, String>()
    private var jsonBody: com.google.gson.JsonElement? = null

    fun uri(value: String) = apply { uri = value }
    fun header(name: String, value: String) = apply { headers[name] = value }
    fun queryParam(key: String, value: String) = apply { queryParams[key] = value }
    fun jsonBody(element: com.google.gson.JsonElement?) = apply { jsonBody = element }

    fun build(): HttpRequest {
        val req = HttpRequest(uri, method)
        req.content = jsonBody
        req.headers = headers.map { HttpPair(it.key, it.value) }.toTypedArray()
        req.queryParams.putAll(queryParams)
        return req
    }
}
