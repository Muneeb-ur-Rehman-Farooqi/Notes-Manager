package com.muneeb.coursemanager.ui.util

import android.content.Context
import android.util.TypedValue
import io.noties.markwon.Markwon
import io.noties.markwon.SoftBreakAddsNewLinePlugin
import io.noties.markwon.ext.latex.JLatexMathPlugin
import io.noties.markwon.inlineparser.MarkwonInlineParserPlugin

object NoteMarkdown {
    const val BODY_TEXT_SP = 18f
    private const val NBSP = "\u00A0"
    private const val ZWSP = "\u200B"
    private const val MATH_DELIMITER = "\$\$"
    private val INLINE_MATH_REGEX = Regex("(?<![\\\\\$])\\\$(?!\\\$)(?=\\S)([^\\\$\n]*?\\S)\\\$(?![\$0-9])")

    // Single place that configures how every note is rendered
    fun createMarkwon(context: Context): Markwon {
        val textSizePx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            BODY_TEXT_SP,
            context.resources.displayMetrics
        )
        return Markwon.builder(context)
            .usePlugin(SoftBreakAddsNewLinePlugin.create())
            .usePlugin(MarkwonInlineParserPlugin.create())
            .usePlugin(
                JLatexMathPlugin.create(textSizePx) { builder ->
                    builder.inlinesEnabled(true)
                }
            )
            .build()
    }

    // Converts inline single-dollar math first, then preserves extra blank lines
    fun prepare(source: String): String = preserveBlankLines(convertInlineMath(source))

    // Converts pandoc-style $...$ to $$...$$ outside code blocks, math blocks, and inline code
    private fun convertInlineMath(source: String): String {
        val lines = source.replace("\r\n", "\n").replace('\r', '\n').split("\n")
        val out = ArrayList<String>(lines.size)
        var inFence = false
        var inMath = false

        for (line in lines) {
            val trimmed = line.trim()
            if (inMath) {
                out.add(line)
                if (trimmed.endsWith(MATH_DELIMITER)) inMath = false
            } else if (inFence) {
                out.add(line)
                if (trimmed.startsWith("```") || trimmed.startsWith("~~~")) inFence = false
            } else if (trimmed.startsWith("```") || trimmed.startsWith("~~~")) {
                out.add(line)
                inFence = true
            } else if (trimmed.startsWith(MATH_DELIMITER) && isMathBlockOpening(trimmed)) {
                out.add(line)
                inMath = true
            } else {
                var converted = convertLineOutsideBackticks(line)
                val originalTrimmedStart = line.trimStart()
                val convertedTrimmedStart = converted.trimStart()
                if (!originalTrimmedStart.startsWith(MATH_DELIMITER) &&
                    convertedTrimmedStart.startsWith(MATH_DELIMITER)
                ) {
                    val leadingSpaceCount = line.length - originalTrimmedStart.length
                    converted = line.substring(0, leadingSpaceCount) + ZWSP + convertedTrimmedStart
                }
                out.add(converted)
            }
        }
        return out.joinToString("\n")
    }

    // Splits a line around inline backtick spans so code spans are never altered
    private fun convertLineOutsideBackticks(line: String): String {
        val result = StringBuilder(line.length)
        var i = 0
        var plainStart = 0

        while (i < line.length) {
            if (line[i] == '`') {
                var tickCount = 0
                while (i + tickCount < line.length && line[i + tickCount] == '`') {
                    tickCount++
                }
                val closeIndex = findClosingBackticks(line, i + tickCount, tickCount)
                if (closeIndex != -1) {
                    if (plainStart < i) {
                        result.append(replaceSingleDollarMath(line.substring(plainStart, i)))
                    }
                    val codeEnd = closeIndex + tickCount
                    result.append(line.substring(i, codeEnd))
                    i = codeEnd
                    plainStart = i
                } else {
                    i += tickCount
                }
            } else {
                i++
            }
        }

        if (plainStart < line.length) {
            result.append(replaceSingleDollarMath(line.substring(plainStart)))
        }
        return result.toString()
    }

    // Finds a matching closing run of exact backtick length on the same line
    private fun findClosingBackticks(line: String, startIndex: Int, tickCount: Int): Int {
        var i = startIndex
        while (i < line.length) {
            if (line[i] == '`') {
                var run = 0
                while (i + run < line.length && line[i + run] == '`') {
                    run++
                }
                if (run == tickCount) return i
                i += run
            } else {
                i++
            }
        }
        return -1
    }

    // Replaces pandoc-style $...$ with $$...$$ using a lambda to avoid replacement-string escaping
    private fun replaceSingleDollarMath(segment: String): String =
        INLINE_MATH_REGEX.replace(segment) { match ->
            MATH_DELIMITER + match.groupValues[1] + MATH_DELIMITER
        }

    // Keeps extra blank lines visible, leaving code fences and math blocks untouched
    fun preserveBlankLines(source: String): String {
        val lines = source.replace("\r\n", "\n").replace('\r', '\n').split("\n")
        val out = StringBuilder()
        var inFence = false
        var inMath = false
        var blankRun = 0
        var wroteContent = false

        for (line in lines) {
            val insideBlock = inFence || inMath
            if (!insideBlock && line.isBlank()) {
                blankRun++
                continue
            }

            if (blankRun > 0) {
                appendBlankRun(out, blankRun, wroteContent)
                blankRun = 0
            }
            out.append(line).append('\n')
            wroteContent = true

            val trimmed = line.trim()
            if (inMath) {
                if (trimmed.endsWith(MATH_DELIMITER)) inMath = false
            } else if (inFence) {
                if (trimmed.startsWith("```") || trimmed.startsWith("~~~")) inFence = false
            } else if (trimmed.startsWith("```") || trimmed.startsWith("~~~")) {
                inFence = true
            } else if (trimmed.startsWith(MATH_DELIMITER) && isMathBlockOpening(trimmed)) {
                inMath = true
            }
        }
        return out.toString().trimEnd('\n')
    }

    // An odd number of delimiters on the line means the math block stays open
    private fun isMathBlockOpening(trimmedLine: String): Boolean =
        trimmedLine.split(MATH_DELIMITER).size % 2 == 0

    // One blank line stays a normal paragraph break, every extra one becomes a visible empty line
    private fun appendBlankRun(out: StringBuilder, blankRun: Int, wroteContent: Boolean) {
        if (!wroteContent) {
            repeat(blankRun) { out.append(NBSP).append('\n') }
        } else {
            out.append('\n')
            repeat(blankRun - 1) { out.append(NBSP).append('\n') }
        }
    }
}