package com.qbutton.qlucene.updater.tokenizer

import com.qbutton.qlucene.dto.Sentence
import com.qbutton.qlucene.dto.Term
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

internal class SentenceTokenizerTest {

    private val sentenceTokenizer = SentenceTokenizer()

    @ParameterizedTest
    @MethodSource("getStringsForTokenization")
    fun tokenize(text: String, expected: Map<Term, Int>) {
        assertEquals(expected, sentenceTokenizer.tokenize(text))
    }

    companion object {
        @JvmStatic
        fun getStringsForTokenization(): List<Arguments> {
            return listOf(
                Arguments.of(
                    "Simple sentence 1. Simple sentence 2... Simple sentence 3! Simple sentence 4, and 5?",
                    listOf("Simple sentence 1.", "Simple sentence 2...", "Simple sentence 3!", "Simple sentence 4, and 5?").toSentenceMap()
                ),
                Arguments.of("hello world", listOf("hello world").toSentenceMap()),
                Arguments.of(
                    "привет! как дела? как-нибудь встретимся?",
                    listOf("привет!", "как дела?", "как-нибудь встретимся?").toSentenceMap()
                ),
                Arguments.of(
                    "This is a test. This is a T.L.A. test. Now with a Dr. in it.",
                    listOf("This is a test.", "This is a T.L.A. test.", "Now with a Dr. in it.").toSentenceMap()

                )
            )
        }

        private fun <T> List<T>.toSentenceMap() = this
            .map { Sentence(it as String) }
            .groupingBy { it }
            .eachCount()
            .toMap()
    }
}
