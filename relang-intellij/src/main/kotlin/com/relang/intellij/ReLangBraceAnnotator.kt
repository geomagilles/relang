package com.relang.intellij

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement

class ReLangBraceAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val file = element.containingFile
        if (!file.name.endsWith(".re")) {
            return
        }

        // Run once per file to avoid duplicate annotations while IntelliJ walks the PSI tree.
        if (element.textRange.startOffset != 0) {
            return
        }

        val text = file.text
        val codeMask = buildCodeMask(text)
        annotateDelimiters(text, codeMask, holder)
        annotateIfStructure(text, codeMask, holder)
    }

    private fun annotateDelimiters(text: String, codeMask: BooleanArray, holder: AnnotationHolder) {
        val openBraces = ArrayDeque<Int>()
        val openParens = ArrayDeque<Int>()

        for (index in text.indices) {
            if (!codeMask[index]) {
                continue
            }

            when (text[index]) {
                '{' -> openBraces.addLast(index)
                '}' -> {
                    if (openBraces.isEmpty()) {
                        holder.newAnnotation(HighlightSeverity.ERROR, "Unmatched closing brace '}'")
                            .range(TextRange(index, index + 1))
                            .create()
                    } else {
                        openBraces.removeLast()
                    }
                }
                '(' -> openParens.addLast(index)
                ')' -> {
                    if (openParens.isEmpty()) {
                        holder.newAnnotation(HighlightSeverity.ERROR, "Unmatched closing parenthesis ')'")
                            .range(TextRange(index, index + 1))
                            .create()
                    } else {
                        openParens.removeLast()
                    }
                }
            }
        }

        for (offset in openBraces) {
            holder.newAnnotation(HighlightSeverity.ERROR, "Missing closing brace '}'")
                .range(TextRange(offset, offset + 1))
                .create()
        }

        for (offset in openParens) {
            holder.newAnnotation(HighlightSeverity.ERROR, "Missing closing parenthesis ')'")
                .range(TextRange(offset, offset + 1))
                .create()
        }
    }

    private fun annotateIfStructure(text: String, codeMask: BooleanArray, holder: AnnotationHolder) {
        for (index in text.indices) {
            if (!isKeywordAt(text, index, "if", codeMask)) {
                continue
            }

            val conditionStart = skipIgnorable(text, index + 2, codeMask)
            if (conditionStart == -1 || text[conditionStart] != '(') {
                holder.newAnnotation(HighlightSeverity.ERROR, "Expected '(' after 'if'")
                    .range(TextRange(index, index + 2))
                    .create()
                continue
            }

            val conditionEnd = findMatchingParen(text, conditionStart, codeMask)
            if (conditionEnd == -1) {
                continue
            }

            val blockStart = skipIgnorable(text, conditionEnd + 1, codeMask)
            if (blockStart == -1 || text[blockStart] != '{') {
                holder.newAnnotation(HighlightSeverity.ERROR, "Expected '{' after if condition")
                    .range(TextRange(index, index + 2))
                    .create()
            }
        }
    }

    private fun buildCodeMask(text: String): BooleanArray {
        val mask = BooleanArray(text.length)
        var index = 0
        var inLineComment = false
        var inBlockComment = false
        var inSingleQuoted = false
        var inDoubleQuoted = false
        var escaped = false

        while (index < text.length) {
            val current = text[index]

            when {
                inLineComment -> {
                    if (current == '\n') {
                        inLineComment = false
                    }
                }
                inBlockComment -> {
                    if (current == '*' && index + 1 < text.length && text[index + 1] == '/') {
                        index += 1
                        inBlockComment = false
                    }
                }
                inSingleQuoted -> {
                    if (!escaped && current == '\'') {
                        inSingleQuoted = false
                    }
                    escaped = !escaped && current == '\\'
                }
                inDoubleQuoted -> {
                    if (!escaped && current == '"') {
                        inDoubleQuoted = false
                    }
                    escaped = !escaped && current == '\\'
                }
                current == '/' && index + 1 < text.length && text[index + 1] == '/' -> {
                    inLineComment = true
                    index += 1
                }
                current == '/' && index + 1 < text.length && text[index + 1] == '*' -> {
                    inBlockComment = true
                    index += 1
                }
                current == '\'' -> {
                    inSingleQuoted = true
                    escaped = false
                }
                current == '"' -> {
                    inDoubleQuoted = true
                    escaped = false
                }
                else -> mask[index] = true
            }

            index += 1
        }

        return mask
    }

    private fun skipIgnorable(text: String, start: Int, codeMask: BooleanArray): Int {
        var index = start
        while (index < text.length) {
            if (codeMask[index] && !text[index].isWhitespace()) {
                return index
            }
            index += 1
        }
        return -1
    }

    private fun findMatchingParen(text: String, openIndex: Int, codeMask: BooleanArray): Int {
        var depth = 1
        for (index in openIndex + 1 until text.length) {
            if (!codeMask[index]) {
                continue
            }

            when (text[index]) {
                '(' -> depth += 1
                ')' -> {
                    depth -= 1
                    if (depth == 0) {
                        return index
                    }
                }
            }
        }
        return -1
    }

    private fun isKeywordAt(text: String, start: Int, keyword: String, codeMask: BooleanArray): Boolean {
        if (start < 0 || start + keyword.length > text.length) {
            return false
        }

        for (index in start until start + keyword.length) {
            if (!codeMask[index] || text[index] != keyword[index - start]) {
                return false
            }
        }

        val beforeOk = start == 0 || !isWordChar(text[start - 1])
        val end = start + keyword.length
        val afterOk = end >= text.length || !isWordChar(text[end])
        return beforeOk && afterOk
    }

    private fun isWordChar(char: Char): Boolean = char.isLetterOrDigit() || char == '_'
}
