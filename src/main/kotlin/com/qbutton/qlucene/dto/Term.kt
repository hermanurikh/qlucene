package com.qbutton.qlucene.dto

sealed class Term

data class Word(var word: String) : Term()