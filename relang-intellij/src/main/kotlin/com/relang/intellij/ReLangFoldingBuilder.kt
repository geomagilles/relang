package com.relang.intellij

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement

class ReLangFoldingBuilder : FoldingBuilderEx() {
    companion object {
        private val LOG = Logger.getInstance(ReLangFoldingBuilder::class.java)
    }

    override fun buildFoldRegions(
        root: PsiElement,
        document: Document,
        quick: Boolean,
    ): Array<FoldingDescriptor> {
        val fileName = root.containingFile.name
        if (!fileName.endsWith(".re")) {
            return emptyArray()
        }

        val text = document.charsSequence
        val stack = ArrayDeque<Int>()
        val descriptors = mutableListOf<FoldingDescriptor>()

        for (index in text.indices) {
            when (text[index]) {
                '{' -> stack.addLast(index)
                '}' -> {
                    if (stack.isEmpty()) {
                        continue
                    }
                    val start = stack.removeLast()
                    if (spansMultipleLines(document, start, index)) {
                        descriptors.add(FoldingDescriptor(root.node, TextRange(start, index + 1)))
                    }
                }
            }
        }

        LOG.info("ReLang folding regions for $fileName: ${descriptors.size}")
        return descriptors.toTypedArray()
    }

    override fun getPlaceholderText(node: ASTNode): String = "{...}"

    override fun isCollapsedByDefault(node: ASTNode): Boolean = false

    private fun spansMultipleLines(document: Document, startOffset: Int, endOffset: Int): Boolean {
        val startLine = document.getLineNumber(startOffset)
        val endLine = document.getLineNumber(endOffset)
        return endLine > startLine
    }
}
