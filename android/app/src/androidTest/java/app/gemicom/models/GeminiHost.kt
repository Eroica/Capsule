package app.gemicom.models

import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
internal class GeminiHostTest {
    @Test
    fun normalizeGeminiHostUri() {
        val host = GeminiHost.fromAddress("gemini://geminiprotocol.net")
        assertEquals("gemini://geminiprotocol.net/", host.location)
    }

    @Test
    fun resolveRelativePathGeminiHostUri() {
        val host = GeminiHost.fromAddress("gemini://geminiprotocol.net")
        host.navigate("software")
        assertEquals("gemini://geminiprotocol.net/software", host.location)
        host.navigate("relative")
        assertEquals("gemini://geminiprotocol.net/relative", host.location)
    }
}
