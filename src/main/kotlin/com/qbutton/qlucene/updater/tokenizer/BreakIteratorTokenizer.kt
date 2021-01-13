package com.qbutton.qlucene.updater.tokenizer

import com.qbutton.qlucene.dto.Sentence
import com.qbutton.qlucene.dto.Term
import com.qbutton.qlucene.dto.Word
import org.springframework.stereotype.Component
import java.text.BreakIterator

/**
A tokenizer which will break text based on Java's {@code BreakIterator}.
 */
abstract class BreakIteratorTokenizer : Tokenizer() {

    override fun tokenize(rawText: String): Map<Term, Int> {
        val iterator = getBreakIterator()
        iterator.setText(rawText)
        val words = mutableMapOf<Term, Int>()
        var start = iterator.first()

        var end = iterator.next()
        while (end != BreakIterator.DONE) {
            val element = rawText.substring(start, end).trim()
            if (element.isNotBlank() && Character.isLetter(element.codePointAt(0))) {
                words.merge(toTerm(element), 1) { oldValue, _ -> oldValue + 1 }
            }
            start = end
            end = iterator.next()
        }
        return words
    }

    abstract fun getBreakIterator(): BreakIterator

    abstract fun toTerm(rawToken: String): Term
}

@Component
class SentenceTokenizer : BreakIteratorTokenizer() {
    // splits into sentences. See tests for examples
    override fun getBreakIterator() = BreakIterator.getSentenceInstance()!!

    override fun toTerm(rawToken: String) = Sentence(rawToken)

    override fun getProducedTermClass() = Sentence::class
}

@Component
class WordTokenizer : BreakIteratorTokenizer() {
    // splits into words according to BreakIterator's understanding of word is, see tests for more
    override fun getBreakIterator() = BreakIterator.getWordInstance()!!

    override fun toTerm(rawToken: String) = Word(rawToken)

    override fun getProducedTermClass() = Word::class
}
