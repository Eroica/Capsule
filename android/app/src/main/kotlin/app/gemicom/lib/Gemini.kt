package app.gemicom.lib

data class Token(val type: String, val value: String)

object Gemini {
    external fun parse(input: String): Array<Token>
    external fun lasterror(): Boolean
}
