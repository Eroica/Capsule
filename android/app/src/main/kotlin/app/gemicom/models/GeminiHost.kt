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
                /**
                 * @since 2025-06-07
                 * URI on Android doesn't resolve relative paths correctly if the host does not end
                 * on /. Always append a trailing slash.
                 */
                val normalizedAddress = if (address.endsWith("/")) address else "$address/"

                val uri = URI.create(normalizedAddress)

                /* Check whether another scheme was accidentally used */
                if (!normalizedAddress.startsWith(GEMINI_SCHEME)) {
                    return fromAddress("$GEMINI_SCHEME$normalizedAddress")
                } else if (uri.scheme != "gemini") {
                    throw InvalidHostError(normalizedAddress)
                } else if (uri.host == null) {
                    throw InvalidHostError(normalizedAddress)
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
        return if (isFullUri(reference)) {
            uri.resolve(reference).toString()
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

    /**
     * @since 2025-06-04
     * The first part in "localhost/img/img.png" and "img/img.png" could both be a host.
     * The only thing one can tell apart is if there is a scheme.
     */
    private fun isFullUri(reference: String): Boolean {
        return URI(reference).scheme != null
    }

    private fun isMalformed(reference: String): Boolean {
        return reference.startsWith("//")
    }
}
