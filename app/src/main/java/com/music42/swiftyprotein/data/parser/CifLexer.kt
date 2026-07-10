package com.music42.swiftyprotein.data.parser

internal fun findCifLoopSection(
    lines: List<String>,
    prefix: String
): Pair<List<String>, List<String>>? {
    var i = 0
    while (i < lines.size) {
        if (lines[i].trim() == "loop_") {
            val headers = mutableListOf<String>()
            var j = i + 1
            while (j < lines.size && lines[j].trim().startsWith("_")) {
                headers.add(lines[j].trim())
                j++
            }
            if (headers.any { it.startsWith(prefix) }) {
                val dataLines = mutableListOf<String>()
                while (j < lines.size) {
                    val trimmed = lines[j].trim()
                    if (trimmed.isEmpty() || trimmed == "#" || trimmed == "loop_" ||
                        trimmed.startsWith("_") || trimmed.startsWith("data_")
                    ) break
                    dataLines.add(trimmed)
                    j++
                }
                return headers to dataLines
            }
        }
        i++
    }
    return null
}

internal fun tokenizeCifLine(line: String): List<String> {
    val tokens = mutableListOf<String>()
    var i = 0
    while (i < line.length) {
        when {
            line[i].isWhitespace() -> i++
            line[i] == '\'' || line[i] == '"' -> {
                val quote = line[i]
                val start = i + 1
                i = start
                while (i < line.length && !(line[i] == quote && (i + 1 >= line.length || line[i + 1].isWhitespace()))) {
                    i++
                }
                tokens.add(line.substring(start, i))
                if (i < line.length) i++
            }
            else -> {
                val start = i
                while (i < line.length && !line[i].isWhitespace()) i++
                tokens.add(line.substring(start, i))
            }
        }
    }
    return tokens
}
