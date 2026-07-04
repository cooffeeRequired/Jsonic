package cz.coffeerequired.jsonic.core.http

enum class HttpMethod {
    GET, POST, PUT, PATCH, DELETE, HEAD, MOCK;

    override fun toString(): String = name
}

enum class RequestStatus { UNKNOWN, OK, FAILED }

data class HttpPair(val key: String, val value: String)

data class HttpResponseData(
    val statusCode: Int,
    val body: String,
    val headersJson: com.google.gson.JsonElement,
    val status: RequestStatus
) {
    companion object {
        fun empty() = HttpResponseData(0, "", com.google.gson.JsonNull.INSTANCE, RequestStatus.UNKNOWN)
    }
}

class HttpRequest internal constructor(
    val uri: String,
    val method: HttpMethod
) {
    var content: com.google.gson.JsonElement? = null
    var headers: Array<HttpPair> = emptyArray()
    val queryParams: LinkedHashMap<String, String> = linkedMapOf()
    var attachments: MutableList<HttpAttachment> = mutableListOf()
    var status: RequestStatus = RequestStatus.UNKNOWN
    var response: HttpResponseData = HttpResponseData.empty()
    var event: Any? = null

    override fun toString(): String = "jsonic request $method $uri ($status)"
}

data class HttpAttachment(val name: String, val filePath: String)
