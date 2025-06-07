package app.gemicom.models

import java.net.URI
import java.net.URLEncoder

private const val GEMINI_SCHEME = "gemini://"

class InvalidHostError(host: String) : Exception("Invalid host: $host")

/* Represents a Gemini URL which can be mutated, "navigating" to locations. */
class GeminiHost private constructor(var uri: URI) {
    companion object {
        fun appendArgs(path: String, query: String): String {
            val parts = path.split("?", limit = 2)
            val queryParams = URLEncoder.encode(query, "UTF-8")
            return "${parts[0]}?$queryParams"
        }

        fun fromAddress(address: String): GeminiHost {
            try {
                val uri = URI.create(address)

                /* Check whether another scheme was accidentally used */
                if (!address.startsWith(GEMINI_SCHEME)) {
                    return fromAddress("$GEMINI_SCHEME$address")
                } else if (uri.scheme != "gemini") {
                    throw InvalidHostError(address)
                } else if (uri.host == null) {
                    throw InvalidHostError(address)
                }

                return GeminiHost(uri)
            } catch (_: Exception) {
                throw InvalidHostError(address)
            }
        }
    }

    val location: String
        get() = uri.toString()

    fun resolve(reference: String): String {
        /* If reference is actually another host, replace */
        return if (isWithHost(reference)) {
            fromAddress(reference).location
        } else if (isMalformed(reference)) {
            resolve(reference.substring(2))
        } else {
            uri.resolve(reference).toString()
        }
    }

    /* Resolves "reference" and updates current state */
    fun navigate(reference: String): String {
        val newLocation = resolve(reference)
        uri = URI.create(newLocation)

        return location
    }

    private fun isWithHost(reference: String): Boolean {
        return !reference.startsWith(".") && !reference.startsWith("/") && reference.contains(".")
    }

    private fun isMalformed(reference: String): Boolean {
        return reference.startsWith("//")
    }
}
