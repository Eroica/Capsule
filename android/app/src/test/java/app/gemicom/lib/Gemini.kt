package app.gemicom.lib

import junit.framework.TestCase.assertEquals
import org.junit.Assert.assertArrayEquals
import org.junit.BeforeClass
import org.junit.Test

internal class GeminiTest {
    companion object {
        @BeforeClass
        @JvmStatic
        fun setUp() {
            System.loadLibrary("gemicom")
        }
    }

    @Test
    fun `Test parse`() {
        val input = """# Hello, Gemicom

text text

```preformat start

=> ignore link
> ignore
```

After text
"""

        val tokens = Gemini.parse(input)
        val expected = arrayOf(
            Token(type = "H1", value = "Hello, Gemicom"),
            Token(type = "NEWLINE", value = ""),
            Token(type = "TEXT", value = "text text"),
            Token(type = "NEWLINE", value = ""),
            Token(type = "PREFORMAT", value = "preformat start\n"),
            Token(type = "PREFORMAT", value = "\n"),
            Token(type = "PREFORMAT", value = "=> ignore link\n"),
            Token(type = "PREFORMAT", value = "> ignore\n"),
            Token(type = "PREFORMAT", value = "\n"),
            Token(type = "NEWLINE", value = ""),
            Token(type = "TEXT", value = "After text")
        )

        assertArrayEquals(expected, tokens)
    }

    @Test
    fun `Test parse error`() {
        val invalid = Gemini.parse("")
        assertEquals(true, Gemini.lasterror())
    }
}
