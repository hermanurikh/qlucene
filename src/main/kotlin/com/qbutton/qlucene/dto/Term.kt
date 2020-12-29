package com.qbutton.qlucene.dto

sealed class Term

data class Word(var word: String) : Term()

data class Sentence(var sentence: String) : Term()