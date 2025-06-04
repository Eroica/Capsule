package app.gemicom.models

import org.apache.commons.io.FilenameUtils

enum class GemtextToken(val symbol: String) {
    LINK("LINK"),
    H1("H1"),
    H2("H2"),
    H3("H3"),
    LIST("LIST"),
    QUOTE("QUOTE"),
    PREFORMAT("PREFORMAT"),
    TEXT("TEXT"),
    NEWLINE("NEWLINE");
}

sealed interface IGemtext {
    val content: String
}

@JvmInline
value class Text(override val content: String) : IGemtext

@JvmInline
value class H1(override val content: String) : IGemtext

@JvmInline
value class H2(override val content: String) : IGemtext

@JvmInline
value class H3(override val content: String) : IGemtext

@JvmInline
value class ListItem(override val content: String) : IGemtext {
    fun htmlTag() = """<li>$content</li>"""
}

data class Anchor(val url: String, override val content: String) : IGemtext

@JvmInline
value class Preformat(override val content: String) : IGemtext

@JvmInline
value class Blockquote(override val content: String) : IGemtext

object Newline : IGemtext {
    override val content = ""
}

/* Specialized Anchor tag */
data class Image(val url: String, override val content: String) : IGemtext {
    var isExpanded: Boolean = false

    companion object {
        val FORMATS = setOf(
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg"
        )
    }
}

/* Container for blocks found in preformatted mode */
data class PreformatBlock(val pres: List<Preformat>) : IGemtext {
    override val content: String
        get() = pres.joinToString("") { it.content }
}

/* Container for list items */
data class ListItemBlock(val lis: List<ListItem>) : IGemtext {
    override val content: String
        get() = """<ul>
    ${lis.joinToString("\n") { it.htmlTag() }}
</ul>
"""
}

/* Container for links */
data class AnchorBlock(val anchors: List<Anchor>) : IGemtext {
    override val content: String
        get() = """<ul>
    ${anchors.joinToString("\n") { "<li>$it</li>" }}
</ul>
"""
}

/* Custom blocks for showing status/error pages */
data object EmptyPageBlock : IGemtext {
    override val content = ""
}

data object InvalidDocumentBlock : IGemtext {
    override val content = ""
}

data object SecurityIssueBlock : IGemtext {
    override val content = ""
}

fun parseGemtext(type: String, value: String): IGemtext {
    return when (type) {
        GemtextToken.LINK.symbol -> {
            val groups = Regex("""(\S+)\s+(.*)""").matchEntire(value)
            if (groups != null) {
                val (reference, label) = groups.destructured

                return if (FilenameUtils.getExtension(reference) in Image.FORMATS) {
                    Image(reference, label)
                } else {
                    Anchor(reference, label)
                }
            } else {
                /* Link is just like:
                   =>   sftp://example.com
                   Take entire URL as label */
                return if (FilenameUtils.getExtension(value) in Image.FORMATS) {
                    Image(value, value)
                } else {
                    Anchor(value, value)
                }
            }
        }

        GemtextToken.H1.symbol -> H1(value)
        GemtextToken.H2.symbol -> H2(value)
        GemtextToken.H3.symbol -> H3(value)
        GemtextToken.LIST.symbol -> ListItem(value)
        GemtextToken.QUOTE.symbol -> Blockquote(value)
        GemtextToken.PREFORMAT.symbol -> Preformat(value)
        GemtextToken.NEWLINE.symbol -> Newline
        else -> Text(value)
    }
}
