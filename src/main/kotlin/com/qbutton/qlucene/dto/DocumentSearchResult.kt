package com.qbutton.qlucene.dto

// TODO do we want to store offsets in the document?
data class DocumentSearchResult(val fileId: String, val termOccurrences: Int)
