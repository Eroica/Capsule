package app.gemicom.models

import junit.framework.TestCase.assertEquals
import org.junit.BeforeClass
import org.junit.Test

class GeminiDocumentTest {
    companion object {
        @BeforeClass
        @JvmStatic
        fun setUp() {
            System.loadLibrary("gemicom")
        }
    }

    val gemtextPre = """Text 1

Text 2
```
=> https://example.com    A cool website
=> gopher://example.com   An even cooler gopherhole
=> gemini://example.com   A supremely cool Gemini capsule
=> sftp://example.com
```"""

    val gemtextChunks = """Text 1

Text 2

=> https://example.com    A cool website
=> gopher://example.com   An even cooler gopherhole
=> gemini://example.com   A supremely cool Gemini capsule
=> sftp://example.com

Text 3

* Li 1
* Li 2

```
=> https://example.com    A cool website
=> gopher://example.com   An even cooler gopherhole
=> gemini://example.com   A supremely cool Gemini capsule
=> sftp://example.com
```
"""

    @Test
    fun `Parse document`() {
        val document = GeminiDocument(Document(content = gemtextPre))
        assertEquals(
            listOf(
                Text("Text 1"),
                Newline,
                Text("Text 2"),
                Preformat("\n"),
                Preformat("=> https://example.com    A cool website\n"),
                Preformat("=> gopher://example.com   An even cooler gopherhole\n"),
                Preformat("=> gemini://example.com   A supremely cool Gemini capsule\n"),
                Preformat("=> sftp://example.com\n")
            ),
            document.tokens
        )
    }

    @Test
    fun `Merge tokens`() {
        val chunkedDoc = ChunkedGeminiDocument.fromText(text = gemtextChunks)
        assertEquals(
            listOf(
                Text("Text 1"),
                Text("Text 2"),
                AnchorBlock(
                    listOf(
                        Anchor("https://example.com", "A cool website"),
                        Anchor("gopher://example.com", "An even cooler gopherhole"),
                        Anchor("gemini://example.com", "A supremely cool Gemini capsule"),
                        Anchor("sftp://example.com", "sftp://example.com")
                    )
                ),
                Text("Text 3"),
                ListItemBlock(
                    listOf(
                        ListItem("Li 1"),
                        ListItem("Li 2")
                    )
                ),
                PreformatBlock(
                    listOf(
                        Preformat("\n"),
                        Preformat("=> https://example.com    A cool website\n"),
                        Preformat("=> gopher://example.com   An even cooler gopherhole\n"),
                        Preformat("=> gemini://example.com   A supremely cool Gemini capsule\n"),
                        Preformat("=> sftp://example.com\n"),
                        Preformat("\n")
                    )
                )
            ),
            chunkedDoc.blocks
        )
    }
}
