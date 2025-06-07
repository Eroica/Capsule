package app.gemicom.platform

import app.gemicom.InvalidGeminiUri
import org.junit.Test

internal class GeminiUriTest {
    @Test
    fun `Valid GeminiUri`() {
        GeminiUri.fromAddress("gemini://example.com")
        GeminiUri.fromAddress("gemini://example.com?arg=parse")
        GeminiUri.fromAddress("gemini://localhost/page")
        GeminiUri.fromAddress("folder/image.png")
        GeminiUri.fromAddress("/folder/image.png")
        GeminiUri.fromAddress("10.0.0.1/page")
        GeminiUri.fromAddress("gemini://gemicom.app/secret/kirby.png")
    }

    @Test(expected = InvalidGeminiUri::class)
    fun `Invalid GeminiUri throws 1`() {
        GeminiUri.fromAddress("gemini:///page")
    }

    @Test(expected = InvalidGeminiUri::class)
    fun `Invalid GeminiUri throws 2`() {
        GeminiUri.fromAddress("gemini://")
    }

    @Test(expected = InvalidGeminiUri::class)
    fun `Invalid GeminiUri throws 3`() {
        GeminiUri.fromAddress("http://example.com")
    }

    @Test(expected = InvalidGeminiUri::class)
    fun `Invalid GeminiUri throws 4`() {
        GeminiUri.fromAddress("//example.com")
    }

    @Test(expected = InvalidGeminiUri::class)
    fun `Invalid GeminiUri throws 5`() {
        GeminiUri.fromAddress("         ")
    }
}
