package cz.coffeerequired.jsonic.core

import java.util.regex.Pattern

object JsonComments {
    private val TRAILING_COMMA = Pattern.compile(",\\s*([}\\]])")

    fun prepare(input: String?): String {
        if (input.isNullOrEmpty()) return input.orEmpty()
        return TRAILING_COMMA.matcher(stripComments(input)).replaceAll("$1")
    }

    private fun stripComments(input: String): String {
        val out = StringBuilder(input.length)
        var inString = false
        var escape = false
        var i = 0
        while (i < input.length) {
            val c = input[i]
            if (inString) {
                out.append(c)
                when {
                    escape -> escape = false
                    c == '\\' -> escape = true
                    c == '"' -> inString = false
                }
                i++
                continue
            }
            if (c == '"') {
                inString = true
                out.append(c)
                i++
                continue
            }
            if (c == '/' && i + 1 < input.length) {
                when (input[i + 1]) {
                    '/' -> {
                        i += 2
                        while (i < input.length && input[i] != '\n') i++
                        continue
                    }
                    '*' -> {
                        i += 2
                        while (i + 1 < input.length) {
                            if (input[i] == '*' && input[i + 1] == '/') {
                                i += 2
                                break
                            }
                            i++
                        }
                        continue
                    }
                }
            }
            out.append(c)
            i++
        }
        return out.toString()
    }
}
