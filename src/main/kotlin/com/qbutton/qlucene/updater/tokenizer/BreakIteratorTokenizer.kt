package com.qbutton.qlucene.updater.tokenizer

import java.text.BreakIterator

/**
A tokenizer which will break text based on Java's {@code BreakIterator}.
 */
abstract class BreakIteratorTokenizer : Tokenizer() {

    override fun tokenize(rawText: String): List<String> {
        val iterator = getBreakIterator()
        iterator.setText(rawText)
        val words = mutableListOf<String>()
        var start = iterator.first()

        var end = iterator.next()
        while (end != BreakIterator.DONE) {
            val element = rawText.substring(start, end).trim()
            if (element.isNotBlank() && Character.isLetter(element.codePointAt(0))) {
                words.add(element)
            }
            start = end
            end = iterator.next()
        }
        return words
    }

    abstract fun getBreakIterator(): BreakIterator
}
