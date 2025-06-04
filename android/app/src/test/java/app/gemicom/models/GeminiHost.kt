package app.gemicom.models

import junit.framework.TestCase.assertEquals
import org.junit.Test

class GeminiHostTest {
    @Test
    fun `Test valid GeminiHost`() {
        GeminiHost.fromAddress("localhost")
        GeminiHost.fromAddress("127.0.0.1")
        GeminiHost.fromAddress("example.com")
        GeminiHost.fromAddress("gemini://127.0.0.1")
        GeminiHost.fromAddress("gemini://127.0.0.1/page")
        GeminiHost.fromAddress("gemini://example.com/page?query=param")
    }

    @Test(expected = InvalidHostError::class)
    fun `Test invalid GeminiHost throws 1`() {
        GeminiHost.fromAddress("//")
    }

    @Test
    fun `Test mutating GeminiHost`() {
        val host = GeminiHost.fromAddress("example.com")
        assertEquals("gemini://example.com", host.location)

        assertEquals("gemini://example.com/page", host.resolve("page"))
        assertEquals("gemini://example.com/page", host.resolve("//page"))

        assertEquals("gemini://example.com/page", host.navigate("//page"))
        assertEquals("gemini://example.com/page", host.location)
        assertEquals("gemini://example.com/", host.navigate(host.resolve("/")))
        assertEquals("gemini://example.com/page", host.navigate("/page"))

        assertEquals("gemini://example.com/subpage", host.resolve("subpage"))
        assertEquals("gemini://example.com/resource/articles/page", host.resolve("/resource/articles/page"))
        assertEquals("gemini://example.com/../../page", host.resolve("../../page"))

        /* Currently at gemini://example.com/page */
        assertEquals("gemini://example.com/../../page", host.navigate("example.com/../../../page"))

        assertEquals("gemini://example2.com/../../page", host.navigate("gemini://example2.com/../../page"))
        assertEquals("gemini://example2.com/page", host.navigate("/page"))

        /* "localhost" without scheme is just a path */
        assertEquals("gemini://example2.com/localhost/image/image.png", host.navigate("localhost/image/image.png"))
    }

    @Test
    fun `Test GeminiHost resolving references`() {
        val host = GeminiHost.fromAddress("example.com")
        assertEquals("gemini://example.com/image", host.resolve("image"))
        assertEquals("gemini://example.com/example2.com/image", host.resolve("example2.com/image"))
        assertEquals("http://example2.com/image", host.resolve("http://example2.com/image"))
        assertEquals("gemini://example.com/kirby.png", host.resolve("/kirby.png"))
    }
}
