package com.qbutton.qlucene.updater.tokenizer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

internal class WordTokenizerTest {

    private val wordTokenizer = WordTokenizer()

    @ParameterizedTest
    @MethodSource("getStringsForTokenization")
    fun tokenize(text: String, expected: List<String>) {
        assertEquals(expected, wordTokenizer.tokenize(text))
    }

    companion object {
        @JvmStatic
        fun getStringsForTokenization(): List<Arguments> {
            return listOf(
                Arguments.of("hello world", listOf("hello", "world")),
                Arguments.of("hi, there,hi Leo", listOf("hi", "there", "hi", "Leo")),
                Arguments.of("that's a no?", listOf("that's", "a", "no")),
                Arguments.of("dog, cat, tarantula; tortoise.", listOf("dog", "cat", "tarantula", "tortoise")),
                Arguments.of(
                    "Salut mon homme, comment ça va aujourd'hui? Ce sera Noël puis Pâques bientôt.",
                    listOf(
                        "Salut", "mon", "homme", "comment", "ça", "va", "aujourd'hui", "Ce",
                        "sera", "Noël", "puis", "Pâques", "bientôt"
                    )
                ),
                // - should be not split upon
                Arguments.of(
                    "привет! как дела? как-нибудь встретимся?",
                    listOf("привет", "как", "дела", "как-нибудь", "встретимся")
                )
                // more tests could be clarified with stakeholders - regarding what is a word
            )
        }
    }
}
